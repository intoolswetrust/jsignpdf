package net.sf.jsignpdf.fx.view;

import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.MonocleAssumption;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;

/**
 * The output file name is session state. A name carried over from the previous run would otherwise be applied to the
 * first document opened in this one, and - being read back as a deliberate choice - would then stop following the
 * suffix for the rest of the session.
 */
public class MainWindowOutputNameTest {

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
    public void persistedOutputNameIsDroppedAtStartup() throws Exception {
        AtomicReference<String> baseName = new AtomicReference<>();
        AtomicReference<String> outFile = new AtomicReference<>();
        AtomicReference<String> persisted = new AtomicReference<>();
        runOnFxThread(() -> {
            MainWindowController controller = loadMainWindow();

            BasicSignerOptions opts = new BasicSignerOptions();
            opts.setInFile("/docs/drawing.pdf");
            opts.setOutSuffix("_EM");
            opts.setOutFile("/docs/final.pdf");
            controller.initFromOptions(opts);

            SigningOptionsViewModel vm = signingViewModel(controller);
            baseName.set(vm.outBaseNameProperty().get());
            outFile.set(vm.outFileProperty().get());
            persisted.set(opts.getOutFile());
        });

        assertNull("the persisted output name must not survive a restart", baseName.get());
        assertNull("no document is open, so there is no output path yet", outFile.get());
        assertNull("the options must not keep the stale path either", persisted.get());
    }

    private static MainWindowController loadMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainWindowOutputNameTest.class.getResource("/net/sf/jsignpdf/fx/view/MainWindow.fxml"),
                    ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE, Locale.ENGLISH));
            loader.<BorderPane>load();
            return loader.getController();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static SigningOptionsViewModel signingViewModel(MainWindowController controller) {
        try {
            Field field = MainWindowController.class.getDeclaredField("signingVM");
            field.setAccessible(true);
            return (SigningOptionsViewModel) field.get(controller);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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
