//! Layout rule: `src/` is codegen only. The native bridge sources ship in the compose
//! renderer pack, and a generated app takes them from the pack.

use std::path::{Path, PathBuf};

use orbital_core::generate::Backend;
use orbital_core::ir::OirModule;
use orbital_shell_android::AndroidBackend;

const PLATFORM_EXTENSIONS: [&str; 4] = ["swift", "kt", "h", "modulemap"];
const BRIDGE_FILES: [&str; 5] = [
    "OrbitalAuth.kt",
    "OrbitalGameBridge.kt",
    "OrbitalGameCanvas.kt",
    "OrbitalKernelBridge.kt",
    "OrbitalSignIn.kt",
];

fn files_under(dir: &Path, out: &mut Vec<PathBuf>) {
    for entry in std::fs::read_dir(dir).unwrap() {
        let path = entry.unwrap().path();
        if path.is_dir() {
            files_under(&path, out);
        } else {
            out.push(path);
        }
    }
}

#[test]
fn no_platform_source_lives_under_src() {
    let mut files = Vec::new();
    files_under(&Path::new(env!("CARGO_MANIFEST_DIR")).join("src"), &mut files);
    let offenders: Vec<_> = files
        .iter()
        .filter(|p| {
            p.extension()
                .and_then(|e| e.to_str())
                .is_some_and(|e| PLATFORM_EXTENSIONS.contains(&e))
        })
        .collect();
    assert!(offenders.is_empty(), "platform sources under src/: {offenders:?}");
}

#[test]
fn the_pack_ships_the_bridge_sources() {
    let pack = orbital_shell_android::require_renderer_pack().unwrap();
    for file in BRIDGE_FILES {
        assert!(pack.source(&format!("bridge/{file}")).is_some(), "pack missing bridge/{file}");
    }
    assert!(pack.source("bridge/tests/OrbitalAuthTest.kt").is_some());
}

#[test]
fn a_generated_app_references_the_packs_bridge_files() {
    let output = AndroidBackend::new().generate(&OirModule::new("Layout", "1.0.0"));
    let pack = orbital_shell_android::require_renderer_pack().unwrap();
    for file in BRIDGE_FILES {
        let emitted = output
            .files
            .iter()
            .find(|f| f.path == format!("patterns/core/bridge/{file}"))
            .unwrap_or_else(|| panic!("app does not carry bridge/{file}"));
        assert_eq!(Some(emitted.content.as_str()), pack.source(&format!("bridge/{file}")));
    }
}

#[test]
fn the_bridge_tests_go_to_the_test_source_set_only() {
    let output = AndroidBackend::new().generate(&OirModule::new("Layout", "1.0.0"));
    assert!(output.files.iter().all(|f| !f.path.contains("bridge/tests/")));
    assert!(output
        .files
        .iter()
        .any(|f| f.path == "app/src/test/kotlin/generated/patterns/core/OrbitalAuthTest.kt"));
}
