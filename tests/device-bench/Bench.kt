// R1-device-android host: loads liborbital_ffi through the JNI forwarders over the kernel C ABI and times the
// parity workloads (`@almadar-io/kernel-bench-tool gen-device-workloads` writes them). Prints one JSON line.
// Runs under app_process (see run.sh); no APK. Usage: Bench <workload-dir> [--sustained]
import generated.patterns.core.OrbitalKernelBridge as Bridge
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

const val VIEWER = """{"id":"viewer-1","name":"Dev Viewer","email":"viewer@example.com","role":""}"""
const val FRAME_BUDGET_MS = 1000.0 / 60.0
const val SUSTAINED_SECONDS = 300.0

fun nowMs(): Double = System.nanoTime() / 1_000_000.0

fun percentile(values: List<Double>, p: Double): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val rank = Math.ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
    return sorted[rank - 1]
}

fun stats(samples: List<Double>): JSONObject = JSONObject()
    .put("samples", samples.size)
    .put("p50Ms", percentile(samples, 50.0))
    .put("p95Ms", percentile(samples, 95.0))
    .put("maxMs", samples.maxOrNull() ?: 0.0)

fun open(spec: JSONObject): Long {
    val handle = Bridge.nativeInit(spec.get("schema").toString(), "bench")
    check(handle != 0L) { "nativeInit returned 0" }
    Bridge.nativeSetCurrentUser(handle, VIEWER)
    return handle
}

fun runDispatch(id: String, spec: JSONObject): JSONObject {
    val handle = open(spec)
    try {
        val steps: JSONArray = spec.getJSONArray("steps")
        val events = spec.getInt("events")
        val latencies = ArrayList<Double>(events)
        var errors = 0
        for (i in 0 until events) {
            val step = steps.getJSONObject(i % steps.length())
            val payload = step.get("payload").toString()
            val start = nowMs()
            val result = Bridge.nativeDispatch(handle, step.getString("trait"), null, step.getString("event"), payload)
            latencies.add(nowMs() - start)
            if (result == null || result.startsWith("{\"error\"")) errors += 1
        }
        return JSONObject().put("id", id).put("kind", "dispatch").put("dispatchMs", stats(latencies)).put("errors", errors)
    } finally {
        Bridge.nativeDestroy(handle)
    }
}

fun runFrames(id: String, spec: JSONObject, sustained: Boolean): JSONObject {
    val handle = open(spec)
    try {
        val ticks = spec.getInt("ticks")
        val step = spec.getDouble("frameStepMs")
        val boot = spec.getJSONArray("bootTraits")
        for (i in 0 until boot.length()) Bridge.nativeDispatch(handle, boot.getString(i), null, "INIT", null)
        val declared = Bridge.nativeTickCount(handle)
        check(declared == ticks) { "$id: kernel declares $declared ticks, expected $ticks" }
        Bridge.nativeFrame(handle, 0L)
        val frames = if (sustained) Int.MAX_VALUE else spec.getInt("frames")
        val kernelMs = ArrayList<Double>()
        val firstMinute = ArrayList<Double>()
        val lastMinute = ArrayList<Double>()
        var fired = 0
        var dropped = 0
        val wallStart = nowMs()
        var i = 1
        while (i <= frames) {
            val start = nowMs()
            val result = Bridge.nativeFrame(handle, (i * step).toLong())
            val spent = nowMs() - start
            if (i == 1 && result != null) fired = JSONObject(result).optJSONArray("fired")?.length() ?: 0
            kernelMs.add(spent)
            if (spent > FRAME_BUDGET_MS) dropped += 1
            if (sustained) {
                val elapsedS = (nowMs() - wallStart) / 1000.0
                if (elapsedS < 60) firstMinute.add(spent)
                if (elapsedS >= SUSTAINED_SECONDS - 60) lastMinute.add(spent)
                if (elapsedS >= SUSTAINED_SECONDS) break
                val wait = wallStart + i * FRAME_BUDGET_MS - nowMs()
                if (wait > 0) Thread.sleep(wait.toLong(), ((wait % 1.0) * 1_000_000).toInt())
            }
            i += 1
        }
        val out = JSONObject().put("id", id).put("kind", "frame").put("ticks", ticks).put("firedOnFirstFrame", fired)
            .put("kernelFrameMs", stats(kernelMs)).put("droppedFrames", dropped).put("frameBudgetMs", FRAME_BUDGET_MS)
        if (sustained) out.put("firstMinuteP50Ms", percentile(firstMinute, 50.0)).put("lastMinuteP50Ms", percentile(lastMinute, 50.0))
        return out
    } finally {
        Bridge.nativeDestroy(handle)
    }
}

fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "usage: Bench <workload-dir> [--sustained]" }
    val sustained = args.contains("--sustained")
    val results = JSONArray()
    try {
        val files = File(args[0]).listFiles { f -> f.extension == "json" }!!.sortedBy { it.path }
        for (file in files) {
            val spec = JSONObject(file.readText())
            val id = spec.optString("id", file.name)
            when (spec.getString("kind")) {
                "dispatch" -> results.put(runDispatch(id, spec))
                "frame" -> {
                    results.put(runFrames(id, spec, false))
                    if (sustained && spec.getInt("ticks") == 1000) results.put(runFrames("sustained-5min", spec, true))
                }
                else -> error("$id: unknown kind")
            }
        }
        println(JSONObject().put("row", "R1-device-android").put("workloads", results).put("outstandingStrings", Bridge.nativeOutstandingStrings()))
    } catch (e: Exception) {
        println(JSONObject().put("row", "R1-device-android").put("error", e.message ?: e.toString()))
        System.exit(1)
    }
}
