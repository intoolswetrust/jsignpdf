package net.sf.jsignpdf.pkcs11;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds PKCS#11 libraries of catalog entries that exist on this machine. Offline; never loads a library.
 */
public final class Pkcs11Detector {

    /** The only catalog entry offered inside Flatpak, where host library paths are not usable. */
    public static final String FLATPAK_ENTRY_ID = "p11-kit-proxy";

    private static final Pattern WIN_VAR = Pattern.compile("%([A-Za-z0-9_()]+)%");
    private static final Pattern UNIX_VAR = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}|\\$([A-Za-z0-9_]+)");

    public record Candidate(Pkcs11Catalog.Entry entry, Path library) {
    }

    private final String os;
    private final String arch;
    private final Function<String, String> env;
    private final String userHome;
    private final boolean flatpak;

    public Pkcs11Detector(String os, String arch, Function<String, String> env, String userHome, boolean flatpak) {
        this.os = os;
        this.arch = arch;
        this.env = env;
        this.userHome = userHome;
        this.flatpak = flatpak;
    }

    public static Pkcs11Detector forCurrentPlatform(boolean flatpak) {
        return new Pkcs11Detector(normalizeOs(System.getProperty("os.name", "")),
                normalizeArch(System.getProperty("os.arch", "")), System::getenv, System.getProperty("user.home"),
                flatpak);
    }

    public String os() {
        return os;
    }

    public String arch() {
        return arch;
    }

    public boolean isFlatpak() {
        return flatpak;
    }

    public static String normalizeOs(String osName) {
        String n = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (n.startsWith("windows")) {
            return "windows";
        }
        if (n.startsWith("mac") || n.contains("darwin")) {
            return "macos";
        }
        return "linux";
    }

    public static String normalizeArch(String osArch) {
        String a = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        return switch (a) {
            case "amd64", "x86_64", "x86-64", "x64" -> "x86_64";
            case "arm64", "aarch64" -> "aarch64";
            default -> a;
        };
    }

    /**
     * Catalog entries usable on this platform (only {@link #FLATPAK_ENTRY_ID} inside Flatpak).
     */
    public List<Pkcs11Catalog.Entry> applicableEntries(Pkcs11Catalog catalog) {
        List<Pkcs11Catalog.Entry> result = new ArrayList<>();
        for (Pkcs11Catalog.Entry e : catalog.entries()) {
            if (flatpak && !FLATPAK_ENTRY_ID.equals(e.id())) {
                continue;
            }
            if (!e.librariesFor(os, arch).isEmpty()) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Libraries of all applicable entries found on this machine; per path pattern the newest file comes first.
     */
    public List<Candidate> detect(Pkcs11Catalog catalog) {
        List<Candidate> result = new ArrayList<>();
        for (Pkcs11Catalog.Entry e : applicableEntries(catalog)) {
            for (Path p : find(e)) {
                result.add(new Candidate(e, p));
            }
        }
        return result;
    }

    /**
     * Existing library files for one entry.
     */
    public List<Path> find(Pkcs11Catalog.Entry entry) {
        Set<Path> found = new LinkedHashSet<>();
        Set<Path> real = new LinkedHashSet<>();
        for (String pattern : expandedPatterns(entry)) {
            List<Path> matches = glob(pattern);
            matches.sort(Comparator.comparingLong(Pkcs11Detector::lastModified).reversed());
            for (Path m : matches) {
                Path r = m;
                try {
                    r = m.toRealPath();
                } catch (IOException ignored) {
                    // keep the path as found
                }
                if (real.add(r)) {
                    found.add(m);
                }
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * Path patterns of the entry for this platform with variables expanded; patterns with an undefined variable are
     * dropped.
     */
    public List<String> expandedPatterns(Pkcs11Catalog.Entry entry) {
        List<String> result = new ArrayList<>();
        for (Pkcs11Catalog.Library lib : entry.librariesFor(os, arch)) {
            for (String p : lib.paths()) {
                String x = expand(p);
                if (x != null) {
                    result.add(x);
                }
            }
        }
        return result;
    }

    String expand(String pattern) {
        String p = pattern;
        if (p.equals("~") || p.startsWith("~/") || p.startsWith("~\\")) {
            if (userHome == null) {
                return null;
            }
            p = userHome + p.substring(1);
        }
        p = replaceVars(p, WIN_VAR);
        if (p == null) {
            return null;
        }
        return replaceVars(p, UNIX_VAR);
    }

    private String replaceVars(String s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1) != null ? m.group(1) : m.group(2);
            String v = env.apply(name);
            if (v == null || v.isEmpty()) {
                return null;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static List<Path> glob(String pattern) {
        String norm = pattern.replace('\\', '/');
        List<Path> result = new ArrayList<>();
        if (!norm.contains("*") && !norm.contains("?")) {
            Path p = Path.of(pattern);
            if (Files.isRegularFile(p)) {
                result.add(p);
            }
            return result;
        }
        String[] segments = norm.split("/");
        StringBuilder base = new StringBuilder();
        int i = 0;
        for (; i < segments.length; i++) {
            if (segments[i].contains("*") || segments[i].contains("?")) {
                break;
            }
            base.append(segments[i]).append('/');
        }
        Path start = Path.of(base.length() == 0 ? "." : base.toString());
        walk(start, segments, i, result);
        return result;
    }

    private static void walk(Path dir, String[] segments, int idx, List<Path> out) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        boolean last = idx == segments.length - 1;
        String seg = segments[idx];
        if (seg.isEmpty()) {
            return;
        }
        if (!seg.contains("*") && !seg.contains("?")) {
            Path next = dir.resolve(seg);
            if (last) {
                if (Files.isRegularFile(next)) {
                    out.add(next);
                }
            } else {
                walk(next, segments, idx + 1, out);
            }
            return;
        }
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + seg);
        List<Path> children = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path child : ds) {
                if (matcher.matches(child.getFileName())) {
                    children.add(child);
                }
            }
        } catch (IOException | RuntimeException e) {
            return;
        }
        children.sort(Comparator.comparing(p -> p.getFileName().toString()));
        for (Path child : children) {
            if (last) {
                if (Files.isRegularFile(child)) {
                    out.add(child);
                }
            } else {
                walk(child, segments, idx + 1, out);
            }
        }
    }

    private static long lastModified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
