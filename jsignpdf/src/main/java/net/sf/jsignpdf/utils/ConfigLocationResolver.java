package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Resolves the application's config directory, the main config file, the presets directory, and the advanced/PKCS#11
 * config files. On first launch, performs a one-shot migration of the legacy {@code ~/.JSignPdf} file into
 * {@code <cfg>/config.properties}.
 * <p>
 * Resolution order:
 * <ol>
 * <li>{@code JSIGNPDF_CONFIG_DIR} env variable — use verbatim if non-empty.
 * <li>Platform-native location (XDG on Linux/BSD, {@code %APPDATA%} on Windows, {@code ~/Library/Application Support} on macOS).
 * <li>Legacy {@code ~/.JSignPdf} as a file only; used to seed the new directory on first write.
 * </ol>
 */
public final class ConfigLocationResolver {

    static final String ENV_CONFIG_DIR = "JSIGNPDF_CONFIG_DIR";
    static final String ENV_XDG_CONFIG_HOME = "XDG_CONFIG_HOME";
    static final String ENV_APPDATA = "APPDATA";
    static final String ENV_USERPROFILE = "USERPROFILE";

    static final String LEGACY_FILE_NAME = ".JSignPdf";
    static final String MAIN_CONFIG_FILE_NAME = "config.properties";
    static final String PRESETS_DIR_NAME = "presets";
    static final String ADVANCED_CONFIG_FILE_NAME = "advanced.properties";
    static final String PKCS11_CONFIG_FILE_NAME = "pkcs11.cfg";

    public enum OsType { LINUX, WINDOWS, MAC }

    private static volatile ConfigLocationResolver instance;

    private final OsType os;
    private final Function<String, String> env;
    private final String userHome;

    private Path configDir;
    private boolean resolved;

    public ConfigLocationResolver(OsType os, Function<String, String> env, String userHome) {
        this.os = os;
        this.env = env;
        this.userHome = userHome;
    }

    public static ConfigLocationResolver getInstance() {
        ConfigLocationResolver ref = instance;
        if (ref == null) {
            synchronized (ConfigLocationResolver.class) {
                ref = instance;
                if (ref == null) {
                    ref = new ConfigLocationResolver(detectOs(), System::getenv, System.getProperty("user.home"));
                    instance = ref;
                }
            }
        }
        return ref;
    }

    /**
     * Returns the config directory, creating it and running the one-shot legacy migration on first call. May return
     * {@code null} if the directory cannot be resolved (e.g. {@code user.home} is unset) — callers should treat that as
     * "persistence disabled" rather than falling back to the CWD.
     */
    public synchronized Path getConfigDir() {
        if (!resolved) {
            configDir = resolveAndMigrate();
            resolved = true;
        }
        return configDir;
    }

    /**
     * Path to the main config file ({@code <cfg>/config.properties}). May be {@code null} if {@link #getConfigDir()} is.
     */
    public Path getMainConfigFile() {
        Path dir = getConfigDir();
        return dir == null ? null : dir.resolve(MAIN_CONFIG_FILE_NAME);
    }

    /**
     * Path to the presets directory ({@code <cfg>/presets/}). Created lazily when the first preset is written. May be
     * {@code null} if {@link #getConfigDir()} is.
     */
    public Path getPresetsDir() {
        Path dir = getConfigDir();
        return dir == null ? null : dir.resolve(PRESETS_DIR_NAME);
    }

    /**
     * Path to the advanced-config file ({@code <cfg>/advanced.properties}). May be {@code null} if {@link #getConfigDir()} is.
     */
    public Path getAdvancedConfigFile() {
        Path dir = getConfigDir();
        return dir == null ? null : dir.resolve(ADVANCED_CONFIG_FILE_NAME);
    }

    /**
     * Path to the PKCS#11 provider config file ({@code <cfg>/pkcs11.cfg}). May be {@code null} if {@link #getConfigDir()} is.
     */
    public Path getPkcs11ConfigFile() {
        Path dir = getConfigDir();
        return dir == null ? null : dir.resolve(PKCS11_CONFIG_FILE_NAME);
    }

    // Visible for tests.
    Path resolveAndMigrate() {
        Path target = resolveTargetDir();
        if (target == null) {
            LOGGER.warning("Cannot resolve config directory: user.home is not set. Persistence disabled.");
            return null;
        }

        if (Files.isDirectory(target)) {
            return target;
        }

        Path legacy = legacyFilePath();
        try {
            Files.createDirectories(target);
            Files.createDirectories(target.resolve(PRESETS_DIR_NAME));
            if (legacy != null && Files.isRegularFile(legacy)) {
                Path mainCfg = target.resolve(MAIN_CONFIG_FILE_NAME);
                Files.copy(legacy, mainCfg, StandardCopyOption.COPY_ATTRIBUTES);
                LOGGER.info("Migrated legacy settings from " + legacy + " to " + mainCfg);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create config directory " + target, e);
            return null;
        }

        return target;
    }

    private Path resolveTargetDir() {
        String override = env.apply(ENV_CONFIG_DIR);
        if (override != null && !override.isEmpty()) {
            return Paths.get(override);
        }
        if (userHome == null || userHome.isEmpty()) {
            return null;
        }
        switch (os) {
            case WINDOWS: {
                String appData = env.apply(ENV_APPDATA);
                if (appData != null && !appData.isEmpty()) {
                    return Paths.get(appData, "JSignPdf");
                }
                String userProfile = env.apply(ENV_USERPROFILE);
                String base = (userProfile != null && !userProfile.isEmpty()) ? userProfile : userHome;
                return Paths.get(base, "AppData", "Roaming", "JSignPdf");
            }
            case MAC:
                return Paths.get(userHome, "Library", "Application Support", "JSignPdf");
            case LINUX:
            default: {
                String xdg = env.apply(ENV_XDG_CONFIG_HOME);
                if (xdg != null && !xdg.isEmpty()) {
                    return Paths.get(xdg, "jsignpdf");
                }
                return Paths.get(userHome, ".config", "jsignpdf");
            }
        }
    }

    private Path legacyFilePath() {
        if (userHome == null || userHome.isEmpty()) {
            return null;
        }
        return Paths.get(userHome, LEGACY_FILE_NAME);
    }

    static OsType detectOs() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return OsType.WINDOWS;
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return OsType.MAC;
        }
        return OsType.LINUX;
    }
}
