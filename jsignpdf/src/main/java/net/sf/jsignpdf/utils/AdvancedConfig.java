package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import net.sf.jsignpdf.utils.PropertyProvider.ProperyProviderException;

import org.apache.commons.lang3.StringUtils;

/**
 * Two-layer configuration store backing {@code <cfg>/advanced.properties}: a mutable user layer that persists to disk and an
 * immutable layer holding the defaults shipped inside the jar. Every getter consults the user layer first and falls back to
 * the bundled defaults; mutators only touch the user layer. Replaces the legacy {@code ConfigProvider} singleton — tests
 * construct it directly with a temp path and an in-memory defaults Properties.
 */
public final class AdvancedConfig {

    private final PropertyProvider userLayer;
    private final Properties bundledDefaults;
    private Properties baseline;

    /**
     * Creates an instance backed by {@code userFile} (may be {@code null} for an in-memory store) with the given bundled
     * defaults. The user file is read immediately if it exists.
     */
    public AdvancedConfig(Path userFile, Properties bundledDefaults) {
        this.userLayer = new PropertyProvider(userFile);
        this.bundledDefaults = (Properties) Objects.requireNonNullElseGet(bundledDefaults, Properties::new).clone();
        this.userLayer.load();
        this.baseline = userLayerSnapshot();
    }

    /** Re-reads {@code userFile} from disk; the bundled-defaults layer is left untouched. */
    public synchronized void reload() {
        userLayer.clear();
        Path path = userLayer.getPath();
        if (path != null && Files.isRegularFile(path)) {
            userLayer.load();
        }
        baseline = userLayerSnapshot();
    }

    /** Drops every user override and deletes the backing file. Subsequent getters return bundled defaults. */
    public synchronized void resetToDefaults() {
        userLayer.clear();
        Path path = userLayer.getPath();
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                LOGGER.fine("Failed to delete " + path + ": " + e.getMessage());
            }
        }
        baseline = userLayerSnapshot();
    }

    /**
     * Persists the current user layer to disk and returns the set of keys that changed since the last load/save. Returned set
     * is never {@code null}.
     */
    public synchronized Set<String> save() throws ProperyProviderException {
        if (userLayer.getPath() != null) {
            userLayer.save();
        }
        Properties current = userLayerSnapshot();
        Set<String> changed = diff(baseline, current);
        baseline = current;
        return changed;
    }

    public synchronized String getProperty(String key) {
        String v = userLayer.getProperty(key);
        return v != null ? v : bundledDefaults.getProperty(key);
    }

    public synchronized String getProperty(String key, String def) {
        String v = getProperty(key);
        return v != null ? v : def;
    }

    public synchronized String getNotEmptyProperty(String key, String def) {
        String v = getProperty(key);
        return StringUtils.isEmpty(v) ? def : v;
    }

    public synchronized boolean getAsBool(String key, boolean def) {
        String v = getProperty(key);
        return v == null ? def : ConvertUtils.toBoolean(v, def);
    }

    public synchronized int getAsInt(String key, int def) {
        String v = getProperty(key);
        return v == null ? def : ConvertUtils.toInt(v, def);
    }

    public synchronized float getAsFloat(String key, float def) {
        String v = getProperty(key);
        return v == null ? def : ConvertUtils.toFloat(v, def);
    }

    public synchronized void setProperty(String key, String value) {
        if (value == null) {
            userLayer.removeProperty(key);
        } else {
            userLayer.setProperty(key, value);
        }
    }

    public synchronized void setProperty(String key, boolean value) {
        userLayer.setProperty(key, value);
    }

    /** Reverts the given key to its bundled default by removing the user override. */
    public synchronized void removeProperty(String key) {
        userLayer.removeProperty(key);
    }

    /** True if the user layer (not the bundled defaults) holds an entry for the given key. */
    public synchronized boolean hasUserOverride(String key) {
        return userLayer.exists(key);
    }

    /** Live default value for the given key — the value the bundled jar resource ships. */
    public String getBundledDefault(String key) {
        return bundledDefaults.getProperty(key);
    }

    /** Path the user layer is bound to, or {@code null} for an in-memory provider. */
    public Path getUserFile() {
        return userLayer.getPath();
    }

    private Properties userLayerSnapshot() {
        Properties snapshot = new Properties();
        for (String key : userLayer.stringPropertyNames()) {
            String v = userLayer.getProperty(key);
            if (v != null) {
                snapshot.setProperty(key, v);
            }
        }
        return snapshot;
    }

    private static Set<String> diff(Properties a, Properties b) {
        Set<String> changed = new HashSet<>();
        Set<String> keys = new HashSet<>();
        keys.addAll(a.stringPropertyNames());
        keys.addAll(b.stringPropertyNames());
        for (String key : keys) {
            if (!Objects.equals(a.getProperty(key), b.getProperty(key))) {
                changed.add(key);
            }
        }
        return changed;
    }
}
