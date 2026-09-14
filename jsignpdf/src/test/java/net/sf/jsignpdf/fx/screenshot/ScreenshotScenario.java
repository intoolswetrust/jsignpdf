package net.sf.jsignpdf.fx.screenshot;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.preferences.PreferencesController;
import net.sf.jsignpdf.fx.view.MainWindowController;
import net.sf.jsignpdf.fx.viewmodel.SignaturePlacementViewModel;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.utils.SupportedLanguages;
import net.sf.jsignpdf.utils.UiLocale;

/**
 * Builds the JavaFX main window and walks it through the states the documentation screenshots show, handing
 * each one to a {@link ShotSink}. Everything that knows about the application - which panel is open, where
 * the signature rectangle sits, what has been signed - lives here, so the two capture backends
 * ({@link FxScreenshotGenerator} off-screen, {@link DecoratedScreenshotRunner} on a real X display) stay
 * thin and produce the same set of images.
 *
 * <p>
 * Fixtures come from {@code distribution/demo/}, staged into a neutral working directory: their paths show up
 * in the Certificate panel and the status bar, so they must not carry whoever ran the build.
 * </p>
 */
public final class ScreenshotScenario {

    /**
     * Receives one finished state. Implementations decide how the pixels are obtained and where they go.
     */
    public interface ShotSink {

        /**
         * Captures {@code node} - either the scene root (a whole window) or a region of it.
         *
         * @param path where the image goes, as {@code guide/...} (the user guide's image directory) or
         *        {@code site/...} (the website's), e.g. {@code guide/document-loaded.png}
         * @param node the node whose bounds delimit the shot
         */
        void shot(String path, Node node) throws Exception;

        /**
         * Captures the window while the signing-complete dialog is up. The dialog is already showing; it is a
         * separate stage, so an off-screen backend has to composite it while a screen-grabbing one gets it for
         * free.
         *
         * @param path where the image goes, rooted as in {@link #shot(String, Node)}
         * @param window the scene root of the main window
         * @param dialog the alert on top of it
         */
        void dialogShot(String path, Node window, Alert dialog) throws Exception;

        /**
         * Called once every shot has been taken, e.g. to mirror images into the website tree.
         */
        void finished() throws Exception;
    }

    public static final int SCENE_WIDTH = Integer.getInteger("jsignpdf.screenshot.width", 1100);
    public static final int SCENE_HEIGHT = Integer.getInteger("jsignpdf.screenshot.height", 806);

    static final long FX_TIMEOUT_SECONDS = 120;

    private static Path workDir;
    private static Path configDir;
    private static Path demoPdf;
    private static Path demoKeystore;
    private static Path demoSigImage;

    private static Scene scene;
    private static Stage stage;
    private static MainWindowController controller;
    private static boolean decorated;

    private ScreenshotScenario() {
    }

    // --- Lifecycle ---

    /**
     * Stages the demo assets, wipes the config directory and starts the JavaFX toolkit. The config directory
     * has to go: an empty one is seeded from the legacy {@code ~/.JSignPdf} file, which would drag the
     * developer's own keystore, recent files and encryption mode into the images.
     */
    public static void prepare() throws Exception {
        applyUiLanguage("en");
        resolveDirectories();
        deleteRecursively(configDir);
        stageDemoFiles();
        startToolkit();
    }

