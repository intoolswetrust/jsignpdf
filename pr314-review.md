## PR Review: #314 — New JavaFX GUI with PDF preview and signing

### Overall Assessment

This is a well-structured, ambitious PR that adds a modern JavaFX GUI as the default interface while preserving the legacy Swing GUI as a fallback. The architecture (MVVM, background services, FXML+CSS separation) is clean and idiomatic. The bug fixes are solid and well-tested.

That said, I have a number of findings ranging from potential bugs to design concerns.

---

### Critical / Bugs

**1. `SignatureOverlay` — dragging upward/leftward creates an inverted rectangle**

`SignatureOverlay.java:151-153` — In `CREATE` mode, `relWidth` and `relHeight` are computed as `(mx - dragStartX) / w`. If the user drags up-left from the start point, these become negative, but `Math.max(0.02, ...)` clamps them to a tiny positive value instead of flipping the origin. The rectangle snaps to a 2%-wide sliver pinned at the start point rather than growing in the opposite direction. Should normalize like `restoreLastSession()` does (use `Math.min`/`Math.max` for both origin and extent).

**2. `SignatureOverlay` — clicking anywhere replaces an existing rectangle**

`SignatureOverlay.java:130-137` — If the user clicks outside the existing rectangle and outside any handle, `dragMode = CREATE` fires and the old placement is silently destroyed. This is surprising UX — a misclick on the PDF background wipes out a carefully placed signature. Consider requiring a double-click or a modifier key to start a new rectangle, or showing a confirmation.

**3. `OutputConsoleController` — log handler is never removed**

`OutputConsoleController.java:30` — `LOGGER.addHandler(logHandler)` is called in `initialize()` but there is no cleanup. If the FXML is reloaded or the window is re-created, handlers accumulate. The handler holds a reference to the `TextArea`, preventing GC. Add a `dispose()` method called from `storeAndCleanup()` that calls `LOGGER.removeHandler(logHandler)`.

**4. `PdfRenderService` — `Thread.interrupted()` is a fragile workaround**

`PdfRenderService.java:34` — Clearing the interrupt flag before rendering works around lingering cancellation state, but it also swallows a legitimate interrupt if one arrives between `cancel()` and `createTask()`. This could mask issues. A comment explaining *why* this is needed (e.g., Pdf2Image uses blocking I/O that throws on interrupt) would help future maintainers.

---

### Medium / Design Issues

**5. `MainWindowController` — `options` is mutable shared state across threads**

`BasicSignerOptions` is mutated on the FX Application Thread (via `syncToOptions()`) and then passed to `SigningService` which reads it on a background thread. `BasicSignerOptions` is not thread-safe (plain fields, no synchronization). The `SigningService` captures a reference to the *same* object. If the user changes a setting in the UI while signing is in progress, the background thread sees a torn state. Create a defensive copy before handing to `SigningService`.

**6. `SigningOptionsViewModel.syncToOptions()` — passwords stored as `String`**

`SigningOptionsViewModel.java:31-33` — `ksPassword`, `keyPassword`, etc. are `StringProperty` fields. Passwords as `String` objects are interned and cannot be zeroed from memory. The existing Swing UI uses `char[]` via `JPasswordField`. The JavaFX `PasswordField` exposes `getText()` as `String`, so this is somewhat unavoidable, but worth a comment acknowledging the trade-off. Also, the `toCharArray()` conversion allocates a new `char[]` every `syncToOptions()` call — the previous array is never zeroed.

**7. `RecentFilesManager` — declared but never used**

`RecentFilesManager.java` is fully implemented but never referenced from any controller or the application. Dead code in a new feature PR — either wire it up (e.g., File > Recent Files submenu) or remove it.

**8. `FxResourceProvider` — `createStringBinding()` is never invalidated**

`FxResourceProvider.java:34-39` — The `StringBinding` has no observable dependencies, so it computes once and never updates if the locale changes at runtime. This is fine if locale is fixed at startup (current behavior), but the name "binding" is misleading. Consider returning a plain `String` or documenting the limitation.

**9. No confirmation dialog before signing**

`MainWindowController.onSign()` immediately starts signing with no user confirmation. For a destructive operation (creates a new file, may overwrite if the user sets the same output), a "Do you want to sign with these settings?" dialog would be safer. The Swing GUI similarly lacks this, but since this is a greenfield rewrite, it's an opportunity to improve UX.

---

### Minor / Suggestions

**10. `Signer.java` — `tmpOpts` may be null in the `FxLauncher.launch()` path**

`Signer.java:130` — When no CLI args are given, `tmpOpts` is `null` and `FxLauncher.launch(null)` is called. This works because `JSignPdfApp.start()` handles `null`, but it's fragile. Consider creating a `new BasicSignerOptions()` before passing.

**11. `MainWindowController` — `openDocument()` creates a new `BasicSignerOptions` on every null check**

`MainWindowController.java:475-478` — If `options` is null, a fresh `BasicSignerOptions` is created and `loadOptions()` is called. This shouldn't normally happen (initFromOptions is always called), but if it does, the load + sync is a side effect buried in `openDocument`. Would be cleaner to fail-fast or initialize earlier.

**12. Zoom combo accepts arbitrary text but silently ignores non-numeric input**

`MainWindowController.java:261-267` — The editable `ComboBox<String>` catches `NumberFormatException` silently. User types "abc" and nothing happens with no feedback. A minor UX annoyance.

**13. `SignaturePlacementViewModel.setRelX/setRelY` clamp to [0,1] but `setRelWidth/setRelHeight` only enforce min 0.02**

This means `relX + relWidth` can exceed 1.0, putting the signature rectangle partially outside the page. The overlay visually clips, but the PDF coordinates will be out of bounds. `toPdfCoordinates` should clamp, or setters should enforce `relX + relWidth <= 1.0`.

**14. Translation quality — all 19 locales have identical English values**

`messages_cs.properties`, `messages_de.properties`, etc. all contain the English text for the `jfx.gui.*` keys (e.g., Czech file has `jfx.gui.menu.file=File`). This means the UI shows English regardless of locale. The `FxTranslationsTest` verifies the FXML loads but doesn't check that translations differ from English. These should either be actually translated or clearly marked as TODOs.

**15. `jsignpdf/pom.xml` — `openjfx-monocle` uses a very old version**

`jdk-12.0.1+2` is from 2019. Consider using a more recent version compatible with Java 17+ (the OpenJFX version used is 17.0.15).

**16. `AGENTS.md` and `design-doc/` — should these be in the PR?**

`AGENTS.md` and `design-doc/jsignpdf-gui-reimplementation-plan.md` are process artifacts, not runtime code. Consider whether these belong in the repository long-term or should be in a wiki/issue instead.

**17. `MainWindow.fxml` — duplicate Sign menu item**

The "Signing" menu contains a `MenuItem` that duplicates the File > Sign entry (same text, same `onAction`, same accelerator `Shortcut+S`). Two menu items with the same accelerator is undefined behavior in JavaFX — the last one wins, but it's confusing. Either make the Signing menu more useful (e.g., add "Configure Signature", "Remove Placement") or drop the duplicate.

**18. CSS — hardcoded colors, no dark mode support**

The stylesheet uses hardcoded light colors (`#ffffff`, `#f0f0f0`, `#e0e0e0`). JavaFX supports `-fx-base` and derived colors for automatic theming. Using derived colors would make future dark mode support trivial.

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
