# JSignPdf guide update plan — JavaFX UI rollout

## Goal

Restructure `website/docs/JSignPdf.adoc` so that:

- The **JavaFX UI** is the primary, first-class desktop experience throughout
  the guide. Descriptions, screenshots, and walkthroughs refer to JavaFX by
  default.
- The **Swing UI** keeps a small, clearly-labelled backward-compatibility
  subsection so existing users on older launchers aren't stranded.
- The **CLI** chapter stays where it is — it's already a separate
  interaction model and needs no UI-driven restructure.
- Everything that is UI-agnostic (keystores, certification level, hash
  algorithms, encryption, visible signatures, TSA, CRL/OCSP, proxy, hardware
  tokens, `conf.properties`, CLI, troubleshooting) stays in **one shared
  "Signing options" reference** instead of being repeated per UI.

The single-source-of-truth invariant is preserved: one `.adoc` file powers
both the HTML site and the Maven-built PDF.

## Target structure (after)

```
1. Introduction, license, history, support        (unchanged)
2. Prerequisites (Java, keystore)                  (unchanged)
3. Launching                                       (unchanged — mention JavaFX default)
4. Using the JavaFX UI                             (NEW, promoted to main chapter)
   4.1 Opening a PDF document
   4.2 Signature properties panel
   4.3 Visible signature placement
   4.4 Signing the document
   4.5 Preferences / settings panel
5. Signing options (reference)                     (renamed from "Advanced view"
   5.1 Keystore selection                          — now UI-agnostic)
   5.2 Key alias & key password
   5.3 Append signature / certification level
   5.4 Hash algorithms
   5.5 Encryption (passwords, certificate, rights)
   5.6 Visible signature options
       (page, corners, display, layers, texts/images)
   5.7 TSA — timestamps
   5.8 Certificate revocation (CRL, OCSP)
   5.9 Proxy settings
6. Classic Swing UI (backward compatibility)       (NEW — ~1 page, screenshot-
                                                    light, points readers at §5
                                                    for option semantics)
7. Using hardware tokens for signing               (unchanged)
8. Advanced application configuration              (unchanged)
9. Solving problems                                (unchanged)
10. Command line (batch mode)                      (unchanged)
11. Other command line tools                       (unchanged)
```

### Why this shape

- **One reference for options, two walkthroughs for UIs.** Readers reach the
  option they're looking for (e.g. "hash algorithms") from one place,
  regardless of which UI they use. When JavaFX adds a new option later, it
  goes in §5 once instead of in two parallel chapters.
- **Swing subsection, not appendix.** Keeping it in-line (as §6) rather than
  in an appendix makes its backward-compat status explicit and keeps the
  legacy walkthrough discoverable without inflating the TOC.
- **Launching chapter** needs one paragraph updated to say that the default
  launcher now opens the JavaFX UI, with a pointer to §6 for the Swing
  fallback command/flag.

## Content-moving checklist

Mapping from the current file (line numbers from the current `JSignPdf.adoc`):

| Current section                                   | Lines      | New home                                  |
|---------------------------------------------------|------------|-------------------------------------------|
| Launching                                         | 75–81      | §3, add JavaFX-default note               |
| Using JSignPdf – Simple / More detailed version   | 82–128     | **Rewrite** as §4 JavaFX walkthrough      |
| Advanced view (parent)                            | 130–135    | Drop header, contents split across §5     |
| Key alias / Key password / Append / Cert level    | 136–155    | §5.2 – §5.3 (UI-agnostic copy)            |
| Hash algorithms                                   | 156–161    | §5.4                                      |
| Encryption (passwords, certificate, rights)       | 162–181    | §5.5                                      |
| Visible signature (page, corners, preview, ...)   | 182–221    | §5.6                                      |
| TSA – timestamps                                  | 222–227    | §5.7                                      |
| Certificate revocation (CRL / OCSP)               | 228–247    | §5.8                                      |
| Proxy settings                                    | 248–251    | §5.9                                      |
| Using hardware tokens for signing                 | 252–296    | §7 (unchanged)                            |
| Advanced application configuration                | 297–323    | §8 (unchanged)                            |
| Solving problems                                  | 324–336    | §9 (unchanged)                            |
| Command line (batch mode) + exit codes + examples | 337–582    | §10 (unchanged)                           |
| Other command line tools / InstallCert            | 583–end    | §11 (unchanged)                           |

