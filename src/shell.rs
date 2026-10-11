//! Android Shell implementation.

use orbital_core::shell::{Shell, ShellError};
use orbital_core::str_utils::{split_to_pascal_case, to_title_case};
use std::path::{Path, PathBuf};

/// Android shell for native Jetpack Compose projects.
pub struct AndroidShell {
    template_path: PathBuf,
}

impl AndroidShell {
    /// Create a new Android shell, locating the template at runtime.
    pub fn new() -> Self {
        let template_path = Self::find_template_path();
        Self { template_path }
    }

    /// Find the `android-shell` template directory at runtime.
    fn find_template_path() -> PathBuf {
        // The one template lookup (`ORBITAL_SHELLS_DIR` → beside the binary → the checkout).
        orbital_core::renderer_packs::shell_template("android-shell", std::path::Path::new(env!("CARGO_MANIFEST_DIR")))
    }

    /// Create with an explicit template path.
    pub fn with_template_path(template_path: impl Into<PathBuf>) -> Self {
        Self {
            template_path: template_path.into(),
        }
    }
}

impl Default for AndroidShell {
    fn default() -> Self {
        Self::new()
    }
}

impl Shell for AndroidShell {
    fn name(&self) -> &'static str {
        "android"
    }

    fn description(&self) -> &'static str {
        "Native Android application with Jetpack Compose"
    }

    fn extensions(&self) -> &[&'static str] {
        &["kt"]
    }

    fn template_path(&self) -> &Path {
        &self.template_path
    }

    fn generated_dir(&self) -> &str {
        "app/src/main/kotlin/generated"
    }

    fn build_command(&self) -> &str {
        "./gradlew assembleRelease"
    }

    fn dev_command(&self) -> &str {
        "./gradlew installDebug"
    }

    fn test_command(&self) -> &str {
        "./gradlew test"
    }

    fn validate_structure(&self, output_dir: &Path) -> Result<(), ShellError> {
        let required_files = [
            "settings.gradle.kts",
            "build.gradle.kts",
            "app/build.gradle.kts",
            "app/src/main/AndroidManifest.xml",
        ];
        for file in &required_files {
            let path = output_dir.join(file);
            if !path.exists() {
                return Err(ShellError::InvalidStructure {
                    path: path.clone(),
                    message: format!("Missing required file: {}", file),
                });
            }
        }
        Ok(())
    }

    fn copy_template(&self, output_dir: &Path) -> Result<(), ShellError> {
        if !self.template_path.exists() {
            return Err(ShellError::NotFound(
                self.template_path.display().to_string(),
            ));
        }
        copy_dir_recursive(&self.template_path, output_dir)?;

        #[cfg(unix)]
        {
            let gradlew = output_dir.join("gradlew");
            if gradlew.exists() {
                use std::os::unix::fs::PermissionsExt;
                let mut perms = std::fs::metadata(&gradlew)?.permissions();
                perms.set_mode(perms.mode() | 0o111);
                std::fs::set_permissions(&gradlew, perms)?;
            }
        }

        let project_name = output_dir
            .file_name()
            .map(|n| n.to_string_lossy().to_string())
            .unwrap_or_else(|| "orbital-app".to_string());
        let replacements = build_replacements(&project_name);
        replace_placeholders_in_dir(output_dir, &replacements)?;
        rename_placeholder_dirs(output_dir, &replacements)?;

        Ok(())
    }

    fn init_project(&self, target_dir: &Path, project_name: &str) -> Result<(), ShellError> {
        self.copy_template(target_dir)?;
        let gen_dir = target_dir.join(self.generated_dir());
        std::fs::create_dir_all(&gen_dir)?;
        write_local_properties(target_dir)?;
        copy_native_lib(target_dir)?;
        let _ = project_name;
        Ok(())
    }
}

/// Write `local.properties` with `sdk.dir` from `ANDROID_HOME`, falling back
/// to `ANDROID_SDK_ROOT`; skipped silently when neither is set.
fn write_local_properties(target_dir: &Path) -> Result<(), ShellError> {
    let sdk_dir = match std::env::var("ANDROID_HOME").or_else(|_| std::env::var("ANDROID_SDK_ROOT"))
    {
        Ok(dir) => dir,
        Err(_) => return Ok(()),
    };
    std::fs::write(
        target_dir.join("local.properties"),
        format!("sdk.dir={}\n", sdk_dir),
    )?;
    Ok(())
}

