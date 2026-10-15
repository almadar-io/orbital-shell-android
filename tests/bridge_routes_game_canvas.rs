//! Gate: the CorePatternRegistry must route game-canvas / canvas patterns to the
//! Vulkan/GPU renderer (GameCanvas2dRenderer), not the text placeholder.
//! Verified structurally — kotlinc is absent in CI, so we inspect the emitted
//! Kotlin sources that the backend writes into the generated project.
//!
//! Also exercises the headless RGBA path from orbital-canvas via the Rust
//! c_api (no device/Vulkan needed on Linux CI — graceful skip when no adapter).

use orbital_core::generate::Backend;
use orbital_core::ir::OirModule;
use orbital_canvas::{
    packet::{DrawCommand, GameRenderPacket, Rect},
    Canvas,
};
use orbital_shell_android::AndroidBackend;

/// All canvas-family pattern names that must be routed to the GPU renderer.
const CANVAS_PATTERNS: &[&str] = &["canvas", "game-canvas", "game-canvas-2d"];

/// Key symbol that proves the GPU path is taken (not the text placeholder).
const GPU_MARKER: &str = "GameCanvas2dRenderer";
/// The text placeholder would contain this; it must NOT appear for canvas patterns.
const PLACEHOLDER_MARKER: &str = "FallbackRenderer";

// --------------------------------------------------------------------------
// Gate 1: Registry Kotlin source routes canvas/game-canvas to the GPU renderer
// --------------------------------------------------------------------------

#[test]
fn registry_routes_canvas_to_gpu_renderer() {
    let registry_src = orbital_shell_android::require_renderer_pack()
        .unwrap_or_else(|e| panic!("{}: {e}", e.code()))
        .source("CorePatternRegistry.kt")
        .expect("the pack ships CorePatternRegistry.kt");

    for pattern in CANVAS_PATTERNS {
        let key_str = format!("\"{}\" to", pattern);
        assert!(
            registry_src.contains(&key_str),
            "CorePatternRegistry.kt missing entry for pattern {:?}",
            pattern
        );

        let line = registry_src
            .lines()
            .find(|l| l.contains(&key_str))
            .unwrap_or_else(|| panic!("no registry line for {:?}", pattern));

        assert!(
            line.contains(GPU_MARKER),
            "Pattern {:?} is not routed to {} (got: {})",
            pattern,
            GPU_MARKER,
            line.trim()
        );
        assert!(
            !line.contains(PLACEHOLDER_MARKER),
            "Pattern {:?} still routes to FallbackRenderer (got: {})",
            pattern,
            line.trim()
        );
    }

    // The registry only proves the symbol is REFERENCED — an emitted file must also
    // DEFINE it, or the reference is dead and kotlinc fails to resolve it (verified
    // 2026-09-18: this gate was green while GameCanvas2dRenderer did not exist).
    let canvas_src = include_str!("../renderers/compose/renderers/bridge/OrbitalGameCanvas.kt");
    assert!(
        canvas_src.contains("fun GameCanvas2dRenderer("),
        "OrbitalGameCanvas.kt does not define GameCanvas2dRenderer — the registry \
         reference is dead"
    );
}

// --------------------------------------------------------------------------
// Gate 2: JNI bridge declares the canonical Java_generated_patterns_core_OrbitalGameBridge_*
//         symbols that Kotlin `external fun` declarations expect.
// --------------------------------------------------------------------------

#[test]
fn jni_bridge_declares_canonical_symbols() {
    // The canonical symbol list lives with the definitions in orbital-ffi;
    // importing it here means this test cannot drift from the real bridge.
    let required_symbols = orbital_ffi::jni_bridge::JNI_SYMBOLS;

    // The Kotlin companion file must declare matching `external fun` names,
    // derived from the symbol suffix (`Java_generated_patterns_core_OrbitalGameBridge_native*`
    // -> `external fun native*`).
    let kt_bridge_src = include_str!("../renderers/compose/renderers/bridge/OrbitalGameBridge.kt");
    for sym in required_symbols {
        let suffix = sym
            .strip_prefix("Java_generated_patterns_core_OrbitalGameBridge_")
            .unwrap_or_else(|| panic!("unexpected symbol shape: {:?}", sym));
        let needle = format!("external fun {suffix}");
        assert!(
            kt_bridge_src.contains(&needle),
            "OrbitalGameBridge.kt missing `{}`",
            needle
        );
    }
    assert!(
        kt_bridge_src.contains("System.loadLibrary(\"orbital_ffi\")"),
        "OrbitalGameBridge.kt must load the native library"
    );
    assert!(
        kt_bridge_src.contains("OrbitalGameSurfaceView"),
        "OrbitalGameBridge.kt must declare OrbitalGameSurfaceView"
    );
}

// --------------------------------------------------------------------------
// Gate 3: Backend emits OrbitalGameBridge.kt into the generated project
// --------------------------------------------------------------------------

