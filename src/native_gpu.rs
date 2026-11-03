use orbital_core_base::generate::{Backend, GeneratedFile, GeneratedOutput};
use orbital_core_base::ir::OirModule;
use orbital_core_base::schema::OrbitalSchema;
use orbital_core_base::shell::{Shell, ShellError};
use serde::Deserialize;
use sha2::{Digest, Sha256};
use std::io::Read;
use std::path::{Path, PathBuf};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct NativeGpuContract {
    contract_version: u32,
    abi: String,
    library: String,
    min_api: u32,
    production_feature_set: String,
    research_feature_set: String,
}

fn native_gpu_contract() -> NativeGpuContract {
    serde_json::from_str(include_str!("../renderers/native/contract.json"))
        .expect("the bundled native GPU contract must be valid")
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct NativeGpuManifest {
    contract_version: u32,
    abi: String,
    shell_version: String,
    feature_set: String,
    sha256: String,
}

pub struct NativeGpuShell {
    template: PathBuf,
    bundle: PathBuf,
    research: bool,
}

impl NativeGpuShell {
    pub fn new() -> Result<Self, ShellError> {
        let research = match std::env::var("ORBITAL_NATIVE_GPU_RESEARCH") {
            Ok(value) if value == "1" => true,
            Ok(_) => {
                return Err(ShellError::Template(
                    "ORBITAL_NATIVE_GPU_RESEARCH must be 1 when set".into(),
                ))
            }
            Err(std::env::VarError::NotPresent) => false,
            Err(error) => return Err(ShellError::Template(error.to_string())),
        };
        if let Some(bundle) = std::env::var_os("ORBITAL_NATIVE_GPU_LIB_DIR") {
            return Ok(Self {
                template: Path::new(env!("CARGO_MANIFEST_DIR")).join("renderers/native/template"),
                bundle: PathBuf::from(bundle),
                research,
            });
        }
        let executable = std::env::current_exe()?;
        let package = executable.parent().and_then(Path::parent).ok_or_else(|| {
            ShellError::NotFound(format!(
                "native GPU shell package beside {}",
                executable.display()
            ))
        })?;
        let mut shell = Self::from_package_root(package);
        shell.research = research;
        Ok(shell)
    }

    fn from_package_root(package: &Path) -> Self {
        Self {
            template: package.join("templates/android-native-gpu"),
            bundle: package.join("native-gpu/android"),
            research: false,
        }
    }

    fn verified_library(&self) -> Result<PathBuf, ShellError> {
        let contract = native_gpu_contract();
        let feature_set = if self.research {
            &contract.research_feature_set
        } else {
            &contract.production_feature_set
        };
        let manifest_path = self.bundle.join("manifest.json");
        let manifest: NativeGpuManifest =
            serde_json::from_slice(&std::fs::read(&manifest_path)?)
                .map_err(|e| ShellError::Template(format!("{}: {e}", manifest_path.display())))?;
        if manifest.contract_version != contract.contract_version
            || manifest.abi != contract.abi
            || manifest.shell_version != env!("CARGO_PKG_VERSION")
            || manifest.feature_set != feature_set.as_str()
        {
            return Err(ShellError::Template(format!(
                "{}: expected native GPU contract {}, ABI {}, shell {}, feature set {}",
                manifest_path.display(),
                contract.contract_version,
                contract.abi,
                env!("CARGO_PKG_VERSION"),
                feature_set
            )));
        }
        let library = self.bundle.join(&contract.abi).join(&contract.library);
        let mut file = std::fs::File::open(&library)?;
        let mut elf = [0u8; 20];
        file.read_exact(&mut elf)?;
        if &elf[..6] != b"\x7fELF\x02\x01" || u16::from_le_bytes([elf[18], elf[19]]) != 183 {
            return Err(ShellError::Template(format!(
                "{}: expected an Android arm64 ELF library",
                library.display()
            )));
        }
        let mut digest = Sha256::new();
        digest.update(elf);
        let mut chunk = [0u8; 65536];
        loop {
            let size = file.read(&mut chunk)?;
            if size == 0 {
                break;
            }
            digest.update(&chunk[..size]);
        }
        if format!("{:x}", digest.finalize()) != manifest.sha256 {
            return Err(ShellError::Template(format!(
                "{}: SHA-256 does not match manifest",
                library.display()
            )));
        }
        Ok(library)
    }
}

impl Shell for NativeGpuShell {
    fn name(&self) -> &'static str {
        "android-native-gpu"
    }
    fn description(&self) -> &'static str {
        "Android GameActivity with the shared Dioxus native GPU renderer"
    }
    fn extensions(&self) -> &[&'static str] {
        &["java"]
    }
    fn template_path(&self) -> &Path {
        &self.template
    }
    fn generated_dir(&self) -> &str {
        "app/src/main/assets"
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

    fn validate_structure(&self, output: &Path) -> Result<(), ShellError> {
        for file in [
            "settings.gradle.kts",
            "app/build.gradle.kts",
            "app/src/main/AndroidManifest.xml",
        ] {
            if !output.join(file).is_file() {
                return Err(ShellError::InvalidStructure {
                    path: output.join(file),
                    message: format!("Missing required file: {file}"),
                });
            }
        }
        Ok(())
    }

    fn copy_template(&self, output: &Path) -> Result<(), ShellError> {
        if !self.template.is_dir() {
            return Err(ShellError::NotFound(self.template.display().to_string()));
        }
        crate::shell::copy_dir_recursive(&self.template, output)?;
        let shared = orbital_core_base::renderer_packs::shell_template(
            "android-shell",
            Path::new(env!("CARGO_MANIFEST_DIR")),
        );
        std::fs::copy(shared.join("gradlew"), output.join("gradlew"))?;
        crate::shell::copy_dir_recursive(
            &shared.join("gradle/wrapper"),
            &output.join("gradle/wrapper"),
        )?;
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            let file = output.join("gradlew");
            let mut permissions = std::fs::metadata(&file)?.permissions();
            permissions.set_mode(permissions.mode() | 0o111);
            std::fs::set_permissions(file, permissions)?;
        }
        Ok(())
    }

    fn init_project(&self, output: &Path, project_name: &str) -> Result<(), ShellError> {
        let library = self.verified_library()?;
        let contract = native_gpu_contract();
        self.copy_template(output)?;
        let package = format!("io.almadar.{}", crate::shell::to_package_name(project_name));
        let replacements = std::collections::HashMap::from([
            ("{{APP_PACKAGE}}", package),
            ("{{APP_TITLE}}", project_name.to_string()),
            ("{{NATIVE_GPU_MIN_API}}", contract.min_api.to_string()),
        ]);
        crate::shell::replace_placeholders_in_dir(output, &replacements)?;
        crate::shell::write_local_properties(output)?;

        let destination = output.join("app/src/main/jniLibs").join(&contract.abi);
        std::fs::create_dir_all(&destination)?;
        std::fs::copy(library, destination.join(&contract.library))?;
        Ok(())
    }
}

