# AGENTS.md

## Project Context

JSignPdf is a multi-module Maven project.

The module:

```
jsignpdf-nogui
```

contains:

* CLI logic
* Core signing logic
* PDF signing implementation
* Currently depends on OpenPDF (LibrePDF fork of iText)

The goal of this phase:

Replace OpenPDF usage in `jsignpdf-nogui` with the DSS framework:

[https://github.com/esig/dss/](https://github.com/esig/dss/)

Relevant DSS modules may include:

* dss-service
* dss-pades
* dss-document
* dss-token
* dss-utils
* others if required

---

## Primary Objective

Replace OpenPDF-based PDF signing implementation with DSS-based PAdES signing.

The replacement must:

* Preserve existing CLI behavior
* Preserve signing semantics
* Preserve visible signature support (if currently supported)
* Preserve certification levels (if supported)
* Preserve incremental update behavior
* Preserve detached/enveloped signature behavior (if applicable)

If any feature cannot be replicated exactly:

* DO NOT silently change behavior
* Provide a clear report
* Propose alternatives
* Wait for confirmation

---

## Strict Constraints

AI agents must:

* NOT modify `jsignpdf` module
* ONLY work inside `jsignpdf-nogui`
* NOT change CLI argument structure unless absolutely required
* NOT remove features without approval
* NOT downgrade cryptographic strength
* NOT introduce network dependencies
* NOT introduce unstable timestamp dependencies
* Keep deterministic behavior

---

## Required Migration Method

Before replacing anything:

1. Identify all OpenPDF usages.
2. Identify:

   * How PDFs are loaded
   * How incremental updates are handled
   * How signature appearance is created
   * How ByteRange is constructed
   * How CMS container is embedded
   * How certification level is applied
3. Map each responsibility to DSS equivalent.
4. Produce a feature parity matrix.

Only after approval may implementation begin.

---

## Required Feature Parity Matrix

The AI must produce a table like:

| Feature | Current (OpenPDF) | DSS Equivalent | Parity | Notes |
| ------- | ----------------- | -------------- | ------ | ----- |

Parity values:

* FULL
* PARTIAL
* REQUIRES ARCH CHANGE
* NOT SUPPORTED

If any entry is not FULL, the AI must:

* Propose options
* Explain trade-offs
* Ask for confirmation

---

## Acceptable Architectural Changes

If necessary, the following are allowed:

* Introduce adapter layer
* Introduce abstraction for PDF signing engine
* Create internal signing service class
* Separate PDF manipulation from business logic

But:

* CLI interface must remain stable
* Signing logic semantics must remain identical
* Tests must pass (if present)

---

## DSS-Specific Requirements

The DSS integration must:

* Use PAdES profile equivalent to current behavior
* Support baseline B at minimum
* Not require remote validation services
* Work offline
* Use local keystore (PKCS#12 or JKS)
* Preserve visible signature placement (if currently supported)

If visible signatures are not straightforward in DSS:

* Explain how DSS handles signature appearance
* Provide migration strategy

---

## Definition of Done

Task is complete when:

* OpenPDF dependency is removed from `jsignpdf-nogui`
* DSS dependencies are added
* Code compiles
* CLI works
* Feature parity report provided
* Any behavior differences explicitly documented
* No silent feature loss
