package net.sf.jsignpdf.fx.preset;

import java.time.Instant;
import java.util.Objects;

/**
 * A named signature-setting preset. Identity is the backing filename; {@code displayName} is the user-visible label stored
 * inside the preset file as {@code preset.displayName}. {@code createdAt} is optional and informational.
 */
public final class Preset {

    private final String filename;
    private final String displayName;
    private final Instant createdAt;

    public Preset(String filename, String displayName, Instant createdAt) {
        this.filename = Objects.requireNonNull(filename, "filename");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.createdAt = createdAt;
    }

    public String getFilename() {
        return filename;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns a copy with a new display name. Filename and createdAt are preserved.
     */
    public Preset withDisplayName(String newDisplayName) {
        return new Preset(filename, newDisplayName, createdAt);
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Preset)) {
            return false;
        }
        Preset other = (Preset) o;
        return filename.equals(other.filename);
    }

    @Override
    public int hashCode() {
        return filename.hashCode();
    }
}
