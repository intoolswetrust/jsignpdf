# AGENTS.md

## Project Overview

JSignPdf is a multi-module Maven project for digitally signing PDF documents.

The core business logic lives in the `jsignpdf` submodule:

* Main entry point:
  `jsignpdf/src/main/java/net/sf/jsignpdf/Signer.java`
* Core signing logic:
  `jsignpdf/src/main/java/net/sf/jsignpdf/SignerLogic.java`

PDF manipulation is currently performed using OpenPdf (LibrePDF), a fork of iText.

## Goal of AI Agent Contributions

AI agents are allowed to:

* Add or improve automated tests
* Add test utilities
* Add test fixtures
* Add build configuration needed for tests
* Improve test coverage

AI agents must **NOT**:

* Modify production logic in `src/main/java` unless absolutely required
* Change signing behavior
* Refactor business logic
* Introduce behavioral changes
* Replace OpenPdf with another library

The production signing logic must remain functionally identical.

## Test Design Rules

New tests must:

1. Live in:

   ```
   jsignpdf/src/test/java
   ```

2. Cover only the `jsignpdf` submodule.

3. Verify generated PDF output using a **different PDF library** than OpenPdf.

   * Preferred: Apache PDFBox
   * Alternative: another mature, independent PDF library
   * Under no circumstances may the same library used for writing the PDF be used to validate it.

4. Validate:

   * Presence of digital signature
   * Signature dictionary structure
   * ByteRange integrity
   * Signed revision correctness
   * Signature coverage of document
   * Signature metadata (signing time, reason, location if present)
   * Certificate presence in CMS container

5. Handle key material in one of two ways:

   * Use pregenerated test certificates with at least 10-year validity
   * Or generate keystores on the fly in tests (preferred if stable and deterministic)

6. Be deterministic and CI-friendly:

   * No network access
   * No dependency on system keystores
   * No OS-specific paths
   * No interactive prompts

## Cryptographic Requirements for Tests

If generating keys dynamically:

* Use RSA (2048 or 3072 bits)
* Self-signed certificate
* Validity >= 10 years
* SHA-256 or stronger
* Use standard Java security APIs or BouncyCastle

Keystores may be:

* PKCS#12 (preferred)
* JKS

Passwords must be test-local constants.

## Dependency Rules

New test dependencies must:

* Be declared in the `jsignpdf` module only
* Have `test` scope
* Not affect production artifact size

Allowed examples:

* Apache PDFBox
* BouncyCastle (test scope only)
* JUnit 5
* AssertJ

## Test Structure Guidelines

Prefer:

* Integration-style tests that:

  1. Create or load a minimal PDF
  2. Sign it via `SignerLogic`
  3. Load result via PDFBox
  4. Assert signature properties

* Minimal but expressive test utilities

* Small PDFs generated programmatically

Avoid:

* Large binary fixtures
* Fragile byte-by-byte comparisons of full PDFs
* Tests that rely on exact byte offsets except for ByteRange validation

## What “Done” Means

The task is complete when:

* Tests compile
* Tests pass
* Coverage of `SignerLogic` meaningfully increases
* No changes were made to production logic unless strictly required
* Maven build passes with `mvn clean verify`
