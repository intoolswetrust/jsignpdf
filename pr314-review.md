## PR Review: #314 — New JavaFX GUI with PDF preview and signing

### Overall Assessment

This is a well-structured, ambitious PR that adds a modern JavaFX GUI as the default interface while preserving the legacy Swing GUI as a fallback. The architecture (MVVM, background services, FXML+CSS separation) is clean and idiomatic. The bug fixes are solid and well-tested.

That said, I have a number of findings ranging from potential bugs to design concerns.

---

### Critical / Bugs

**2. `SignatureOverlay` — clicking anywhere replaces an existing rectangle**

`SignatureOverlay.java:130-137` — If the user clicks outside the existing rectangle and outside any handle, `dragMode = CREATE` fires and the old placement is silently destroyed. This is surprising UX — a misclick on the PDF background wipes out a carefully placed signature. Consider requiring a double-click or a modifier key to start a new rectangle, or showing a confirmation.

**4. `PdfRenderService` — `Thread.interrupted()` is a fragile workaround**

`PdfRenderService.java:34` — Clearing the interrupt flag before rendering works around lingering cancellation state, but it also swallows a legitimate interrupt if one arrives between `cancel()` and `createTask()`. This could mask issues. A comment explaining *why* this is needed (e.g., Pdf2Image uses blocking I/O that throws on interrupt) would help future maintainers.

---

### Medium / Design Issues

**6. `SigningOptionsViewModel.syncToOptions()` — passwords stored as `String`**

`SigningOptionsViewModel.java:31-33` — `ksPassword`, `keyPassword`, etc. are `StringProperty` fields. Passwords as `String` objects are interned and cannot be zeroed from memory. The existing Swing UI uses `char[]` via `JPasswordField`. The JavaFX `PasswordField` exposes `getText()` as `String`, so this is somewhat unavoidable, but worth a comment acknowledging the trade-off. Also, the `toCharArray()` conversion allocates a new `char[]` every `syncToOptions()` call — the previous array is never zeroed.

**7. `RecentFilesManager` — declared but never used**

`RecentFilesManager.java` is fully implemented but never referenced from any controller or the application. Dead code in a new feature PR — either wire it up (e.g., File > Recent Files submenu) or remove it.

**8. `FxResourceProvider` — `createStringBinding()` is never invalidated**

`FxResourceProvider.java:34-39` — The `StringBinding` has no observable dependencies, so it computes once and never updates if the locale changes at runtime. This is fine if locale is fixed at startup (current behavior), but the name "binding" is misleading. Consider returning a plain `String` or documenting the limitation.

---

### Minor / Suggestions

**10. `Signer.java` — `tmpOpts` may be null in the `FxLauncher.launch()` path**

`Signer.java:130` — When no CLI args are given, `tmpOpts` is `null` and `FxLauncher.launch(null)` is called. This works because `JSignPdfApp.start()` handles `null`, but it's fragile. Consider creating a `new BasicSignerOptions()` before passing.

**13. `SignaturePlacementViewModel.setRelX/setRelY` clamp to [0,1] but `setRelWidth/setRelHeight` only enforce min 0.02**

This means `relX + relWidth` can exceed 1.0, putting the signature rectangle partially outside the page. The overlay visually clips, but the PDF coordinates will be out of bounds. `toPdfCoordinates` should clamp, or setters should enforce `relX + relWidth <= 1.0`.

**14. Translation quality — all 19 locales have identical English values**

`messages_cs.properties`, `messages_de.properties`, etc. all contain the English text for the `jfx.gui.*` keys (e.g., Czech file has `jfx.gui.menu.file=File`). This means the UI shows English regardless of locale. The `FxTranslationsTest` verifies the FXML loads but doesn't check that translations differ from English. These should either be actually translated or clearly marked as TODOs.

**17. `MainWindow.fxml` — duplicate Sign menu item**

The "Signing" menu contains a `MenuItem` that duplicates the File > Sign entry (same text, same `onAction`, same accelerator `Shortcut+S`). Two menu items with the same accelerator is undefined behavior in JavaFX — the last one wins, but it's confusing. Either make the Signing menu more useful (e.g., add "Configure Signature", "Remove Placement") or drop the duplicate.

---

### What's Good

- Clean MVVM architecture with proper separation of concerns
- Reuse of existing `SignerLogic`, `BasicSignerOptions`, `Pdf2Image` — no changes to signing core
- Signature placement coordinate math (relative coords, PDF Y-axis inversion) is correct and well-tested via `fromPdfCoordinates`/`toPdfCoordinates` roundtrip
- The NPE bug fixes (`KeyStoreUtils.getPkInfo`, `SignerLogic.signFile`) are correct and have test coverage
- Drag-and-drop, session restore, and keyboard shortcuts are nice touches
- The FXML/controller split is clean and follows JavaFX best practices
- Smart use of `fx:include` for sub-panels with the naming convention for sub-controller injection

---

### Summary

The PR needs attention on:
1. **Must fix**: Thread safety of `BasicSignerOptions` shared between FX thread and signing thread (item 5)
2. **Must fix**: Inverted drag direction in `SignatureOverlay` (item 1)
3. **Should fix**: Log handler leak (item 3), accidental rectangle replacement (item 2), untranslated locale files (item 14)
4. **Consider**: Remove dead code (`RecentFilesManager`), remove duplicate menu accelerator, confirm-before-sign dialog
