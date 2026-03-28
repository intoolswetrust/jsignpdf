# AGENTS.md - JSignPdf

## Project Overview

JSignPdf is a Java application for adding digital signatures to PDF documents. It provides both a GUI (currently Swing, migrating to JavaFX) and a CLI interface. Licensed under MPL-2.0 / LGPL-2.1.

- **Group ID**: `com.github.kwart.jsign`
- **Version**: 2.4.0-SNAPSHOT
- **Java**: 11+ (enforced via `maven.compiler.release=11`)
- **Build**: Apache Maven multi-module
- **Homepage**: https://intoolswetrust.github.io/jsignpdf/
- **Repository**: https://github.com/intoolswetrust/jsignpdf

## Quick Reference

```bash
# Build everything
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests only
mvn test

# Run a single test class
mvn test -Dtest=BasicSigningTest

# CI verification (same as GitHub Actions PR builder)
mvn --batch-mode --update-snapshots verify

# Run the application (after build)
java -jar jsignpdf/target/jsignpdf-2.4.0-SNAPSHOT-jar-with-dependencies.jar

# Run with CLI arguments
java -jar jsignpdf/target/jsignpdf-2.4.0-SNAPSHOT-jar-with-dependencies.jar --help
```

## Module Structure

```
jsignpdf-root/                      # Parent POM (version props, dependency management)
├── jsignpdf/                       # Main application (signing logic + GUI + CLI)
├── installcert/                    # Standalone certificate installer utility
├── distribution/                   # Packaging: ZIP assembly, Windows installer, docs
└── website/                        # Docusaurus documentation site (not a Maven module)
```

### `jsignpdf/` - Main Application Module

This is where nearly all code lives. Produces:
- Regular JAR (classes only)
- Shaded fat JAR (`*-jar-with-dependencies.jar`) via maven-shade-plugin

### `installcert/` - Certificate Installer

Single-file utility (`InstallCert.java`) for installing SSL certificates into Java keystores.

### `distribution/` - Packaging

- `src/assembly/assembly.xml` - Maven assembly descriptor (ZIP distribution)
- `bin/jsignpdf.sh` - Unix launch script (handles Java 9+ module exports)
- `conf/conf.properties` - Default configuration template
- `conf/pkcs11.cfg` - PKCS#11 configuration template
- `windows/` - InnoSetup Windows installer scripts
- `doc/` - ChangeLog, ReleaseNotes
- `licenses/` - MPL-2.0, LGPL-2.1, third-party licenses

## Source Code Layout

All source is under `jsignpdf/src/main/java/net/sf/jsignpdf/`:

```
net.sf.jsignpdf
├── Signer.java                 # ENTRY POINT - main(), launches CLI or GUI
├── SignerLogic.java             # Core signing logic (runs on background thread)
├── BasicSignerOptions.java      # Options/model - all signing configuration fields
├── SignerOptionsFromCmdLine.java # CLI argument parsing (Apache Commons CLI)
├── SignPdfForm.java             # Swing main window (generated from .form)
├── VisibleSignatureDialog.java  # Swing dialog for visible signature settings
├── TsaDialog.java               # Swing dialog for TSA/OCSP/CRL settings
├── SignResultListener.java      # Callback interface for signing completion
├── Constants.java               # Application constants
├── crl/                         # Certificate Revocation List handling
├── extcsp/                      # External crypto providers (CloudFoxy)
├── preview/                     # PDF page rendering (Pdf2Image, SelectionImage)
├── ssl/                         # SSL/TLS initialization, dynamic trust manager
├── types/                       # Enums: HashAlgorithm, CertificationLevel,
│                                #   PDFEncryption, RenderMode, PrintRight,
│                                #   ServerAuthentication, PdfVersion, PageInfo, RelRect
└── utils/                       # Utilities: ConfigProvider, ResourceProvider,
                                 #   KeyStoreUtils, PdfUtils, PKCS11Utils,
                                 #   GuiUtils, FontUtils, ConvertUtils, PropertyProvider
```

