## PR Review: #314 — New JavaFX GUI with PDF preview and signing


### Minor / Suggestions

**13. `SignaturePlacementViewModel.setRelX/setRelY` clamp to [0,1] but `setRelWidth/setRelHeight` only enforce min 0.02**

This means `relX + relWidth` can exceed 1.0, putting the signature rectangle partially outside the page. The overlay visually clips, but the PDF coordinates will be out of bounds. `toPdfCoordinates` should clamp, or setters should enforce `relX + relWidth <= 1.0`.

**14. Translation quality — all 19 locales have identical English values**

`messages_cs.properties`, `messages_de.properties`, etc. all contain the English text for the `jfx.gui.*` keys (e.g., Czech file has `jfx.gui.menu.file=File`). This means the UI shows English regardless of locale. The `FxTranslationsTest` verifies the FXML loads but doesn't check that translations differ from English. These should either be actually translated or clearly marked as TODOs.

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
