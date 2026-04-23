package net.sf.jsignpdf.fx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.Constants;

/**
 * Tests that the JavaFX UI loads correctly with different locales and that
 * translated text appears in the actual UI nodes.
 */
public class FxTranslationsTest {

    private static final String BUNDLE_BASE = Constants.RESOURCE_BUNDLE_BASE;

    private static final Locale[] TEST_LOCALES = {
            Locale.ENGLISH,
            new Locale("cs"),
            new Locale("de"),
            new Locale("fr"),
            new Locale("es"),
            new Locale("ja"),
            new Locale("zh", "CN"),
    };

    @BeforeClass
    public static void initFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testMainWindowLoadsWithAllLocales() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            Parent root = loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
            assertNotNull("MainWindow failed to load for locale " + locale, root);
        }
    }

    @Test
    public void testMenuTextsMatchTranslations() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
            MenuBar menuBar = getMenuBar(root);

            assertEquals("File menu for " + locale,
                    bundle.getString("jfx.gui.menu.file"), menuBar.getMenus().get(0).getText());
            assertEquals("View menu for " + locale,
                    bundle.getString("jfx.gui.menu.view"), menuBar.getMenus().get(1).getText());
            assertEquals("Signing menu for " + locale,
                    bundle.getString("jfx.gui.menu.signing"), menuBar.getMenus().get(2).getText());
            assertEquals("Presets menu for " + locale,
                    bundle.getString("jfx.gui.menu.presets"), menuBar.getMenus().get(3).getText());
            assertEquals("Help menu for " + locale,
                    bundle.getString("jfx.gui.menu.help"), menuBar.getMenus().get(4).getText());
        }
    }

    @Test
    public void testMenuItemTextsMatchTranslations() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
            Menu fileMenu = getMenuBar(root).getMenus().get(0);

            // Find items by fx:id semantics (search for matching translated text rather
            // than relying on positional indices, which are fragile)
            assertMenuContains(fileMenu, bundle, "jfx.gui.menu.file.open", locale);
            assertMenuContains(fileMenu, bundle, "jfx.gui.menu.file.close", locale);
            assertMenuContains(fileMenu, bundle, "jfx.gui.menu.file.saveAs", locale);
            assertMenuContains(fileMenu, bundle, "jfx.gui.menu.file.resetSettings", locale);
            assertMenuContains(fileMenu, bundle, "jfx.gui.menu.file.exit", locale);
        }
    }

    @Test
    public void testSigningMenuItemTextsMatchTranslations() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
            Menu signingMenu = getMenuBar(root).getMenus().get(2);

            assertMenuContains(signingMenu, bundle, "jfx.gui.menu.signing.visibleSig", locale);
            assertMenuContains(signingMenu, bundle, "jfx.gui.menu.file.sign", locale);

            // The visible-signature item must be a CheckMenuItem so it reflects state
            boolean hasCheck = signingMenu.getItems().stream()
                    .anyMatch(it -> it instanceof CheckMenuItem
                            && bundle.getString("jfx.gui.menu.signing.visibleSig").equals(it.getText()));
            assertEquals("Visible-signature item must be a CheckMenuItem for " + locale,
                    true, hasCheck);

            // The Signing menu should contain exactly one CheckMenuItem (visible sig)
            // and exactly one enabled MenuItem (Sign) — no separate Clear entry.
            long checkItems = signingMenu.getItems().stream()
                    .filter(it -> it instanceof CheckMenuItem).count();
            long signItems = signingMenu.getItems().stream()
                    .filter(it -> !(it instanceof CheckMenuItem)
                            && it.getText() != null
                            && it.getText().equals(bundle.getString("jfx.gui.menu.file.sign")))
                    .count();
            assertEquals("Exactly one visible-sig CheckMenuItem for " + locale, 1L, checkItems);
            assertEquals("Exactly one Sign menu item for " + locale, 1L, signItems);
        }
    }

    private static void assertMenuContains(Menu menu, ResourceBundle bundle, String key, Locale locale) {
        String expected = bundle.getString(key);
        for (MenuItem item : menu.getItems()) {
            if (expected.equals(item.getText())) {
                return;
            }
        }
        fail("Menu " + menu.getText() + " missing item '" + expected + "' (key " + key
                + ") for locale " + locale);
    }

    @Test
    public void testToolbarTextsMatchTranslations() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
            ToolBar toolBar = getToolBar(root);

            // First item is the Open button
            Button openBtn = (Button) toolBar.getItems().get(0);
            assertEquals("Open button for " + locale,
                    bundle.getString("jfx.gui.toolbar.open"), openBtn.getText());

            // Last item is the Sign button
            Button signBtn = (Button) toolBar.getItems().get(toolBar.getItems().size() - 1);
            assertEquals("Sign button for " + locale,
                    bundle.getString("jfx.gui.toolbar.sign"), signBtn.getText());

            // The toolbar must contain two icon-only ToggleButtons (visible
            // signature + TSA), each with a translated tooltip.
            String expectedVisibleTip = bundle.getString("jfx.gui.toolbar.visibleSig.tooltip");
            String expectedTsaTip = bundle.getString("jfx.gui.toolbar.tsa.tooltip");
            long toggleCount = toolBar.getItems().stream()
                    .filter(n -> n instanceof ToggleButton)
                    .count();
            assertEquals("Two toolbar ToggleButtons expected for " + locale, 2L, toggleCount);

            boolean foundVisibleTip = toolBar.getItems().stream()
                    .filter(n -> n instanceof ToggleButton)
                    .map(n -> ((ToggleButton) n).getTooltip())
                    .filter(t -> t != null)
                    .map(Tooltip::getText)
                    .anyMatch(expectedVisibleTip::equals);
            boolean foundTsaTip = toolBar.getItems().stream()
                    .filter(n -> n instanceof ToggleButton)
                    .map(n -> ((ToggleButton) n).getTooltip())
                    .filter(t -> t != null)
                    .map(Tooltip::getText)
                    .anyMatch(expectedTsaTip::equals);
            assertEquals("Visible-signature toggle tooltip missing for " + locale,
                    true, foundVisibleTip);
            assertEquals("TSA toggle tooltip missing for " + locale,
                    true, foundTsaTip);
        }
    }

    @Test
    public void testStatusBarBadgeAndOutputLabelExist() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
            HBox statusBar = (HBox) root.getBottom();
            assertNotNull("Status bar missing for " + locale, statusBar);

            // Both translations must exist in the bundle, and the badge label
            // must carry one of them (the initial state when no document is
            // loaded shows "invisible signature").
            String expectedVisible = bundle.getString("jfx.gui.status.visibleSigEnabled");
            String expectedInvisible = bundle.getString("jfx.gui.status.invisibleSig");
            assertNotNull("visibleSig text missing for " + locale, expectedVisible);
            assertNotNull("invisibleSig text missing for " + locale, expectedInvisible);

            boolean badgeFound = statusBar.getChildren().stream()
                    .filter(n -> n instanceof Label)
                    .map(n -> ((Label) n).getText())
                    .anyMatch(t -> expectedVisible.equals(t) || expectedInvisible.equals(t));
            assertEquals("Sig-state badge missing for " + locale, true, badgeFound);
        }
    }

    @Test
    public void testAccordionPanelTitlesMatchTranslations() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
            Accordion accordion = getAccordion(root);
            assertNotNull("Accordion not found for " + locale, accordion);

            assertEquals("Certificate panel for " + locale,
                    bundle.getString("jfx.gui.panel.certificate"),
                    accordion.getPanes().get(0).getText());
            assertEquals("Signature properties panel for " + locale,
                    bundle.getString("jfx.gui.panel.signatureProperties"),
                    accordion.getPanes().get(1).getText());
            assertEquals("Signature appearance panel for " + locale,
                    bundle.getString("jfx.gui.panel.signatureAppearance"),
                    accordion.getPanes().get(2).getText());
            assertEquals("TSA panel for " + locale,
                    bundle.getString("jfx.gui.panel.timestampValidation"),
                    accordion.getPanes().get(3).getText());
            assertEquals("Encryption panel for " + locale,
                    bundle.getString("jfx.gui.panel.encryptionRights"),
                    accordion.getPanes().get(4).getText());
        }
    }

    @Test
    public void testDropHintMatchesTranslation() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
            Label dropHint = getDropHintLabel(root);
            assertNotNull("Drop hint not found for " + locale, dropHint);
            assertEquals("Drop hint for " + locale,
                    bundle.getString("jfx.gui.dropHint"), dropHint.getText());
        }
    }

    @Test
    public void testCertificateSettingsLabels() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            VBox root = (VBox) loadFxml("/net/sf/jsignpdf/fx/view/CertificateSettings.fxml", bundle);
            assertNotNull("CertificateSettings load failed for " + locale, root);

            // First child is the "Keystore type:" label
            Label ksTypeLabel = (Label) root.getChildren().get(0);
            assertEquals("Keystore type label for " + locale,
                    bundle.getString("jfx.gui.cert.keystoreType"), ksTypeLabel.getText());

            // Find "Store passwords" checkbox (last child)
            CheckBox storePasswords = (CheckBox) root.getChildren().get(root.getChildren().size() - 1);
            assertEquals("Store passwords checkbox for " + locale,
                    bundle.getString("jfx.gui.cert.storePasswords"), storePasswords.getText());
        }
    }

    @Test
    public void testSignatureSettingsLabels() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            VBox root = (VBox) loadFxml("/net/sf/jsignpdf/fx/view/SignatureSettings.fxml", bundle);
            assertNotNull("SignatureSettings load failed for " + locale, root);

            // First child is the "Enable visible signature" checkbox
            CheckBox visibleSig = (CheckBox) root.getChildren().get(0);
            assertEquals("Enable visible sig for " + locale,
                    bundle.getString("jfx.gui.sig.enableVisible"), visibleSig.getText());
        }
    }

    @Test
    public void testSignaturePropertiesLabels() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            VBox root = (VBox) loadFxml("/net/sf/jsignpdf/fx/view/SignatureProperties.fxml", bundle);
            assertNotNull("SignatureProperties load failed for " + locale, root);

            // First child is the "Output file:" label
            Label outFileLabel = (Label) root.getChildren().get(0);
            assertEquals("Output file label for " + locale,
                    bundle.getString("jfx.gui.sig.outputFile"), outFileLabel.getText());
        }
    }

    @Test
    public void testTsaSettingsLabels() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            VBox root = (VBox) loadFxml("/net/sf/jsignpdf/fx/view/TsaSettings.fxml", bundle);
            assertNotNull("TsaSettings load failed for " + locale, root);

            // First child is the "Enable Timestamp (TSA)" checkbox
            CheckBox tsaEnabled = (CheckBox) root.getChildren().get(0);
            assertEquals("TSA checkbox for " + locale,
                    bundle.getString("jfx.gui.tsa.enableTimestamp"), tsaEnabled.getText());
        }
    }

    @Test
    public void testEncryptionSettingsLabels() throws Exception {
        for (Locale locale : TEST_LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
            VBox root = (VBox) loadFxml("/net/sf/jsignpdf/fx/view/EncryptionSettings.fxml", bundle);
            assertNotNull("EncryptionSettings load failed for " + locale, root);

            // First child is the "PDF Encryption:" label
            Label encLabel = (Label) root.getChildren().get(0);
            assertEquals("PDF Encryption label for " + locale,
                    bundle.getString("jfx.gui.enc.pdfEncryption"), encLabel.getText());
        }
    }

    @Test
    public void testEnglishSpecificTexts() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, Locale.ENGLISH);
        BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
        MenuBar menuBar = getMenuBar(root);

        assertEquals("File", menuBar.getMenus().get(0).getText());
        assertEquals("View", menuBar.getMenus().get(1).getText());
        assertEquals("Signing", menuBar.getMenus().get(2).getText());
        assertEquals("Presets", menuBar.getMenus().get(3).getText());
        assertEquals("Help", menuBar.getMenus().get(4).getText());
        assertEquals("Drop a PDF file here or use File > Open", getDropHintLabel(root).getText());
    }

    @Test
    public void testGermanSpecificTexts() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, new Locale("de"));
        BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
        MenuBar menuBar = getMenuBar(root);

        assertEquals("Datei", menuBar.getMenus().get(0).getText());
        assertEquals("Ansicht", menuBar.getMenus().get(1).getText());
        assertEquals("Signierung", menuBar.getMenus().get(2).getText());
        // "Presets" key has no German translation yet — falls back to English.
        assertEquals("Presets", menuBar.getMenus().get(3).getText());
        assertEquals("Hilfe", menuBar.getMenus().get(4).getText());
    }

    @Test
    public void testCzechSpecificTexts() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, new Locale("cs"));
        BorderPane root = (BorderPane) loadFxml("/net/sf/jsignpdf/fx/view/MainWindow.fxml", bundle);
        MenuBar menuBar = getMenuBar(root);
        Accordion accordion = getAccordion(root);

        assertEquals("Soubor", menuBar.getMenus().get(0).getText());
        assertEquals("Certifik\u00e1t", accordion.getPanes().get(0).getText());
    }

    // --- Helper methods for navigating the node tree ---

    private MenuBar getMenuBar(BorderPane root) {
        VBox topBox = (VBox) root.getTop();
        return (MenuBar) topBox.getChildren().get(0);
    }

    private ToolBar getToolBar(BorderPane root) {
        VBox topBox = (VBox) root.getTop();
        return (ToolBar) topBox.getChildren().get(1);
    }

    private Accordion getAccordion(BorderPane root) {
        // center -> verticalSplit -> splitPane -> AnchorPane -> Accordion
        SplitPane verticalSplit = (SplitPane) root.getCenter();
        SplitPane splitPane = (SplitPane) verticalSplit.getItems().get(0);
        AnchorPane sidePanel = (AnchorPane) splitPane.getItems().get(0);
        return (Accordion) sidePanel.getChildren().get(0);
    }

    private Label getDropHintLabel(BorderPane root) {
        // center -> verticalSplit -> splitPane -> ScrollPane -> StackPane -> Label
        SplitPane verticalSplit = (SplitPane) root.getCenter();
        SplitPane splitPane = (SplitPane) verticalSplit.getItems().get(0);
        javafx.scene.control.ScrollPane scrollPane =
                (javafx.scene.control.ScrollPane) splitPane.getItems().get(1);
        StackPane pdfArea = (StackPane) scrollPane.getContent();
        for (Node child : pdfArea.getChildren()) {
            if (child instanceof Label) {
                return (Label) child;
            }
        }
        return null;
    }

    private Parent loadFxml(String fxmlPath, ResourceBundle bundle) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Parent> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource(fxmlPath), bundle);
                result.set(loader.load());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("FXML loading timed out for " + fxmlPath);
        }
        if (error.get() != null) {
            Throwable t = error.get();
            StringBuilder msg = new StringBuilder(t.toString());
            Throwable cause = t.getCause();
            while (cause != null) {
                msg.append(" | caused by: ").append(cause);
                cause = cause.getCause();
            }
            fail("FXML loading failed for " + fxmlPath + " with " + bundle.getLocale()
                    + ": " + msg);
        }
        return result.get();
    }
}
