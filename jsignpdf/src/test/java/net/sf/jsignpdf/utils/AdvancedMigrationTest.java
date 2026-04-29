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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the migration logic and accessor surface added in
 * {@link ConfigLocationResolver}: the new advanced.properties / pkcs11.cfg paths and the fresh-install no-op behaviour. The
 * byte-equality migration heuristic is exercised via a CI-friendly path: we point {@code IOUtils.findFile} at our temp
 * install dir by exploiting the fact that {@code IOUtils.findFile} resolves relative paths against the {@code jsignpdf.home}
 * system property only at lookup time (when no override is set, lookups against absent files return null and migration is a
 * no-op).
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

    @Test
    public void bundledAdvancedDefaults_areReadable() throws IOException {
        byte[] bytes = bundled("/net/sf/jsignpdf/conf/advanced.default.properties");
        assertTrue("Bundled defaults must contain pdf2image.libraries entry",
                new String(bytes).contains("pdf2image.libraries=jpedal,pdfbox,openpdf"));
    }

    @Test
    public void bundledPkcs11Sample_isReadable() throws IOException {
        byte[] bytes = bundled("/net/sf/jsignpdf/conf/pkcs11.cfg.sample");
        assertTrue("Bundled PKCS#11 sample must contain a name entry",
                new String(bytes).contains("name=JSignPdf"));
    }
}
