You are working on the JSignPdf project:
[https://github.com/intoolswetrust/jsignpdf](https://github.com/intoolswetrust/jsignpdf)

This is a multi-module Maven project.

Your task is to design and implement new automated tests for the `jsignpdf` submodule only.

Do NOT modify production code unless strictly necessary. Assume production logic is correct.

Before writing any code, perform the following steps in order:

---

## Step 1 — Analyze the Project

1. Inspect the Maven structure.
2. Identify:

   * Test framework used
   * Current test coverage
   * Signing flow in:

     * `Signer.java`
     * `SignerLogic.java`
3. Identify how PDFs are currently created and signed.
4. Identify current crypto handling.

Then produce a structured summary of:

* Signing pipeline
* External dependencies
* Where to hook integration tests

Do NOT generate code yet.

---

## Step 2 — Propose a Test Architecture

Design a test strategy that:

* Uses Apache PDFBox (or another independent library) to validate signed PDFs
* Does NOT use OpenPdf for validation
* Covers:

  * Signature presence
  * ByteRange correctness
  * CMS container existence
  * Certificate embedding
  * Signed revision integrity
  * Signature metadata

You must decide:

* Whether to generate PKCS#12 keystores dynamically or use pregenerated material
* How to ensure certificate validity >= 10 years
* How to avoid flakiness
* How to isolate filesystem use (use temporary directories)

Explain trade-offs.

Do NOT write code yet.

---

## Step 3 — Produce a Concrete Implementation Plan

Provide:

* Exact test classes to create
* Exact helper classes
* Required Maven test-scope dependencies
* Any required changes to `pom.xml` in `jsignpdf` module
* Expected directory layout

Keep production code untouched.

Wait for confirmation before implementing.

---

## Step 4 — Implement

After approval:

* Implement tests
* Add dependencies (test scope only)
* Ensure `mvn clean verify` passes
* Ensure no changes to `src/main` unless absolutely required
* If changes are required, explain why first

---

Constraints:

* Deterministic tests
* No network access
* No interactive prompts
* No OS-specific paths
* Must run in CI

Deliver clean, production-grade test code.