/// Copy the prebuilt `orbital-ffi` shared library (renderer + kernel C ABI)
/// into the project's jniLibs when `ORBITAL_NATIVE_LIB_DIR` is set; errors
/// clearly if the expected file is missing rather than guessing another
/// location.
fn copy_native_lib(target_dir: &Path) -> Result<(), ShellError> {
    let lib_dir = match std::env::var("ORBITAL_NATIVE_LIB_DIR") {
        Ok(dir) => dir,
        Err(_) => return Ok(()),
    };
    let src = Path::new(&lib_dir)
        .join("arm64-v8a")
        .join("liborbital_ffi.so");
    if !src.exists() {
        return Err(ShellError::NotFound(format!(
            "ORBITAL_NATIVE_LIB_DIR={} is set but {} does not exist",
            lib_dir,
            src.display()
        )));
    }
    let dst_dir = target_dir.join("app/src/main/jniLibs/arm64-v8a");
    std::fs::create_dir_all(&dst_dir)?;
    std::fs::copy(&src, dst_dir.join("liborbital_ffi.so"))?;
    Ok(())
}

fn copy_dir_recursive(src: &Path, dst: &Path) -> Result<(), ShellError> {
    if !dst.exists() {
        std::fs::create_dir_all(dst)?;
    }

    for entry in std::fs::read_dir(src)? {
        let entry = entry?;
        let src_path = entry.path();
        let dst_path = dst.join(entry.file_name());
        let file_name = entry.file_name();
        let file_name_str = file_name.to_string_lossy();

        if file_name_str == "generated"
            || file_name_str == "build"
            || file_name_str == ".gradle"
            || file_name_str == ".git"
            || file_name_str == "local.properties"
        {
            continue;
        }

        if src_path.is_dir() {
            copy_dir_recursive(&src_path, &dst_path)?;
        } else {
            std::fs::copy(&src_path, &dst_path)?;
        }
    }

    Ok(())
}

fn build_replacements(project_name: &str) -> std::collections::HashMap<&'static str, String> {
    let mut map = std::collections::HashMap::new();
    let package = to_package_name(project_name);
    let class_name = split_to_pascal_case(&package);
    map.insert("{{APP_NAME}}", package.clone());
    // Gradle's manifest merger rejects an applicationId without a dot.
    map.insert("{{APP_PACKAGE}}", format!("com.orbital.{package}"));
    map.insert("{{APP_CLASS_NAME}}", class_name);
    map.insert("{{APP_TITLE}}", to_title_case(project_name));
    map.insert("{{PROJECT_NAME}}", project_name.to_string());
    map
}

fn replace_placeholders_in_dir(
    dir: &Path,
    replacements: &std::collections::HashMap<&str, String>,
) -> Result<(), ShellError> {
    for entry in std::fs::read_dir(dir)? {
        let entry = entry?;
        let path = entry.path();
        if path.is_dir() {
            replace_placeholders_in_dir(&path, replacements)?;
            continue;
        }

        let ext = path.extension().and_then(|e| e.to_str()).unwrap_or("");
        let is_text = matches!(
            ext,
            "kt" | "kts" | "xml" | "properties" | "md" | "txt" | "gradle"
        );
        if !is_text {
            continue;
        }

        let content = std::fs::read_to_string(&path)?;
        let mut updated = content;
        for (placeholder, value) in replacements {
            updated = updated.replace(placeholder, value);
        }
        std::fs::write(&path, updated)?;
    }
    Ok(())
}

fn to_package_name(s: &str) -> String {
    s.to_lowercase()
        .replace(['-', ' '], "_")
        .replace(|c: char| !c.is_alphanumeric() && c != '_', "")
}

fn rename_placeholder_dirs(
    dir: &Path,
    replacements: &std::collections::HashMap<&str, String>,
) -> Result<(), ShellError> {
    // Rename deepest directories first so parent paths remain valid.
    let mut dirs: Vec<PathBuf> = vec![];
    collect_dirs(dir, &mut dirs)?;
    dirs.sort_by_key(|p| p.components().count());
    dirs.reverse();

    for path in dirs {
        if let Some(name) = path.file_name().and_then(|n| n.to_str()) {
            let mut new_name = name.to_string();
            for (placeholder, value) in replacements {
                new_name = new_name.replace(placeholder, value);
            }
            if new_name != name {
                let new_path = path.with_file_name(new_name.replace('.', "/"));
                if let Some(parent) = new_path.parent() {
                    std::fs::create_dir_all(parent)?;
                }
                std::fs::rename(&path, &new_path)?;
            }
        }
    }
    Ok(())
}

