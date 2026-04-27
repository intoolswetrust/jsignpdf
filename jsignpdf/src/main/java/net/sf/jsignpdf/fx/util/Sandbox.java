package net.sf.jsignpdf.fx.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

public final class Sandbox {

    /**
     * Files opened via the XDG Document portal appear at
     * {@code /run/user/<uid>/doc/<docid>/<original-name>}. The mount only exposes the
     * single granted file back to the host — writing a sibling with a different name
     * stays trapped inside the FUSE namespace. Detecting this prefix lets us redirect
     * the Save target to a real host location.
     */
    private static final Pattern DOC_PORTAL_PATH = Pattern.compile("^/run/user/\\d+/doc/.+");

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

    public static boolean isDocPortalPath(String path) {
        return path != null && DOC_PORTAL_PATH.matcher(path).matches();
    }
}
