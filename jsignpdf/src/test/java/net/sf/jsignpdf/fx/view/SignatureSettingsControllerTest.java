package net.sf.jsignpdf.fx.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.fx.EngineCapabilities;
import net.sf.jsignpdf.fx.MonocleAssumption;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.types.SignatureFieldInfo;

/**
 * The signature-field combo is the only place a field can be chosen in the GUI, so the view-model value must
 * never disagree with what the combo shows - a name armed invisibly would sign into a box the user did not
 * pick, or fail on a document that has no such field.
 */
public class SignatureSettingsControllerTest {

    private static final SignatureFieldInfo EMPLOYEE =
            new SignatureFieldInfo(1, "Employee", 1, 70, 700, 300, 760, false, false);
    private static final SignatureFieldInfo MANAGER =
            new SignatureFieldInfo(2, "Manager", 3, 70, 500, 300, 560, false, false);

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

    /** Loading a preset writes the view model directly - the combo has to follow it. */
    @Test
    public void aFieldNameSetOnTheViewModelSelectsItInTheCombo() throws Exception {
        AtomicReference<String> selected = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.controller.setSignatureFields(List.of(EMPLOYEE, MANAGER));

            f.vm.sigFieldNameProperty().set("Manager");

            selected.set(name(f.selectedField()));
        });
        assertEquals("Manager", selected.get());
    }

    /** A preset saved while working on another document names a field this one doesn't have. */
    @Test
    public void aFieldNameTheDocumentDoesNotOfferIsDropped() throws Exception {
        AtomicReference<String> vmValue = new AtomicReference<>("unset");
        AtomicReference<String> selected = new AtomicReference<>("unset");
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.controller.setSignatureFields(List.of(EMPLOYEE));

            f.vm.sigFieldNameProperty().set("FromAnotherDocument");

            vmValue.set(f.vm.sigFieldNameProperty().get());
            selected.set(name(f.selectedField()));
        });
        assertNull("the view model must not keep a name the combo can't show", vmValue.get());
        assertNull("nothing is selected, so a new field is created", selected.get());
    }

    /** Opening another document replaces the offered fields and drops the previous choice. */
    @Test
    public void openingAnotherDocumentResetsTheSelection() throws Exception {
        AtomicReference<String> vmValue = new AtomicReference<>("unset");
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.controller.setSignatureFields(List.of(EMPLOYEE, MANAGER));
            f.vm.sigFieldNameProperty().set("Manager");

            f.controller.setSignatureFields(List.of(EMPLOYEE));

            vmValue.set(f.vm.sigFieldNameProperty().get());
        });
        assertNull(vmValue.get());
    }

    /** A disabled combo still showing a field would sign into it (or fail the capability check at signing). */
    @Test
    public void switchingToAnEngineWithoutTheCapabilityClearsTheSelection() throws Exception {
        AtomicReference<String> vmValue = new AtomicReference<>("unset");
        AtomicReference<String> selected = new AtomicReference<>("unset");
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            EngineCapabilities caps = new EngineCapabilities();
            caps.activeEngineProperty().set(new FakeEngine(EnumSet.of(Capability.SIGN_EXISTING_FIELD)));
            f.controller.gateSigFieldCombo(caps.unsupported(Capability.SIGN_EXISTING_FIELD));
            f.controller.setSignatureFields(List.of(EMPLOYEE, MANAGER));
            f.vm.sigFieldNameProperty().set("Manager");

            caps.activeEngineProperty().set(new FakeEngine(EnumSet.of(Capability.VISIBLE_SIGNATURE)));

            vmValue.set(f.vm.sigFieldNameProperty().get());
            selected.set(name(f.selectedField()));
        });
        assertNull(vmValue.get());
        assertNull(selected.get());
    }

    private static String name(SignatureFieldInfo field) {
        return field == null ? null : field.name();
    }

    /** The controller wired up from its own FXML, with a fresh view model. */
    private static final class Fixture {
        final SigningOptionsViewModel vm = new SigningOptionsViewModel();
        final SignatureSettingsController controller;
        private final javafx.scene.control.ComboBox<SignatureFieldInfo> combo;

        @SuppressWarnings("unchecked")
        Fixture() {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/net/sf/jsignpdf/fx/view/SignatureSettings.fxml"),
                        ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE, Locale.ENGLISH));
                javafx.scene.Parent root = loader.load();
                controller = loader.getController();
                combo = (javafx.scene.control.ComboBox<SignatureFieldInfo>) root.lookup("#cmbSigField");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            controller.setViewModel(vm);
        }

        SignatureFieldInfo selectedField() {
            return combo.getValue();
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

    private static final class FakeEngine implements SigningEngine {
        private final Set<Capability> capabilities;

        FakeEngine(Set<Capability> capabilities) {
            this.capabilities = Set.copyOf(capabilities);
        }

        @Override
        public String id() {
            return "fake";
        }

        @Override
        public String displayName() {
            return "Fake";
        }

        @Override
        public Set<Capability> capabilities() {
            return capabilities;
        }

        @Override
        public boolean sign(BasicSignerOptions options, EngineConfig engineConfig) {
            return false;
        }
    }
}
