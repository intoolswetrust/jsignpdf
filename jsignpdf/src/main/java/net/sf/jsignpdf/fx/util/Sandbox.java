package net.sf.jsignpdf.fx.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Sandbox {

    private Sandbox() {}

    public static boolean isLinux() {
        return isLinux(System.getProperty("os.name", ""));
    }

    static boolean isLinux(String osName) {
        return osName.toLowerCase().contains("linux");
    }

    public static boolean isSandboxed() {
        return isSandboxed(Path.of("/.flatpak-info"), System.getenv());
    }

    static boolean isSandboxed(Path flatpakInfo, Map<String, String> env) {
        return Files.exists(flatpakInfo)
                || env.get("FLATPAK_ID") != null
                || env.get("SNAP") != null;
    }
}