pub struct NativeGpuBackend {
    program: String,
    language: String,
    host: String,
}

impl NativeGpuBackend {
    pub fn new(
        schema: &OrbitalSchema,
        language: String,
        backend_url: Option<&str>,
    ) -> Result<Self, serde_json::Error> {
        let value = serde_json::to_value(schema)?;
        let program = serde_json::to_string(&orbital_core_eval::util::canonical_json(&value))?;
        let host = match backend_url {
            Some(url) => serde_json::json!({ "mode": "server", "backendUrl": url }),
            None => serde_json::json!({ "mode": "embedded" }),
        };
        Ok(Self {
            program,
            language,
            host: serde_json::to_string(&host)?,
        })
    }
}

impl Backend for NativeGpuBackend {
    fn name(&self) -> &'static str {
        "android-native-gpu"
    }

    fn generate(&self, _module: &OirModule) -> GeneratedOutput {
        GeneratedOutput {
            files: vec![
                GeneratedFile::new("app/src/main/assets/program.orb", self.program.clone()),
                GeneratedFile::new("app/src/main/assets/language.json", self.language.clone()),
                GeneratedFile::new("app/src/main/assets/native-host.json", self.host.clone()),
            ],
            warnings: Vec::new(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn native_gpu_template_bundles_the_shared_library_and_names_the_app() {
        let contract = native_gpu_contract();
        let root = std::env::temp_dir().join(format!("orbital-native-gpu-{}", std::process::id()));
        let package = root.join("package");
        let lib_dir = package.join("native-gpu/android").join(&contract.abi);
        std::fs::create_dir_all(&lib_dir).unwrap();
        let mut library = vec![0u8; 64];
        library[..6].copy_from_slice(b"\x7fELF\x02\x01");
        library[18..20].copy_from_slice(&183u16.to_le_bytes());
        std::fs::write(lib_dir.join(&contract.library), &library).unwrap();
        let manifest = serde_json::json!({
            "contractVersion": contract.contract_version,
            "abi": contract.abi,
            "shellVersion": env!("CARGO_PKG_VERSION"),
            "featureSet": contract.production_feature_set,
            "sha256": format!("{:x}", Sha256::digest(&library)),
        });
        std::fs::write(
            package.join("native-gpu/android/manifest.json"),
            manifest.to_string(),
        )
        .unwrap();
        crate::shell::copy_dir_recursive(
            &Path::new(env!("CARGO_MANIFEST_DIR")).join("renderers/native/template"),
            &package.join("templates/android-native-gpu"),
        )
        .unwrap();
        let app = root.join("todo-app");
        let shell = NativeGpuShell::from_package_root(&package);
        shell.init_project(&app, "todo-app").unwrap();
        let activity = std::fs::read_to_string(
            app.join("app/src/main/java/io/almadar/nativegpu/MainActivity.java"),
        )
        .unwrap();
        let gradle = std::fs::read_to_string(app.join("app/build.gradle.kts")).unwrap();
        let android_manifest =
            std::fs::read_to_string(app.join("app/src/main/AndroidManifest.xml")).unwrap();
        let library_name = contract
            .library
            .strip_prefix("lib")
            .and_then(|name| name.strip_suffix(".so"))
            .unwrap();
        assert!(activity.contains("package io.almadar.todo_app;"));
        assert!(activity.contains(&format!("System.loadLibrary(\"{library_name}\")")));
        assert!(android_manifest.contains(&format!("android:value=\"{library_name}\"")));
        assert!(gradle.contains("applicationId = \"io.almadar.todo_app\""));
        assert!(gradle.contains(&format!("minSdk = {}", contract.min_api)));
        assert!(app.join("gradlew").is_file());
        assert!(app.join("gradle/wrapper/gradle-wrapper.jar").is_file());
        assert_eq!(
            std::fs::read(app.join("app/src/main/jniLibs/arm64-v8a/liborbital_android_native_gpu.so"))
                .unwrap(),
            library
        );
        let mut research = manifest.clone();
        research["featureSet"] = serde_json::json!(contract.research_feature_set);
        std::fs::write(
            package.join("native-gpu/android/manifest.json"),
            research.to_string(),
        )
        .unwrap();
        assert!(shell.verified_library().is_err());
        let mut research_shell = NativeGpuShell::from_package_root(&package);
        research_shell.research = true;
        assert!(research_shell.verified_library().is_ok());

        let mut bad = manifest;
        bad["featureSet"] = serde_json::json!("native");
        std::fs::write(
            package.join("native-gpu/android/manifest.json"),
            bad.to_string(),
        )
        .unwrap();
        assert!(shell.verified_library().is_err());
        let backend = NativeGpuBackend {
            program: "{}".into(),
            language: "{}".into(),
            host: r#"{"mode":"embedded"}"#.into(),
        };
        let assets = backend.generate(&OirModule::new("test", "1.0.0")).files;
        assert_eq!(
            assets
                .iter()
                .map(|file| file.path.as_str())
                .collect::<Vec<_>>(),
            [
                "app/src/main/assets/program.orb",
                "app/src/main/assets/language.json",
                "app/src/main/assets/native-host.json"
            ]
        );
        assert_eq!(assets[2].content, r#"{"mode":"embedded"}"#);
        std::fs::remove_dir_all(root).unwrap();
    }
}
