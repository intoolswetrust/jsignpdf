package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.jsignpdf.types.BufferingMode;

/**
 * Covers {@link AppConfig#bufferingMode()} / {@link AppConfig#bufferingTempDir()}: the default must stay
 * {@code memory} (a silent flip would be invisible in the output bytes), an unrecognised mode must degrade
 * rather than fail, and an unusable {@code buffering.tempDir} must be rejected instead of silently falling
 * back to {@code java.io.tmpdir}.
 */
public class BufferingConfigTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final AdvancedConfig cfg = PropertyStoreFactory.getInstance().advancedConfig();

    @After
    public void restore() {
        cfg.removeProperty(AppConfig.KEY_BUFFERING_MODE);
        cfg.removeProperty(AppConfig.KEY_BUFFERING_TEMP_DIR);
    }

    @Test
    public void defaultModeIsMemory() {
        cfg.removeProperty(AppConfig.KEY_BUFFERING_MODE);
        assertSame("The bundled default must stay MEMORY", BufferingMode.MEMORY, AppConfig.bufferingMode());
    }

    @Test
    public void modeIsReadFromTheKeyCaseInsensitively() {
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "temp");
        assertSame(BufferingMode.TEMP, AppConfig.bufferingMode());

        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "TEMP");
        assertSame(BufferingMode.TEMP, AppConfig.bufferingMode());

        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, " Memory ");
        assertSame(BufferingMode.MEMORY, AppConfig.bufferingMode());
    }

    @Test
    public void unknownModeFallsBackToMemory() {
        cfg.setProperty(AppConfig.KEY_BUFFERING_MODE, "disk");
        assertSame("A typo must degrade to today's behaviour, not fail the sign",
                BufferingMode.MEMORY, AppConfig.bufferingMode());
    }

    @Test
    public void emptyTempDirMeansSystemDefault() throws IOException {
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, "");
        assertNull("Empty means java.io.tmpdir", AppConfig.bufferingTempDir());
    }

    @Test
    public void configuredTempDirIsReturned() throws Exception {
        File dir = tempFolder.newFolder("staging");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, dir.getAbsolutePath());
        assertEquals(dir.getAbsolutePath(), AppConfig.bufferingTempDir().getAbsolutePath());
    }

    @Test
    public void missingTempDirIsRejected() {
        File missing = new File(tempFolder.getRoot(), "does-not-exist");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, missing.getAbsolutePath());
        try {
            AppConfig.bufferingTempDir();
            fail("A missing buffering.tempDir must be rejected, not created or ignored");
        } catch (IOException expected) {
            // no silent fallback to java.io.tmpdir, and no mkdirs()
        }
        org.junit.Assert.assertFalse("The directory must not be created", missing.exists());
    }

    @Test
    public void fileInsteadOfDirectoryIsRejected() throws Exception {
        File notADir = tempFolder.newFile("regular.txt");
        cfg.setProperty(AppConfig.KEY_BUFFERING_TEMP_DIR, notADir.getAbsolutePath());
        try {
            AppConfig.bufferingTempDir();
            fail("A regular file must not be accepted as buffering.tempDir");
        } catch (IOException expected) {
            // expected
        }
    }
}
