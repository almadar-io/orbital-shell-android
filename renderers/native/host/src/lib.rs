#![cfg(target_os = "android")]

use serde::Deserialize;
use std::ffi::CString;
use std::io::Read;

#[derive(Deserialize)]
#[serde(tag = "mode", rename_all = "kebab-case", deny_unknown_fields)]
enum NativeHost {
    Embedded,
    Server {
        #[serde(rename = "backendUrl")]
        backend_url: String,
    },
}

fn read_asset(app: &android_activity::AndroidApp, name: &str) -> String {
    let name = CString::new(name).expect("asset name");
    let mut asset = app.asset_manager().open(&name).expect("native GPU asset");
    let mut value = String::new();
    asset
        .read_to_string(&mut value)
        .expect("UTF-8 native GPU asset");
    value
}

/// The program's lazy pages (EXEC-LOAD-2), read from the app's assets where the shell wrote them beside `program.orb`.
struct AssetLazySource(android_activity::AndroidApp);

impl orbital_core::runtime::LazySource for AssetLazySource {
    fn read(&self, file: &str) -> Result<orbital_core::runtime::LazyProgram, String> {
        let name = CString::new(file).map_err(|e| format!("{file}: {e}"))?;
        let mut asset = self.0.asset_manager().open(&name).ok_or_else(|| format!("{file}: no such asset"))?;
        let mut text = String::new();
        asset.read_to_string(&mut text).map_err(|e| format!("{file}: {e}"))?;
        let schema = orbital_core::schema::parse_schema(&text).map_err(|e| format!("{file}: {e}"))?;
        Ok(orbital_core::runtime::LazyProgram { schema })
    }
}

#[cfg(feature = "native-a11y")]
#[no_mangle]
pub extern "C" fn orbital_native_gpu_accessibility_v1() -> u32 {
    1
}

#[no_mangle]
pub extern "system" fn Java_com_google_androidgamesdk_OrbitalTextInput_nativeIsEditableAt(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    x: f32,
    y: f32,
) -> u8 {
    orbital_client_dioxus::native::android_is_editable_at(x, y) as u8
}

#[no_mangle]
pub extern "system" fn Java_com_google_androidgamesdk_OrbitalFirstFrame_nativePresented(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
) -> u8 {
    orbital_client_dioxus::native::first_frame_presented() as u8
}

#[no_mangle]
pub extern "system" fn Java_com_google_androidgamesdk_OrbitalMotion_nativeSetReducedMotion(
    _env: *mut std::ffi::c_void,
    _class: *mut std::ffi::c_void,
    reduced: u8,
) {
    orbital_client_dioxus::native::set_reduced_motion(reduced != 0);
}

#[no_mangle]
pub fn android_main(app: android_activity::AndroidApp) {
    let _ = tracing_subscriber::fmt()
        .with_max_level(tracing::Level::INFO)
        .try_init();
    // A device has no terminal: a panic is logged with its backtrace, or the crash names no frame.
    std::panic::set_hook(Box::new(|info| {
        tracing::error!(target: "almadar::android", "{info}\n{}", std::backtrace::Backtrace::force_capture());
    }));
    let language = read_asset(&app, "language.json");
    orbital_core::language_data::install_language_bundle(&language)
        .expect("install language bundle");
    let schema = read_asset(&app, "program.orb");
    let host: NativeHost = serde_json::from_str(&read_asset(&app, "native-host.json"))
        .expect("native-host.json contract");
    let carried = |path: &str| -> Result<Vec<u8>, String> {
        let name = CString::new(path).map_err(|e| e.to_string())?;
        let mut asset = app.asset_manager().open(&name).ok_or_else(|| "no such asset".to_string())?;
        let mut bytes = Vec::new();
        asset.read_to_end(&mut bytes).map_err(|e| e.to_string())?;
        Ok(bytes)
    };
    orbital_client_dioxus::native::register_bundled_files(&read_asset(&app, orbital_core::native_bundle::BUNDLED_FILES), carried)
        .expect("bundled files");
    let lazy: std::sync::Arc<dyn orbital_core::runtime::LazySource> = std::sync::Arc::new(AssetLazySource(app.clone()));
    orbital_client_dioxus::native::set_android_app(app);
    let schema = orbital_core::schema::parse_schema(&schema).expect("bundled program.orb");
    let css = orbital_client_dioxus::styles::program_stylesheets(&schema);
    match host {
        NativeHost::Embedded => orbital_client_dioxus::native::start(schema, Some(lazy), css, "Native App"),
        NativeHost::Server { backend_url } => {
            orbital_client_dioxus::native::start_server(schema, &backend_url, Some(lazy), css, "Native App")
        }
    }
    .expect("native GPU mount");
}