## Architecture

### Separation of Concerns

```
CLI (SignerOptionsFromCmdLine)  ──┐
                                  ├──> BasicSignerOptions ──> SignerLogic.signFile()
GUI (SignPdfForm)              ──┘         (model)              (signing engine)
```

- **`BasicSignerOptions`** is the central model. Both CLI and GUI populate it, then pass it to `SignerLogic`.
- **`SignerLogic`** is the signing engine. It reads options, loads keystores/certificates, creates the PDF signature, and optionally timestamps it. It has no UI dependencies.
- **`SignResultListener`** provides async completion callbacks from `SignerLogic` to the GUI.
- **GUI classes** (`SignPdfForm`, `VisibleSignatureDialog`, `TsaDialog`) are Swing forms. They read/write `BasicSignerOptions` via `updateFromOptions()` / `storeToOptions()`.

### PDF Preview Pipeline

`Pdf2Image` converts PDF pages to `BufferedImage` using one of three backends (in priority order):
1. JPedal (`jsign-jpedal`)
2. PDFBox (`pdfbox`)
3. PDF-Renderer (`pdf-renderer`)

The backend order is configurable via `pdf2image.libraries` in `conf.properties`.

### i18n

- Resource bundles: `net/sf/jsignpdf/translations/messages*.properties`
- 18+ languages, managed via [Weblate](https://hosted.weblate.org/projects/jsignpdf/messages/)
- Access: `ResourceProvider.getInstance().get("key")` or `RES.get("key")`

### Configuration & Persistence

- **User settings**: `~/.JSignPdf` (properties file, passwords encrypted per-user)
- **App config**: `conf/conf.properties` (fonts, certificate validation, SSL, PKCS#11, TSA, PDF rendering)
- **PKCS#11**: `conf/pkcs11.cfg`
- **System properties**: `jsignpdf.home` (app home dir), `JSIGNPDF_HOME` (env var fallback)

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| OpenPDF | 1.3.30 | PDF manipulation and signing |
| BouncyCastle | 1.70 | Cryptography (bcprov, bcpkix) |
| Apache Commons CLI | 1.11.0 | Command-line parsing |
| Apache Commons IO | 2.21.0 | File utilities |
| Apache Commons Lang | 3.20.0 | String utilities |
| PDFBox | 2.0.27 | PDF rendering (alternative backend) |
| PDF-Renderer | 140 | PDF rendering (alternative backend) |
| jsign-jpedal | 4.92.13 | PDF rendering (preferred backend) |
| jsign-pkcs11 | 1.1.0 | PKCS#11 hardware token support |
| JUnit 4 | 4.13.2 | Testing |
| JaCoCo | 0.8.14 | Code coverage |

## Testing

### Framework & Conventions

- **JUnit 4** with `@Test`, `@Before`, `@Rule` annotations
- Test sources: `jsignpdf/src/test/java/net/sf/jsignpdf/`
- Test resources: `jsignpdf/src/test/resources/` (test keystores)

### Test Structure

```
net.sf.jsignpdf
├── TestConstants.java                    # Keystore configs, test key definitions
├── signing/
│   ├── SigningTestBase.java              # Abstract base: creates temp PDF, loads keystores
│   ├── BasicSigningTest.java             # Simple sign + validate
│   ├── AppendModeSigningTest.java        # Append vs overwrite signatures
│   ├── CertificationLevelSigningTest.java
│   ├── HashAlgorithmSigningTest.java     # SHA-1, SHA-256, SHA-384, SHA-512
│   ├── KeyTypeSigningTest.java           # RSA-1024/2048/4096, DSA-1024
│   ├── MultipleSignaturesTest.java
│   ├── SignatureMetadataTest.java
│   └── VisibleSignatureSigningTest.java
└── signing/validation/
    └── PdfSignatureValidator.java        # Validates signatures in signed PDFs
```

### Test Infrastructure

- **`SigningTestBase`**: Creates a minimal unsigned PDF (PDF 1.7) in a `@Rule TemporaryFolder` before each test. Provides helper methods for signing and validation.
- **Test keystores**: `test-keystore.jks` and `test-keystore.p12` in test resources
  - Keystore password: `keystorepass`
  - Keys: `expired`, `rsa1024`, `rsa2048`, `rsa4096`, `dsa1024`
  - Key password pattern: `<alias>pass` (e.g., `rsa2048pass`)
- **`PdfSignatureValidator`**: Uses PDFBox to verify signature validity on signed PDFs.

### Running Tests

```bash
mvn test                                    # All tests
mvn test -Dtest=BasicSigningTest            # Single test class
mvn test -Dtest=HashAlgorithmSigningTest     # Specific test
mvn verify                                   # Full verification
```

## CI/CD (GitHub Actions)

| Workflow | Trigger | Purpose |
|---|---|---|
| `pr-builder.yaml` | PR/push to master | `mvn verify` with Java 11 (Temurin) |
| `push-snapshots.yaml` | Push to master | Deploy SNAPSHOTs to Maven Central |
| `do-release.yml` | Manual dispatch | Full release: Maven Central + SourceForge + GitHub |
| `codeql-analysis.yml` | Push to master | Security scanning |
| `doc-builder.yaml` | Push (website/ changes) | Build & deploy Docusaurus docs to GH Pages |

CI uses **Java 11 Temurin** on **ubuntu-latest**.

## Code Conventions

- **Naming**: PascalCase classes, camelCase methods/fields, UPPER_SNAKE_CASE constants
- **Packages**: `net.sf.jsignpdf.*` (lowercase, domain-based)
- **Tests**: `*Test.java` suffix, in matching package structure under `src/test/`
- **Logging**: `java.util.logging` (JUL), logger name = class FQCN
- **No strict formatter/checkstyle enforced** - follow existing code style
- **NetBeans Form files**: `.form` XML files alongside Swing Java classes - these are IDE-generated, do not hand-edit

## Supported Keystore Types

- **WINDOWS-MY** - Windows Certificate Store (Windows only)
- **PKCS12** - `.pfx` / `.p12` files
- **JKS** - Java KeyStore
- **BKS** - Bouncy Castle KeyStore
- **PKCS11** - Hardware tokens (smart cards) via `pkcs11.cfg`
- **KeychainStore** - macOS Keychain (if available)
- **CloudFoxy** - External signing service

## Important Notes for AI Agents

1. **Do not modify `.form` files** - these are NetBeans GUI designer files. The corresponding `.java` files contain generated code sections marked with `// <editor-fold>` comments.

2. **`BasicSignerOptions` is the contract** between GUI, CLI, and `SignerLogic`. Changes here affect all three interfaces. The field `rightScreanReaders` is a known typo (not `ScreenReaders`) - do not "fix" it as it would break serialized user settings.

3. **GUI migration in progress** - a JavaFX replacement is planned (see `jsignpdf-gui-reimplementation-plan.md`). New GUI code goes under `net.sf.jsignpdf.fx.*` packages. The Swing code remains for fallback.

4. **PDF rendering has three backends** - JPedal, PDFBox, PDF-Renderer. If one fails, the next is tried. Always test with the default priority order.

5. **Passwords in `BasicSignerOptions`** are stored as `char[]` (not `String`) for security. The `~/.JSignPdf` file encrypts them. Do not log or print passwords.

6. **OpenPDF vs iText** - The project uses OpenPDF (LGPL fork of iText 4). Do not confuse with iText 5+ (AGPL). Import paths are `com.lowagie.text.*`.

7. **Java module exports** - The app needs `--add-exports` and `--add-opens` for PKCS#11 on Java 9+. These are set in `distribution/bin/jsignpdf.sh`. If adding new reflective access, update the launch script.

8. **Test PDFs are generated dynamically** - tests create minimal PDFs via PDFBox in `@Before` methods. No static test PDF files exist in the repo.
