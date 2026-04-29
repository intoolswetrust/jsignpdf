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
├── jsignpdf/          # Main application (signing logic + GUI + CLI)
├── installcert/       # Certificate installer utility
├── distribution/      # Packaging: ZIP assembly, Windows installer, docs
└── website/           # Hugo documentation site (not a Maven module)
    └── docs/JSignPdf.adoc  # ★ authoritative user guide (see below)
```

## Key Documentation Artifacts

Keep these in sync with the code; user-facing features are not "done" until they land here too.

| File | Role | When to update |
|---|---|---|
| `website/docs/JSignPdf.adoc` | **Authoritative user guide.** Single source of truth consumed by both the Hugo site (`website/content/docs/guide/index.adoc` is regenerated from it by `website/prepare.sh`) and the Maven PDF build in `distribution/`. | Any new or changed user-visible feature: new CLI flags, new GUI panels, changed defaults, new keystore types, new exit codes, etc. Update the synopsis block, the relevant option table, and add a dedicated subsection if the feature has non-obvious usage. |
| `distribution/doc/release-notes/<version>.md` | Release notes bundled with the artifact and used as the GitHub Release body. | Every release-worthy change. |
| `README.md` | Top-level project landing page. Concise feature overview + pointers to the guide. | Only for high-signal changes (new feature categories, platform support, install paths). |
| `jsignpdf/src/main/resources/net/sf/jsignpdf/translations/messages.properties` | Canonical English resource bundle (all others are Weblate-synced). CLI `--help` text comes from here. | Any new CLI option or GUI string. Do not hand-edit non-English `messages_*.properties` files. |
| `design-doc/<version>-<topic>.md` | Design notes for larger changes. | Before implementing a non-trivial feature. |

When a PR touches a user-visible feature without updating `JSignPdf.adoc`, flag it as incomplete.

## Source Code Layout

All source is under `jsignpdf/src/main/java/net/sf/jsignpdf/`:

```
net.sf.jsignpdf
├── Signer.java                 # Entry point - launches CLI or GUI
├── SignerLogic.java             # Core signing engine (no UI dependencies)
├── BasicSignerOptions.java      # Central model for all signing configuration
├── SignPdfForm.java             # Swing GUI (legacy, .form files are IDE-generated)
├── fx/                          # JavaFX GUI (default)
│   ├── JSignPdfApp.java         #   Application entry point
│   ├── FxLauncher.java          #   Static launcher called from Signer.main()
│   ├── view/                    #   FXML files + controllers (MainWindow, settings panels)
│   ├── viewmodel/               #   DocumentViewModel, SigningOptionsViewModel, SignaturePlacementViewModel
│   ├── service/                 #   PdfRenderService, SigningService, KeyStoreService
│   ├── control/                 #   PdfPageView, SignatureOverlay
│   └── util/                    #   FxResourceProvider, SwingFxImageConverter, RecentFilesManager
├── crl/                         # Certificate Revocation List handling
├── extcsp/                      # External crypto providers (CloudFoxy)
├── preview/                     # PDF page rendering (Pdf2Image)
├── ssl/                         # SSL/TLS initialization
├── types/                       # Enums and value types
└── utils/                       # KeyStoreUtils, ResourceProvider, PropertyProvider, etc.
```

## Architecture

```
CLI (SignerOptionsFromCmdLine)  ──┐
                                  ├──> BasicSignerOptions ──> SignerLogic.signFile()
GUI (JavaFX / Swing)            ──┘         (model)              (signing engine)
```

- **`BasicSignerOptions`** is the central model. Both CLI and GUI populate it, then pass it to `SignerLogic`.
- **`SignerLogic`** is the signing engine. It has no UI dependencies.
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
- **Advanced config** -- `<config-dir>/advanced.properties` -- app-global tweaks (font, certificate checks, relax SSL, PDF preview backends, default TSA hash). Two-layer overlay: user file → bundled defaults from `/net/sf/jsignpdf/conf/advanced.default.properties`. Read via `AppConfig` static accessors; edited via the JavaFX _Tools > Preferences..._ dialog (`PreferencesController`).
- **PKCS#11 config** -- `<config-dir>/pkcs11.cfg` -- raw SunPKCS11 provider config at a fixed well-known location (the legacy `pkcs11config.path` key was removed in 3.0.0). Loaded by `PKCS11Utils.registerProvidersFromDefaultLocation()` once at startup; restart required to re-register.
- **Legacy migration** -- on first launch, `ConfigLocationResolver.resolveAndMigrate()` copies `~/.JSignPdf` to `config.properties` and migrates edited install-dir `conf/conf.properties` / `conf/pkcs11.cfg` files (byte-equality against bundled samples = skip). Originals are left in place for downgrade safety.

## Testing

- **JUnit 4** - test sources under `jsignpdf/src/test/java/`
- **Signing tests** (`signing/` package): use `SigningTestBase` which creates temp PDFs dynamically
- **JavaFX UI tests** (`fx/FxTranslationsTest`): headless via Monocle, loads FXML with different locales and verifies node text

## CI/CD (GitHub Actions)

| Workflow | Trigger | Purpose |
|---|---|---|
| `pr-builder.yaml` | PR/push to master | `mvn verify` with Java 21 |
| `push-snapshots.yaml` | Push to master | Deploy SNAPSHOTs to Maven Central |
| `do-release.yml` | Manual dispatch | Full release |
