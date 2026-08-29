package net.sf.jsignpdf.fx.view;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.MonocleAssumption;

/**
 * The timestamp button and menu item are disabled through a single binding (engine capability + document
 * loaded), while the rest of the document controls are disabled imperatively. Mixing the two on one control
 * throws {@code A bound value cannot be set} the first time a document is opened - a runtime failure nothing
 * else would catch.
 */
public class MainWindowTimestampGatingTest {

    @BeforeClass
    public static void initFx() throws Exception {
        MonocleAssumption.assumeUsable();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void timestampControlsAreBoundAndSignControlsAreNot() throws Exception {
        runOnFxThread(() -> {
            BorderPane root = loadMainWindow();
            ToolBar toolBar = (ToolBar) ((VBox) root.getTop()).getChildren().get(1);
            Button btnTimestamp = toolbarButton(toolBar, "btnTimestamp");
            Button btnSign = toolbarButton(toolBar, "btnSign");
            assertNotNull("the toolbar must carry the timestamp button", btnTimestamp);
            assertNotNull(btnSign);

            assertTrue("the timestamp button must be gated by a binding",
                    btnTimestamp.disableProperty().isBound());
            assertFalse("the sign button stays under the imperative document-controls handling",
                    btnSign.disableProperty().isBound());

            MenuItem menuTimestamp = findMenuItem(root, "menuTimestamp");
            MenuItem menuSign = findMenuItem(root, "menuSign");
            assertNotNull("the Signing menu must carry the timestamp item", menuTimestamp);
            assertTrue("the timestamp menu item must be gated by a binding",
                    menuTimestamp.disableProperty().isBound());
            assertFalse("the sign menu item stays under the imperative handling",
                    menuSign.disableProperty().isBound());
        });
    }

    private static Button toolbarButton(ToolBar toolBar, String id) {
        return toolBar.getItems().stream()
                .filter(node -> node instanceof Button && id.equals(node.getId()))
                .map(Button.class::cast)
                .findFirst().orElse(null);
    }

    private static BorderPane loadMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainWindowTimestampGatingTest.class.getResource("/net/sf/jsignpdf/fx/view/MainWindow.fxml"),
                    ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE, Locale.ENGLISH));
            return loader.load();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static MenuItem findMenuItem(BorderPane root, String id) {
        MenuBar menuBar = (MenuBar) ((VBox) root.getTop()).getChildren().get(0);
        return menuBar.getMenus().stream()
                .flatMap(menu -> menu.getItems().stream())
                .filter(item -> id.equals(item.getId()))
                .findFirst().orElse(null);
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }
}
