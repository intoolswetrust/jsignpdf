package net.sf.jsignpdf.fx.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    /**
     * Output directories the user has picked through the portal directory chooser this session. A portal
     * {@code OpenFile(directory=true)} grant is scoped to the chosen folder subtree and lasts only for the running
     * instance, so a directory that merely came from a preset or the stored config carries no write grant until the
     * user re-picks it. Tracked absolute and normalized.
     */
    private static final Set<String> GRANTED_DIRS = ConcurrentHashMap.newKeySet();

    /**
     * Records a directory (and thus its subtree) as granted for writing this session. Blank paths are ignored.
     */
    public static void recordDirectoryGrant(String dir) {
        String key = normalizeDir(dir);
        if (key != null) {
            GRANTED_DIRS.add(key);
        }
    }

    /**
     * Returns true when writes into {@code dir} are known to be permitted: always outside the sandbox, and inside it
     * only when the directory or one of its ancestors was granted this session via {@link #recordDirectoryGrant}.
     */
    public static boolean hasDirectoryGrant(String dir) {
        return hasDirectoryGrant(dir, isSandboxed());
    }

    static boolean hasDirectoryGrant(String dir, boolean sandboxed) {
        if (!sandboxed) {
            return true;
        }
        String key = normalizeDir(dir);
        if (key == null) {
            return false;
        }
        for (String granted : GRANTED_DIRS) {
            if (key.equals(granted) || key.startsWith(granted + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears recorded grants. Test-only.
     */
    static void clearDirectoryGrants() {
        GRANTED_DIRS.clear();
    }

    private static String normalizeDir(String dir) {
        if (dir == null || dir.isBlank()) {
            return null;
        }
        String abs = new File(dir).getAbsolutePath().replace('\\', '/');
        return abs.length() > 1 && abs.endsWith("/") ? abs.substring(0, abs.length() - 1) : abs;
    }
}
