# AGENTS.md

## Project Context

JSignPdf is a multi-module Maven project.

### Module: `jsignpdf-nogui`

Contains CLI logic, core signing logic, and PDF signing implementation.

Base path: `jsignpdf-nogui/src/main/java/net/sf/jsignpdf/`

#### Entry Points & Core Logic

| Class | Lines | Role |
|---|---|---|
| `Signer.java` | ~260 | **Main entry point.** CLI dispatcher — parses args, expands wildcards, delegates to `SignerLogic` |
| `SignerLogic.java` | ~500 | **Core signing orchestrator.** Implements `Runnable`. Workflow: validate files → load key → configure DSS PAdES params → sign → write output. Also handles visible signatures, PDF encryption (encrypt-before-sign), and TSA integration |
| `SignerOptionsFromCmdLine.java` | ~530 | Extends `BasicSignerOptions`. Parses CLI args via Apache Commons CLI into configuration fields |
| `BasicSignerOptions.java` | ~760 | POJO holding all signing configuration: keystore, files, signature metadata, PDF security, visible signature, TSA, proxy, CRL settings |
| `Constants.java` | ~420 | Static config values, logger setup, `ARG_*` CLI option names, `DEFVAL_*` defaults, exit codes, i18n resource bundle |

#### Type Enumerations (`types/`)

| Enum | Purpose |
|---|---|
| `HashAlgorithm` | SHA1, SHA256, SHA384, SHA512, RIPEMD160 — each with PDF version requirement and `toDssDigestAlgorithm()` |
| `CertificationLevel` | NOT_CERTIFIED, CERTIFIED_NO_CHANGES_ALLOWED, CERTIFIED_FORM_FILLING, CERTIFIED_FORM_FILLING_AND_ANNOTATIONS — with `toDssCertificationPermission()` |
| `PDFEncryption` | NONE, PASSWORD |
| `PrintRight` | DISALLOW_PRINTING, ALLOW_DEGRADED_PRINTING, ALLOW_PRINTING — numeric right masks |
| `ServerAuthentication` | NONE, PASSWORD, CERTIFICATE — for TSA auth |
| `PdfVersion` | PDF_1_2 through PDF_1_7 |
| `PageInfo` | Simple data holder: width, height |

#### Utility Classes (`utils/`)

| Class | Lines | Role |
|---|---|---|
| `KeyStoreUtils.java` | ~525 | **Static utility for keystore operations.** Load keystores, list aliases, retrieve `PrivateKeyInfo`, validate certificates (expiry, key usage, critical extensions). Registers BouncyCastle provider in static block |
| `PrivateKeySignatureToken.java` | ~130 | **Adapter** bridging JSignPdf's key management to DSS `SignatureTokenConnection`. Wraps `PrivateKey` + `Certificate[]` into DSS-compatible token |
| `PKCS11Utils.java` | ~150 | Registers/unregisters PKCS#11 providers (SunPKCS11, JSignPKCS11) via reflection |
| `PropertyProvider.java` | ~470 | Base class for property file management with singleton pattern support |
| `ConfigProvider.java` | ~75 | Singleton app config from `jsignpdf.conf` |
| `ResourceProvider.java` | ~140 | i18n wrapper around `ResourceBundle` with fallback handling |
| `ConvertUtils.java` | ~220 | Type conversion utilities (color, enum, number) |
| `PdfUtils.java` | ~95 | PDF document loading with password support |
| `FontUtils.java` | ~80 | Font handling for visible signature text rendering |
| `IOUtils.java` | ~70 | File path resolution with env var / system property expansion |

#### Supporting Classes

| Class | Role |
|---|---|
| `PrivateKeyInfo.java` | POJO: `PrivateKey` + `Certificate[]` |
| `PdfExtraInfo.java` | Extracts PDF metadata (page count, page dimensions) via PDFBox |
| `UncompressPdf.java` | PDF stream uncompression utility |
| `JavaVersion.java` | JVM version detection |

