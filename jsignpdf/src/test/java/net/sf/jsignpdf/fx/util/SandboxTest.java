package net.sf.jsignpdf.fx.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Test;

public class SandboxTest {

    @Test
    public void isLinux_trueForLinuxOsName() {
        assertTrue(Sandbox.isLinux("Linux"));
        assertTrue(Sandbox.isLinux("linux"));
    }

    @Test
    public void isLinux_falseForNonLinuxOsName() {
        assertFalse(Sandbox.isLinux("Windows 10"));
        assertFalse(Sandbox.isLinux("Mac OS X"));
        assertFalse(Sandbox.isLinux(""));
    }

    @Test
    public void isSandboxed_trueWhenFlatpakInfoExists() throws Exception {
        Path tmp = Files.createTempFile("flatpak-info-test", "");
        try {
            assertTrue(Sandbox.isSandboxed(tmp, Map.of()));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void isSandboxed_trueWhenFlatpakIdEnvSet() {
        assertTrue(Sandbox.isSandboxed(Path.of("/nonexistent"),
                Map.of("FLATPAK_ID", "com.example.App")));
    }

    @Test
    public void isSandboxed_trueWhenSnapEnvSet() {
        assertTrue(Sandbox.isSandboxed(Path.of("/nonexistent"),
                Map.of("SNAP", "/snap/myapp/1")));
    }

    @Test
    public void isSandboxed_falseWhenNoSignals() {
        assertFalse(Sandbox.isSandboxed(Path.of("/nonexistent"), Map.of()));
    }
}