fn collect_dirs(dir: &Path, out: &mut Vec<PathBuf>) -> Result<(), ShellError> {
    for entry in std::fs::read_dir(dir)? {
        let entry = entry?;
        let path = entry.path();
        if path.is_dir() {
            out.push(path.clone());
            collect_dirs(&path, out)?;
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_shell_properties() {
        let shell = AndroidShell::new();
        assert_eq!(shell.name(), "android");
        assert_eq!(shell.extensions(), &["kt"]);
        assert_eq!(shell.generated_dir(), "app/src/main/kotlin/generated");
    }

    #[test]
    fn app_package_is_reverse_dns_and_placeholder_dir_nests() {
        let map = build_replacements("Native Smoke");
        assert_eq!(map["{{APP_PACKAGE}}"], "com.orbital.native_smoke");
        let root = std::env::temp_dir().join(format!(
            "orbital-android-pkg-{}-{}",
            std::process::id(),
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        let theme = root.join("kotlin/{{APP_PACKAGE}}/ui/theme");
        std::fs::create_dir_all(&theme).unwrap();
        std::fs::write(theme.join("Theme.kt"), "package {{APP_PACKAGE}}.ui.theme\n").unwrap();
        let replacements: std::collections::HashMap<&str, String> =
            map.iter().map(|(k, v)| (*k, v.clone())).collect();
        rename_placeholder_dirs(&root, &replacements).unwrap();
        assert!(root
            .join("kotlin/com/orbital/native_smoke/ui/theme/Theme.kt")
            .is_file());
        assert!(!root.join("kotlin/{{APP_PACKAGE}}").exists());
        std::fs::remove_dir_all(&root).unwrap();
    }

    #[test]
    fn test_to_package_name() {
        assert_eq!(to_package_name("My-App"), "my_app");
        assert_eq!(to_package_name("hello world"), "hello_world");
    }

    // `ANDROID_HOME` / `ANDROID_SDK_ROOT` / `ORBITAL_NATIVE_LIB_DIR` are
    // process-global; this lock serializes the tests below so they don't
    // stomp each other when the test harness runs them on parallel threads.
    static ENV_LOCK: std::sync::Mutex<()> = std::sync::Mutex::new(());

    fn unique_temp_dir(label: &str) -> PathBuf {
        let nanos = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        std::env::temp_dir().join(format!(
            "orbital_android_shell_test_{label}_{}_{nanos}",
            std::process::id()
        ))
    }

    #[test]
    fn local_properties_written_from_android_home() {
        let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
        let prev_home = std::env::var("ANDROID_HOME").ok();
        let prev_sdk = std::env::var("ANDROID_SDK_ROOT").ok();
        std::env::remove_var("ANDROID_SDK_ROOT");
        std::env::set_var("ANDROID_HOME", "/fake/sdk/path");

        let target = unique_temp_dir("local_properties");
        std::fs::create_dir_all(&target).unwrap();
        let result = write_local_properties(&target);

        match prev_home {
            Some(v) => std::env::set_var("ANDROID_HOME", v),
            None => std::env::remove_var("ANDROID_HOME"),
        }
        match prev_sdk {
            Some(v) => std::env::set_var("ANDROID_SDK_ROOT", v),
            None => std::env::remove_var("ANDROID_SDK_ROOT"),
        }

        result.expect("write_local_properties should succeed when ANDROID_HOME is set");
        let contents = std::fs::read_to_string(target.join("local.properties")).unwrap();
        assert_eq!(contents, "sdk.dir=/fake/sdk/path\n");
        std::fs::remove_dir_all(&target).ok();
    }

    #[test]
    fn local_properties_skipped_when_no_sdk_env() {
        let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
        let prev_home = std::env::var("ANDROID_HOME").ok();
        let prev_sdk = std::env::var("ANDROID_SDK_ROOT").ok();
        std::env::remove_var("ANDROID_HOME");
        std::env::remove_var("ANDROID_SDK_ROOT");

        let target = unique_temp_dir("local_properties_skip");
        std::fs::create_dir_all(&target).unwrap();
        let result = write_local_properties(&target);

        if let Some(v) = prev_home {
            std::env::set_var("ANDROID_HOME", v);
        }
        if let Some(v) = prev_sdk {
            std::env::set_var("ANDROID_SDK_ROOT", v);
        }

        result.expect("write_local_properties should not error when neither var is set");
        assert!(!target.join("local.properties").exists());
        std::fs::remove_dir_all(&target).ok();
    }

    #[test]
    fn native_lib_copied_when_dir_set() {
        let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
        let prev = std::env::var("ORBITAL_NATIVE_LIB_DIR").ok();

        let lib_dir = unique_temp_dir("native_lib_src");
        std::fs::create_dir_all(lib_dir.join("arm64-v8a")).unwrap();
        std::fs::write(
            lib_dir.join("arm64-v8a").join("liborbital_ffi.so"),
            b"fake-so",
        )
        .unwrap();
        std::env::set_var("ORBITAL_NATIVE_LIB_DIR", &lib_dir);

        let target = unique_temp_dir("native_lib_dst");
        std::fs::create_dir_all(&target).unwrap();
        let result = copy_native_lib(&target);

        match prev {
            Some(v) => std::env::set_var("ORBITAL_NATIVE_LIB_DIR", v),
            None => std::env::remove_var("ORBITAL_NATIVE_LIB_DIR"),
        }

        result.expect("copy_native_lib should succeed when the .so exists");
        let copied = target.join("app/src/main/jniLibs/arm64-v8a/liborbital_ffi.so");
        assert!(copied.exists());
        assert_eq!(std::fs::read(&copied).unwrap(), b"fake-so");
        std::fs::remove_dir_all(&lib_dir).ok();
        std::fs::remove_dir_all(&target).ok();
    }

    #[test]
    fn native_lib_errors_clearly_when_so_missing() {
        let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
        let prev = std::env::var("ORBITAL_NATIVE_LIB_DIR").ok();

        let lib_dir = unique_temp_dir("native_lib_missing_src");
        std::fs::create_dir_all(&lib_dir).unwrap(); // no arm64-v8a/*.so inside
        std::env::set_var("ORBITAL_NATIVE_LIB_DIR", &lib_dir);

        let target = unique_temp_dir("native_lib_missing_dst");
        std::fs::create_dir_all(&target).unwrap();
        let result = copy_native_lib(&target);

        match prev {
            Some(v) => std::env::set_var("ORBITAL_NATIVE_LIB_DIR", v),
            None => std::env::remove_var("ORBITAL_NATIVE_LIB_DIR"),
        }

        let err = result.expect_err("copy_native_lib must error when the .so is missing");
        let msg = err.to_string();
        assert!(
            msg.contains("ORBITAL_NATIVE_LIB_DIR"),
            "error names the env var: {msg}"
        );
        assert!(
            msg.contains("liborbital_ffi.so"),
            "error names the expected file: {msg}"
        );
        assert!(
            !target.join("app/src/main/jniLibs").exists(),
            "must not create jniLibs on error"
        );
        std::fs::remove_dir_all(&lib_dir).ok();
        std::fs::remove_dir_all(&target).ok();
    }

    #[test]
    fn native_lib_skipped_when_env_unset() {
        let _guard = ENV_LOCK.lock().unwrap_or_else(|e| e.into_inner());
        let prev = std::env::var("ORBITAL_NATIVE_LIB_DIR").ok();
        std::env::remove_var("ORBITAL_NATIVE_LIB_DIR");

        let target = unique_temp_dir("native_lib_env_unset");
        std::fs::create_dir_all(&target).unwrap();
        let result = copy_native_lib(&target);

        if let Some(v) = prev {
            std::env::set_var("ORBITAL_NATIVE_LIB_DIR", v);
        }

        result.expect("copy_native_lib should no-op when ORBITAL_NATIVE_LIB_DIR is unset");
        assert!(!target.join("app").exists());
        std::fs::remove_dir_all(&target).ok();
    }

    fn template_root() -> std::path::PathBuf {
        std::path::Path::new(env!("CARGO_MANIFEST_DIR")).join("templates/android-shell")
    }

    #[test]
    fn template_ships_no_notes_specific_android_test() {
        assert!(!template_root().join("app/src/androidTest").exists());
        let gradle = std::fs::read_to_string(template_root().join("app/build.gradle.kts")).unwrap();
        assert!(!gradle.contains("NewNoteFlowTest"));
    }

    #[test]
    fn template_loads_network_images_with_coil_two_pinned_to_one_stable_version() {
        let gradle = std::fs::read_to_string(template_root().join("app/build.gradle.kts")).unwrap();
        assert!(gradle.contains("implementation(\"io.coil-kt:coil-compose:2.6.0\")"));
        assert!(
            !gradle.contains("io.coil-kt.coil3"),
            "Coil 3 needs Kotlin 2; the template is on 1.9"
        );
        assert!(
            gradle.contains("implementation(\"io.coil-kt:coil-svg:2.6.0\")"),
            "SVG decoder at the coil-compose version"
        );
        assert_eq!(
            gradle.matches("io.coil-kt:").count(),
            2,
            "one image loader (coil-compose) plus its SVG decoder, no second loader"
        );
    }

}
