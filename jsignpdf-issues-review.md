# JSignPdf — Open Issues Review and Action Plan

**Date:** 2026-04-19 (revised 2026-06-21)
**Scope:** open issues at https://github.com/intoolswetrust/jsignpdf/issues
**Baseline:** `master` after OpenPDF 3 / Java 21 migration (commits `97c8fbe`, `b806893`, `158d4a7`)

> **2026-06-21 revision.** Issues that have since been closed on GitHub, and issues
> fully resolved by the EU DSS (PAdES) signing engine PR (#422), have been removed
> from this document. Issues that the DSS engine PR covers only *partially* are kept
> and annotated inline. The original review covered 47 open issues; 19 have been
> removed (10 closed on GitHub, 9 resolved by the DSS engine). With #349 (opened after
> the original review) added, 29 issues are tracked below.

---

## Review team

| Expert | Domain |
|---|---|
| UX / usability | GUI layout, CLI ergonomics, error messages, i18n, accessibility, visible-signature rendering, install friction |
| Java / JVM | Java 21 compatibility, JCA/PKCS#11, BouncyCastle, threading, performance, packaging (jpackage, Flatpak), Swing vs JavaFX |
| PDF digital signatures | PAdES (B-B / B-T / B-LT / B-LTA), PKCS#7/CMS, RFC 3161 timestamping, OCSP/CRL, LTV, DSS/VRI, signature algorithms, eIDAS |
| Documentation | User manual (`website/docs/JSignPdf.adoc`), CLI `--help`, website, troubleshooting / FAQ, i18n strings, release notes |

All experts reviewed the same 47 issues against the code on disk, flagged duplicates, and gave a priority from their domain perspective. This document consolidates their verdicts — where the experts disagreed, the most informed domain wins (e.g. "is feature X in the code?" is decided by the Java or PDF-sig expert, not by UX).

---

## Priority legend

| Code | Meaning |
|---|---|
| **P0** | Correctness / compliance bug, or will break on a supported JVM — fix next release |
| **P1** | Significant friction or compliance gap — fix in this or the following milestone |
| **P2** | Valued improvement — schedule opportunistically |
| **P3** | Nice-to-have or cosmetic — accept PR, but do not plan |
| **Close** | Already covered in code/docs, not reproducible, or out of project scope |

**Effort:** S ≤ 1 day • M = 1–3 days • L = 1–2 weeks • XL > 2 weeks

---

## Executive summary

