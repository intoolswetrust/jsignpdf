# JSignPdf — Open Issues Review and Action Plan

**Date:** 2026-04-19
**Scope:** all 47 open issues at https://github.com/intoolswetrust/jsignpdf/issues
**Baseline:** `master` after OpenPDF 3 / Java 21 migration (commits `97c8fbe`, `b806893`, `158d4a7`)

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

- **14 issues can be closed** as already fixed by the OpenPDF 3 / Java 21 / JavaFX refresh, as resolved in comments, as duplicates, or as out-of-project-scope user questions.
- **One compliance cluster (6 issues: #27, #46, #95, #141, #247, #254)** dominates the real bug surface. Signatures produced today are not reliably LTV / PAdES B-LT — they claim to be but fail DSS/VRI in Adobe and the EU DSS demo. A focused "LTV hardening" milestone would close the largest and oldest tickets in one sweep.
- **Three long-standing quick wins** (#7 stdin password, #126/#181 TSA hash-name case, #254 CRL HTTPS redirect, #179 dark-theme selection, #282 hardcoded fonts) are each S-effort and P1. None should outlive the next release.
- **PKCS#11 stability** is the largest *support-traffic* cluster (7 issues). Most are environment-specific; a dedicated PKCS#11 troubleshooting page plus better diagnostics would absorb the recurring tickets at low engineering cost.
- **Visible-signature rendering** (date/time, timezone, alignment, width/height, font size) is the second largest user-visible cluster. Bundling them into a single "Visible Signature v2" release would close seven tickets and materially raise perceived quality vs. Adobe's output.
- **Documentation debt is real**: at least 9 open tickets are wholly or partly "user did not find the existing docs." A FAQ / troubleshooting chapter plus four focused cookbook sections (TSA, PKCS#11, LTV, install channels) would retire those without touching code.

---

## Quick-close candidates

Issues recommended for closing with a short comment pointing to the current state:

| # | Title | Reason |
|---|---|---|
| **#11** | Use jsignpdf server-side | Support question; no actionable request. Point to CLI batch-mode docs and close. |
| **#14** | "File is certified instead of signed" | User chose a `CERTIFIED_*` level. Not a bug; clarify `certificationLevel` defaults in FAQ and close. |
| **#124** | TSA user/pass in console mode | Resolved in comments — use `JSignPdfC.exe` wrapper. Add to FAQ and close. |
| **#149** | Doc PDF for 2.2.0 missing `-sn` | `-sn/--signer-name` is present in the current guide (line 326). Close as fixed. |
| **#151** | "Cryptographically invalid" in Okular | Old poppler bug; `pdfsig` and Adobe accept. External-tool issue. Close. |
| **#172** | No window on Win11 | Packaging / JRE issue. The 3.x installer already ships a bundled JRE via jpackage. Add a troubleshooting note and close unless reproducible on the current installer. |
| **#177** | Sign not working from PHP on Windows | Apache/XAMPP `shell_exec` environment issue, not JSignPdf. Close with troubleshooting note. |
| **#226** | "Slot with tokens: (none)" on server | No token attached to the server host. User misconfiguration; close. |
| **#307** | Flatpak support | Already implemented: `distribution/linux/flatpak/` exists and deps are regenerated (commit `158d4a7`). Close when Flathub submission is filed. |

Close-eligible conditional on verification (PR already merged or behaviour changed by OpenPDF 3 / JavaFX migration):

| # | Title | Verify |
|---|---|---|
| **#23** | "Private keys must be RSAPrivate(Crt)Key" | Retest on Java 21 + BC 1.84; probably fixed via current `SunPKCS11`/`JSignPKCS11` path. |
| **#63** | `LoginException: Unable to perform password callback` | Retest — likely benign on 3.x. |
| **#114** | Show date/time on visible signature | `${timestamp}` placeholder exists and is documented; close after a docs callout. |
| **#139** | Comodo AAA auto-added to PKCS7 | Reporter never followed up. Close after a short investigation note in the FAQ. |
| **#253** | PKCS11 not displayed by `-lkt` | Partly diagnostics; retest on Java 21 and improve error logging before closing. |

---

## The LTV compliance cluster (strategic)

Six issues describe the same underlying gap: JSignPdf can produce signatures that **claim** long-term validation but don't meet ETSI EN 319 142-1 baseline PAdES B-LT, let alone B-LTA.

| # | Aspect | Current state |
|---|---|---|
| **#27** | LTV not recognized by Adobe | DSS dictionary is not written; only SignerInfo-embedded CRL/OCSP. |
| **#46** | PAdES B-LTA level support | No document timestamps; no VRI; author keeps `jsignpdf-pades` as a separate experiment. |
| **#95** | Embed revocation info for TSA cert | TSA chain revocation is not collected or embedded. |
| **#141** | Append-only document timestamp | Not supported (no `ETSI.RFC3161` DocTimeStamp). |
| **#247** | OCSP missing for intermediate certs | Loop walks only `chain[0]/chain[1]`; one OCSP response. |
| **#254** | CRL fetch fails on HTTP→HTTPS redirect | `URL.openConnection().getInputStream()` doesn't follow cross-scheme redirects. |

**Recommendation — "LTV Hardening" milestone (L–XL, 2–3 weeks):**
1. Fix #247 (full-chain OCSP) and #254 (redirect-tolerant CRL fetch) first — both are S-effort and make existing B-LT-ish output genuinely B-LT-compliant.
2. Add a DSS/VRI writer (files `SignerLogic.java` lines 208–218 / 366–437, `crl/CRLInfo.java:126`). This resolves #27, #95, and the "B-LT" half of #46.
3. Implement document timestamps (#141) on top of the DSS writer — this is the missing piece for #46 B-LTA.
4. Document the compliance level honestly in the manual (current claim in `JSignPdf.adoc` overstates what is produced).

All six tickets close together. The author's separate `jsignpdf-pades` repo can feed this work.

---

## Algorithm-agility cluster

Today `SignerLogic.java:411` hardcodes `sgn.setExternalDigest(..., "RSA")`. This forces PKCS#1 v1.5 output even when the certificate mandates PSS, and blocks pluggable EC / EdDSA signatures.

| # | Aspect |
|---|---|
| **#255** | RSASSA-PSS required by PSS-only certificates (increasingly common for eIDAS QSCDs) |
| **#23** | EC / non-RSA private keys fail in the RSA path |
| **#33** | RFC 3161 TSA nonce (same abstraction, small add) |

**Recommendation — "Algorithm pluggability" (M, ~1 week):** introduce a `SignatureAlgorithm` abstraction (RSA / RSA-PSS / ECDSA / EdDSA), wire it through `SignerLogic` and the TSA client, and expose a CLI / GUI selector. Covers #255, parts of #23, and opens the door to #33 nonce support without more refactoring.

---

## Key-source pluggability cluster

| # | Request |
|---|---|
| **#7** | Read keystore password from stdin (not argv) |
| **#20** | Remote signatures via web API |
| **#180** | Generic JCA provider (Azure Key Vault, AWS KMS, GCP HSM) |
| **#187** | Multiple PKCS#11 providers |

**Recommendation:** start with **#7 (S, P1, security hygiene)** and **#180 (M, P1)** — `--provider-class`/`--provider-arg` mirrors `jarsigner`, requires no new code paths, and largely subsumes #20. #187 (multi-PKCS#11) is a small follow-up once the provider mechanism is generalized.

---

## Visible-signature rendering cluster

| # | Request | Priority |
|---|---|---|
| **#67** | Text alignment (left/center/right) | P2 |
| **#99** | Font size honored when signer name is shown | P2 (OpenPDF 3 may have fixed; verify) |
| **#114** | Date/time on visible signature | P1 (docs — already supported via `${timestamp}`) |
| **#165** | Width/height input (not four corners) | P2 |
| **#179** | Selection box invisible on dark themes | P1 (1-line fix in `SelectionImage.java`) |
| **#231** | Configurable date format | P2 |
| **#55** | Configurable timezone | P2 |
| **#282** | I18N fonts (hardcoded "Tahoma"/"Courier New") | P1 |

**Recommendation — "Visible Signature v2" (M, 3–5 days):** bundle all of these into one release. Most are one- or two-line changes in `SignerLogic.java`, `VisibleSignatureDialog.java`, `SignPdfForm.java`, and `SelectionImage.java`. Combined, they close eight tickets and materially raise parity with Adobe's rendered signature.

---

## PKCS#11 / hardware token cluster

Seven issues, largely environment-specific:

| # | Nature |
|---|---|
| **#23**, **#63** | Probably already fixed; retest and close |
| **#184** | Windows batch-mode hang after unregister — real bug in `PKCS11Utils.unregisterProviders` (P1) |
| **#186** | First-click "No private key" — driver warm-up race (P2, retry-on-empty) |
| **#187** | Multi-provider support (P2, see pluggability cluster) |
| **#226**, **#253** | Environment / server deployment issues — diagnostics + docs |

**Recommendation:** fix **#184** in code (this is a reproducible Windows bug, not user env) and invest in a dedicated **PKCS#11 troubleshooting chapter** (`docs/pkcs11.md`) covering driver paths per OS, headless servers, login modes, and common errors. This one doc page will absorb the majority of PKCS#11 support issues at low cost.

---

## Per-issue consolidated table

Columns: **Status** — `close` (see quick-close list), `valid` (open, action needed), `docs` (resolvable by documentation), `cluster` (tracked in a cluster above); **E** effort S/M/L/XL; **Pri** priority.

| # | Title (short) | Status | E | Pri | Recommendation |
|---|---|---|---|---|---|
| 7 | Read password from stdin | valid | S | P1 | Quick win. `System.console().readPassword()` in `SignerOptionsFromCmdLine.java`. |
| 11 | Server-side usage | close | — | — | Support question. Close with pointer to batch-mode docs. |
| 14 | "File certified instead of signed" | close | — | — | User config. Clarify in FAQ and close. |
| 20 | Remote signatures via web API | cluster | L | P2 | Subsumed by #180 (JCA provider). |
| 23 | RSAPrivate(Crt)Key error | close? | S | P3 | Retest on Java 21; likely already fixed. |
| 27 | LTV Long-Term Validation | cluster | XL | P0 | Part of LTV Hardening milestone. |
| 30 | Sign multiple docs in GUI | valid | M | P2 | Multi-select in JavaFX file chooser. CLI already supports it. |
| 33 | TSA Nonce | valid | S | P2 | Wire BouncyCastle nonce; do alongside algorithm pluggability. |
| 46 | PAdES B-LTA level | cluster | XL | P0 | Strategic — LTV Hardening + DocTimeStamp (#141). |
| 51 | Remove "Contact (optional)" | valid | S | P3 | Low priority — `/ContactInfo` is still a valid PAdES field; consider keeping but de-emphasizing. |
| 55 | Timezone of signature date | cluster | S | P2 | Visible Signature v2. |
| 63 | LoginException with PKCS11 | close? | S | P3 | Retest; add log suppression if cosmetic. |
| 67 | Visible signature alignment | cluster | S | P2 | Visible Signature v2. |
| 95 | OCSP/CRL for TSA cert | cluster | M | P0 | LTV Hardening — required for PAdES B-LT conformance. |
| 99 | Font size ignored with signer name | cluster | M | P2 | Visible Signature v2; verify vs. OpenPDF 3. |
| 114 | Show date/time on signature | docs | S | P1 | Already supported via `${timestamp}`; add a prominent example. |
| 124 | TSA user/pass in console mode | close | — | — | Resolved — use `.exe` wrapper. Add FAQ entry. |
| 126 | TSA NPE on lowercase hash | valid | S | P1 | Uppercase + validate in `TsaDialog`/`BasicSignerOptions`. Duplicate of #181. |
| 139 | Comodo AAA auto-added | close? | S | P3 | Reporter silent; investigate once, add FAQ, close. |
| 140 | Validate-only mode | valid | XL | P2 | Out of historical focus; if pursued, delegate to EU DSS or PDFBox rather than re-implement. |
| 141 | Append-only timestamp | cluster | L | P1 | LTV Hardening — enables B-LTA (#46). |
| 148 | Show equivalent CLI in GUI | valid | M | P2 | High-value learning aid; nice-to-have. |
| 149 | Docs PDF missing `-sn` | close | — | — | Already present in current guide. Close. |
| 151 | Okular reports invalid | close | — | — | External tool bug. Close. |
| 165 | Width/height for visible sig | cluster | S | P2 | Visible Signature v2. |
| 172 | Win11 window does not open | close? | S | P1 | Push users to bundled-JRE installer; add troubleshooting. |
| 177 | PHP shell_exec on Windows | close | — | — | Environment, not JSignPdf. |
| 178 | Signing 1 GB PDFs | valid | XL | P2 | Constrained by OpenPDF architecture; document memory guidance meanwhile. |
| 179 | Dark-theme selection invisible | valid | S | P1 | One-line fix in `SelectionImage.java:162` — theme-aware color or XOR. |
| 180 | JCA provider support | cluster | M | P1 | Key-source pluggability — `--provider-class`/`--provider-arg`. |
| 181 | "One working TSA" | docs | S | P1 | Cookbook + GUI hash dropdown; fix #126 NPE. |
| 184 | Batch-mode hangs after PKCS11 | valid | M | P1 | `AuthProvider.logout()`, remove the blind `Thread.sleep(1000)` in `PKCS11Utils.java:82-90`; force `System.exit` on CLI. |
| 186 | "Private key not found" first click | valid | M | P2 | Retry-on-empty keystore load. |
| 187 | Multiple PKCS11 providers | cluster | M | P2 | After #180 generalization. |
| 223 | Sign existing sig form fields | valid | M | P1 | Expose `sap.setVisibleSignature(fieldName)`; remove the "clear existing fields" step when a named field is specified. |
| 226 | PKCS11 slot empty (server) | close | — | — | No token attached. Close. |
| 231 | Date format | cluster | S | P2 | Visible Signature v2. Duplicate of #55 in spirit. |
| 243 | `sun.misc.Unsafe` deprecation | valid | S | P2 | Track OpenPDF upstream; bump `openpdf.version` when fix lands. **Will be P0 on a future JDK.** |
| 247 | OCSP for intermediate certs | cluster | M | P0 | LTV Hardening — loop over `chain[0..n-1]`. |
| 251 | `--overwrite` / delete-source CLI | valid | S | P1 | Add flags in `SignerOptionsFromCmdLine.java`. Silent overwrite of signed output is a data-risk. |
| 252 | `$XDG_CONFIG_HOME` support | valid | S | P2 | `PropertyProvider.java:60` — honor XDG; matters for Flatpak (#307). |
| 253 | PKCS11 not in `-lkt` | close? | M | P2 | Diagnostics + doc; possibly env-only. |
| 254 | CRL HTTP→HTTPS redirect | cluster | S | P1 | `crl/CRLInfo.java:126` — switch to `java.net.http.HttpClient` with `Redirect.NORMAL`. |
| 255 | RSASSA-PSS signing | cluster | M | P0 | Algorithm pluggability. P0 for certs that mandate PSS. |
| 259 | Configurable filename suffix | valid | S | P2 | CLI already supports `--out-suffix`; expose in GUI preferences. |
| 282 | I18N font glyphs (tofu) | valid | S | P1 | `SignPdfForm.java:531,993` — replace `"Tahoma"`/`"Courier New"` with `Font.DIALOG` or UIManager defaults. |
| 307 | Flatpak support | close | S | — | Already implemented in `distribution/linux/flatpak/`. Close when Flathub submission is filed. |

---

## Cross-cutting themes

1. **LTV is the single most valuable engineering investment.** Six tickets, from 2019 onward, converge on the same gap. One focused milestone closes them all.
2. **Error messages are the cheapest UX upgrade.** #126, #151, #226, #254, #63 all surface stack traces where a one-line "Hash name must be uppercase (SHA256)" or "CRL redirect to HTTPS not followed" would do. Adding a thin user-facing error layer pays off across dozens of tickets.
3. **CLI ↔ GUI feature parity** (#7, #30, #148, #251, #259) — the CLI has options the GUI lacks and vice versa. A small parity audit exposes most of them.
4. **Packaging has quietly matured**: Flatpak, Windows jpackage with bundled JRE, macOS DMG. Several "it doesn't run" issues (#172, #184) can be retired by steering users toward the bundled installer rather than `java -jar`.
5. **Swing/JavaFX duality**: JavaFX is now the default GUI. Several Swing-only tickets (#30, #51, #67, #114, #165, #179, #259, #282) should first be verified against the FX code path before being worked on — some may already be moot.
6. **Documentation discoverability** is a silent source of issues: #14, #30, #114, #124, #149, #181, #259 are partially or wholly "user didn't find the docs." A FAQ plus four cookbook pages (TSA, PKCS#11, LTV, install channels) would absorb most of them.

---

## Suggested roadmap

| Milestone | Contents | Effort | Closes |
|---|---|---|---|
| **3.1 — Quick wins** | #7, #126/#181, #179, #254, #282, #247; FAQ skeleton + TSA cookbook; housekeeping closes for #11, #14, #124, #149, #151, #177, #226, #307 | ~1 week | ≥12 issues |
| **3.2 — LTV Hardening** | DSS/VRI writer, full-chain OCSP done, TSA-chain revocation (#95), redirect-tolerant CRL. Honest compliance chapter in manual | 2–3 weeks | #27, #46 (B-LT), #95, #247, #254 |
| **3.3 — Algorithm pluggability + Key-source pluggability** | `SignatureAlgorithm` abstraction (#255, #23, #33), `--provider-class`/`--provider-arg` (#180), stdin password (#7 if not already), multi-PKCS#11 (#187), remote signing hook (#20) | ~2 weeks | #20, #23, #33, #180, #187, #255 |
| **3.4 — Visible Signature v2 + GUI parity** | #30, #51, #55, #67, #99, #114, #165, #231, #251, #259; JavaFX multi-select; verbose CLI preview (#148) | ~1 week | ≥10 issues |
| **3.5 — B-LTA** | Document timestamps (#141) on top of 3.2 | ~1 week | #46 (B-LTA), #141 |
| **Ongoing / low** | #140 (validation mode), #178 (large files), #186 (retry), #252 (XDG), #243 (track OpenPDF), #253 (diagnostics) | — | as PRs arrive |

With this sequence, roughly **35 of the 47 open issues are actionable within ~2 months of focused work**, and the remainder are either environment-specific or low-priority cosmetic.

---

## Notes on methodology and caveats

- Each expert report is based on the issue text, comment threads, and a fresh read of the code. Where an expert said "covered in current code," it was grep-verified. Where they said "duplicate," the referenced ticket was cross-checked.
- Two recurring edge cases: (a) `jsignpdf-pades` — the separate LTV experimentation repo — means some LTV work may be further along than this master branch reflects; (b) the Swing → JavaFX migration means several "GUI bug" tickets should be retested on the FX code path before implementation effort is spent.
- Priorities reflect project-maintainer perspective, not end-user urgency for a specific workflow. A user dependent on PAdES B-LTA would see #46 as P0 today regardless of our P0/P1 label.
- This plan omits estimates for administrative work (release notes, Flathub listing, website updates) — assume ~1 day per milestone for that.
