package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link AdvancedConfig}: bundled-default fallback, user overrides, reload, reset and the change-set returned by
 * save.
 */
public class AdvancedConfigTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Properties bundledDefaults;

    @Before
    public void loadDefaults() throws IOException {
        bundledDefaults = new Properties();
        try (InputStream is = AdvancedConfigTest.class
                .getResourceAsStream("/net/sf/jsignpdf/conf/advanced.default.properties")) {
            bundledDefaults.load(is);
        }
    }

    @Test
    public void bundledDefaultsFallback_whenUserFileAbsent() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        assertFalse(Files.exists(file));
        assertFalse(cfg.getAsBool("relax.ssl.security", true));
        assertEquals("SHA-256", cfg.getNotEmptyProperty("tsa.hashAlgorithm", "should-not-be-used"));
        assertEquals("jpedal,pdfbox,openpdf", cfg.getNotEmptyProperty("pdf2image.libraries", null));
        assertTrue(cfg.getAsBool("certificate.checkValidity", false));
        assertTrue(cfg.getAsBool("certificate.checkKeyUsage", false));
        assertFalse(cfg.getAsBool("certificate.checkCriticalExtensions", true));
    }

    @Test
    public void userFileOverridesBundledDefaults() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        Files.writeString(file,
                "relax.ssl.security=true\n" +
                "tsa.hashAlgorithm=SHA-512\n" +
                "pdf2image.libraries=pdfbox\n");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        assertTrue(cfg.getAsBool("relax.ssl.security", false));
        assertEquals("SHA-512", cfg.getNotEmptyProperty("tsa.hashAlgorithm", null));
        assertEquals("pdfbox", cfg.getNotEmptyProperty("pdf2image.libraries", null));
        // Unset key still falls back to bundled default.
        assertTrue(cfg.getAsBool("certificate.checkValidity", false));
    }

    @Test
    public void reload_picksUpOutOfBandEdits() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        assertFalse(cfg.getAsBool("relax.ssl.security", true));
        Files.writeString(file, "relax.ssl.security=true\n");
        cfg.reload();
        assertTrue(cfg.getAsBool("relax.ssl.security", false));
    }

    @Test
    public void resetToDefaults_deletesUserFile() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        Files.writeString(file, "relax.ssl.security=true\n");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        assertTrue(cfg.getAsBool("relax.ssl.security", false));
        cfg.resetToDefaults();
        assertFalse(Files.exists(file));
        assertFalse(cfg.getAsBool("relax.ssl.security", true));
    }

    @Test
    public void removeProperty_revertsSingleKey() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        Files.writeString(file, "relax.ssl.security=true\ntsa.hashAlgorithm=SHA-1\n");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        cfg.removeProperty("relax.ssl.security");
        // Falls back to bundled default.
        assertFalse(cfg.getAsBool("relax.ssl.security", true));
        // Other override survives.
        assertEquals("SHA-1", cfg.getNotEmptyProperty("tsa.hashAlgorithm", null));
    }

    @Test
    public void save_returnsChangedKeys() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        // First save with no edits -> no changes.
        Set<String> changed = cfg.save();
        assertTrue("No edits, no changes", changed.isEmpty());

        cfg.setProperty("relax.ssl.security", true);
        cfg.setProperty("tsa.hashAlgorithm", "SHA-1");
        changed = cfg.save();
        assertTrue(changed.contains("relax.ssl.security"));
        assertTrue(changed.contains("tsa.hashAlgorithm"));

        // Subsequent identical save -> no changes.
        changed = cfg.save();
        assertTrue(changed.isEmpty());

        // Removing a key shows up as a change.
        cfg.removeProperty("relax.ssl.security");
        changed = cfg.save();
        assertTrue(changed.contains("relax.ssl.security"));
    }

    @Test
    public void save_persistsToDisk() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        cfg.setProperty("tsa.hashAlgorithm", "SHA-512");
        cfg.save();
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.readString(file).contains("tsa.hashAlgorithm=SHA-512"));
    }

    @Test
    public void getProperty_unknownKey_returnsNull() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        assertNull(cfg.getProperty("does.not.exist"));
    }

    @Test
    public void applyOverrides_winsOverUserLayerAndBundledDefaults() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        Files.writeString(file, "relax.ssl.security=false\ntsa.hashAlgorithm=SHA-256\n");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        cfg.applyOverrides(Map.of(
                "relax.ssl.security", "true",
                "tsa.hashAlgorithm", "SHA-512",
                "pdf2image.libraries", "pdfbox"));
        assertTrue("override beats user layer", cfg.getAsBool("relax.ssl.security", false));
        assertEquals("override beats user layer", "SHA-512", cfg.getNotEmptyProperty("tsa.hashAlgorithm", null));
        assertEquals("override beats bundled default", "pdfbox", cfg.getNotEmptyProperty("pdf2image.libraries", null));
    }

    @Test
    public void applyOverrides_emptyValueSetsEmptyString() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        cfg.applyOverrides(Map.of("tsa.hashAlgorithm", ""));
        assertEquals("", cfg.getProperty("tsa.hashAlgorithm"));
    }

    @Test
    public void applyOverrides_isNotPersistedNorReportedAsUserOverride() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        cfg.applyOverrides(Map.of("relax.ssl.security", "true"));
        assertFalse("transient override is not a user override", cfg.hasUserOverride("relax.ssl.security"));
        Set<String> changed = cfg.save();
        assertTrue("overrides never reach the change set", changed.isEmpty());
        assertFalse("overrides are never written to disk",
                Files.exists(file) && Files.readString(file).contains("relax.ssl.security"));
    }

    @Test
    public void applyOverrides_nullMapIsNoOp() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        AdvancedConfig cfg = new AdvancedConfig(file, bundledDefaults);
        cfg.applyOverrides(null);
        assertFalse("falls through to the bundled default", cfg.getAsBool("relax.ssl.security", true));
    }
}
