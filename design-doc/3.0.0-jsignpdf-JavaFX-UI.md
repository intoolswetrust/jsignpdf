# JSignPdf JavaFX GUI Redesign - Implementation Plan

## Context

JSignPdf's current Swing GUI is form-centric: users fill in text fields and click "Sign It." Modern PDF tools (Adobe Acrobat, Foxit) are document-centric: users open a PDF first, then interact with it visually. This redesign replaces the Swing UI with a JavaFX PDF viewer-style application where the document is the focal point, signature placement is drag-on-preview, and settings live in a collapsible side panel. The business logic (`SignerLogic`, `BasicSignerOptions`) stays untouched.

---

## Main Window Layout

```
+------------------------------------------------------------------+
| Menu: File | View | Signing | Help                               |
+------------------------------------------------------------------+
| Toolbar: [Open] | [ZoomIn][ZoomOut][Fit] | [<][Page X/Y][>] | [Place Sig] | [SIGN] |
+------------------------------------------------------------------+
|  Side Panel (collapsible)    |   PDF Preview Area                |
|  +------------------------+  |   +----------------------------+  |
|  | > Certificate          |  |   |                            |  |
|  |   Keystore type: [___] |  |   |   Rendered PDF page        |  |
|  |   File: [____] [Browse]|  |   |                            |  |
|  |   Password: [____]     |  |   |   [signature rectangle]    |  |
|  |   [Load Keys]          |  |   |     (draggable/resizable)  |  |
|  |   Alias: [_________]   |  |   |                            |  |
|  +------------------------+  |   +----------------------------+  |
|  | > Signature Appearance |  |                                   |
|  | > Timestamp & Validation| |                                   |
|  | > Encryption & Rights  |  |                                   |
+------------------------------------------------------------------+
| Status: document.pdf - Page 3/12 - 1 signature   [====] Ready    |
+------------------------------------------------------------------+
```

## UX Improvements (prioritized)

**Core (must-have for the new paradigm):**
1. **Document-centric workflow** - open PDF first, then configure and sign
2. **Signature placement directly on PDF preview** - click+drag rectangle, no coordinate dialogs
3. **Drag-to-resize** signature rectangle with corner/edge handles
4. **Page navigation** - toolbar buttons, keyboard (PgUp/PgDn), page number field
5. **Zoom controls** - fit page, fit width, zoom in/out, percentage combo
6. **Drag-and-drop** PDF files onto the window
7. **Status bar** - filename, page count, existing signatures count, signing progress
8. **Progress indication** - ProgressBar in status bar during signing

**Should-have:**
9. **Signature preview** - live rendering of what the stamp will look like
10. **Existing signatures panel** - list signatures already on the document
11. **Keyboard shortcuts** - Ctrl+O (open), Ctrl+S (sign), Ctrl+Z (undo placement), +/- (zoom)
12. **Undo signature placement** before signing

**Nice-to-have (future):**
13. **Dark mode** via CSS theme switching
14. **Recent files** in File menu
15. **Page thumbnails** sidebar

---

## Architecture: MVVM

```
View (FXML + Controllers)  <-->  ViewModel (JavaFX Properties)  <-->  Model (BasicSignerOptions)
                                       |
                                  Service layer (javafx.concurrent.Service)
                                       |
                                  SignerLogic, KeyStoreUtils, Pdf2Image (unchanged)
```

### Package Structure

```
net.sf.jsignpdf.fx/
  JSignPdfApp.java              # extends Application, sets up primary Stage
  FxLauncher.java               # static launch() called from Signer.main()
net.sf.jsignpdf.fx.view/
  MainWindowController.java     # MainWindow.fxml controller
  CertificateSettingsController.java
  SignatureSettingsController.java
  TsaSettingsController.java
  EncryptionSettingsController.java
  OutputConsoleController.java
net.sf.jsignpdf.fx.viewmodel/
  DocumentViewModel.java        # loaded PDF state, page, zoom
  SigningOptionsViewModel.java   # wraps BasicSignerOptions with FX properties
  SignaturePlacementViewModel.java  # rectangle position, drag state
  AppStateViewModel.java        # app-level: doc loaded?, signing in progress?, recent files
net.sf.jsignpdf.fx.service/
  PdfRenderService.java         # wraps Pdf2Image -> JavaFX Image (background thread)
  SigningService.java           # wraps SignerLogic.signFile() (background thread)
  KeyStoreService.java          # wraps KeyStoreUtils.getKeyAliases() (background thread)
net.sf.jsignpdf.fx.control/
  PdfPageView.java              # custom Region displaying rendered PDF page
  SignatureOverlay.java         # transparent overlay for click-drag signature rectangle
net.sf.jsignpdf.fx.util/
  FxResourceProvider.java       # adapts ResourceProvider for JavaFX StringBindings
  SwingFxImageConverter.java    # BufferedImage <-> javafx.scene.image.Image
  RecentFilesManager.java       # persists recent file list
```

