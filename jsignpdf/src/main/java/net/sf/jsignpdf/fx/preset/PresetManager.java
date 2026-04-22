package net.sf.jsignpdf.fx.preset;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.utils.ConfigLocationResolver;
import net.sf.jsignpdf.utils.PropertyProvider;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

/**
 * Manages the collection of signature-setting presets stored under {@code <cfg>/presets/}.
 * <p>
 * This class is stateless with respect to "active preset" — loading a preset is a one-way transfer from the preset file into
 * a {@link BasicSignerOptions} instance; the manager does not remember which preset (if any) the live options came from.
 */
public class PresetManager {

    static final String KEY_DISPLAY_NAME = "preset.displayName";
    static final String KEY_CREATED_AT = "preset.createdAt";

    private final PropertyStoreFactory factory;
    private final ConfigLocationResolver resolver;
    private final ObservableList<Preset> presets;

    public PresetManager() {
        this(PropertyStoreFactory.getInstance());
    }

    PresetManager(PropertyStoreFactory factory) {
        this.factory = factory;
        this.resolver = factory.getResolver();
        this.presets = FXCollections.observableArrayList();
        scan();
    }

    /**
     * Returns the observable list of presets. UI controls (e.g. the toolbar combo) should bind to this directly; the list is
     * kept in sync with disk by mutating operations on this manager.
     */
    public ObservableList<Preset> getPresets() {
        return presets;
    }

    /**
     * (Re)scans the presets directory and repopulates the observable list. Files that cannot be parsed or that lack the
     * {@code preset.displayName} key are silently skipped.
     */
    public void scan() {
        List<Preset> found = new ArrayList<>();
        Path dir = resolver.getPresetsDir();
        if (dir != null && Files.isDirectory(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.properties")) {
                for (Path file : stream) {
                    if (!Files.isRegularFile(file)) {
                        continue;
                    }
                    Preset preset = tryRead(file);
                    if (preset != null) {
                        found.add(preset);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to scan presets directory " + dir, e);
            }
        }
        found.sort(Comparator.comparing(Preset::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        presets.setAll(found);
    }

    /**
     * Returns true if a preset with the given display name (case-insensitive, after trim) already exists.
     */
    public boolean hasDisplayName(String displayName) {
        return hasDisplayName(displayName, null);
    }

    /**
     * Returns true if a preset with the given display name (case-insensitive, after trim) already exists — ignoring the
     * given {@code exclude} preset (useful when validating a rename of an existing preset).
     */
    public boolean hasDisplayName(String displayName, Preset exclude) {
        String needle = PresetValidation.trim(displayName);
        for (Preset p : presets) {
            if (p.equals(exclude)) {
                continue;
            }
            if (p.getDisplayName().equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new {@code preset-<epochMillis>.properties} file from the given options and display name. Returns the new
     * {@link Preset}. The display name is written as-is (expected to be pre-validated by the caller); the caller should
     * run {@link PresetValidation} first.
     */
    public Preset saveAsNew(BasicSignerOptions options, String displayName) {
        String trimmed = PresetValidation.trim(displayName);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Preset display name must not be empty");
        }
        PropertyProvider store = factory.newPreset();
        Path path = store.getPath();
        if (path == null) {
            throw new IllegalStateException("Cannot create preset — presets directory is unavailable.");
        }
        Instant createdAt = Instant.now();
        store.setProperty(KEY_DISPLAY_NAME, trimmed);
        store.setProperty(KEY_CREATED_AT, createdAt.toString());
        options.storeToPreset(store);
        store.save();

        Preset preset = new Preset(path.getFileName().toString(), trimmed, createdAt);
        presets.add(preset);
        presets.sort(Comparator.comparing(Preset::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return preset;
    }

    /**
     * Overwrites the given preset's contents with the current options. Preserves filename, display name, and createdAt.
     */
    public void overwrite(Preset preset, BasicSignerOptions options) {
        PropertyProvider store = factory.preset(preset.getFilename());
        store.setProperty(KEY_DISPLAY_NAME, preset.getDisplayName());
        if (preset.getCreatedAt() != null) {
            store.setProperty(KEY_CREATED_AT, preset.getCreatedAt().toString());
        }
        options.storeToPreset(store);
        store.save();
    }

    /**
     * Changes the display name of an existing preset in-place. Keeps the same backing file (no rename on disk).
     */
    public void rename(Preset preset, String newDisplayName) {
        String trimmed = PresetValidation.trim(newDisplayName);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Preset display name must not be empty");
        }
        PropertyProvider store = factory.preset(preset.getFilename());
        store.setProperty(KEY_DISPLAY_NAME, trimmed);
        if (preset.getCreatedAt() != null) {
            store.setProperty(KEY_CREATED_AT, preset.getCreatedAt().toString());
        }
        store.save();

        Preset renamed = preset.withDisplayName(trimmed);
        int idx = presets.indexOf(preset);
        if (idx >= 0) {
            presets.set(idx, renamed);
        }
        presets.sort(Comparator.comparing(Preset::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Deletes the preset file and removes it from the observable list.
     */
    public void delete(Preset preset) {
        Path dir = resolver.getPresetsDir();
        if (dir != null) {
            Path file = dir.resolve(preset.getFilename());
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to delete preset " + file, e);
            }
        }
        presets.remove(preset);
    }

    /**
     * Loads the preset's signing configuration into the given options. Session state (input/output paths) is not touched.
     * Passwords are loaded only when the preset was saved with _Store passwords_ on a machine with a matching
     * {@code user.home}; otherwise the in-memory password fields are cleared so a prior preset's credentials cannot
     * bleed into this load.
     */
    public void load(Preset preset, BasicSignerOptions options) {
        PropertyProvider store = factory.preset(preset.getFilename());
        options.loadFromPreset(store);
    }

    private Preset tryRead(Path file) {
        PropertyProvider store = new PropertyProvider(file);
        store.load();
        String displayName = store.getProperty(KEY_DISPLAY_NAME);
        if (displayName == null || displayName.trim().isEmpty()) {
            LOGGER.fine("Skipping preset file without preset.displayName: " + file);
            return null;
        }
        Instant createdAt = null;
        String createdStr = store.getProperty(KEY_CREATED_AT);
        if (createdStr != null && !createdStr.isEmpty()) {
            try {
                createdAt = Instant.parse(createdStr);
            } catch (DateTimeParseException e) {
                LOGGER.fine("Unparseable preset.createdAt on " + file + ": " + createdStr);
            }
        }
        return new Preset(file.getFileName().toString(), displayName.trim(), createdAt);
    }
}
