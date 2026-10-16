//! `orb-shell-android`: the Android shell as a program (`orbital_core::shell_protocol`).

use orbital_core_base::project::write_project;
use orbital_core_base::shell_protocol::{
    description, written_files, GenerateRequest, GenerateResponse, ShellDescription, ShellFailure, ShellProgram,
    CODE_BAD_REQUEST, CODE_GENERATE_FAILED,
};
use std::path::Path;

pub struct AndroidProgram;

fn resolved(request: &GenerateRequest) -> Result<orbital_core_base::schema::types::OrbitalSchema, ShellFailure> {
    request
        .resolved
        .clone()
        .ok_or_else(|| ShellFailure::new(CODE_BAD_REQUEST, "the android shell needs the resolved program"))
}

impl ShellProgram for AndroidProgram {
    fn describe(&self) -> ShellDescription {
        description("android", env!("CARGO_PKG_VERSION"), true, Vec::new())
    }

    fn generate(&self, request: &GenerateRequest, output: &Path) -> Result<GenerateResponse, ShellFailure> {
        crate::require_renderer_pack().map_err(|e| ShellFailure::new(CODE_GENERATE_FAILED, format!("{}: {e}", e.code())))?;
        let backend = crate::AndroidBackend::new().with_resolved_schema(resolved(request)?);
        let generated = write_project(&request.module, output, &crate::AndroidShell::new(), &backend, &request.write)?;
        let mut next_steps = Vec::new();
        if std::env::var("ANDROID_HOME").is_err() && std::env::var("ANDROID_SDK_ROOT").is_err() {
            next_steps.push("Neither ANDROID_HOME nor ANDROID_SDK_ROOT is set — local.properties was not written; set one before running ./gradlew".to_string());
        }
        next_steps.push(format!("cd {}", output.display()));
        next_steps.push("./gradlew assembleDebug".to_string());
        Ok(GenerateResponse { files: written_files(&generated), warnings: generated.warnings, next_steps })
    }
}