### FXML Files (in `src/main/resources/net/sf/jsignpdf/fx/view/`)
- `MainWindow.fxml` - BorderPane with menu, toolbar, SplitPane, status bar
- `CertificateSettings.fxml` - keystore type, file, password, alias
- `SignatureSettings.fxml` - render mode, L2/L4 text, images, font size
- `TsaSettings.fxml` - TSA, OCSP, CRL, proxy
- `EncryptionSettings.fxml` - encryption type, passwords, rights

### CSS (in `src/main/resources/net/sf/jsignpdf/fx/styles/`)
- `jsignpdf.css` - light theme
- `jsignpdf-dark.css` - dark theme (future)

### Key Design Decisions

**Why MVVM**: JavaFX property bindings are a natural fit. ViewModels are testable without UI.

**Why keep BasicSignerOptions unchanged**: It's the contract with `SignerLogic` and CLI mode. `SigningOptionsViewModel` is a JavaFX adapter that syncs bidirectionally via `syncToOptions()` / `syncFromOptions()`.

**Why `javafx-swing` dependency**: Reuses existing `Pdf2Image` (returns `BufferedImage`) via `SwingFXUtils.toFXImage()`. Avoids rewriting PDF rendering backends.

**Why parallel Swing/JavaFX**: Risk mitigation. Old UI available via `-Djsignpdf.swing=true` flag until JavaFX is proven stable.

---

## Migration Strategy

In `Signer.java`, the GUI launch becomes:

```java
if (showGui) {
    if (Boolean.getBoolean("jsignpdf.swing")) {
        // Legacy Swing (preserved for fallback)
        SignPdfForm tmpForm = new SignPdfForm(...);
        ...
    } else {
        // New JavaFX (default)
        FxLauncher.launch(tmpOpts);
    }
}
```

Swing code is NOT deleted until the JavaFX UI ships in at least one stable release.

---

## Maven Changes

### Dependencies (add to `jsignpdf/pom.xml`)

```xml
<!-- JavaFX (OpenJFX 17 LTS - runs on Java 11+) -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>${openjfx.version}</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>${openjfx.version}</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-swing</artifactId>
    <version>${openjfx.version}</version>
</dependency>

<!-- TestFX (test scope) -->
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-core</artifactId>
    <version>${testfx.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-junit</artifactId>
    <version>${testfx.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>openjfx-monocle</artifactId>
    <version>jdk-12.0.1+2</version>
    <scope>test</scope>
</dependency>
```

### Properties (add to root `pom.xml`)
```xml
<openjfx.version>17.0.2</openjfx.version>
<testfx.version>4.0.18</testfx.version>
```

### Plugin (add to `jsignpdf/pom.xml`)
```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>net.sf.jsignpdf.Signer</mainClass>
    </configuration>
</plugin>
```

### Surefire argLine for headless TestFX
```xml
-Dtestfx.robot=glass -Dglass.platform=Monocle -Dmonocle.platform=Headless
```

---

## Testing Strategy

### Unit Tests (plain JUnit, no FX runtime needed)
- `SigningOptionsViewModelTest` - verify `syncToOptions()` / `syncFromOptions()` round-trips all fields correctly
- `SignaturePlacementViewModelTest` - verify coordinate calculations, bounds clamping
- `DocumentViewModelTest` - verify page navigation logic, zoom level calculations

### UI Integration Tests (TestFX)
- `MainWindowTest` - app starts, menu bar renders, toolbar present, status bar shows
- `DocumentLoadTest` - open test PDF, page count in status bar, page navigation works
- `SignaturePlacementTest` - click+drag on preview, verify rectangle coordinates on ViewModel
- `SigningWorkflowTest` - full end-to-end: open PDF, load test keystore, place sig, click Sign, verify output file has valid signature (reuses existing `PdfSignatureValidator`)

