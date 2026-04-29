package net.sf.jsignpdf.fx.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

import net.sf.jsignpdf.utils.AdvancedConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link PreferencesViewModel}: load/write round-trip, default application, and the PDF-libraries CSV encoding.
 */
public class PreferencesViewModelTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Properties bundledDefaults;

    @Before
    public void loadDefaults() throws IOException {
        bundledDefaults = new Properties();
        try (InputStream is = PreferencesViewModelTest.class
                .getResourceAsStream("/net/sf/jsignpdf/conf/advanced.default.properties")) {
            bundledDefaults.load(is);
        }
    }

    @Test
    public void loadFrom_appliesBundledDefaultsWhenUserFileEmpty() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        PreferencesViewModel vm = new PreferencesViewModel();
        vm.loadFrom(cfg, "");
        assertEquals("", vm.fontPathProperty().get());
        assertEquals("", vm.fontNameProperty().get());
        assertEquals("", vm.fontEncodingProperty().get());
        assertTrue(vm.checkValidityProperty().get());
        assertTrue(vm.checkKeyUsageProperty().get());
        assertFalse(vm.checkCriticalExtensionsProperty().get());
        assertFalse(vm.relaxSslSecurityProperty().get());
        assertEquals("SHA-256", vm.tsaHashAlgorithmProperty().get());
        assertEquals("jpedal,pdfbox,openpdf", vm.encodePdfLibraries());
    }

    @Test
    public void writeTo_emptyStringClearsKey() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        cfg.setProperty("font.path", "/some/path");
        PreferencesViewModel vm = new PreferencesViewModel();
        vm.loadFrom(cfg, "");
        // VM round-trips the value.
        assertEquals("/some/path", vm.fontPathProperty().get());
        // User clears the field.
        vm.fontPathProperty().set("");
        vm.writeTo(cfg);
        // After writeTo, getProperty falls back to bundled default (empty string in advanced.default.properties).
        assertFalse(cfg.hasUserOverride("font.path"));
    }

    @Test
    public void encodePdfLibraries_default_isFullOrderedCsv() throws Exception {
        PreferencesViewModel vm = new PreferencesViewModel();
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        vm.loadFrom(new AdvancedConfig(file, bundledDefaults), "");
        assertEquals("jpedal,pdfbox,openpdf", vm.encodePdfLibraries());
    }

    @Test
    public void encodePdfLibraries_disabledLibsOmitted() {
        PreferencesViewModel vm = new PreferencesViewModel();
        vm.decodePdfLibraries("jpedal,pdfbox,openpdf");
        vm.pdfLibOpenpdfProperty().set(false);
        assertEquals("jpedal,pdfbox", vm.encodePdfLibraries());
    }

    @Test
    public void decodeAndReencode_reflectsExplicitOrder() {
        PreferencesViewModel vm = new PreferencesViewModel();
        vm.decodePdfLibraries("openpdf,jpedal,pdfbox");
        assertEquals("openpdf,jpedal,pdfbox", vm.encodePdfLibraries());
    }

    @Test
    public void moveUpAndDown_reordersLibraries() {
        PreferencesViewModel vm = new PreferencesViewModel();
        vm.decodePdfLibraries("jpedal,pdfbox,openpdf");
        vm.moveUp(PreferencesViewModel.LIB_OPENPDF);
        assertEquals("jpedal,openpdf,pdfbox", vm.encodePdfLibraries());
        vm.moveDown(PreferencesViewModel.LIB_JPEDAL);
        assertEquals("openpdf,jpedal,pdfbox", vm.encodePdfLibraries());
    }

    @Test
    public void pkcs11Body_roundTripsExactly() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        PreferencesViewModel vm = new PreferencesViewModel();
        String body = "name=JSignPdf\n  library = /usr/lib/lib.so  \nslot=1\n\n";
        vm.loadFrom(new AdvancedConfig(file, bundledDefaults), body);
        assertEquals(body, vm.pkcs11BodyProperty().get());
    }

    @Test
    public void applyDefaults_loadsBundled() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        cfg.setProperty("relax.ssl.security", true);
        cfg.setProperty("tsa.hashAlgorithm", "SHA-1");
        PreferencesViewModel vm = new PreferencesViewModel();
        vm.loadFrom(cfg, "");
        assertTrue(vm.relaxSslSecurityProperty().get());
        assertEquals("SHA-1", vm.tsaHashAlgorithmProperty().get());
        vm.applyDefaults(new AdvancedConfig(null, bundledDefaults));
        assertFalse(vm.relaxSslSecurityProperty().get());
        assertEquals("SHA-256", vm.tsaHashAlgorithmProperty().get());
    }

    @Test
    public void validateFontPath_emptyOk_invalidNotOk() {
        assertTrue(PreferencesValidation.validateFontPath(""));
        assertTrue(PreferencesValidation.validateFontPath(null));
        assertFalse(PreferencesValidation.validateFontPath("/no/such/path/should/exist/forsure"));
    }

    @Test
    public void validatePdfLibSelection_atLeastOne() {
        assertTrue(PreferencesValidation.validatePdfLibSelection(true, false, false));
        assertTrue(PreferencesValidation.validatePdfLibSelection(false, true, true));
        assertFalse(PreferencesValidation.validatePdfLibSelection(false, false, false));
    }
}