Edits that need writing from scratch:

1. **§4 JavaFX walkthrough** — 4–5 short subsections describing the
   document-centric flow (open a PDF → configure signature properties →
   place visible signature → sign). Each with one screenshot and a pointer
   to the relevant §5 subsection for option semantics. The JavaFX UI works
   with a single document at a time — emphasise this as the "load → sign →
   repeat" pattern (use the CLI for bulk/batch signing).
2. **§6 Classic Swing UI** — ~one screen of text. State that Swing is kept
   for backward compatibility, show how to launch it (flag / legacy
   launcher), and include one or two small screenshots of the
   simple/advanced view. For every option in the Swing dialog, link to the
   corresponding §5 subsection.
3. **Introduction / Launching paragraphs** — one sentence noting that
   the default UI changed to JavaFX in this release.

Edits that are plain moves with wording tweaks:

- Every "click the X button in the Advanced view dialog" phrasing gets
  neutralised to "set the X option" (or similar), so §5 reads as a
  reference regardless of which UI is driving it.
- `==== ` levels for the option sub-sections drop by one (from depth 4 to
  depth 3) once the "Advanced view" parent goes away.

## JavaFX screenshots

Stored in `website/docs/img/javafx/` (all 1123x806 except the panel crop).

### Available now

| File                                    | Shows                                                                                           | Used in   |
|-----------------------------------------|-------------------------------------------------------------------------------------------------|-----------|
| `javafx/main-window-empty.png`          | Empty main window — open/drop prompt, toolbar, collapsed sidebar panels, Output Console         | §4.1      |
| `javafx/document-loaded.png`            | PDF open ("SERVICE AGREEMENT") — document preview, sidebar, status bar                          | §4.1      |
| `javafx/signature-properties.png`       | Signature Properties panel expanded (hash, cert level, append, reason/location/contact)         | §4.2      |
| `javafx/visible-signature-placement.png`| Placing the visible-signature rectangle on the document; Signature Appearance panel on the left  | §4.3      |
| `javafx/signing-result.png`             | "Signing Complete" success dialog after signing                                                 | §4.4      |
| `javafx/visible-signature-panel.png`    | Close-up of Signature Appearance panel (render mode, text, images, font)                        | §5.6      |

### Deferred (capture later if needed)

| File                              | Shows                                                          | Used in        |
|-----------------------------------|----------------------------------------------------------------|----------------|
| `javafx/preferences.png`         | Preferences window (TSA, CRL/OCSP, proxy, passwords toggle)   | §4.5, §5.7–5.9 |
| `javafx/keystore-types.png`      | Keystore-type dropdown expanded                                | §5.1           |
| `javafx/cert-level.png`          | Certification-level dropdown                                   | §5.3           |
| `javafx/hash-algorithm.png`      | Hash-algorithm dropdown                                        | §5.4           |
| `javafx/encryption-rights.png`   | Encryption-rights panel                                        | §5.5           |

For this first pass the §5 reference sections will be written as text-only
(UI-agnostic). The deferred screenshots can be added in a follow-up without
any restructuring.

## Deferred / explicitly out of scope

- Migrating the `website/docs/img/*.png` Swing screenshots into a `swing/`
  subdirectory — orthogonal refactor, do it once the Swing subsection is
  stable.
- Adding a UI-toggle widget (JavaFX vs Swing tabs on the same page) —
  Hextra supports `tabs` shortcodes but AsciiDoc pages can't use
  Hugo shortcodes directly, and duplicating content in two tabs defeats
  the point of §5 being shared.
- Re-enabling search for the guide (`excludeSearch: true` removal). That
  depends on whether FlexSearch indexes the now-populated
  `Page.Fragments.Headings` cleanly — tracked separately in the README's
  "What you'll want to tweak" section.