### CI Configuration
- TestFX runs headlessly via Monocle (no display server needed)
- Existing `signing/` JUnit tests remain unchanged and keep running

---

## Phased Implementation Order

### Phase 1: Foundation
- Add OpenJFX + TestFX dependencies to pom.xml
- Create `JSignPdfApp`, `FxLauncher`, parallel launch in `Signer.java`
- Create `MainWindow.fxml` with BorderPane skeleton: menu bar, toolbar (placeholder buttons), center StackPane with "Drop PDF here" label, status bar
- Create `MainWindowController` with stub handlers
- Create `jsignpdf.css` base stylesheet
- **Verify**: JavaFX app starts and shows the empty window

### Phase 2: PDF Viewing
- Create `PdfPageView` custom control
- Create `DocumentViewModel` (file, page count, current page, zoom level)
- Create `PdfRenderService` wrapping `Pdf2Image` + `SwingFXUtils`
- Implement File > Open (FileChooser) and drag-and-drop
- Implement page navigation (toolbar, PgUp/PgDn)
- Implement zoom (fit page, fit width, +/-)
- Show document info in status bar
- **Verify**: Can open a PDF, navigate pages, zoom

### Phase 3: Certificate Configuration
- Create `SigningOptionsViewModel` wrapping `BasicSignerOptions`
- Create `CertificateSettings.fxml` + controller
- Bind keystore type, file, password, alias to ViewModel
- Implement "Load Keys" via `KeyStoreService`
- Wire into MainWindow via `<fx:include>` in the side panel Accordion
- **Verify**: Can select keystore, load keys, pick alias

### Phase 4: Signature Placement
- Create `SignatureOverlay` with mouse drag handling + resize handles
- Create `SignaturePlacementViewModel`
- Implement "Place Signature" toggle in toolbar
- Show semi-transparent rectangle on PDF with drag/resize
- Coordinate display in side panel
- Undo placement (Ctrl+Z)
- **Verify**: Can place, move, resize, undo signature rectangle

### Phase 5: Signing Workflow
- Create `SigningService` wrapping `SignerLogic`
- Wire "Sign" button: sync ViewModel -> options -> SigningService
- ProgressBar in status bar
- Success/failure notification
- Output console pane for log viewing
- **Verify**: Full sign workflow works, output PDF has valid signature

### Phase 6: Advanced Settings
- Create `SignatureSettings.fxml` - render mode, L2/L4 text, images, font size, Acro6 layers
- Create `TsaSettings.fxml` - TSA, OCSP, CRL, proxy
- Create `EncryptionSettings.fxml` - encryption, passwords, rights
- Collapsed by default, "Show Advanced" toggle expands them
- **Verify**: All settings from original UI are accessible

### Phase 7: Polish & Testing
- Write TestFX tests (MainWindowTest, DocumentLoadTest, SignaturePlacementTest, SigningWorkflowTest)
- Write ViewModel unit tests
- Signature preview (live render of stamp appearance)
- Existing signatures panel
- Keyboard shortcuts
- Final i18n pass - add keys for new UI elements
- **Verify**: All tests pass, headless CI works

### Phase 8: Cleanup (future release)
- Remove Swing classes (SignPdfForm, VisibleSignatureDialog, TsaDialog, etc.)
- Remove `-Djsignpdf.swing` fallback flag
- Update distribution scripts

---

## Critical Files

| File | Role | Change |
|------|------|--------|
| `jsignpdf/pom.xml` | Module build | Add OpenJFX, TestFX deps + plugins |
| `pom.xml` (root) | Parent build | Add version properties |
| `Signer.java` | Entry point | Add JavaFX launch path |
| `BasicSignerOptions.java` | Model | **No change** |
| `SignerLogic.java` | Signing logic | **No change** |
| `preview/Pdf2Image.java` | PDF rendering | **No change** (reused by PdfRenderService) |
| `preview/SelectionImage.java` | Rectangle selection | **No change** (reference for SignatureOverlay) |
| `utils/ResourceProvider.java` | i18n | **No change** (wrapped by FxResourceProvider) |
| All files under `net.sf.jsignpdf.fx/` | New JavaFX GUI | **All new** |
