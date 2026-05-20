package com.intoolswetrust.jsignpdf;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Launcher entry point used by the cross-platform JSignPdf ZIPs.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Refuse to run on a JRE older than {@link #MIN_JAVA} and print a friendly message.</li>
 *   <li>Detect the operating system / architecture and pick the matching JavaFX classifier set
 *       from a {@code javafx/} folder sitting next to this jar (only present in the
 *       <em>full</em> ZIP).</li>
 *   <li>Assemble a {@link URLClassLoader} containing all sibling jars plus the selected
 *       JavaFX jars, then reflectively invoke the real {@code Signer.main}.</li>
 * </ol>
 *
 * <p>The class is intentionally compiled at bytecode level 52 (Java 8) so that the JRE-version
 * check is reachable on older JREs — a Java 21 class would fail with
 * {@code UnsupportedClassVersionError} before any code runs. Only APIs available in Java 8 are used.
 */
public final class Bootstrap {

    /** Minimum Java release JSignPdf supports at runtime. */
    static final int MIN_JAVA = 21;

    /** Real application entry point invoked reflectively. */
    static final String MAIN_CLASS = "net.sf.jsignpdf.Signer";

    /** Sub-directory (relative to the bootstrap jar) that holds classified JavaFX jars. */
    static final String JFX_DIR = "javafx";

    private Bootstrap() {
    }

    public static void main(String[] args) throws Exception {
        ensureJavaVersion();
        URL[] classpath = buildClasspath();
        URLClassLoader loader = new URLClassLoader(classpath, platformClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> signer = Class.forName(MAIN_CLASS, true, loader);
        Method main = signer.getMethod("main", String[].class);
        main.invoke(null, (Object) args);
    }

    /**
     * Returns the JDK platform class loader so the URLClassLoader we build sits directly above the
     * JDK and below nothing — in particular, not below the system classloader. The launcher scripts
     * put every sibling jar (including the real {@code jsignpdf.jar}) on the {@code -cp lib/*}
     * system classpath; if we kept the system classloader as parent, parent-first delegation would
     * load {@code Signer} from there, and {@code Signer}'s defining loader would have no visibility
     * into the {@code lib/javafx/} jars added only to our URLClassLoader. Reflective because this
     * class is compiled at bytecode level 52 ({@link #MIN_JAVA} guarantees the method exists).
     */
    private static ClassLoader platformClassLoader() throws Exception {
        Method m = ClassLoader.class.getMethod("getPlatformClassLoader");
        return (ClassLoader) m.invoke(null);
    }

    private static void ensureJavaVersion() {
        int major = currentJavaMajor();
        if (major < MIN_JAVA) {
            String found = System.getProperty("java.version", "unknown");
            System.err.println("JSignPdf requires Java " + MIN_JAVA + " or newer, but found " + found + ".");
            System.err.println("Download a recent JDK (e.g. https://adoptium.net/) and try again.");
            System.exit(1);
        }
    }

    static int currentJavaMajor() {
        return parseJavaMajor(System.getProperty("java.specification.version", ""));
    }

    static int parseJavaMajor(String spec) {
        if (spec == null || spec.isEmpty()) {
            return 0;
        }
        String s = spec;
        if (s.startsWith("1.")) {
            s = s.substring(2);
        }
        int dot = s.indexOf('.');
        if (dot >= 0) {
            s = s.substring(0, dot);
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static URL[] buildClasspath() throws Exception {
        File libDir = bootstrapLibDir();
        List<URL> urls = new ArrayList<URL>();
        File[] libJars = libDir.listFiles(new JarFilter());
        if (libJars != null) {
            Arrays.sort(libJars);
            for (File jar : libJars) {
                urls.add(jar.toURI().toURL());
            }
        }
        String classifier = detectFxClassifier();
        File fxDir = new File(libDir, JFX_DIR);
        if (classifier != null && fxDir.isDirectory()) {
            File[] fxJars = fxDir.listFiles(new FxClassifierFilter(classifier));
            if (fxJars != null) {
                Arrays.sort(fxJars);
                for (File jar : fxJars) {
                    urls.add(jar.toURI().toURL());
                }
            }
        }
        return urls.toArray(new URL[0]);
    }

    private static File bootstrapLibDir() throws Exception {
        URL self = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation();
        File jar = new File(self.toURI());
        File parent = jar.getParentFile();
        if (parent == null) {
            throw new IllegalStateException("Cannot locate bootstrap jar directory: " + jar);
        }
        return parent;
    }

    static String detectFxClassifier() {
        return detectFxClassifier(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    static String detectFxClassifier(String osNameRaw, String osArchRaw) {
        String os = osNameRaw == null ? "" : osNameRaw.toLowerCase(Locale.ROOT);
        String arch = osArchRaw == null ? "" : osArchRaw.toLowerCase(Locale.ROOT);
        boolean aarch64 = arch.contains("aarch64") || arch.contains("arm64");
        if (os.contains("win")) {
            // Windows aarch64 has no JavaFX classifier we ship today — fall through to Swing.
            return aarch64 ? null : "win";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return aarch64 ? "mac-aarch64" : "mac";
        }
        if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
            return aarch64 ? "linux-aarch64" : "linux";
        }
        return null;
    }

    private static final class JarFilter implements FileFilter {
        @Override
        public boolean accept(File f) {
            return f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".jar");
        }
    }

    /**
     * Matches JavaFX jars whose Maven classifier equals the running platform. Filename shape:
     * {@code javafx-<artifact>-<version>-<classifier>.jar}.
     */
    private static final class FxClassifierFilter implements FileFilter {
        private final String suffix;

        FxClassifierFilter(String classifier) {
            this.suffix = "-" + classifier.toLowerCase(Locale.ROOT) + ".jar";
        }

        @Override
        public boolean accept(File f) {
            return f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(suffix);
        }
    }
}
