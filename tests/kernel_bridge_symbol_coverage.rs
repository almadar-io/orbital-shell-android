//! Gate: `OrbitalKernelBridge.kt`'s `external fun` declarations cover every
//! `Java_generated_patterns_core_OrbitalKernelBridge_*` symbol `orbital-ffi::jni_kernel_bridge`
//! defines (read from the crate constant, never a hand-typed twin — mirrors
//! `bridge_routes_game_canvas.rs`'s `jni_bridge_declares_canonical_symbols`
//! for the game bridge), and the backend actually emits the file into the
//! generated Gradle project.

use orbital_core_base::generate::Backend;
use orbital_core_base::ir::OirModule;
use orbital_shell_android::AndroidBackend;

const KERNEL_BRIDGE_KT: &str = include_str!("../renderers/compose/renderers/bridge/OrbitalKernelBridge.kt");

#[test]
fn kernel_bridge_declares_every_jni_symbol() {
    for symbol in orbital_ffi::jni_kernel_bridge::KERNEL_JNI_SYMBOLS {
        let suffix = symbol
            .strip_prefix("Java_generated_patterns_core_OrbitalKernelBridge_")
            .unwrap_or_else(|| panic!("unexpected symbol shape: {:?}", symbol));
        let needle = format!("external fun {suffix}");
        assert!(
            KERNEL_BRIDGE_KT.contains(&needle),
            "OrbitalKernelBridge.kt missing `{}`",
            needle
        );
    }
    assert!(
        KERNEL_BRIDGE_KT.contains("System.loadLibrary(\"orbital_ffi\")"),
        "OrbitalKernelBridge.kt must load the native library"
    );
}

#[test]
fn backend_emits_kernel_bridge_file() {
    let backend = AndroidBackend::new();
    let module = OirModule::new("TestGame", "1.0.0");
    let output = backend.generate(&module);

    let paths: Vec<&str> = output.files.iter().map(|f| f.path.as_str()).collect();
    assert!(
        paths.contains(&"patterns/core/bridge/OrbitalKernelBridge.kt"),
        "backend does not emit OrbitalKernelBridge.kt; got: {:?}",
        paths
    );

    let bridge = output
        .files
        .iter()
        .find(|f| f.path == "patterns/core/bridge/OrbitalKernelBridge.kt")
        .unwrap();
    assert!(bridge.content.contains("external fun nativeInit"));
    assert!(bridge.content.contains("class OrbitalKernel"));
}