    /**
     * Prepares a JVM whose only job is one image of the translation gallery. It must have been started in that
     * language ({@code -Duser.language}/{@code -Duser.country}): JavaFX asks fontconfig for its fallback chain
     * once per JVM, for the locale current when the font system starts, and orders it by that language - which is
     * what a user starting the application in that language gets, and the point of the gallery.
     *
     * <p>
     * Even then, OpenJFX 21 leaves CJK fonts out of the chain entirely, so ja / zh-CN / zh-TW render as empty
     * boxes; OpenJFX 23 picks the CJK font. Run the gallery with {@code -Dopenjfx.version=23.0.2} to get
     * readable images of those translations.
     * </p>
     *
     * @param tag a BCP-47 tag from {@link SupportedLanguages#tags()}
     */
    public static void prepareGallery(String tag) throws Exception {
        Locale requested = Locale.forLanguageTag(tag);
        if (!requested.getLanguage().equals(Locale.getDefault().getLanguage())) {
            throw new IllegalStateException("The gallery shot for '" + tag + "' needs a JVM started with"
                    + " -Duser.language=" + requested.getLanguage() + " (this one runs in " + Locale.getDefault()
                    + "); JavaFX would otherwise have no fallback fonts for the script");
        }
        configDir = requiredConfigDir();
        deleteRecursively(configDir);
        applyUiLanguage(tag);
        startToolkit();
    }

    /**
     * JVM options that start a gallery JVM in the language of {@code tag}.
     */
    public static List<String> jvmLocaleOptions(String tag) {
        Locale locale = Locale.forLanguageTag(tag);
        List<String> options = new ArrayList<>();
        options.add("-Duser.language=" + locale.getLanguage());
        if (!locale.getCountry().isEmpty()) {
            options.add("-Duser.country=" + locale.getCountry());
        }
        return options;
    }

    /**
     * Creates and shows the main window.
     *
     * @param decorated {@code true} for a normal, window-manager-decorated stage (screen capture),
     *        {@code false} for an undecorated one (off-screen snapshots, where chrome does not exist anyway)
     */
    public static void buildWindow(boolean wantDecorations) throws Exception {
        decorated = wantDecorations;
        runFx(() -> {
            ResourceBundle bundle = UiLocale.bundle();
            FXMLLoader loader = new FXMLLoader(
                    ScreenshotScenario.class.getResource("/net/sf/jsignpdf/fx/view/MainWindow.fxml"), bundle);
            Parent root;
            try {
                root = loader.load();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load MainWindow.fxml", e);
            }
            controller = loader.getController();

            stage = new Stage();
            if (!decorated) {
                stage.initStyle(StageStyle.UNDECORATED);
            }
            controller.setStage(stage);

            BasicSignerOptions options = new BasicSignerOptions();
            options.loadOptions();
            if (workDir != null) {
                options.setOutPath(workDir.toString());
            }
            controller.initFromOptions(options);

            scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            scene.getStylesheets().add(ScreenshotScenario.class
                    .getResource("/net/sf/jsignpdf/fx/styles/jsignpdf.css").toExternalForm());
            stage.setTitle("JSignPdf " + Constants.VERSION);
            stage.setScene(scene);
            stage.show();
        });
        settle();
    }

    public static Scene scene() {
        return scene;
    }

    // --- The state walk ---

    /**
     * Drives the UI through every documented state, handing each to {@code sink} in the order the guide uses.
     */
    public static void run(ShotSink sink) throws Exception {
        sink.shot("guide/main-window-empty.png", scene.getRoot());

        openDocument(demoPdf.toFile());
        sink.shot("guide/document-loaded.png", scene.getRoot());

        signatureProperties();
        sink.shot("guide/signature-properties.png", scene.getRoot());
        // Back to 1:1 for the shots that follow, which are about the signature rectangle rather than the page.
        runFx(() -> controller.getDocumentViewModel().setZoomLevel(1.0));

        visibleSignature();
        sink.shot("guide/visible-signature-placement.png", scene.getRoot());
        sink.shot("guide/visible-signature-panel.png", lookup("#sidePanelAccordion"));

        sign();
        // The guide points at the output path in this panel right after the signing screenshot, and it keeps
        // the image from being a near-duplicate of the placement one.
        expandPane("jfx.gui.panel.signatureProperties");
        sink.shot("guide/signing-result.png", scene.getRoot());

        Alert complete = showSigningCompleteAlert();
        sink.dialogShot("site/jsignpdf-javafx-signed.png", scene.getRoot(), complete);
        runFx(complete::close);

        preferences(sink);

        sink.finished();
    }

