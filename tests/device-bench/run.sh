#!/bin/bash
# R1-device-android: compile the bench host to a dex, push it with the workloads and the arm64 library, run it under app_process.
#   run.sh <workload-dir> [--sustained]
# Needs `make android-lib`, kotlinc, an Android SDK (d8, android.jar via ANDROID_HOME) and a booted emulator or device (adb).
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUST_ROOT="$(cd "$HERE/../../../.." && pwd)"
LIB_DIR="${ORBITAL_NATIVE_LIB_DIR:-$RUST_ROOT/target/jniLibs}/arm64-v8a"
OUT="${ORB_BENCH_OUT:-$RUST_ROOT/target/device-bench/android}"
WORK="${1:?usage: run.sh <workload-dir> [--sustained]}"
[ -f "$LIB_DIR/liborbital_ffi.so" ] || { echo "missing $LIB_DIR/liborbital_ffi.so: run 'make android-lib' in orbital-rust" >&2; exit 1; }
for tool in kotlinc adb; do command -v "$tool" >/dev/null || { echo "$tool not on PATH" >&2; exit 1; }; done
: "${ANDROID_HOME:?set ANDROID_HOME}"
D8="$(ls -d "$ANDROID_HOME"/build-tools/*/d8 | sort -V | tail -1)"
ANDROID_JAR="$(ls -d "$ANDROID_HOME"/platforms/android-*/android.jar | sort -V | tail -1)"
mkdir -p "$OUT"
# The library binds JNI by class name, so the host carries the one declaration it calls.
cat > "$OUT/OrbitalKernelBridge.kt" <<'K'
package generated.patterns.core

object OrbitalKernelBridge {
    init { System.loadLibrary("orbital_ffi") }
    external fun nativeInit(resolvedSchemaJson: String, clientId: String): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeDispatch(handle: Long, traitName: String, entityId: String?, event: String, payloadJson: String?): String?
    external fun nativeFrame(handle: Long, nowMs: Long): String?
    external fun nativeTickCount(handle: Long): Int
    external fun nativeSetCurrentUser(handle: Long, userJson: String)
    external fun nativeOutstandingStrings(): Long
}
K
kotlinc -cp "$ANDROID_JAR" "$HERE/Bench.kt" "$OUT/OrbitalKernelBridge.kt" -d "$OUT/bench.jar"
"$D8" --lib "$ANDROID_JAR" --output "$OUT" "$OUT/bench.jar" "$(dirname "$(command -v kotlinc)")/../lib/kotlin-stdlib.jar"
REMOTE=/data/local/tmp/orb-bench
adb shell "rm -rf $REMOTE && mkdir -p $REMOTE"
adb push "$OUT/classes.dex" "$LIB_DIR/liborbital_ffi.so" "$REMOTE/" >/dev/null
adb push "$WORK" "$REMOTE/workloads" >/dev/null
adb shell "cd $REMOTE && LD_LIBRARY_PATH=$REMOTE CLASSPATH=$REMOTE/classes.dex app_process / BenchKt $REMOTE/workloads ${*:2}"
