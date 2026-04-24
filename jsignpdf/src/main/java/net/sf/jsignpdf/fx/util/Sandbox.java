package net.sf.jsignpdf.fx.util;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Sandbox {

    private Sandbox() {}

    public static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    public static boolean isSandboxed() {
        return Files.exists(Path.of("/.flatpak-info"))
                || System.getenv("FLATPAK_ID") != null
                || System.getenv("SNAP") != null;
    }
}