    /**
     * Captures the Preferences dialog on its General tab (the one it opens on).
     *
     * <p>
     * {@link PreferencesController#show(Stage)} ends in {@code showAndWait()}, so it is posted to the FX thread
     * without waiting for it: the nested event loop it enters still runs everything queued afterwards, which is
     * what lets the shot be taken and the dialog closed again.
     * </p>
     */
    private static void preferences(ShotSink sink) throws Exception {
        final String title = Constants.RES.get("jfx.gui.preferences.title");
        Platform.runLater(() -> PreferencesController.show(stage));
        waitUntil("the Preferences dialog to open", () -> findWindow(title) != null);
        settle();

        AtomicReference<Stage> dialog = new AtomicReference<>();
        runFx(() -> dialog.set(findWindow(title)));
        sink.shot("guide/preferences-general.png", dialog.get().getScene().getRoot());

        runFx(() -> dialog.get().hide());
        settle();
    }

    /**
     * The one shot a gallery JVM exists for; see {@link #prepareGallery(String)}.
     */
    public static void galleryShot(ShotSink sink, String tag) throws Exception {
        sink.shot("site/locales/main-window-" + tag + ".png", scene.getRoot());
    }

    private static void applyUiLanguage(String tag) {
        UiLocale.init(new String[] { "-o", "ui.language=" + tag });
    }