- **The LTV compliance cluster is resolved** by the EU DSS (PAdES) signing engine (PR #422). Selecting `-eng dss` produces genuine PAdES B / B-T / B-LT / B-LTA output with a real DSS dictionary, full-chain revocation, and TSA-chain revocation material — closing #27, #46, #95, #247, and #254, which the OpenPDF engine structurally cannot satisfy. #141 is partially covered (an archive timestamp is produced at LTA signing time; standalone DocTimeStamp / LTA refresh remains open).
- **The remaining headline gap is algorithm agility.** `SignerLogic.java:411` still hardcodes RSA PKCS#1 v1.5 on the OpenPDF path, so **#255 (RSASSA-PSS)** is unaddressed. The DSS path derives the algorithm from the key, which should unblock **#23 (EC keys)** — needs retest on an EC token.
- **PKCS#11 stability** is the largest *support-traffic* cluster. Most are environment-specific; a dedicated PKCS#11 troubleshooting page plus better diagnostics would absorb the recurring tickets at low engineering cost.
- **Visible-signature rendering** (timezone, alignment, width/height, font size, date format) is the largest remaining user-visible cluster. Bundling them into a single "Visible Signature v2" release would close several tickets and materially raise perceived quality vs. Adobe's output.
- **Documentation debt is real**: several open tickets are wholly or partly "user did not find the existing docs." A FAQ / troubleshooting chapter plus focused cookbook sections (TSA, PKCS#11, LTV, install channels) would retire those without touching code.

---

## Quick-close candidates

Issues recommended for closing with a short comment pointing to the current state:

| # | Title | Reason |
|---|---|---|
| **#172** | No window on Win11 | Packaging / JRE issue. The 3.x installer already ships a bundled JRE via jpackage. Add a troubleshooting note and close unless reproducible on the current installer. |
| **#307** | Flatpak support | Already implemented: `distribution/linux/flatpak/` exists and deps are regenerated (commit `158d4a7`). Close when Flathub submission is filed. |

Close-eligible conditional on verification (PR already merged or behaviour changed by OpenPDF 3 / JavaFX migration):

| # | Title | Verify |
|---|---|---|
| **#23** | "Private keys must be RSAPrivate(Crt)Key" | The DSS engine derives the algorithm from the key (`EncryptionAlgorithm.forKey`), so EC keys should sign via `-eng dss`. Retest on an EC PKCS#11 token. |
| **#63** | `LoginException: Unable to perform password callback` | Retest — likely benign on 3.x. |
| **#139** | Comodo AAA auto-added to PKCS7 | Reporter never followed up. Close after a short investigation note in the FAQ. |
| **#253** | PKCS11 not displayed by `-lkt` | Partly diagnostics; retest on Java 21 and improve error logging before closing. |

---

## The LTV compliance cluster — resolved by the DSS engine (PR #422)

Five of the six issues in this historically dominant cluster are closed by the EU DSS (PAdES) signing engine. With `-eng dss` (and `engine.dss.online.enabled=true` or local trust material for LT/LTA), JSignPdf now produces signatures that meet ETSI EN 319 142-1 baseline PAdES B-LT / B-LTA — something the OpenPDF engine structurally cannot do.

| # | Aspect | Resolution |
|---|---|---|
| **#27** | LTV not recognized by Adobe | DSS writes a real DSS dictionary at LT/LTA. **Closed.** |
| **#46** | PAdES B-LTA level support | Delivered directly (B / T / LT / LTA). **Closed.** |
| **#95** | Embed revocation info for TSA cert | Embedded as part of LT/LTA validation material. **Closed.** |
| **#247** | OCSP missing for intermediate certs | DSS collects the full chain. **Closed.** |
| **#254** | CRL fetch fails on HTTP→HTTPS redirect | DSS's own fetchers follow redirects; the legacy `CRLInfo` path is not used by `dss`. **Closed.** |
| **#141** | Append-only document timestamp | *Partial* — an archive timestamp is produced at LTA signing time, but a standalone `ETSI.RFC3161` DocTimeStamp and LTA refresh on an already-signed PDF remain out of scope. |

**Remaining work:** #141 (standalone DocTimeStamp / LTA refresh). The DSS engine produces the timestamp inline at signing time; refreshing the LTA material on an existing signature is a separate, smaller feature on top of the DSS path.

---

## Algorithm-agility cluster

Today `SignerLogic.java:411` hardcodes `sgn.setExternalDigest(..., "RSA")` on the OpenPDF path. This forces PKCS#1 v1.5 output even when the certificate mandates PSS, and blocks pluggable EC / EdDSA signatures. The DSS engine sidesteps part of this by deriving the algorithm from the key, but PSS is still not produced by either path.

| # | Aspect | State |
|---|---|---|
| **#255** | RSASSA-PSS required by PSS-only certificates (increasingly common for eIDAS QSCDs) | **Not covered** — both the OpenPDF and DSS tokens still emit RSA PKCS#1 v1.5, not PSS. |
| **#23** | EC / non-RSA private keys fail in the RSA path | *Partial* — the DSS token uses `EncryptionAlgorithm.forKey`, so EC keys should sign via `-eng dss`. Retest needed. |
| **#33** | RFC 3161 TSA nonce | *Partial* — the TSA policy OID is now wired through the DSS `OnlineTSPSource`; the **nonce is still not implemented**. |

**Recommendation — "Algorithm pluggability" (M, ~1 week):** introduce a `SignatureAlgorithm` abstraction (RSA / RSA-PSS / ECDSA / EdDSA), wire it through `SignerLogic` and the TSA client, and expose a CLI / GUI selector. Covers #255, the remaining part of #23 on the OpenPDF path, and the #33 nonce without more refactoring.

---

## Key-source pluggability cluster

| # | Request |
|---|---|
| **#20** | Remote signatures via web API |
| **#180** | Generic JCA provider (Azure Key Vault, AWS KMS, GCP HSM) |
| **#187** | Multiple PKCS#11 providers |

**Recommendation:** start with **#180 (M, P1)** — `--provider-class`/`--provider-arg` mirrors `jarsigner`, requires no new code paths, and largely subsumes #20. #187 (multi-PKCS#11) is a small follow-up once the provider mechanism is generalized.

---

## Visible-signature rendering cluster

| # | Request | Priority |
|---|---|---|
| **#67** | Text alignment (left/center/right) | P2 |
| **#99** | Font size honored when signer name is shown | P2 (OpenPDF 3 may have fixed; verify) |
| **#165** | Width/height input (not four corners) | P2 |
| **#179** | Selection box invisible on dark themes | P1 (1-line fix in `SelectionImage.java`) |
| **#231** | Configurable date format | P2 |
| **#55** | Configurable timezone | P2 |

**Recommendation — "Visible Signature v2" (M, 3–5 days):** bundle these into one release. Most are one- or two-line changes in `SignerLogic.java`, `VisibleSignatureDialog.java`, `SignPdfForm.java`, and `SelectionImage.java`. Combined, they close several tickets and materially raise parity with Adobe's rendered signature.

---

## PKCS#11 / hardware token cluster

Largely environment-specific:

| # | Nature |
|---|---|
| **#23**, **#63** | Probably already fixed; retest and close (see algorithm cluster for #23) |
| **#184** | Windows batch-mode hang after unregister — real bug in `PKCS11Utils.unregisterProviders` (P1) |
| **#186** | First-click "No private key" — driver warm-up race (P2, retry-on-empty) |
| **#187** | Multi-provider support (P2, see pluggability cluster) |
| **#253** | Environment / server deployment issue — diagnostics + docs |

**Recommendation:** fix **#184** in code (this is a reproducible Windows bug, not user env) and invest in a dedicated **PKCS#11 troubleshooting chapter** (`docs/pkcs11.md`) covering driver paths per OS, headless servers, login modes, and common errors. This one doc page will absorb the majority of PKCS#11 support issues at low cost.

---

## Per-issue consolidated table

Columns: **Status** — `close` (see quick-close list), `valid` (open, action needed), `docs` (resolvable by documentation), `cluster` (tracked in a cluster above), `partial` (partly covered by the DSS engine PR #422); **E** effort S/M/L/XL; **Pri** priority.

| # | Title (short) | Status | E | Pri | Recommendation |
|---|---|---|---|---|---|
| 20 | Remote signatures via web API | cluster | L | P2 | Subsumed by #180 (JCA provider). |
| 23 | RSAPrivate(Crt)Key error | partial | S | P3 | DSS token derives algo from key (`EncryptionAlgorithm.forKey`); EC keys should sign via `-eng dss`. Retest on an EC PKCS#11 token. |
| 30 | Sign multiple docs in GUI | valid | M | P2 | Multi-select in JavaFX file chooser. CLI already supports it. |
| 33 | TSA Nonce | partial | S | P2 | TSA policy OID now wired through the DSS `OnlineTSPSource`; nonce still not implemented. Do alongside algorithm pluggability. |
| 51 | Remove "Contact (optional)" | valid | S | P3 | Low priority — `/ContactInfo` is still a valid PAdES field; consider keeping but de-emphasizing. |
| 55 | Timezone of signature date | cluster | S | P2 | Visible Signature v2. |
| 63 | LoginException with PKCS11 | close? | S | P3 | Retest; add log suppression if cosmetic. |
| 67 | Visible signature alignment | cluster | S | P2 | Visible Signature v2. |
| 99 | Font size ignored with signer name | cluster | M | P2 | Visible Signature v2; verify vs. OpenPDF 3. |
| 139 | Comodo AAA auto-added | close? | S | P3 | Reporter silent; investigate once, add FAQ, close. |
| 140 | Validate-only mode | valid | XL | P2 | Out of historical focus; if pursued, delegate to EU DSS or PDFBox rather than re-implement. |
| 141 | Append-only timestamp | partial | L | P1 | DSS engine emits an archive timestamp at LTA signing; standalone DocTimeStamp / LTA refresh on an already-signed PDF still out of scope. |
| 148 | Show equivalent CLI in GUI | valid | M | P2 | High-value learning aid; nice-to-have. |
| 165 | Width/height for visible sig | cluster | S | P2 | Visible Signature v2. |
| 172 | Win11 window does not open | close? | S | P1 | Push users to bundled-JRE installer; add troubleshooting. |
| 178 | Signing 1 GB PDFs | valid | XL | P2 | Constrained by OpenPDF architecture; document memory guidance meanwhile. |
| 179 | Dark-theme selection invisible | valid | S | P1 | One-line fix in `SelectionImage.java:162` — theme-aware color or XOR. |
| 180 | JCA provider support | cluster | M | P1 | Key-source pluggability — `--provider-class`/`--provider-arg`. |
| 184 | Batch-mode hangs after PKCS11 | valid | M | P1 | `AuthProvider.logout()`, remove the blind `Thread.sleep(1000)` in `PKCS11Utils.java:82-90`; force `System.exit` on CLI. |
| 186 | "Private key not found" first click | valid | M | P2 | Retry-on-empty keystore load. |
| 187 | Multiple PKCS11 providers | cluster | M | P2 | After #180 generalization. |
| 223 | Sign existing sig form fields | valid | M | P1 | Expose `sap.setVisibleSignature(fieldName)`; remove the "clear existing fields" step when a named field is specified. |
| 231 | Date format | cluster | S | P2 | Visible Signature v2. Duplicate of #55 in spirit. |
| 243 | `sun.misc.Unsafe` deprecation | valid | S | P2 | Track OpenPDF upstream; bump `openpdf.version` when fix lands. **Will be P0 on a future JDK.** |
| 253 | PKCS11 not in `-lkt` | close? | M | P2 | Diagnostics + doc; possibly env-only. |
| 255 | RSASSA-PSS signing | cluster | M | P0 | Algorithm pluggability. Not covered by the DSS engine — still RSA PKCS#1 v1.5. P0 for certs that mandate PSS. |
| 259 | Configurable filename suffix | valid | S | P2 | CLI already supports `--out-suffix`; expose in GUI preferences. |
| 307 | Flatpak support | close | S | — | Already implemented in `distribution/linux/flatpak/`. Close when Flathub submission is filed. |
| 349 | Translate website | valid | M | P3 | Website i18n (Docusaurus i18n). Community-PR-friendly; not planned. |

---

## Cross-cutting themes

1. **LTV was the single most valuable engineering investment — now delivered.** Six tickets, from 2019 onward, converged on the same gap; the DSS engine (PR #422) closes five of them and partially covers #141.
2. **Error messages are the cheapest UX upgrade.** Several tickets surface stack traces where a one-line user-facing message would do (e.g. the residual #63 login noise). Adding a thin user-facing error layer pays off across dozens of tickets.
3. **CLI ↔ GUI feature parity** (#30, #148, #259) — the CLI has options the GUI lacks and vice versa. A small parity audit exposes most of them.
4. **Packaging has quietly matured**: Flatpak, Windows jpackage with bundled JRE, macOS DMG. Several "it doesn't run" issues (#172, #184) can be retired by steering users toward the bundled installer rather than `java -jar`.
5. **Swing/JavaFX duality**: JavaFX is now the default GUI. Several Swing-only tickets (#30, #51, #67, #165, #179, #259) should first be verified against the FX code path before being worked on — some may already be moot.
6. **Documentation discoverability** is a silent source of issues: #30 and #259 are partially or wholly "user didn't find the docs." A FAQ plus cookbook pages (TSA, PKCS#11, LTV, install channels) would absorb most of them.

---

## Suggested roadmap

| Milestone | Contents | Effort | Closes |
|---|---|---|---|
| **3.1 — DSS engine (PAdES)** | EU DSS signing engine: PAdES B / T / LT / LTA, DSS dictionary, full-chain + TSA-chain revocation, TSA hash hardening, `--pades-level`, `--overwrite` (PR #422) | delivered | #27, #46, #95, #247, #254 (+ #141 partial) |
| **3.2 — Algorithm pluggability + Key-source pluggability** | `SignatureAlgorithm` abstraction (#255, residual #23, #33 nonce), `--provider-class`/`--provider-arg` (#180), multi-PKCS#11 (#187), remote signing hook (#20) | ~2 weeks | #20, #33, #180, #187, #255 |
| **3.3 — Visible Signature v2 + GUI parity** | #30, #51, #55, #67, #99, #165, #231, #259; JavaFX multi-select; verbose CLI preview (#148) | ~1 week | several |
| **3.4 — LTA refresh** | Standalone DocTimeStamp / LTA refresh on already-signed PDFs (#141) on top of the DSS engine | ~1 week | #141 |
| **Ongoing / low** | #140 (validation mode), #178 (large files), #186 (retry), #243 (track OpenPDF), #253 (diagnostics), #63/#139/#172 (retest & close) | — | as PRs arrive |

---

## Notes on methodology and caveats

- Each expert report is based on the issue text, comment threads, and a fresh read of the code. Where an expert said "covered in current code," it was grep-verified. Where they said "duplicate," the referenced ticket was cross-checked.
- The Swing → JavaFX migration means several "GUI bug" tickets should be retested on the FX code path before implementation effort is spent.
- Priorities reflect project-maintainer perspective, not end-user urgency for a specific workflow.
- This plan omits estimates for administrative work (release notes, Flathub listing, website updates) — assume ~1 day per milestone for that.
