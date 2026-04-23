package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link ConfigLocationResolver}.
 */
public class ConfigLocationResolverTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static ConfigLocationResolver newResolver(ConfigLocationResolver.OsType os, Map<String, String> env, String userHome) {
        return new ConfigLocationResolver(os, env::get, userHome);
    }

    @Test
    public void linux_usesXdgConfigHome_whenSet() throws Exception {
        Path xdg = tmp.newFolder("xdg-config").toPath();
        Map<String, String> env = new HashMap<>();
        env.put("XDG_CONFIG_HOME", xdg.toString());

        Path home = tmp.newFolder("home").toPath();
        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.LINUX, env, home.toString());
        assertEquals(xdg.resolve("jsignpdf"), r.resolveAndMigrate());
    }

    @Test
    public void linux_fallsBackToDotConfig_whenXdgUnset() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.LINUX, new HashMap<>(), home.toString());
        Path resolved = r.resolveAndMigrate();
        assertEquals(home.resolve(".config").resolve("jsignpdf"), resolved);
    }

    @Test
    public void windows_usesAppData_whenSet() throws Exception {
        Path appData = tmp.newFolder("appdata").toPath();
        Map<String, String> env = new HashMap<>();
        env.put("APPDATA", appData.toString());

        Path home = tmp.newFolder("home").toPath();
        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.WINDOWS, env, home.toString());
        assertEquals(appData.resolve("JSignPdf"), r.resolveAndMigrate());
    }

    @Test
    public void windows_fallsBackToUserProfile_whenAppDataUnset() throws Exception {
        Path userProfile = tmp.newFolder("userprofile").toPath();
        Map<String, String> env = new HashMap<>();
        env.put("USERPROFILE", userProfile.toString());

        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.WINDOWS, env, userProfile.toString());
        assertEquals(userProfile.resolve("AppData").resolve("Roaming").resolve("JSignPdf"), r.resolveAndMigrate());
    }

    @Test
    public void mac_usesLibraryApplicationSupport() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.MAC, new HashMap<>(), home.toString());
        assertEquals(home.resolve("Library").resolve("Application Support").resolve("JSignPdf"),
                r.resolveAndMigrate());
    }

    @Test
    public void envOverride_takesPrecedence() throws Exception {
        Path override = tmp.newFolder("override").toPath();
        Map<String, String> env = new HashMap<>();
        env.put("JSIGNPDF_CONFIG_DIR", override.toString());
        env.put("XDG_CONFIG_HOME", "/should/not/be/used");

        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.LINUX, env, "/home/irrelevant");
        assertEquals(override, r.resolveAndMigrate());
    }

    @Test
    public void missingHome_returnsNull() {
        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.LINUX, new HashMap<>(), null);
        assertNull(r.resolveAndMigrate());
    }

    @Test
    public void migration_copiesLegacyFileToNewLocation() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        Path legacy = home.resolve(".JSignPdf");
        Files.writeString(legacy, "keystore.type=PKCS12\nfoo=bar\n");

        Map<String, String> env = new HashMap<>();
        env.put("XDG_CONFIG_HOME", tmp.newFolder("xdg").toString());
        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.LINUX, env, home.toString());

        Path cfgDir = r.getConfigDir();
        assertNotNull(cfgDir);
        Path newCfg = cfgDir.resolve("config.properties");
        assertTrue("Migrated config should exist", Files.isRegularFile(newCfg));
        assertTrue("Presets subdir should exist", Files.isDirectory(cfgDir.resolve("presets")));
        assertEquals(Files.readString(legacy), Files.readString(newCfg));
        assertTrue("Legacy file must not be removed", Files.exists(legacy));
    }

    @Test
    public void migration_isNoOp_whenTargetExists() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        Path legacy = home.resolve(".JSignPdf");
        Files.writeString(legacy, "legacy=yes");

        Path xdgRoot = tmp.newFolder("xdg").toPath();
        Path existingCfg = xdgRoot.resolve("jsignpdf");
        Files.createDirectories(existingCfg);
        Path existingMain = existingCfg.resolve("config.properties");
        Files.writeString(existingMain, "preexisting=true");

        Map<String, String> env = new HashMap<>();
        env.put("XDG_CONFIG_HOME", xdgRoot.toString());
        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.LINUX, env, home.toString());

        Path cfgDir = r.getConfigDir();
        assertEquals(existingCfg, cfgDir);
        assertEquals("preexisting=true", Files.readString(existingMain));
    }

    @Test
    public void freshInstall_createsEmptyDirectory_whenLegacyAbsent() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        Path xdgRoot = tmp.newFolder("xdg").toPath();

        Map<String, String> env = new HashMap<>();
        env.put("XDG_CONFIG_HOME", xdgRoot.toString());
        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.LINUX, env, home.toString());

        Path cfgDir = r.getConfigDir();
        assertNotNull(cfgDir);
        assertTrue(Files.isDirectory(cfgDir));
        assertFalse(Files.exists(cfgDir.resolve("config.properties")));
    }

    @Test
    public void getMainConfigFile_returnsExpectedPath() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        Path xdgRoot = tmp.newFolder("xdg").toPath();
        Map<String, String> env = new HashMap<>();
        env.put("XDG_CONFIG_HOME", xdgRoot.toString());

        ConfigLocationResolver r = newResolver(ConfigLocationResolver.OsType.LINUX, env, home.toString());
        assertEquals(xdgRoot.resolve("jsignpdf").resolve("config.properties"), r.getMainConfigFile());
        assertEquals(xdgRoot.resolve("jsignpdf").resolve("presets"), r.getPresetsDir());
    }
}
