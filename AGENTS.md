# AGENTS.md - JSignPdf

## Project Overview

JSignPdf is a Java application for adding digital signatures to PDF documents. It provides both a GUI (JavaFX default, Swing fallback via `-Djsignpdf.swing=true`) and a CLI interface.

- **Java**: 11+
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
└── website/           # Docusaurus documentation site (not a Maven module)
```

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

- **User settings**: `~/.JSignPdf` (properties file, passwords encrypted per-user)
- **App config**: `conf/conf.properties`

## Testing

- **JUnit 4** - test sources under `jsignpdf/src/test/java/`
- **Signing tests** (`signing/` package): use `SigningTestBase` which creates temp PDFs dynamically
- **JavaFX UI tests** (`fx/FxTranslationsTest`): headless via Monocle, loads FXML with different locales and verifies node text

## CI/CD (GitHub Actions)

| Workflow | Trigger | Purpose |
|---|---|---|
| `pr-builder.yaml` | PR/push to master | `mvn verify` with Java 11 |
| `push-snapshots.yaml` | Push to master | Deploy SNAPSHOTs to Maven Central |
| `do-release.yml` | Manual dispatch | Full release |
