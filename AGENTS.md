# AGENTS.md - JSignPdf

## Project Overview

JSignPdf is a Java application for adding digital signatures to PDF documents. It provides both a GUI (JavaFX default, Swing fallback via `-Djsignpdf.swing=true`) and a CLI interface.

- **Java**: 21+
- **Build**: Apache Maven multi-module
- **Repository**: https://github.com/intoolswetrust/jsignpdf

## Build Commands

```bash
mvn clean install                    # Build everything (with tests)
mvn clean install -DskipTests        # Build without tests
mvn test -Dtest=BasicSigningTest     # Run a single test class
```

## Module Structure

```
jsignpdf-root/
├── jsignpdf-bootstrap/  # Java-8 launcher (Bootstrap.java): JRE-version check + JFX
│                          classifier picker + reflective Signer.main. Used by the
│                          cross-platform ZIPs (bin/jsignpdf.sh|.cmd → Bootstrap → Signer).
├── engines/            # Signing-engine SPI + bundled engines (Maven parent module)
│   ├── api/            #   Engine SPI (SigningEngine, Capability) + the shared core model
│   │                       (BasicSignerOptions, Constants, types/) and the canonical
│   │                       messages*.properties resource bundle.
│   ├── openpdf/        #   Default OpenPDF engine (id `openpdf`) — unchanged signatures.
│   └── dss/            #   EU DSS PAdES engine (id `dss`) — PAdES B/T/LT/LTA.
├── jsignpdf/            # Main application (GUI + CLI + engine discovery/gating)
├── installcert/         # Certificate installer utility
├── distribution/        # Per-platform packaging — assembles full + minimal ZIPs and
│                          drives jpackage for MSI / DEB / RPM / DMG (windows/, linux/,
│                          macos/ subfolders) plus the Flatpak manifest.
└── website/             # Hugo documentation site (not a Maven module)
    └── docs/JSignPdf.adoc  # ★ authoritative user guide (see below)
```

## Key Documentation Artifacts

Keep these in sync with the code; user-facing features are not "done" until they land here too.

| File | Role | When to update |
|---|---|---|
| `website/docs/JSignPdf.adoc` | **Authoritative user guide.** Single source of truth consumed by both the Hugo site (`website/content/docs/guide/index.adoc` is regenerated from it by `website/prepare.sh`) and the Maven PDF build in `distribution/`. | Any new or changed user-visible feature: new CLI flags, new GUI panels, changed defaults, new keystore types, new exit codes, etc. Update the synopsis block, the relevant option table, and add a dedicated subsection if the feature has non-obvious usage. |
| `distribution/doc/release-notes/<version>.md` | Release notes for one version — the single source feeding three sinks: the GitHub Release body, the bundled `README.md`, and the generated AppStream `<release>` entry in the Linux metainfo. **Must follow the strict format documented in `distribution/doc/release-notes/README.md`** (H1 + intro paragraph + one flat bullet list; only `**bold**`/`` `code` `` inline; no links, headings, or nested bullets) or the release workflow's `appstreamcli validate` step fails. | Every release-worthy change. |
| `README.md` | Top-level project landing page. Concise feature overview + pointers to the guide. | Only for high-signal changes (new feature categories, platform support, install paths). |
| `engines/api/src/main/resources/net/sf/jsignpdf/translations/messages.properties` | Canonical English resource bundle (all others are Weblate-synced). CLI `--help` text comes from here. Lives in the `engines/api` module so both the engines and the app can read it. | Any new CLI option or GUI string. Do not hand-edit non-English `messages_*.properties` files. |
| `design-doc/<version>-<topic>.md` | Design notes for larger changes. | Before implementing a non-trivial feature. |

When a PR touches a user-visible feature without updating `JSignPdf.adoc`, flag it as incomplete.

## Source Code Layout

Java code lives in two places under the `net.sf.jsignpdf` package root: the shared core +
engine SPI in `engines/api/` (and the bundled engines in `engines/openpdf/` and `engines/dss/`),
and the application (GUI + CLI) in `jsignpdf/`.

**`engines/api/src/main/java/net/sf/jsignpdf/`** — shared core model + engine SPI (no UI deps):

```
net.sf.jsignpdf
├── BasicSignerOptions.java      # Central model for all signing configuration
├── Constants.java               # CLI arg names, default values, config keys
├── JSignEncryptor.java          # PDF encryption helper
├── engine/                      # Engine SPI: SigningEngine, Capability, EngineConfig
├── extcsp/                      # External crypto providers (CloudFoxy)
├── ssl/                         # SSL/TLS initialization
├── types/                       # Enums and value types (HashAlgorithm, ...)
└── utils/                       # KeyStoreUtils, ResourceProvider, PropertyProvider, etc.
```

The bundled engines register via `META-INF/services/net.sf.jsignpdf.engine.SigningEngine`:
`engines/openpdf/` (id `openpdf`, default) and `engines/dss/` (id `dss`, PAdES).

**`jsignpdf/src/main/java/net/sf/jsignpdf/`** — application (GUI + CLI + engine wiring):

