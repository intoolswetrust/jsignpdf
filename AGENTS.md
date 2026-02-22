# AGENTS.md

## Project Overview

JSignPdf is a multi-module Maven project.

The `jsignpdf` module currently contains:

* CLI logic
* Core signing logic
* Swing-based GUI
* OpenPDF-based PDF manipulation

Main classes:

* Entry point:
  `jsignpdf/src/main/java/net/sf/jsignpdf/Signer.java`

* Core signing logic:
  `jsignpdf/src/main/java/net/sf/jsignpdf/SignerLogic.java`

The goal is to prepare a new module:

```
jsignpdf-nogui
```

This module must contain:

* CLI entry point
* Core signing logic
* All non-GUI supporting classes

It must NOT contain:

* Any Swing UI logic
* Any AWT logic
* Any GUI-specific resources
* Any UI-specific configuration classes

This is a preparation step for replacing OpenPDF in a later phase.

---

## Primary Objective

Create a new module `jsignpdf-nogui` that:

* Is a clean copy of `jsignpdf`
* Removes all GUI-related logic
* Compiles independently
* Preserves CLI functionality
* Keeps signing behavior unchanged

The existing `jsignpdf` module must remain untouched.

No production logic refactoring unless absolutely necessary.

---

## Strict Constraints

AI agents must:

* NOT modify the original `jsignpdf` module
* NOT change signing behavior
* NOT change cryptographic logic
* NOT alter business rules in `SignerLogic`
* NOT replace OpenPDF in this phase
* NOT merge modules

The task is structural separation only.

---

## Definition of GUI Code

GUI code includes:

* Classes importing:

  * `javax.swing.*`
  * `java.awt.*`
  * Swing event handling
  * UI models, renderers, dialogs, forms
* Any classes whose primary responsibility is UI interaction
* GUI resource files

If a class mixes UI and logic:

* Extract only if strictly necessary
* Prefer removing UI layer rather than modifying logic

Core signing and CLI behavior must remain identical.

---

## Build Requirements

`jsignpdf-nogui` must:

* Be a proper Maven module
* Be included in the parent `pom.xml`
* Compile with `mvn clean verify`
* Produce a working CLI artifact
* Have no Swing dependency
* Have no AWT dependency

Dependencies must be minimal and identical to original except for GUI-related ones.

---

## CLI Preservation Rules

The CLI:

* Must function the same way as in original module
* Must keep argument parsing intact
* Must keep exit codes intact
* Must keep logging behavior intact

The new module should behave identically when used from command line.

---

## Definition of Done

The task is complete when:

* `jsignpdf-nogui` exists as a separate Maven module
* It compiles independently
* It contains no Swing or AWT imports
* CLI works
* Original module remains unchanged
* `mvn clean verify` succeeds