    /**
     * The translations to include in the locale gallery: {@code -Djsignpdf.screenshot.locales=all} for every
     * bundled one, a comma-separated list of BCP-47 tags for a subset, and nothing at all by default - the
     * gallery is twenty extra images that do not need refreshing with every UI change. Each one is taken in a
     * JVM of its own; see {@link #prepareGallery(String)}.
     */
    public static List<String> requestedLocales() {
        String value = System.getProperty("jsignpdf.screenshot.locales", "").trim();
        if (value.isEmpty()) {
            return List.of();
        }
        if ("all".equalsIgnoreCase(value)) {
            return SupportedLanguages.tags();
        }
        List<String> tags = new ArrayList<>();
        for (String tag : value.split(",")) {
            String trimmed = tag.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!SupportedLanguages.tags().contains(trimmed)) {
                throw new IllegalArgumentException("No bundled translation for '" + trimmed + "'; known tags: "
                        + SupportedLanguages.tags());
            }
            tags.add(trimmed);
        }
        return tags;
    }

    /**
     * The showing stage with this exact title, or {@code null}. Must run on the FX thread.
     */
    private static Stage findWindow(String title) {
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage showing && window.isShowing() && title.equals(showing.getTitle())) {
                return showing;
            }
        }
        return null;
    }

    private static void signatureProperties() throws Exception {
        SigningOptionsViewModel signing = controller.getSigningOptionsViewModel();
        runFx(() -> {
            signing.signerNameProperty().set("John Smith");
            signing.reasonProperty().set("Contract execution on behalf of Acme Consulting Group");
            signing.locationProperty().set("San Francisco");
            signing.contactProperty().set("jsmith@example.com");
            signing.hashAlgorithmProperty().set(HashAlgorithm.SHA256);
            signing.certLevelProperty().set(CertificationLevel.NOT_CERTIFIED);
        });
        expandPane("jfx.gui.panel.signatureProperties");
        settle();
        zoomToFit();
    }

    private static void visibleSignature() throws Exception {
        SigningOptionsViewModel signing = controller.getSigningOptionsViewModel();
        SignaturePlacementViewModel placement = controller.getPlacementViewModel();

        configureKeystore();
        Image previousPage = controller.getDocumentViewModel().getCurrentPageImage();
        runFx(() -> controller.getDocumentViewModel().setCurrentPage(2));
        waitForRenderedPage(previousPage);

        runFx(() -> {
            signing.bgImgPathProperty().set(demoSigImage.toString());
            signing.visibleProperty().set(true);
            // Over the provider's signature line on page 2 of the demo agreement, page-relative.
            placement.setRelX(0.12);
            placement.setRelY(0.675);
            placement.setRelWidth(0.31);
            placement.setRelHeight(0.07);
            placement.setPlaced(true);
        });
        expandPane("jfx.gui.panel.signatureAppearance");
        scrollPreviewTo(placement.getRelY() + placement.getRelHeight() / 2);
    }

    private static void configureKeystore() throws Exception {
        SigningOptionsViewModel signing = controller.getSigningOptionsViewModel();
        runFx(() -> {
            signing.ksTypeProperty().set("PKCS12");
            signing.ksFileProperty().set(demoKeystore.toString());
            signing.ksPasswordProperty().set("123456");
            signing.keyAliasProperty().set("jsmith");
            signing.keyPasswordProperty().set("123456");
        });
    }

    /**
     * Recreates the alert {@code MainWindowController.showAlert} pops after a successful run.
     */
    private static Alert showSigningCompleteAlert() throws Exception {
        AtomicReference<Alert> alertRef = new AtomicReference<>();
        runFx(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Constants.RES.get("jfx.gui.dialog.signingComplete.title"));
            alert.setHeaderText(null);
            alert.setContentText(Constants.RES.get("jfx.gui.dialog.signingComplete.text"));
            alert.initOwner(scene.getWindow());
            alert.show();
            alertRef.set(alert);
        });
        settle();
        return alertRef.get();
    }

    /**
     * Opens a document the way the Open action does. The controller method is private because nothing in the
     * application calls it from outside; driving it reflectively keeps every side effect (preview render,
     * status bar, recent files, console log line) exactly as a user would see them.
     */
    private static void openDocument(File pdf) throws Exception {
        Method openDocument = MainWindowController.class.getDeclaredMethod("openDocument", File.class);
        openDocument.setAccessible(true);
        runFx(() -> {
            try {
                openDocument.invoke(controller, pdf);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("openDocument(File) failed - has the controller changed?", e);
            }
        });
        waitForRenderedPage(null);
    }

    /**
     * Runs a real signing pass. The controller's own success handler pops a modal alert, which would block the
     * FX thread forever off-screen, so it is swapped for one that just counts down; the scenario shows the same
     * dialog later, when a shot actually needs it.
     */
    private static void sign() throws Exception {
        Field serviceField = MainWindowController.class.getDeclaredField("signingService");
        serviceField.setAccessible(true);
        Service<?> signingService = (Service<?>) serviceField.get(controller);

        CountDownLatch done = new CountDownLatch(1);
        Method onSign = MainWindowController.class.getDeclaredMethod("onSign");
        onSign.setAccessible(true);

        runFx(() -> {
            signingService.setOnSucceeded(e -> done.countDown());
            signingService.setOnFailed(e -> done.countDown());
            try {
                onSign.invoke(controller);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("onSign() failed - has the controller changed?", e);
            }
        });

        if (!done.await(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Signing did not finish within " + FX_TIMEOUT_SECONDS + "s");
        }

        runFx(() -> {
            ((ProgressBar) lookup("#progressBar")).setVisible(false);
            ((Label) lookup("#lblStatus")).setText(Constants.RES.get("jfx.gui.status.signingOk"));
        });
    }

    /**
     * Applies the toolbar's Fit action. Zoom only rescales the rendered page, so no re-render follows.
     */
    private static void zoomToFit() throws Exception {
        Method onZoomFit = MainWindowController.class.getDeclaredMethod("onZoomFit");
        onZoomFit.setAccessible(true);
        runFx(() -> {
            try {
                onZoomFit.invoke(controller);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("onZoomFit() failed - has the controller changed?", e);
            }
        });
    }

    /**
     * Scrolls the preview so the given page-relative vertical position sits in the middle of the viewport.
     */
    private static void scrollPreviewTo(double relY) throws Exception {
        runFx(() -> {
            ScrollPane preview = (ScrollPane) lookup("#scrollPane");
            double contentHeight = preview.getContent().getBoundsInLocal().getHeight();
            double viewportHeight = preview.getViewportBounds().getHeight();
            if (contentHeight <= viewportHeight) {
                return;
            }
            double value = (relY * contentHeight - viewportHeight / 2) / (contentHeight - viewportHeight);
            preview.setVvalue(Math.max(0, Math.min(1, value)));
        });
    }

    private static void expandPane(String titleKey) throws Exception {
        String title = Constants.RES.get(titleKey);
        runFx(() -> {
            Accordion accordion = (Accordion) lookup("#sidePanelAccordion");
            for (TitledPane pane : accordion.getPanes()) {
                if (title.equals(pane.getText())) {
                    accordion.setExpandedPane(pane);
                    return;
                }
            }
            throw new IllegalStateException("No accordion pane titled '" + title + "'");
        });
    }

    // --- FX thread helpers, shared by the sinks ---

    static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            started.countDown();
        }
        if (!started.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX toolkit did not start");
        }
        Platform.setImplicitExit(false);
    }

    static void runFx(Runnable task) throws Exception {
        if (Platform.isFxApplicationThread()) {
            task.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("FX task timed out - the most likely cause is a modal dialog:"
                    + " showAndWait() never returns while nothing can click it away");
        }
        if (failure.get() != null) {
            throw new IllegalStateException("FX task failed", failure.get());
        }
    }

    static void waitUntil(String description, Callable<Boolean> condition) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(FX_TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            AtomicBoolean satisfied = new AtomicBoolean();
            runFx(() -> {
                try {
                    satisfied.set(Boolean.TRUE.equals(condition.call()));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            if (satisfied.get()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Timed out waiting for " + description);
    }

    /**
     * Waits for a page image that is not {@code previous}, so a re-render is not mistaken for the old one.
     */
    private static void waitForRenderedPage(Image previous) throws Exception {
        waitUntil("the page preview to render", () -> {
            Image current = controller.getDocumentViewModel().getCurrentPageImage();
            return current != null && current != previous;
        });
        settle();
    }

    /**
     * Lets pending layout, bindings and CSS settle before a shot is taken.
     */
    static void settle() throws Exception {
        for (int i = 0; i < 4; i++) {
            runFx(() -> {
            });
            Thread.sleep(150);
        }
    }

    static Node lookup(String selector) {
        Node node = scene.lookup(selector);
        if (node == null) {
            throw new IllegalStateException("No node matching " + selector);
        }
        return node;
    }

    // --- Fixtures ---

    static Path requiredDir(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing -D" + property);
        }
        Path path = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(path + " (from -D" + property + ") is not a directory");
        }
        return path;
    }

    private static void resolveDirectories() {
        Path demoDir = requiredDir("jsignpdf.screenshot.demoDir");
        demoPdf = demoDir.resolve("service-agreement.pdf");
        demoKeystore = demoDir.resolve("jsmith.p12");
        demoSigImage = demoDir.resolve("jsmith-sig.png");
        workDir = Path.of(System.getProperty("java.io.tmpdir"), "jsignpdf-screenshots");
        configDir = requiredConfigDir();
    }

    static Path requiredConfigDir() {
        String value = System.getenv("JSIGNPDF_CONFIG_DIR");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("JSIGNPDF_CONFIG_DIR is not set");
        }
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static void stageDemoFiles() throws IOException {
        deleteRecursively(workDir);
        Files.createDirectories(workDir);
        demoPdf = copyInto(demoPdf, workDir);
        demoKeystore = copyInto(demoKeystore, workDir);
        demoSigImage = copyInto(demoSigImage, workDir);
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static Path copyInto(Path source, Path directory) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IllegalStateException("Missing demo asset " + source);
        }
        Path target = directory.resolve(source.getFileName());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
}
