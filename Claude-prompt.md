You are working on the JSignPdf project:
[https://github.com/intoolswetrust/jsignpdf](https://github.com/intoolswetrust/jsignpdf)

This is a multi-module Maven project.

Your task is to create a new module:

```
jsignpdf-nogui
```

This module must be a GUI-free copy of the existing `jsignpdf` module.

Do NOT modify the original `jsignpdf` module.

Do NOT replace OpenPDF.

Do NOT refactor signing logic unless strictly required for separation.

Before making any changes, follow the steps below.

---

## Step 1 — Analyze Current Module Structure

Inspect the `jsignpdf` module and:

1. Identify:

   * GUI-related packages
   * CLI-related classes
   * Core signing classes
2. Identify all classes importing:

   * `javax.swing`
   * `java.awt`
3. Detect any mixed classes (UI + logic).
4. Produce a structured dependency map:

   * GUI → core
   * CLI → core
   * Core → PDF
5. Explain:

   * Whether CLI depends on GUI
   * Whether GUI depends on CLI
   * Whether core is properly separable

Do NOT change anything yet.

---

## Step 2 — Propose a Separation Strategy

Provide a concrete plan:

* Which packages go to `jsignpdf-nogui`
* Which packages stay only in original module
* Whether any minimal extraction is needed
* Whether any interfaces need to be introduced (only if absolutely necessary)
* Exact steps to create new Maven module
* Exact `pom.xml` changes required

The plan must:

* Avoid modifying business logic
* Avoid refactoring unless strictly necessary
* Preserve CLI behavior exactly

Wait for confirmation before implementing.

---

## Step 3 — Implement Module Copy

After approval:

1. Create `jsignpdf-nogui` module.
2. Copy required source files.
3. Remove GUI classes.
4. Remove Swing/AWT imports.
5. Adjust package references if necessary.
6. Update parent `pom.xml`.
7. Ensure:

   * `mvn clean verify` passes
   * CLI works
   * No Swing/AWT remains in `jsignpdf-nogui`

If any production logic modification is required:

* Stop
* Explain precisely why
* Wait for confirmation

---

## Constraints

* No behavior changes
* No dependency upgrades
* No OpenPDF replacement
* No formatting-only refactors
* No opportunistic improvements

This task is structural only.

Deliver clean, minimal changes.
