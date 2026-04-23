package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link PropertyStoreFactory#resetAll()} — the shared logic behind the "Reset Settings" menu item.
 */
public class PropertyStoreFactoryResetTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private PropertyStoreFactory factory;
    private Path configDir;

    @Before
    public void setUp() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        Path xdg = tmp.newFolder("xdg").toPath();
        Map<String, String> env = new HashMap<>();
        env.put("XDG_CONFIG_HOME", xdg.toString());
        ConfigLocationResolver resolver = new ConfigLocationResolver(
                ConfigLocationResolver.OsType.LINUX, env::get, home.toString());
        configDir = resolver.getConfigDir();
        factory = new PropertyStoreFactory(resolver);
    }

    @Test
    public void resetAll_removesMainConfigFile() throws Exception {
        PropertyProvider main = factory.mainConfig();
        main.setProperty("keystore.type", "PKCS12");
        main.save();
        Path mainFile = main.getPath();
        assertTrue(Files.exists(mainFile));

        factory.resetAll();

        assertFalse("main config file must be gone after resetAll", Files.exists(mainFile));
    }

    @Test
    public void resetAll_removesAllPresetFiles() throws Exception {
        PropertyProvider p1 = factory.newPreset();
        p1.setProperty("preset.displayName", "First");
        p1.save();
        PropertyProvider p2 = factory.newPreset();
        p2.setProperty("preset.displayName", "Second");
        p2.save();

        Path presetsDir = factory.getResolver().getPresetsDir();
        assertEquals(2, Files.list(presetsDir).count());

        factory.resetAll();

        // Presets dir may have been removed along with its contents — either way, no files survive.
        if (Files.isDirectory(presetsDir)) {
            assertEquals(0, Files.list(presetsDir).count());
        }
    }

    @Test
    public void resetAll_clearsInMemoryMainConfig() {
        PropertyProvider main = factory.mainConfig();
        main.setProperty("keystore.type", "PKCS12");
        main.setProperty("signature.reason", "audit");

        factory.resetAll();

        assertNull("main config keys must be cleared in memory", main.getProperty("keystore.type"));
        assertNull(main.getProperty("signature.reason"));
    }

    @Test
    public void resetAll_keepsCachedMainConfigInstance() {
        PropertyProvider before = factory.mainConfig();
        factory.resetAll();
        PropertyProvider after = factory.mainConfig();
        // Every holder (BasicSignerOptions, RecentFilesManager) still references `before`; after reset they must see the
        // cleared state, which only works if the cached instance is preserved.
        assertSame("resetAll must keep the cached mainConfig instance", before, after);
    }

    @Test
    public void resetAll_keepsConfigDir() {
        factory.mainConfig().setProperty("k", "v");
        factory.resetAll();
        assertTrue("config dir itself must be preserved so future writes succeed",
                Files.isDirectory(configDir));
    }

    @Test
    public void resetAll_isNoOp_whenNothingHasBeenWritten() {
        // Fresh factory, no mainConfig() call, no preset writes. resetAll must not throw.
        factory.resetAll();
        assertTrue(Files.isDirectory(configDir));
    }

    @Test
    public void resetAll_removesUnknownSubdirectoriesAndFiles() throws Exception {
        // If a future version drops state into a sibling dir, "reset settings" should sweep it away too.
        Path customFile = configDir.resolve("extra.txt");
        Files.writeString(customFile, "leftover");
        Path customSub = configDir.resolve("nested").resolve("deep");
        Files.createDirectories(customSub);
        Files.writeString(customSub.resolve("note.txt"), "buried");

        factory.resetAll();

        assertFalse(Files.exists(customFile));
        assertFalse(Files.exists(customSub));
        assertFalse(Files.exists(configDir.resolve("nested")));
    }

    @Test
    public void resetAll_allowsSubsequentWrite() throws Exception {
        PropertyProvider main = factory.mainConfig();
        main.setProperty("k1", "v1");
        main.save();

        factory.resetAll();

        // After reset, a fresh write must succeed (dir was kept, in-memory was cleared).
        main.setProperty("k2", "v2");
        main.save();

        assertTrue(Files.exists(main.getPath()));
        // Reload from disk to verify only the post-reset write is present.
        PropertyProvider reloaded = new PropertyProvider(main.getPath());
        reloaded.load();
        assertNull(reloaded.getProperty("k1"));
        assertEquals("v2", reloaded.getProperty("k2"));
    }
}
