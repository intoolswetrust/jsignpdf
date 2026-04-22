package net.sf.jsignpdf.fx.preset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.ConfigLocationResolver;
import net.sf.jsignpdf.utils.PropertyProvider;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

public class PresetManagerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private PropertyStoreFactory factory;

    @Before
    public void setUp() throws Exception {
        Path home = tmp.newFolder("home").toPath();
        Path xdg = tmp.newFolder("xdg").toPath();
        HashMap<String, String> env = new HashMap<>();
        env.put("XDG_CONFIG_HOME", xdg.toString());
        Function<String, String> envFn = env::get;
        ConfigLocationResolver resolver = new ConfigLocationResolver(
                ConfigLocationResolver.OsType.LINUX, envFn, home.toString());
        // Force dir creation up-front so PresetManager's scan sees the presets subdir.
        resolver.getConfigDir();
        factory = new PropertyStoreFactory(resolver);
    }

    private BasicSignerOptions sampleOptions() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setReason("Signed for audit");
        opts.setLocation("Prague");
        opts.setContact("sig@example.com");
        opts.setTimestamp(true);
        opts.setTsaUrl("http://tsa.example.com/tsa");
        return opts;
    }

    @Test
    public void saveAsNew_persistsAndAppearsInList() {
        PresetManager mgr = new PresetManager(factory);
        Preset preset = mgr.saveAsNew(sampleOptions(), "My preset");

        assertNotNull(preset);
        assertEquals("My preset", preset.getDisplayName());
        assertEquals(1, mgr.getPresets().size());
        assertTrue(preset.getFilename().startsWith("preset-"));
        assertTrue(preset.getFilename().endsWith(".properties"));
        assertNotNull(preset.getCreatedAt());
    }

    @Test
    public void savedPreset_survivesScanByNewManager() {
        PresetManager mgr = new PresetManager(factory);
        mgr.saveAsNew(sampleOptions(), "First");
        mgr.saveAsNew(sampleOptions(), "Second");

        PresetManager reloaded = new PresetManager(factory);
        List<Preset> found = reloaded.getPresets();
        assertEquals(2, found.size());
        // Sorted case-insensitively by display name
        assertEquals("First", found.get(0).getDisplayName());
        assertEquals("Second", found.get(1).getDisplayName());
    }

    @Test
    public void load_populatesOptions_includingPasswords() {
        PresetManager mgr = new PresetManager(factory);
        BasicSignerOptions source = sampleOptions();
        source.setKsPasswd("should-persist");
        Preset p = mgr.saveAsNew(source, "With TSA");

        BasicSignerOptions target = new BasicSignerOptions();
        target.setKsPasswd("live-pwd");
        mgr.load(p, target);

        assertEquals("Signed for audit", target.getReason());
        assertEquals("Prague", target.getLocation());
        assertEquals("http://tsa.example.com/tsa", target.getTsaUrl());
        // Password fields must be overwritten by preset load
        assertEquals("should-persist", target.getKsPasswdStr());
    }

    @Test
    public void load_passwordlessPreset_clearsStalePasswords() {
        // Save a preset explicitly opting out of storing passwords. The file
        // therefore contains no encrypted password keys.
        PresetManager mgr = new PresetManager(factory);
        BasicSignerOptions source = sampleOptions();
        source.setStorePasswords(false);
        source.setKsPasswd("not-persisted");
        Preset p = mgr.saveAsNew(source, "Shareable");

        // Simulate the bleed scenario: a previous preset load left credentials
        // in the live options; the user now switches to the passwordless preset.
        BasicSignerOptions target = new BasicSignerOptions();
        target.setKsPasswd("stale-from-earlier");
        target.setKeyPasswd("stale-key");
        target.setPdfOwnerPwd("stale-owner");
        target.setPdfUserPwd("stale-user");
        target.setTsaPasswd("stale-tsa");
        target.setTsaCertFilePwd("stale-tsa-cert");
        mgr.load(p, target);

        assertNull("ks password must be cleared on passwordless preset load", target.getKsPasswd());
        assertNull(target.getKeyPasswd());
        assertNull(target.getPdfOwnerPwd());
        assertNull(target.getPdfUserPwd());
        assertNull(target.getTsaPasswd());
        assertNull(target.getTsaCertFilePwd());
    }

    @Test
    public void rename_preservesFilenameAndCreatedAt() {
        PresetManager mgr = new PresetManager(factory);
        Preset p = mgr.saveAsNew(sampleOptions(), "Old name");
        String origFilename = p.getFilename();

        mgr.rename(p, "New name");

        assertEquals(1, mgr.getPresets().size());
        Preset renamed = mgr.getPresets().get(0);
        assertEquals("New name", renamed.getDisplayName());
        assertEquals(origFilename, renamed.getFilename());
        assertEquals(p.getCreatedAt(), renamed.getCreatedAt());
    }

    @Test
    public void overwrite_replacesContent_butKeepsFilenameAndDisplayName() throws Exception {
        PresetManager mgr = new PresetManager(factory);
        Preset p = mgr.saveAsNew(sampleOptions(), "Stable name");

        BasicSignerOptions updated = new BasicSignerOptions();
        updated.setReason("Updated reason");
        mgr.overwrite(p, updated);

        // File still exists under the same name
        PropertyProvider store = factory.preset(p.getFilename());
        assertEquals("Stable name", store.getProperty(PresetManager.KEY_DISPLAY_NAME));
        assertEquals("Updated reason", store.getProperty(Constants.PROPERTY_REASON));
    }

    @Test
    public void delete_removesFileAndFromList() {
        PresetManager mgr = new PresetManager(factory);
        Preset p = mgr.saveAsNew(sampleOptions(), "Throwaway");
        Path file = factory.getResolver().getPresetsDir().resolve(p.getFilename());
        assertTrue(Files.exists(file));

        mgr.delete(p);

        assertFalse(Files.exists(file));
        assertTrue(mgr.getPresets().isEmpty());
    }

    @Test
    public void scan_ignoresFilesMissingDisplayName() throws Exception {
        Path presetsDir = factory.getResolver().getPresetsDir();
        Files.createDirectories(presetsDir);
        Files.writeString(presetsDir.resolve("orphan.properties"),
                Constants.PROPERTY_REASON + "=no name here\n");

        PresetManager mgr = new PresetManager(factory);
        assertTrue("Preset file without displayName must be skipped",
                mgr.getPresets().isEmpty());
    }

    @Test
    public void hasDisplayName_caseInsensitiveCheck() {
        PresetManager mgr = new PresetManager(factory);
        mgr.saveAsNew(sampleOptions(), "Work");
        assertTrue(mgr.hasDisplayName("work"));
        assertTrue(mgr.hasDisplayName("WORK"));
        assertFalse(mgr.hasDisplayName("Home"));
    }

    @Test
    public void hasDisplayName_excludesGivenPreset() {
        PresetManager mgr = new PresetManager(factory);
        Preset p = mgr.saveAsNew(sampleOptions(), "Work");
        // Same name but excluding itself: not a duplicate (enables rename to same name / case).
        assertFalse(mgr.hasDisplayName("work", p));
    }

    @Test
    public void newPreset_collidingTimestamp_getsCounterSuffix() {
        // Two newPreset() calls within the same millisecond must get different paths.
        PropertyProvider first = factory.newPreset();
        first.save();
        PropertyProvider second = factory.newPreset();
        second.save();

        assertNotNull(first.getPath());
        assertNotNull(second.getPath());
        assertFalse("Collision-retry should produce distinct filenames",
                first.getPath().getFileName().equals(second.getPath().getFileName()));
    }

    @Test
    public void preset_withCreatedAt_roundTripsInstant() {
        PresetManager mgr = new PresetManager(factory);
        Preset p = mgr.saveAsNew(sampleOptions(), "ISO test");

        PresetManager reloaded = new PresetManager(factory);
        Preset found = reloaded.getPresets().get(0);
        assertNotNull("createdAt should round-trip", found.getCreatedAt());
        assertEquals(p.getCreatedAt().getEpochSecond(), found.getCreatedAt().getEpochSecond());
    }

    @Test
    public void load_nonExistentPreset_leavesNoTrace() {
        PresetManager mgr = new PresetManager(factory);
        BasicSignerOptions target = new BasicSignerOptions();
        Preset phantom = new Preset("preset-nothing.properties", "Phantom", null);
        // Should not throw; options just stay at defaults.
        mgr.load(phantom, target);
        assertNull(target.getReason());
    }
}