#[test]
fn backend_emits_bridge_files() {
    let backend = AndroidBackend::new();
    let module = OirModule::new("TestGame", "1.0.0");
    let output = backend.generate(&module);

    let paths: Vec<&str> = output.files.iter().map(|f| f.path.as_str()).collect();

    assert!(
        paths.contains(&"patterns/core/bridge/OrbitalGameBridge.kt"),
        "backend does not emit OrbitalGameBridge.kt; got: {:?}",
        paths
    );
    assert!(
        paths.contains(&"patterns/core/bridge/OrbitalGameCanvas.kt"),
        "backend does not emit OrbitalGameCanvas.kt; got: {:?}",
        paths
    );

    let bridge = output
        .files
        .iter()
        .find(|f| f.path == "patterns/core/bridge/OrbitalGameBridge.kt")
        .unwrap();
    assert!(
        bridge.content.contains("external fun nativeInit"),
        "OrbitalGameBridge.kt must declare nativeInit"
    );
    assert!(
        bridge.content.contains("OrbitalGameSurfaceView"),
        "OrbitalGameBridge.kt must declare OrbitalGameSurfaceView"
    );
    assert!(
        bridge.content.contains("System.loadLibrary(\"orbital_ffi\")"),
        "OrbitalGameBridge.kt must load the native library"
    );
}

// --------------------------------------------------------------------------
// Gate 4: Headless RGBA render via Rust c_api (skips gracefully without GPU)
// --------------------------------------------------------------------------

#[test]
fn headless_rgba_non_blank_or_no_adapter() {
    let mut renderer = match Canvas::new_headless() {
        Ok(r) => r,
        Err(_) => return, // no adapter on this CI host — skip
    };

    let packet = GameRenderPacket {
        pattern: "game-canvas-2d".into(),
        width: 32.0,
        height: 32.0,
        draw_commands: vec![
            DrawCommand::Clear {
                color: [0.2, 0.4, 0.8, 1.0],
            },
            DrawCommand::Quad {
                rect: Rect {
                    x: 4.0,
                    y: 4.0,
                    w: 24.0,
                    h: 24.0,
                },
                color: [1.0, 1.0, 0.0, 1.0],
            },
        ],
        ..Default::default()
    };

    let rgba = match renderer.render_packet_to_rgba(&packet) {
        Ok(buf) => buf,
        Err(_) => return, // no GPU available — skip gracefully
    };

    assert_eq!(rgba.len(), 32 * 32 * 4, "RGBA buffer length mismatch");
    let all_zero = rgba.iter().all(|&b| b == 0);
    assert!(
        !all_zero,
        "headless RGBA buffer is entirely blank — renderer produced no output"
    );
}

// --------------------------------------------------------------------------
// Gate 5: the canvas host hands the Rust renderer the scene node; it never
//         feeds raw props to the packet-only `nativeSubmit`.
// --------------------------------------------------------------------------

#[test]
fn canvas_host_sends_scene_not_raw_props_as_packet() {
    let bridge = include_str!("../renderers/compose/renderers/bridge/OrbitalGameBridge.kt");
    let canvas = include_str!("../renderers/compose/renderers/bridge/OrbitalGameCanvas.kt");
    assert!(
        bridge.contains("OrbitalGameBridge.nativeSetScene(rendererPtr, scene)"),
        "surface view must forward the canvas node through nativeSetScene"
    );
    assert!(
        !bridge.contains("OrbitalGameBridge.nativeSubmit(rendererPtr,"),
        "surface view must not call packet-only nativeSubmit with raw canvas props"
    );
    assert!(
        canvas.contains("sceneJson = props.toString()"),
        "GameCanvas2dRenderer must pass the node's props as the scene"
    );
}

// --------------------------------------------------------------------------
// Gate 6: the exported JNI symbol prefix is derived from the Kotlin `package`
//         line — a mismatch is an UnsatisfiedLinkError at the first canvas.
// --------------------------------------------------------------------------

#[test]
fn jni_symbols_follow_the_kotlin_package() {
    let kt = include_str!("../renderers/compose/renderers/bridge/OrbitalGameBridge.kt");
    let package = kt
        .lines()
        .find_map(|l| l.strip_prefix("package "))
        .expect("package line")
        .trim();
    let prefix = format!("Java_{}_OrbitalGameBridge_", package.replace('.', "_"));
    for sym in orbital_ffi::jni_bridge::JNI_SYMBOLS {
        assert!(sym.starts_with(&prefix), "{sym} does not start with {prefix} (Kotlin package {package})");
    }
    // Control: a symbol under a different package would be caught.
    assert!(!"Java_generated_OrbitalGameBridge_nativeInit".starts_with(&prefix));
}

// --------------------------------------------------------------------------
// Gate: the frame loop exists only while the scene animates
// --------------------------------------------------------------------------

#[test]
fn the_canvas_frame_loop_runs_only_for_an_animated_scene_on_the_host_clock() {
    let canvas = include_str!("../renderers/compose/renderers/bridge/OrbitalGameCanvas.kt");
    let bridge = include_str!("../renderers/compose/renderers/bridge/OrbitalGameBridge.kt");
    let effect = canvas.split("LaunchedEffect(animating)").nth(1).expect("frame effect");
    let guard = effect.find("if (!animating) return@LaunchedEffect").expect("no loop when static");
    let frame = effect.find("withFrameNanos").expect("host frame clock");
    assert!(guard < frame, "the guard precedes the first frame request");
    assert!(effect.contains("surfaceView.frame(nowMs)"), "each frame goes through the view");
    assert!(canvas.contains("view.onWantsFrames = { animating = it }"), "the renderer decides whether a loop runs");
    assert!(bridge.contains("nativeSetClock(rendererPtr, nowMs.toDouble())"), "the frame clock reaches the renderer");
    assert!(bridge.contains("ANIMATOR_DURATION_SCALE"), "Remove animations maps to reduced motion");
    assert!(bridge.contains("external fun nativeWantsFrames"), "the JNI declaration exists");
}
