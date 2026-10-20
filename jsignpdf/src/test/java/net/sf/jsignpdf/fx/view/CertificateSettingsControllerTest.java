package net.sf.jsignpdf.fx.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProviderException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.MonocleAssumption;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;

/**
 * The keystore combo carries two view-model values (type and PKCS#11 profile). It must follow the view model when a
 * preset or the stored configuration is loaded, and must never show a profile the view model does not hold.
 */
public class CertificateSettingsControllerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path cfgDir;
    private Pkcs11Profiles profiles;

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

    @Before
    public void setUp() throws Exception {
        cfgDir = tmp.newFolder("cfg").toPath();
        Files.createDirectories(cfgDir.resolve("pkcs11"));
    }

    @After
    public void tearDown() {
        Pkcs11Profiles.setInstance(null);
    }

    private void profile(String id, String extra) throws Exception {
        Files.writeString(cfgDir.resolve("pkcs11/" + id + ".cfg"), extra + "name=" + id + "\n");
    }

    private void load() {
        profiles = new Pkcs11Profiles(cfgDir, (backend, file) -> {
            throw new ProviderException("driver unavailable");
        });
        Pkcs11Profiles.setInstance(profiles);
    }

    @Test
    public void viewModelSelectionWithProfileIsShown() throws Exception {
        profile("a", "#jsignpdf:label=Card A\n");
        profile("b", "");
        load();
        AtomicReference<String> shown = new AtomicReference<>();
        AtomicReference<Boolean> fileVisible = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.vm.ksProviderProperty().set("b");
            f.vm.ksTypeProperty().set("PKCS11");
            shown.set(f.shown());
            fileVisible.set(f.fileBox.isVisible());
        });
        assertEquals("PKCS11 — b", shown.get());
        assertFalse("keystore file controls are hidden for PKCS#11", fileVisible.get());
    }

    @Test
    public void singleProfileIsPickedWhenNoneIsNamed() throws Exception {
        profile("only", "");
        load();
        AtomicReference<String> shown = new AtomicReference<>();
        AtomicReference<String> provider = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.vm.ksTypeProperty().set("PKCS11");
            shown.set(f.shown());
            provider.set(f.vm.ksProviderProperty().get());
        });
        assertEquals("PKCS11 — only", shown.get());
        assertEquals("only", provider.get());
    }

    @Test
    public void severalProfilesWithoutANameForceAPick() throws Exception {
        profile("a", "");
        profile("b", "");
        load();
        AtomicReference<String> shown = new AtomicReference<>("unset");
        AtomicReference<String> status = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.vm.ksTypeProperty().set("PKCS11");
            shown.set(f.shown());
            status.set(f.status.getText());
        });
        assertNull(shown.get());
        assertEquals(Constants.RES.get("jfx.gui.cert.pkcs11.pick"), status.get());
    }

    @Test
    public void missingProfileClearsTheSelectionAndSaysWhy() throws Exception {
        profile("a", "");
        load();
        AtomicReference<String> shown = new AtomicReference<>("unset");
        AtomicReference<String> status = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.vm.ksProviderProperty().set("from-another-machine");
            f.vm.ksTypeProperty().set("PKCS11");
            shown.set(f.shown());
            status.set(f.status.getText());
        });
        assertNull(shown.get());
        assertTrue(status.get(), status.get().contains("from-another-machine"));
    }

    @Test
    public void pickingAnEntryWritesTypeAndProfileToTheViewModel() throws Exception {
        profile("a", "");
        profile("b", "");
        load();
        AtomicReference<String> type = new AtomicReference<>();
        AtomicReference<String> provider = new AtomicReference<>("unset");
        AtomicReference<String> providerAfterPlain = new AtomicReference<>("unset");
        AtomicReference<Boolean> fileVisible = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.select("PKCS11", "b");
            type.set(f.vm.ksTypeProperty().get());
            provider.set(f.vm.ksProviderProperty().get());
            f.select("PKCS12", null);
            providerAfterPlain.set(f.vm.ksProviderProperty().get());
            fileVisible.set(f.fileBox.isVisible());
        });
        assertEquals("PKCS11", type.get());
        assertEquals("b", provider.get());
        assertNull(providerAfterPlain.get());
        assertTrue(fileVisible.get());
    }

    @Test
    public void failedProfileStaysSelectedAfterARefresh() throws Exception {
        profile("a", "");
        load();
        AtomicReference<String> shown = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.vm.ksProviderProperty().set("a");
            f.vm.ksTypeProperty().set("PKCS11");
            profiles.register("a");
            f.controller.refreshKeystoreChoices();
            shown.set(f.shown());
        });
        assertEquals("PKCS11 — a", shown.get());
    }

    @Test
    public void renamedProfileIsFollowed() throws Exception {
        profile("old", "");
        load();
        AtomicReference<String> provider = new AtomicReference<>();
        AtomicReference<String> shown = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.vm.ksProviderProperty().set("old");
            f.vm.ksTypeProperty().set("PKCS11");
            try {
                profiles.applyEdits(List.of(new Pkcs11Profiles.ProfileEdit("old", "new", "name=old\n")));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            f.controller.refreshKeystoreChoices();
            provider.set(f.vm.ksProviderProperty().get());
            shown.set(f.shown());
        });
        assertEquals("new", provider.get());
        assertEquals("PKCS11 — new", shown.get());
    }

    /** The controller wired up from its own FXML, with a fresh view model. */
    private static final class Fixture {
        final SigningOptionsViewModel vm = new SigningOptionsViewModel();
        final CertificateSettingsController controller;
        final ComboBox<KeystoreChoice> combo;
        final javafx.scene.Node fileBox;
        final Label status;

        @SuppressWarnings("unchecked")
        Fixture() {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/net/sf/jsignpdf/fx/view/CertificateSettings.fxml"),
                        ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE, Locale.ENGLISH));
                Parent root = loader.load();
                controller = loader.getController();
                combo = (ComboBox<KeystoreChoice>) root.lookup("#cmbKeystoreType");
                fileBox = root.lookup("#boxKeystoreFile");
                status = (Label) root.lookup("#lblLoadKeysStatus");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            controller.setViewModel(vm);
        }

        String shown() {
            return combo.getValue() == null ? null : combo.getValue().display();
        }

        void select(String type, String profileId) {
            combo.setValue(combo.getItems().stream().filter(c -> c.matches(type, profileId)).findFirst().orElseThrow());
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
