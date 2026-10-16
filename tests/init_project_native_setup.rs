//! `AndroidShell::init_project` writes `local.properties` from the SDK env
//! vars and copies the prebuilt game FFI `.so` into jniLibs when
//! `ORBITAL_NATIVE_LIB_DIR` is set (W10). Exercises the public `Shell` API
//! end to end, the way `orb create --shell android` would call it.
//!
//! `ANDROID_HOME` / `ANDROID_SDK_ROOT` / `ORBITAL_NATIVE_LIB_DIR` are
//! process-global, so tests that touch them share `ENV_LOCK` to avoid
//! stomping each other across the harness's parallel test threads.

use orbital_core_base::shell::Shell;
use orbital_shell_android::AndroidShell;
use std::path::PathBuf;
use std::sync::Mutex;

static ENV_LOCK: Mutex<()> = Mutex::new(());

fn unique_temp_dir(label: &str) -> PathBuf {
    let nanos = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    std::env::temp_dir().join(format!(
        "orbital_android_init_project_test_{label}_{}_{nanos}",
        std::process::id()
    ))
}

#[test]
fn init_project_writes_local_properties_from_android_home() {
    let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
    let prev_home = std::env::var("ANDROID_HOME").ok();
    let prev_sdk = std::env::var("ANDROID_SDK_ROOT").ok();
    std::env::remove_var("ANDROID_SDK_ROOT");
    std::env::set_var("ANDROID_HOME", "/fake/sdk/from-android-home");

    let target = unique_temp_dir("local_properties");
    let result = AndroidShell::new().init_project(&target, "test-app");

    match prev_home {
        Some(v) => std::env::set_var("ANDROID_HOME", v),
        None => std::env::remove_var("ANDROID_HOME"),
    }
    match prev_sdk {
        Some(v) => std::env::set_var("ANDROID_SDK_ROOT", v),
        None => std::env::remove_var("ANDROID_SDK_ROOT"),
    }

    result.expect("init_project should succeed with ANDROID_HOME set");
    let contents = std::fs::read_to_string(target.join("local.properties"))
        .expect("local.properties should have been written");
    assert_eq!(contents, "sdk.dir=/fake/sdk/from-android-home\n");

    std::fs::remove_dir_all(&target).ok();
}

#[test]
fn init_project_copies_native_lib_when_dir_set() {
    let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
    let prev = std::env::var("ORBITAL_NATIVE_LIB_DIR").ok();

    let lib_dir = unique_temp_dir("native_lib_src");
    std::fs::create_dir_all(lib_dir.join("arm64-v8a")).unwrap();
    std::fs::write(
        lib_dir.join("arm64-v8a").join("liborbital_ffi.so"),
        b"fake-so-bytes",
    )
    .unwrap();
    std::env::set_var("ORBITAL_NATIVE_LIB_DIR", &lib_dir);

    let target = unique_temp_dir("native_lib_dst");
    let result = AndroidShell::new().init_project(&target, "test-app");

    match prev {
        Some(v) => std::env::set_var("ORBITAL_NATIVE_LIB_DIR", v),
        None => std::env::remove_var("ORBITAL_NATIVE_LIB_DIR"),
    }

    result.expect("init_project should succeed when the native lib is present");
    let copied = target.join("app/src/main/jniLibs/arm64-v8a/liborbital_ffi.so");
    assert!(copied.exists(), "expected {} to exist", copied.display());
    assert_eq!(std::fs::read(&copied).unwrap(), b"fake-so-bytes");

    std::fs::remove_dir_all(&lib_dir).ok();
    std::fs::remove_dir_all(&target).ok();
}

/// `init_project` (via `copy_template`) must carry the network security
/// config into every generated project, referenced from the manifest — the
/// dev kernel server (`10.0.2.2`/`localhost`, cleartext HTTP) is otherwise
/// silently unreachable on a real device/emulator (no crash, no log; see
/// `docs/Orbital_Shell_Android.md`). `validate_structure` must still pass
/// (it doesn't name this file, so this also guards it isn't required there).
#[test]
fn init_project_copies_network_security_config_and_manifest_references_it() {
    let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
    let prev_lib = std::env::var_os("ORBITAL_NATIVE_LIB_DIR");
    std::env::remove_var("ORBITAL_NATIVE_LIB_DIR");
    let target = unique_temp_dir("network_security_config");
    let shell = AndroidShell::new();
    let result = shell.init_project(&target, "test-app");
    if let Some(v) = prev_lib {
        std::env::set_var("ORBITAL_NATIVE_LIB_DIR", v);
    }
    result.expect("init_project should succeed with no SDK/native-lib env set");

    let config_path = target.join("app/src/main/res/xml/network_security_config.xml");
    let config = std::fs::read_to_string(&config_path)
        .unwrap_or_else(|e| panic!("expected {} to exist: {e}", config_path.display()));
    assert!(config.contains("cleartextTrafficPermitted=\"true\""));
    assert!(config.contains(">10.0.2.2<"));
    assert!(config.contains(">localhost<"));

    let manifest = std::fs::read_to_string(target.join("app/src/main/AndroidManifest.xml"))
        .expect("manifest should exist");
    assert!(manifest.contains(r#"android:networkSecurityConfig="@xml/network_security_config""#));

    shell
        .validate_structure(&target)
        .expect("validate_structure must still pass with the new file present");

    // Regression: a "--" inside an XML comment BODY (e.g. a `--headless` flag
    // name) fails Android's real resource compiler with a fatal XML parse
    // error that no structural/`cargo test` assertion catches — only
    // `./gradlew` does (which is exactly how this was first found).
    if let (Some(start), Some(end)) = (config.find("<!--"), config.find("-->")) {
        let body = &config[start + 4..end];
        assert!(
            !body.contains("--"),
            "network_security_config.xml comment body must not contain \"--\": {body:?}"
        );
    }

    std::fs::remove_dir_all(&target).ok();
}

#[test]
fn init_project_errors_clearly_when_native_lib_missing() {
    let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
    let prev = std::env::var("ORBITAL_NATIVE_LIB_DIR").ok();

    // Directory exists but has no arm64-v8a/liborbital_ffi.so inside.
    let lib_dir = unique_temp_dir("native_lib_missing_src");
    std::fs::create_dir_all(&lib_dir).unwrap();
    std::env::set_var("ORBITAL_NATIVE_LIB_DIR", &lib_dir);

    let target = unique_temp_dir("native_lib_missing_dst");
    let result = AndroidShell::new().init_project(&target, "test-app");

    match prev {
        Some(v) => std::env::set_var("ORBITAL_NATIVE_LIB_DIR", v),
        None => std::env::remove_var("ORBITAL_NATIVE_LIB_DIR"),
    }

    let err = result.expect_err("init_project must fail when the configured .so is missing");
    let msg = err.to_string();
    assert!(
        msg.contains("ORBITAL_NATIVE_LIB_DIR"),
        "error should name the env var: {msg}"
    );
    assert!(
        msg.contains("liborbital_ffi.so"),
        "error should name the expected file, never guess another path: {msg}"
    );

    std::fs::remove_dir_all(&lib_dir).ok();
    std::fs::remove_dir_all(&target).ok();
}
