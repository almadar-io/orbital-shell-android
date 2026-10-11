fn main() -> std::process::ExitCode {
    orbital_core::shell_protocol::run(&orbital_shell_android::shell_program::AndroidProgram)
}
