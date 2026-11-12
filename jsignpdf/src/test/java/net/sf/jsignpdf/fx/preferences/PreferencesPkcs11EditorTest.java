package net.sf.jsignpdf.fx.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;

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
            renameDisabledForDefault.set(f.changeId.isDisable());
            f.table.getSelectionModel().select(1);
            label.set(f.label.getText());
            library.set(f.library.getText());
            provider.set(f.provider.getValue());
            renameDisabledForA.set(f.changeId.isDisable());
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
            f.table.getSelectionModel().select(1);
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
            f.table.getSelectionModel().select(1);
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
            f.table.getSelectionModel().select(1);
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
            f.table.getSelectionModel().select(1);
            f.resetSample.fire();
            body.set(f.vm.pkcs11Edits().get(1).body());
        });
        assertTrue(body.get(), body.get().contains("\nname=a\n"));
        assertFalse(body.get(), body.get().contains("name=JSignPdf"));
    }

    @Test
    public void addedProfileTakesItsIdFromTheLabel() throws Exception {
        AtomicReference<String> initialId = new AtomicReference<>();
        AtomicReference<Pkcs11Profiles.ProfileEdit> edit = new AtomicReference<>();
        AtomicReference<String> hint = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.add.fire();
            initialId.set(f.vm.pkcs11Profiles().get(2).id());
            f.label.setText("Česká karta");
            edit.set(f.vm.pkcs11Edits().get(2));
            hint.set(f.cliHint.getText());
        });
        assertEquals("new-token", initialId.get());
        assertEquals("ceska-karta", edit.get().id());
        assertTrue(edit.get().body(), edit.get().body().contains("#jsignpdf:label=Česká karta\n"));
        assertTrue(edit.get().body(), edit.get().body().contains("\nname=ceska-karta\n"));
        assertEquals("Command line: --keystore-provider ceska-karta", hint.get());
    }

    @Test
    public void existingProfileKeepsItsIdWhenTheLabelChanges() throws Exception {
        AtomicReference<String> id = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.table.getSelectionModel().select(1);
            f.label.setText("Something else");
            id.set(f.vm.pkcs11Edits().get(1).id());
        });
        assertEquals("a", id.get());
    }

    @Test
    public void duplicateGetsACopyLabelAndAFreeId() throws Exception {
        AtomicReference<Pkcs11Profiles.ProfileEdit> edit = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.table.getSelectionModel().select(1);
            f.duplicate.fire();
            f.table.getSelectionModel().select(1);
            f.duplicate.fire();
            edit.set(f.vm.pkcs11Edits().get(3));
        });
        assertEquals("card-a-copy-2", edit.get().id());
        assertTrue(edit.get().body(), edit.get().body().contains("#jsignpdf:label=Card A (copy)\n"));
        assertTrue(edit.get().body(), edit.get().body().contains("\nname=card-a-copy-2\n"));
    }

    @Test
    public void advancedSectionOpensOnlyWhenNeeded() throws Exception {
        AtomicReference<Boolean> forSun = new AtomicReference<>();
        AtomicReference<Boolean> forBoth = new AtomicReference<>();
        runOnFxThread(() -> {
            Fixture f = new Fixture();
            f.table.getSelectionModel().select(1);
            forSun.set(f.advanced.isExpanded());
            f.vm.pkcs11Profiles().add(new Pkcs11ProfileDraft("b", "b",
                    "#jsignpdf:provider=both\nname=b\nlibrary=/usr/lib/b.so\n", null));
            f.table.getSelectionModel().select(2);
            forBoth.set(f.advanced.isExpanded());
        });
        assertFalse(forSun.get());
        assertTrue(forBoth.get());
    }

    /** The Preferences FXML bound to a view model holding the legacy profile and one named profile. */
    private static final class Fixture {
        final PreferencesViewModel vm = new PreferencesViewModel();
        final TableView<Pkcs11ProfileDraft> table;
        final TextField label;
        final TextField library;
        final ChoiceBox<ProviderMode> provider;
        final TextArea body;
        final Button changeId;
        final Button add;
        final Button duplicate;
        final TitledPane advanced;
        final Label cliHint;
        final Button remove;
        final Button resetSample;

        @SuppressWarnings("unchecked")
        Fixture() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/sf/jsignpdf/fx/view/Preferences.fxml"),
                        ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE, Locale.ENGLISH));
                loader.load();
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
                Map<String, Object> ns = loader.getNamespace();
                table = (TableView<Pkcs11ProfileDraft>) ns.get("tblPkcs11Profiles");
                label = (TextField) ns.get("txtPkcs11Label");
                library = (TextField) ns.get("txtPkcs11Library");
                provider = (ChoiceBox<ProviderMode>) ns.get("cmbPkcs11Provider");
                body = (TextArea) ns.get("txtPkcs11Body");
                changeId = (Button) ns.get("btnPkcs11ChangeId");
                add = (Button) ns.get("btnPkcs11Add");
                duplicate = (Button) ns.get("btnPkcs11Duplicate");
                advanced = (TitledPane) ns.get("ttlPkcs11Advanced");
                cliHint = (Label) ns.get("lblPkcs11CliHint");
                remove = (Button) ns.get("btnPkcs11Remove");
                resetSample = (Button) ns.get("btnPkcs11ResetSample");
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
