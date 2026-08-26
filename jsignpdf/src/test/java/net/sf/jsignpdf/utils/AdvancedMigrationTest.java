package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.jsignpdf.Constants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the accessor surface added in {@link ConfigLocationResolver}: the new advanced.properties / pkcs11.cfg paths
 * and the fresh-install no-op behaviour. Bundled jar resources are also exercised here.
 */
public class AdvancedMigrationTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    static byte[] bundled(String resource) throws IOException {
        try (InputStream is = AdvancedMigrationTest.class.getResourceAsStream(resource)) {
            assertNotNull("Bundled resource missing: " + resource, is);
            return is.readAllBytes();
        }
    }

    @Test
    public void freshInstall_noLegacyAnywhere_doesNotCreateAdvancedFile() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        Path xdg = tmp.newFolder("xdg").toPath();
        Map<String, String> env = new HashMap<>();
        env.put(ConfigLocationResolver.ENV_XDG_CONFIG_HOME, xdg.toString());

        ConfigLocationResolver r = new ConfigLocationResolver(
                ConfigLocationResolver.OsType.LINUX, env::get, home.toString());
        Path cfgDir = r.getConfigDir();
        assertNotNull(cfgDir);
        assertTrue(Files.isDirectory(cfgDir));
        // No edited install-dir file anywhere on disk -> migration is a no-op.
        assertFalse(Files.exists(cfgDir.resolve("advanced.properties")));
        assertFalse(Files.exists(cfgDir.resolve("pkcs11.cfg")));
        // Presets dir is still always present.
        assertTrue(Files.isDirectory(cfgDir.resolve("presets")));
    }

    @Test
    public void getAdvancedConfigFile_returnsExpectedPath() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        Path xdg = tmp.newFolder("xdg").toPath();
        Map<String, String> env = new HashMap<>();
        env.put(ConfigLocationResolver.ENV_XDG_CONFIG_HOME, xdg.toString());
        ConfigLocationResolver r = new ConfigLocationResolver(
                ConfigLocationResolver.OsType.LINUX, env::get, home.toString());
        assertEquals(xdg.resolve("jsignpdf").resolve("advanced.properties"), r.getAdvancedConfigFile());
        assertEquals(xdg.resolve("jsignpdf").resolve("pkcs11.cfg"), r.getPkcs11ConfigFile());
    }

    private static Properties pdfLibraryDefaults() {
        Properties bundled = new Properties();
        bundled.setProperty("pdf2image.libraries", Constants.PDF2IMAGE_LIBRARIES_DEFAULT);
        return bundled;
    }

    private Path advancedFileWith(String body) throws IOException {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        Files.writeString(file, body);
        return file;
    }

    @Test
    public void unstampedStaleLibraryOrder_isUpgradedAndStamped() throws Exception {
        Path file = advancedFileWith("pdf2image.libraries=" + Constants.PDF2IMAGE_LIBRARIES_LEGACY_DEFAULT + "\n");

        AdvancedConfig cfg = new AdvancedConfig(file, pdfLibraryDefaults());

        assertEquals(Constants.PDF2IMAGE_LIBRARIES_DEFAULT, cfg.getNotEmptyProperty("pdf2image.libraries", null));
        String onDisk = Files.readString(file);
        assertFalse("The superseded default must not survive on disk", onDisk.contains("pdf2image.libraries"));
        assertTrue("The migration must stamp the config version so it never runs again",
                onDisk.contains(Constants.PROPERTY_CONFIG_VERSION + "=" + Constants.CONFIG_VERSION));
    }

    @Test
    public void stampedLibraryOrder_survivesEvenWhenItMatchesTheOldDefault() throws Exception {
        // A user who deliberately reorders back to jpedal-first must keep that choice: once the config
        // carries the stamp, the migration is done second-guessing the value.
        Path file = advancedFileWith("pdf2image.libraries=" + Constants.PDF2IMAGE_LIBRARIES_LEGACY_DEFAULT + "\n"
                + Constants.PROPERTY_CONFIG_VERSION + "=" + Constants.CONFIG_VERSION + "\n");

        assertEquals(Constants.PDF2IMAGE_LIBRARIES_LEGACY_DEFAULT,
                new AdvancedConfig(file, pdfLibraryDefaults()).getNotEmptyProperty("pdf2image.libraries", null));
    }

    @Test
    public void migratedConfig_isNotMigratedAgainAfterAReorder() throws Exception {
        Path file = advancedFileWith("pdf2image.libraries=" + Constants.PDF2IMAGE_LIBRARIES_LEGACY_DEFAULT + "\n");
        new AdvancedConfig(file, pdfLibraryDefaults());

        // The user now picks the old order on purpose, the way Preferences would write it.
        AdvancedConfig cfg = new AdvancedConfig(file, pdfLibraryDefaults());
        cfg.setProperty("pdf2image.libraries", Constants.PDF2IMAGE_LIBRARIES_LEGACY_DEFAULT);
        cfg.save();

        assertEquals(Constants.PDF2IMAGE_LIBRARIES_LEGACY_DEFAULT,
                new AdvancedConfig(file, pdfLibraryDefaults()).getNotEmptyProperty("pdf2image.libraries", null));
    }

    @Test
    public void customLibraryOrder_isLeftAlone() throws Exception {
        Path file = advancedFileWith("pdf2image.libraries=openpdf,pdfbox\n");
        assertEquals("openpdf,pdfbox",
                new AdvancedConfig(file, pdfLibraryDefaults()).getNotEmptyProperty("pdf2image.libraries", null));
    }

    @Test
    public void emptyConfig_isNotStampedIntoExistence() throws Exception {
        Path file = tmp.newFolder().toPath().resolve("advanced.properties");
        new AdvancedConfig(file, pdfLibraryDefaults());
        assertFalse("A fresh install must still need no config file", Files.exists(file));
    }

    @Test
    public void bundledAdvancedDefaults_areReadable() throws IOException {
        byte[] bytes = bundled("/net/sf/jsignpdf/conf/advanced.default.properties");
        assertTrue("Bundled defaults must contain pdf2image.libraries entry",
                new String(bytes).contains("pdf2image.libraries=pdfbox,jpedal,openpdf"));
    }

    @Test
    public void bundledPkcs11Sample_isReadable() throws IOException {
        byte[] bytes = bundled("/net/sf/jsignpdf/conf/pkcs11.cfg.sample");
        assertTrue("Bundled PKCS#11 sample must contain a name entry",
                new String(bytes).contains("name=JSignPdf"));
    }
}
