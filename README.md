<div align="center">

# orbital-shell-android

**The native Android shell: generates a Gradle project (Jetpack Compose) from a compiled Orbital program.**

[![CI](https://github.com/almadar-io/orbital-shell-android/actions/workflows/ci.yml/badge.svg)](https://github.com/almadar-io/orbital-shell-android/actions/workflows/ci.yml) [![License: BUSL-1.1](https://img.shields.io/badge/license-BUSL--1.1-blue)](LICENSE)

</div>

The Android shell turns a compiled Orbital program into a Jetpack Compose app project. It carries the Android project template and the generated Compose renderer pack, and bundles the Almadar kernel and canvas as a native library.

## Where this fits

```mermaid
flowchart TB
  subgraph kernel["Kernel and substrate"]
    core["orbital-core<br/>IR · evaluator · kernel"]
    canvas["orbital-canvas<br/>2D/3D render primitives"]
  end
  subgraph lang["Language"]
    lolo["orbital-lolo<br/>.lolo → .orb"]
    compiler["orbital-compiler<br/>validate · codegen"]
  end
  subgraph hosts["Shells and hosts"]
    ts["orbital-shell-typescript<br/>web · node · @almadar/core"]
    rs["orbital-shell-rust<br/>desktop · tui"]
    py["orbital-shell-python"]
    ios["orbital-shell-ios"]
    android["orbital-shell-android"]
    os["orbital-shell-os"]
    wasm["orbital-wasm<br/>browser kernel"]
    ffi["orbital-ffi<br/>C ABI for mobile"]
    router["orbital-router"]
  end
  cli["orbital-cli<br/>the orb binary"]
  ws["orbital-rust<br/>workspace · cross-crate tests"]
  dist["orb<br/>npm · releases"]
  subgraph pkgs["Language packages"]
    std["almadar-std<br/>@almadar/std substrate"]
    beh["behaviors<br/>themed organisms"]
    langs["lolo-ar · lolo-sl<br/>language packs"]
  end
  subgraph apps["Apps and sites"]
    builder["builder (studio)"]
    kflow["kflow-academy"]
    nata["nata"]
    rawi["rawi"]
    site["almadar (websites)"]
  end
  core --> lolo & compiler & ts & rs & py & ios & android & os & wasm & ffi & router
  canvas --> ts & rs & ffi
  lolo --> compiler
  compiler --> cli
  ts & rs & py & ios & android & os --> cli
  cli --> dist
  ws -.pins.-> core & lolo & compiler & cli
  dist --> std
  std --> beh
  std --> langs
  beh --> builder & kflow & nata & rawi & site
  click core "https://github.com/almadar-io/orbital-core"
  click canvas "https://github.com/almadar-io/orbital-canvas"
  click lolo "https://github.com/almadar-io/orbital-lolo"
  click compiler "https://github.com/almadar-io/orbital-compiler"
  click ts "https://github.com/almadar-io/orbital-shell-typescript"
  click rs "https://github.com/almadar-io/orbital-shell-rust"
  click py "https://github.com/almadar-io/orbital-shell-python"
  click ios "https://github.com/almadar-io/orbital-shell-ios"
  click android "https://github.com/almadar-io/orbital-shell-android"
  click os "https://github.com/almadar-io/orbital-shell-os"
  click wasm "https://github.com/almadar-io/orbital-wasm"
  click ffi "https://github.com/almadar-io/orbital-ffi"
  click router "https://github.com/almadar-io/orbital-router"
  click cli "https://github.com/almadar-io/orbital-cli"
  click ws "https://github.com/almadar-io/orbital-rust"
  click dist "https://github.com/almadar-io/orb"
  click std "https://github.com/almadar-io/almadar-std"
  click beh "https://github.com/almadar-io/behaviors"
  click langs "https://github.com/almadar-io/lolo-ar"
  click builder "https://github.com/itechdom/almadar-studio"
  click kflow "https://github.com/almadar-io/almadar-kflow-academy"
  click nata "https://github.com/almadar-io/almadar-nata"
  click rawi "https://github.com/almadar-io/almadar-rawi"
  click site "https://github.com/almadar-io/almadar"
  classDef here fill:#2563eb,color:#fff,stroke:#1e40af,stroke-width:3px
  class android here
```

| Upstream (this repo depends on) | Downstream (depends on this repo) |
|---|---|
| [orbital-core](https://github.com/almadar-io/orbital-core): IR, evaluator and kernel (`orbital-core-base`, `orbital-core-eval`) | [orbital-cli](https://github.com/almadar-io/orbital-cli): the `orb` binary, which runs this shell as `orb-shell-android` for `orb compile --shell android` |
| [orbital-canvas](https://github.com/almadar-io/orbital-canvas): 2D/3D render primitives |  |
| [orbital-ffi](https://github.com/almadar-io/orbital-ffi): the C ABI library the generated app loads |  |

## Quick start

Build the kernel library once from the `orbital-rust` workspace (needs `cargo-ndk` and `ANDROID_NDK_HOME`), then generate and build the app:

```bash
make android-lib        # in orbital-rust: builds liborbital_ffi.so for arm64-v8a into target/jniLibs
ORBITAL_NATIVE_LIB_DIR=<orbital-rust>/target/jniLibs orb compile <program.orb> --shell android -o <out-dir>
cd <out-dir> && ./gradlew assembleDebug
```

Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` before generating, or `local.properties` is not written. In `orbital-rust`, `make android-run ORB=<program.orb> OUT=<dir>` boots a device, compiles and installs.

## How `orb compile --shell android` uses it

`orb` runs the `orb-shell-android` program. It needs the resolved program and the installed `compose` renderer pack, copies `templates/android-shell` (Gradle wrapper, `app/`), generates the Kotlin sources and writes `local.properties` from the Android SDK location. When `ORBITAL_NATIVE_LIB_DIR` is set, it copies `<dir>/arm64-v8a/liborbital_ffi.so` into the project's `app/src/main/jniLibs/arm64-v8a/` and fails if the file is missing.

The generated app runs the same kernel as every other host, through `orbital-ffi` and its JNI bridge; `make android-lib` builds that library and checks that it exports the bridge symbols.

## Layout

| Path | Contents |
|---|---|
| `src/` | The crate: backend, pack, shell, `orb-shell-android` program |
| `templates/android-shell/` | Gradle project scaffold |
| `renderers/compose/` | Generated Compose renderer sources and pattern fixtures. Generated: fix the template or generator, not the output |
| `hosts/`, `runtime/` | Kotlin templates (`APIClient`, `OrbitalViewModel`, `AwaitRenders`, `EffectHandlers`) |
| `tests/` | Pack layout, renderer coverage, project native setup, kernel-bridge symbol coverage, `device-bench/` |

## Development

The crate reads the Almadar language data (patterns, operators, themes, vocabularies) from the installed npm packages, so install those first (this is the CI recipe):

```bash
npm install --no-save --no-package-lock @almadar/core@^10 @almadar/std@^16 @almadar/lolo-ar@^1 @almadar/lolo-sl@^1
cargo test
```

Open gaps are tracked in [Almadar_Gaps.md](Almadar_Gaps.md). Work on `main`; never create a feature branch.

CI also runs `node renderers/compose/scripts/check-pack.mjs` before `cargo test`.

## Docs

- [Orb documentation](https://orb.almadar.io/docs): the language, the `orb` CLI and how the pieces fit

## License

[Business Source License 1.1](LICENSE): free for non-commercial and evaluation use; converts to Apache-2.0 on the change date.
