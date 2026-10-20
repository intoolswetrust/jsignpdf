package net.sf.jsignpdf.fx.view;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.MonocleAssumption;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;

/**
 * The proxy port is typed as text and kept as an int, so nothing reaches the signing options unless the
 * field is committed - a port left uncommitted silently sends the request to the default port instead.
 */
public class TsaSettingsControllerTest {

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

    /** Signing and persisting are reachable without the field ever losing focus. */
    @Test
    public void aTypedPortIsCommittedOnRequest() throws Exception {
        AtomicReference<Integer> port = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.portField.setText("3128");

            f.controller.commitPendingEdits();

            port.set(f.vm.proxyPortProperty().get());
        });
        assertEquals(Integer.valueOf(3128), port.get());
    }

    /** A port pasted with surrounding whitespace is still a port. */
    @Test
    public void surroundingWhitespaceIsIgnored() throws Exception {
        AtomicReference<Integer> port = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.portField.setText(" 3128 ");

            f.controller.commitPendingEdits();

            port.set(f.vm.proxyPortProperty().get());
        });
        assertEquals(Integer.valueOf(3128), port.get());
    }

    /** Out of range or non-numeric: keep the last good port and show it, so screen and model agree. */
    @Test
    public void anUnusablePortRevertsToTheCommittedOne() throws Exception {
        AtomicReference<Integer> port = new AtomicReference<>();
        AtomicReference<String> shown = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.portField.setText("3128");
            f.controller.commitPendingEdits();

            for (String unusable : new String[] { "", "http", "0", "-1", "65536" }) {
                f.portField.setText(unusable);
                f.controller.commitPendingEdits();
            }

            port.set(f.vm.proxyPortProperty().get());
            shown.set(f.portField.getText());
        });
        assertEquals(Integer.valueOf(3128), port.get());
        assertEquals("3128", shown.get());
    }

    /** The field starts from the persisted value rather than from a literal in the FXML. */
    @Test
    public void theFieldStartsFromTheViewModel() throws Exception {
        AtomicReference<String> shown = new AtomicReference<>();
        runOnFxThread(() -> {
            SigningOptionsViewModel vm = new SigningOptionsViewModel();
            vm.proxyPortProperty().set(8080);

            Fixture f = new Fixture(vm);

            shown.set(f.portField.getText());
        });
        assertEquals("8080", shown.get());
    }

    /** The controller wired up from its own FXML, with a fresh view model. */
    private static final class Fixture {
        final SigningOptionsViewModel vm;
        final TsaSettingsController controller;
        final TextField portField;

        Fixture() {
            this(new SigningOptionsViewModel());
        }

        Fixture(SigningOptionsViewModel vm) {
            this.vm = vm;
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/net/sf/jsignpdf/fx/view/TsaSettings.fxml"),
                        ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE, Locale.ENGLISH));
                javafx.scene.Parent root = loader.load();
                controller = loader.getController();
                portField = (TextField) root.lookup("#txtProxyPort");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            controller.setViewModel(vm);
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
        latch.await(5, TimeUnit.SECONDS);
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }
}