#### SSL & Security (`ssl/`)

| Class | Role |
|---|---|
| `SSLInitializer.java` | HTTPS/TLS configuration. Basic init (trust-all) and advanced init with client cert auth for TSA |
| `DynamicX509TrustManager.java` | Custom `X509TrustManager` that accepts all certificates |

#### External Crypto Provider (`extcsp/`)

| Class | Role |
|---|---|
| `IExternalCryptoProvider.java` | SPI interface for pluggable crypto backends: `getName()`, `getChain()`, `getSignature()`, `getAliasesList()` |
| `CloudFoxy.java` | Cloud signing implementation (currently marked "not yet supported with DSS PAdES signing") |

#### CRL Support (`crl/`)

| Class | Role |
|---|---|
| `CRLInfo.java` | CRL distribution point parsing and handling |

#### Resources

* `src/main/resources/logging.properties` — Java logging config
* `src/main/resources/net/sf/jsignpdf/fonts/` — DejaVuSans.ttf, pokrytie.ttf (for visible signature text)
* `src/main/resources/net/sf/jsignpdf/translations/messages*.properties` — i18n (en, cs, de, el, es, fr, hr, hu, hy, it, ja, nb-NO, pl, pt, ru, sk, ta, zh_CN, zh_TW)

#### Test Structure

Base path: `jsignpdf-nogui/src/test/java/net/sf/jsignpdf/`

* `signing/SigningTestBase.java` — Abstract base: registers BouncyCastle, generates test PDFs, provides `createOptions()` and `signAndValidate()` helpers
* `signing/BasicSigningTest.java` — Fundamental signing operations
* `signing/AppendModeSigningTest.java` — Multiple signatures on same doc
* `signing/VisibleSignatureSigningTest.java` — Visible signature rendering
* `signing/PasswordProtectedPdfSigningTest.java` — PDF password encryption
* `signing/CertificationLevelSigningTest.java` — Certification level constraints
* `signing/HashAlgorithmSigningTest.java` — Various hash algorithms
* `signing/KeyTypeSigningTest.java` — Different key types (RSA, ECDSA, etc.)
* `signing/MultipleSignaturesTest.java` — Sequential signatures
* `signing/SignatureMetadataTest.java` — Reason, location, contact fields
* `signing/TimestampSigningTest.java` — TSA integration (uses `tsa/EmbeddedTsaServer.java`)
* `signing/validation/PdfSignatureValidator.java` — Post-signing signature validation
* `utils/KeyStoreUtilsTest.java` — Keystore operations

Test resources: `src/test/resources/` — `test-keystore.jks`, `test-keystore.p12`

#### Key Dependencies

* **DSS Framework** (`eu.europa.ec.joinup.sd-dss`) — PAdES signing (dss-pades-pdfbox, dss-token, dss-service)
* **PDFBox** — PDF document manipulation
* **BouncyCastle** — Cryptographic algorithms (bcprov-jdk18on, bcpkix-jdk18on)
* **Apache Commons** — CLI parsing, file/string utilities
* **jsign-pkcs11** — PKCS#11 smart card/token support

#### Design Patterns

* **Singleton** — `ConfigProvider`, `ResourceProvider`, `CloudFoxy` (lazy-init via `InstanceHolder`)
* **Adapter** — `PrivateKeySignatureToken` adapts keys to DSS `SignatureTokenConnection`
* **SPI/Strategy** — `IExternalCryptoProvider` for pluggable crypto backends
* **Configuration Object** — `BasicSignerOptions` passed through most services
* **No DI framework** — direct instantiation, constructor injection where needed

The goal of this phase:


## Strict Constraints

AI agents must:

* NOT modify `jsignpdf` module
* ONLY work inside `jsignpdf-nogui`
* NOT remove features without approval
* NOT downgrade cryptographic strength
* NOT introduce network dependencies
* NOT introduce unstable timestamp dependencies
* Keep deterministic behavior
