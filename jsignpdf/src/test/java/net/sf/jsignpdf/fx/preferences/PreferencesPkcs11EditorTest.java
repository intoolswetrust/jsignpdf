package net.sf.jsignpdf.fx.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.MonocleAssumption;
import net.sf.jsignpdf.pkcs11.Pkcs11Profile;
import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;
import net.sf.jsignpdf.pkcs11.ProviderMode;
import net.sf.jsignpdf.utils.AdvancedConfig;

/**
 * The PKCS#11 tab edits one config text through three fields and a raw textarea. Whatever is typed in one place must
 * land in the profile that is saved, and only in the selected one.
 */
public class PreferencesPkcs11EditorTest {

    private static final String BODY_A = "#jsignpdf:label=Card A\nname=a\nlibrary=/usr/lib/a.so\n";
    private static final String BODY_DEFAULT = "name=JSignPdf\nlibrary=/usr/lib/legacy.so\n";

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
    public void selectingAProfileFillsTheFields() throws Exception {
        AtomicReference<String> label = new AtomicReference<>();
        AtomicReference<String> library = new AtomicReference<>();
        AtomicReference<ProviderMode> provider = new AtomicReference<>();
        AtomicReference<Boolean> renameDisabledForDefault = new AtomicReference<>();
        AtomicReference<Boolean> renameDisabledForA = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            renameDisabledForDefault.set(f.rename.isDisable());
            f.list.getSelectionModel().select(1);
            label.set(f.label.getText());
            library.set(f.library.getText());
            provider.set(f.provider.getValue());
            renameDisabledForA.set(f.rename.isDisable());
        });
        assertEquals("Card A", label.get());
        assertEquals("/usr/lib/a.so", library.get());
        assertEquals(ProviderMode.SUN, provider.get());
        assertTrue("the default profile cannot be renamed", renameDisabledForDefault.get());
        assertFalse(renameDisabledForA.get());
    }

    @Test
    public void fieldEditsLandInTheSelectedProfileOnly() throws Exception {
        AtomicReference<List<Pkcs11Profiles.ProfileEdit>> edits = new AtomicReference<>();
        AtomicReference<String> raw = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.list.getSelectionModel().select(1);
            f.label.setText("Renamed card");
            f.provider.setValue(ProviderMode.BOTH);
            f.library.setText("C:\\Program Files\\Vendor\\p11.dll");
            raw.set(f.body.getText());
            edits.set(f.vm.pkcs11Edits());
        });
        String expected = "#jsignpdf:label=Renamed card\n#jsignpdf:provider=both\nname=a\n"
                + "library=\"C:/Program Files/Vendor/p11.dll\"\n";
        assertEquals(expected, raw.get());
        assertEquals(BODY_DEFAULT, edits.get().get(0).body());
        assertEquals(expected, edits.get().get(1).body());
    }

    @Test
    public void rawTextEditsUpdateTheFields() throws Exception {
        AtomicReference<String> label = new AtomicReference<>();
        AtomicReference<String> library = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.list.getSelectionModel().select(1);
            f.body.setText("#jsignpdf:label=Typed\nname=a\nlibrary=/opt/typed.so\n");
            label.set(f.label.getText());
            library.set(f.library.getText());
        });
        assertEquals("Typed", label.get());
        assertEquals("/opt/typed.so", library.get());
    }

    @Test
    public void removeDropsTheProfileFromTheSavedSet() throws Exception {
        AtomicReference<List<String>> ids = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.list.getSelectionModel().select(1);
            f.remove.fire();
            ids.set(f.vm.pkcs11Edits().stream().map(Pkcs11Profiles.ProfileEdit::id).collect(Collectors.toList()));
        });
        assertEquals(List.of("default"), ids.get());
    }

    @Test
    public void resetToSampleKeepsTheProfileName() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.list.getSelectionModel().select(1);
            f.resetSample.fire();
            body.set(f.vm.pkcs11Edits().get(1).body());
        });
        assertTrue(body.get(), body.get().contains("\nname=a\n"));
        assertFalse(body.get(), body.get().contains("name=JSignPdf"));
    }

    /** The Preferences FXML bound to a view model holding the legacy profile and one named profile. */
    private static final class Fixture {
        final PreferencesViewModel vm = new PreferencesViewModel();
        final ListView<Pkcs11ProfileDraft> list;
        final TextField label;
        final TextField library;
        final ChoiceBox<ProviderMode> provider;
        final TextArea body;
        final Button rename;
        final Button remove;
        final Button resetSample;

        @SuppressWarnings("unchecked")
        Fixture() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/sf/jsignpdf/fx/view/Preferences.fxml"),
                        ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE, Locale.ENGLISH));
                TabPane tabs = loader.load();
                PreferencesController controller = loader.getController();
                Properties defaults = new Properties();
                try (InputStream is = getClass().getResourceAsStream("/net/sf/jsignpdf/conf/advanced.default.properties")) {
                    defaults.load(is);
                }
                vm.loadFrom(new AdvancedConfig(null, defaults));
                vm.loadPkcs11Profiles(List.of(
                        new Pkcs11Profile("default", Path.of("pkcs11.cfg"), true, BODY_DEFAULT),
                        new Pkcs11Profile("a", Path.of("pkcs11", "a.cfg"), false, BODY_A)), id -> null);
                controller.bind(vm);
                Node tab = tabs.getTabs().stream().filter(t -> "tabPkcs11".equals(t.getId())).findFirst().orElseThrow()
                        .getContent();
                list = (ListView<Pkcs11ProfileDraft>) tab.lookup("#lstPkcs11Profiles");
                label = (TextField) tab.lookup("#txtPkcs11Label");
                library = (TextField) tab.lookup("#txtPkcs11Library");
                provider = (ChoiceBox<ProviderMode>) tab.lookup("#cmbPkcs11Provider");
                body = (TextArea) tab.lookup("#txtPkcs11Body");
                rename = (Button) tab.lookup("#btnPkcs11Rename");
                remove = (Button) tab.lookup("#btnPkcs11Remove");
                resetSample = (Button) tab.lookup("#btnPkcs11ResetSample");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
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
