# JSignPdf — Open Issues Review and Action Plan

**Date:** 2026-04-19 (revised 2026-08-08, 2026-08-09)
**Scope:** open issues at https://github.com/intoolswetrust/jsignpdf/issues
**Baseline:** `master` after OpenPDF 3 / Java 21 migration (commits `97c8fbe`, `b806893`, `158d4a7`)

> **2026-08-08 revision.** All issues closed on GitHub have been removed from this
> document, as have the issues fully resolved by the EU DSS (PAdES) signing engine
> PR (#422). Issues that the DSS engine PR covers only *partially* are kept and
> annotated inline.
>
> This pass dropped **8** newly closed issues from the tracked list (#172, #178,
> #179, #186, #223, #253, #259, #307), plus the 5 reference rows for the DSS-resolved
> LTV cluster (#27, #46, #95, #247, #254), taking the list from 29 to **21**.
> Cumulatively: of the 47 open issues in the original review, **27 have been
> removed** (18 closed on GitHub, 9 resolved by the DSS engine); the remaining 20,
> plus #349 (opened after the original review), make up the **21 issues tracked
> below**.
>
> **2026-08-09 revision.** The algorithm-agility cluster (#23, #33, #255) is
> **fixed in code** and removed from this document, taking the list from 21 to
> **18**. Three premises of the original review turned out to be wrong; the
> corrected analysis, the delivered fixes, and the one deliberately deferred item
> (an optional `--signature-algorithm` selector to force PSS on a plain
> `rsaEncryption` certificate, §5b) are recorded in
> `design-doc/3.2-algorithm-agility.md`, which is now the reference for this
> cluster.

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

- **The LTV compliance cluster is resolved** by the EU DSS (PAdES) signing engine (PR #422). Selecting `-eng dss` produces genuine PAdES B / B-T / B-LT / B-LTA output with a real DSS dictionary, full-chain revocation, and TSA-chain revocation material — something the OpenPDF engine structurally cannot satisfy. The single remaining piece is **#141** (standalone DocTimeStamp / LTA refresh).
- **The algorithm-agility cluster (#23, #33, #255) is fixed** and no longer tracked here — see `design-doc/3.2-algorithm-agility.md`. It turned out to be mostly already solved; only the PKCS#11 PSS shape and the DSS TSA nonce needed code.
- **PKCS#11 stability** is the largest *support-traffic* cluster. Most are environment-specific; a dedicated PKCS#11 troubleshooting page plus better diagnostics would absorb the recurring tickets at low engineering cost.
- **Visible-signature rendering** (timezone, alignment, width/height, font size, date format) is the largest remaining user-visible cluster. Bundling them into a single "Visible Signature v2" release would close several tickets and materially raise perceived quality vs. Adobe's output.
- **Documentation debt is real**: several open tickets are wholly or partly "user did not find the existing docs." A FAQ / troubleshooting chapter plus focused cookbook sections (TSA, PKCS#11, LTV, install channels) would retire those without touching code.

---

## Quick-close candidates

Close-eligible conditional on verification (PR already merged or behaviour changed by OpenPDF 3 / JavaFX migration):

| # | Title | Verify |
|---|---|---|
| **#63** | `LoginException: Unable to perform password callback` | Retest — likely benign on 3.x. |
| **#139** | Comodo AAA auto-added to PKCS7 | Reporter never followed up. Close after a short investigation note in the FAQ. |

---

## LTV compliance — remaining work after the DSS engine (PR #422)

The historically dominant LTV cluster is resolved. With `-eng dss` (and `engine.dss.online.enabled=true` or local trust material for LT/LTA), JSignPdf now produces signatures that meet ETSI EN 319 142-1 baseline PAdES B-LT / B-LTA.

| # | Aspect | State |
|---|---|---|
| **#141** | Append-only document timestamp | *Partial* — an archive timestamp is produced at LTA signing time, but a standalone `ETSI.RFC3161` DocTimeStamp and LTA refresh on an already-signed PDF remain out of scope. |

**Remaining work:** #141. The DSS engine produces the timestamp inline at signing time; refreshing the LTA material on an existing signature is a separate, smaller feature on top of the DSS path.

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
| **#231** | Configurable date format | P2 |
| **#55** | Configurable timezone | P2 |

**Recommendation — "Visible Signature v2" (M, 3–5 days):** bundle these into one release. Most are one- or two-line changes in `SignerLogic.java`, `VisibleSignatureDialog.java`, and `SignPdfForm.java`. Combined, they close several tickets and materially raise parity with Adobe's rendered signature.

---

## PKCS#11 / hardware token cluster

Largely environment-specific:

| # | Nature |
|---|---|
| **#63** | Still needs a retest on 3.x |
| **#184** | Windows batch-mode hang after unregister — real bug in `PKCS11Utils.unregisterProviders` (P1) |
| **#187** | Multi-provider support (P2, see pluggability cluster) |

**Recommendation:** fix **#184** in code (this is a reproducible Windows bug, not user env) and invest in a dedicated **PKCS#11 troubleshooting chapter** (`docs/pkcs11.md`) covering driver paths per OS, headless servers, login modes, and common errors. This one doc page will absorb the majority of PKCS#11 support issues at low cost.

---

## Per-issue consolidated table

Columns: **Status** — `close` (see quick-close list), `valid` (open, action needed), `docs` (resolvable by documentation), `cluster` (tracked in a cluster above), `partial` (partly covered by the DSS engine PR #422); **E** effort S/M/L/XL; **Pri** priority.

| # | Title (short) | Status | E | Pri | Recommendation |
|---|---|---|---|---|---|
| 20 | Remote signatures via web API | cluster | L | P2 | Subsumed by #180 (JCA provider). |
| 30 | Sign multiple docs in GUI | valid | M | P2 | Multi-select in JavaFX file chooser. CLI already supports it. |
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
| 180 | JCA provider support | cluster | M | P1 | Key-source pluggability — `--provider-class`/`--provider-arg`. |
| 184 | Batch-mode hangs after PKCS11 | valid | M | P1 | `AuthProvider.logout()`, remove the blind `Thread.sleep(1000)` in `PKCS11Utils.java:82-90`; force `System.exit` on CLI. |
| 187 | Multiple PKCS11 providers | cluster | M | P2 | After #180 generalization. |
| 231 | Date format | cluster | S | P2 | Visible Signature v2. Duplicate of #55 in spirit. |
| 243 | `sun.misc.Unsafe` deprecation | valid | S | P2 | Track OpenPDF upstream; bump `openpdf.version` when fix lands. **Will be P0 on a future JDK.** |
| 349 | Translate website | valid | M | P3 | Website i18n (Docusaurus i18n). Community-PR-friendly; not planned. |

---

## Cross-cutting themes

1. **LTV was the single most valuable engineering investment — now delivered.** Six tickets, from 2019 onward, converged on the same gap; the DSS engine (PR #422) closed five of them outright and partially covers #141.
2. **Error messages are the cheapest UX upgrade.** Several tickets surface stack traces where a one-line user-facing message would do (e.g. the residual #63 login noise). Adding a thin user-facing error layer pays off across dozens of tickets.
3. **CLI ↔ GUI feature parity** (#30, #148) — the CLI has options the GUI lacks and vice versa. A small parity audit exposes most of them.
4. **Packaging has quietly matured**: Flatpak, Windows jpackage with bundled JRE, macOS DMG. Several "it doesn't run" issues (#184, and #172 which was closed on these grounds) can be retired by steering users toward the bundled installer rather than `java -jar`.
5. **Swing/JavaFX duality**: JavaFX is now the default GUI. Several Swing-only tickets (#30, #51, #67, #165) should first be verified against the FX code path before being worked on — some may already be moot.
6. **Documentation discoverability** is a silent source of issues: #30 is partially "user didn't find the docs." A FAQ plus cookbook pages (TSA, PKCS#11, LTV, install channels) would absorb most of them.

---

## Suggested roadmap

| Milestone | Contents | Effort | Closes |
|---|---|---|---|
| **3.1 — DSS engine (PAdES)** | EU DSS signing engine: PAdES B / T / LT / LTA, DSS dictionary, full-chain + TSA-chain revocation, TSA hash hardening, `--pades-level`, `--overwrite` (PR #422) | delivered | LTV cluster (+ #141 partial) |
| **3.2 — Algorithm agility + Key-source pluggability** | Algorithm agility delivered (#23, #33, #255 — see `design-doc/3.2-algorithm-agility.md`). Remaining: `--provider-class`/`--provider-arg` (#180), multi-PKCS#11 (#187), remote signing hook (#20) | ~1 week | #20, #180, #187 |
| **3.3 — Visible Signature v2 + GUI parity** | #51, #55, #67, #99, #165, #231; JavaFX multi-select (#30); verbose CLI preview (#148) | ~1 week | several |
| **3.4 — LTA refresh** | Standalone DocTimeStamp / LTA refresh on already-signed PDFs (#141) on top of the DSS engine | ~1 week | #141 |
| **Ongoing / low** | #140 (validation mode), #243 (track OpenPDF), #63/#139 (retest & close), #349 (website i18n) | — | as PRs arrive |

---

## Notes on methodology and caveats

- Each expert report is based on the issue text, comment threads, and a fresh read of the code. Where an expert said "covered in current code," it was grep-verified. Where they said "duplicate," the referenced ticket was cross-checked.
- The Swing → JavaFX migration means several "GUI bug" tickets should be retested on the FX code path before implementation effort is spent.
- Priorities reflect project-maintainer perspective, not end-user urgency for a specific workflow.
- This plan omits estimates for administrative work (release notes, Flathub listing, website updates) — assume ~1 day per milestone for that.
