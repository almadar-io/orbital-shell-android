//! Native Android shell for the Orbital compiler.
//!
//! Emits a Jetpack Compose Gradle project from an `OirModule`.

mod backend;
mod pack;
mod shell;
pub mod shell_program;

pub use backend::{classify_oir_type, AndroidBackend, ANDROID_EXTRA_SNAPSHOTS};
pub use pack::{require_renderer_pack, RENDERER_INTERFACE};
pub use shell::AndroidShell;
