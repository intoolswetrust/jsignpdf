# 🧠 Prompt for Claude Code

You are working on the JSignPdf project.

Focus ONLY on the module:

```
jsignpdf-nogui
```

Your task:

Replace OpenPDF usage with the DSS framework:
[https://github.com/esig/dss/](https://github.com/esig/dss/)

This is a full signing engine migration.

Before changing any code, follow these steps strictly.

---

## Step 1 — Analyze Current OpenPDF Usage

Scan the entire `jsignpdf-nogui` module and:

1. Identify all imports from:

   * com.lowagie
   * org.openpdf
   * iText-related packages
2. List:

   * All classes directly using OpenPDF
   * What each usage does (loading, signing, appearance, incremental save, etc.)
3. Extract current signing flow:

   * Input → processing → signature creation → output
4. Identify:

   * Visible signature logic
   * Certification level handling
   * ByteRange handling
   * CMS creation logic
   * Keystore handling
5. Summarize the complete signing pipeline in structured form.

Do NOT modify code yet.

---

## Step 2 — Research DSS Capabilities

Using DSS documentation:

1. Identify:

   * How to perform PAdES signing
   * How to handle visible signatures
   * How to control certification level
   * How to perform incremental signing
   * How to work with local keystore
2. Determine which DSS modules are required.
3. Produce a mapping of OpenPDF responsibilities → DSS equivalents.

---

## Step 3 — Produce Feature Parity Matrix

Create a structured matrix:

| Feature | Current Behavior | DSS Equivalent | Parity | Required Changes |

If any feature is not FULL parity:

* Explain precisely why
* Provide alternative solutions
* Provide at least 2 possible approaches if feasible
* Wait for user confirmation

Do NOT implement yet.

---

## Step 4 — Propose Migration Architecture

Design a clean replacement approach:

* Direct rewrite inside existing classes?
* Introduce abstraction layer?
* Introduce PdfSigningService?
* Separate appearance handling?

Explain trade-offs.

Keep business logic intact.

Wait for confirmation.

---

## Step 5 — Implement Migration

After approval:

1. Remove OpenPDF dependencies from `jsignpdf-nogui`.
2. Add required DSS dependencies.
3. Implement signing using DSS.
4. Preserve CLI behavior.
5. Preserve feature semantics.
6. Ensure build passes.

If unexpected incompatibilities arise:

* Stop
* Explain
* Ask for guidance

---

## Constraints

* No silent feature loss
* No behavior downgrade
* No new network dependencies
* No reliance on online OCSP unless already present
* Deterministic behavior

Deliver:

1. Feature parity report
2. Migration design summary
3. Implementation