```
net.sf.jsignpdf
├── Signer.java                 # Entry point - launches CLI or GUI
├── SignerLogic.java             # Drives a SigningEngine (no UI dependencies)
├── SignerOptionsFromCmdLine.java # CLI parsing into BasicSignerOptions
├── SignPdfForm.java             # Swing GUI (legacy, .form files are IDE-generated)
├── engine/                      # EngineRegistry (discovery), EngineMismatchValidator (CLI gating)
├── fx/                          # JavaFX GUI (default)
│   ├── JSignPdfApp.java         #   Application entry point
│   ├── FxLauncher.java          #   Static launcher called from Signer.main()
│   ├── view/                    #   FXML files + controllers (MainWindow, settings panels)
│   ├── viewmodel/               #   DocumentViewModel, SigningOptionsViewModel, SignaturePlacementViewModel
│   ├── service/                 #   PdfRenderService, SigningService, KeyStoreService
│   ├── control/                 #   PdfPageView, SignatureOverlay
│   └── util/                    #   FxResourceProvider, SwingFxImageConverter, RecentFilesManager
├── preview/                     # PDF page rendering (Pdf2Image)
├── types/                       # App-only enums and value types
└── utils/                       # App-only helpers
```

## Architecture

```
CLI (SignerOptionsFromCmdLine)  ──┐
                                  ├──> BasicSignerOptions ──> SignerLogic.signFile() ──> SigningEngine
GUI (JavaFX / Swing)            ──┘         (model)              (orchestration)         (openpdf | dss)
```

- **`BasicSignerOptions`** is the central model. Both CLI and GUI populate it, then pass it to `SignerLogic`.
- **`SignerLogic`** has no UI dependencies. It resolves a `SigningEngine` (via `EngineRegistry`) and delegates the actual signing to it.
- **Signing engines** are discovered with `ServiceLoader`. Each declares a set of `Capability` values; the CLI fails fast on unsupported options via `EngineMismatchValidator`, and the GUI disables the matching controls.
- **JavaFX GUI** uses MVVM: ViewModels with JavaFX properties, FXML views with `%key` i18n, background services wrapping `SignerLogic` and `Pdf2Image`.

### i18n

- Resource bundles: `net/sf/jsignpdf/translations/messages*.properties`
- CLI keys: `console.*`, `hlp.*`
- Swing keys: `gui.*`
- JavaFX keys: `jfx.gui.*`

### Configuration

Per-user state lives under a platform-native `<config-dir>` resolved by `ConfigLocationResolver` (Linux: `$XDG_CONFIG_HOME/jsignpdf` or `~/.config/jsignpdf`; Windows: `%APPDATA%\JSignPdf`; macOS: `~/Library/Application Support/JSignPdf`). Override with `JSIGNPDF_CONFIG_DIR`. `PropertyStoreFactory` hands out `PropertyProvider` / `AdvancedConfig` instances backed by these files; tests construct them directly with a temp path instead of going through the singleton.

- **Main config** -- `<config-dir>/config.properties` -- last-used signing settings; passwords encrypted per-user. Backed by `BasicSignerOptions` / `PropertyProvider`.
- **Presets** -- `<config-dir>/presets/preset-<epoch-millis>.properties` -- saved signing-option bundles. Display name lives inside as `preset.displayName`. Loaded/managed by `PresetManager`.
- **Advanced config** -- `<config-dir>/advanced.properties` -- app-global tweaks (font, certificate checks, relax SSL, PDF preview backends, default TSA hash). Two-layer overlay: user file → bundled defaults from `/net/sf/jsignpdf/conf/advanced.default.properties`. Read via `AppConfig` static accessors; edited via the JavaFX _File > Preferences..._ dialog (`PreferencesController`).
- **PKCS#11 config** -- `<config-dir>/pkcs11.cfg` -- raw SunPKCS11 provider config at a fixed well-known location (the legacy `pkcs11config.path` key was removed in 3.0.0). Loaded by `PKCS11Utils.registerProvidersFromDefaultLocation()` once at startup; restart required to re-register.
- **Legacy migration** -- on first launch, `ConfigLocationResolver.resolveAndMigrate()` copies `~/.JSignPdf` to `config.properties`. The original is left in place for downgrade safety.

## Testing

- **JUnit 4** - test sources under `jsignpdf/src/test/java/`
- **Signing tests** (`signing/` package): use `SigningTestBase` which creates temp PDFs dynamically
- **JavaFX UI tests** (`fx/FxTranslationsTest`): headless via Monocle, loads FXML with different locales and verifies node text

## CI/CD (GitHub Actions)

| Workflow | Trigger | Purpose |
|---|---|---|
| `pr-builder.yaml` | PR/push to master | `mvn verify` with Java 21 |
| `push-snapshots.yaml` | Push to master | Deploy SNAPSHOTs to Maven Central |
| `do-release.yml` | Manual dispatch | **Irreversible half** of a release: `mvn release:prepare`/`perform` → version-bump commits, git tag, Maven Central deploy of the library jars **and** the `full`/`minimal` distribution ZIPs (classified artifacts on `com.github.kwart.jsign:jsignpdf-distribution`, signed via the `release` profile). On success it chains to `package-release.yml`. **Do not re-run on a packaging failure** — re-dispatch `package-release.yml` instead. |
| `package-release.yml` | Auto (called by `do-release.yml`) **or** manual dispatch | **Resumable half**: downloads the published `full`/`minimal` ZIPs from Maven Central (bounded retry for repo1 propagation), then per-platform jpackage matrix (windows-2022 / ubuntu-24.04 x2 / macos-14) → Flatpak matrix (x86_64 + aarch64) → publish (SHA-256 + SourceForge mirror + GitHub Release). The matrix packages the **exact Central bits** (scripts read the unpacked ZIP via `JSIGNPDF_LIB_DIR`), so re-dispatching it with the same `release-version` cleanly recovers a timed-out run without re-releasing. Native installers use Azul Zulu+FX 21 (`java-package: jdk+fx`) so JavaFX modules are available to `jlink`. No Intel-macOS build is shipped (Apple discontinued Intel; `macos-13` retiring). See `design-doc/3.1-separate-release-steps.md`. |
