package net.sf.jsignpdf.fx.preferences;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.sf.jsignpdf.pkcs11.Pkcs11ConfigText;
import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;
import net.sf.jsignpdf.pkcs11.ProfileStatus;
import net.sf.jsignpdf.pkcs11.ProviderMode;

import org.apache.commons.lang3.StringUtils;

/**
 * A PKCS#11 profile being edited in the Preferences dialog. The config text is the source of truth; label, provider
 * and library are read from and written into it.
 */
public final class Pkcs11ProfileDraft {

    public static final int MAX_ID_LENGTH = 32;
    public static final String FALLBACK_ID = "token";

    /**
     * What the profile list shows in its status column.
     */
    public enum Display {
        NEW, EDITED, NOT_LOADED, OK, FAILED, INVALID
    }

    private final String originalId;
    private final String loadedBody;
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty body = new SimpleStringProperty("");
    private final ProfileStatus status;
    private boolean idFollowsLabel;

    public Pkcs11ProfileDraft(String originalId, String id, String body, ProfileStatus status) {
        this.originalId = originalId;
        this.id.set(id);
        this.body.set(body == null ? "" : body);
        this.loadedBody = this.body.get();
        this.status = status;
        this.idFollowsLabel = originalId == null;
    }

    public String originalId() {
        return originalId;
    }

    public StringProperty idProperty() {
        return id;
    }

    public String id() {
        return id.get();
    }

    public StringProperty bodyProperty() {
        return body;
    }

    public String body() {
        return body.get();
    }

    /**
     * Registration state when loaded; {@code null} for a new profile.
     */
    public ProfileStatus status() {
        return status;
    }

    public boolean isNew() {
        return originalId == null;
    }

    public boolean isModified() {
        return !isNew() && (!loadedBody.equals(body()) || !originalId.equals(id()));
    }

    public Display display() {
        if (isNew()) {
            return Display.NEW;
        }
        if (isModified()) {
            return Display.EDITED;
        }
        return status == null ? Display.NOT_LOADED : Display.valueOf(status.state().name());
    }

    /**
     * A new profile takes its id from the label until the id is changed by hand.
     */
    public boolean idFollowsLabel() {
        return idFollowsLabel;
    }

    public void setIdFollowsLabel(boolean follows) {
        this.idFollowsLabel = follows;
    }

    public boolean isDefault() {
        return Pkcs11Profiles.DEFAULT_ID.equalsIgnoreCase(id());
    }

    /**
     * Changes the id. The {@code name=} line follows when it still holds the old id, so provider names stay unique.
     */
    public void changeId(String newId) {
        String old = id();
        id.set(newId);
        if (!isDefault() && old != null && old.equals(Pkcs11ConfigText.value(body(), Pkcs11ConfigText.KEY_NAME))) {
            body.set(Pkcs11ConfigText.withValue(body(), Pkcs11ConfigText.KEY_NAME, newId));
        }
    }

    public String label() {
        return Pkcs11ConfigText.metadata(body()).get(Pkcs11ConfigText.META_LABEL);
    }

    public void setLabel(String label) {
        body.set(Pkcs11ConfigText.withMetadata(body(), Pkcs11ConfigText.META_LABEL, StringUtils.trimToNull(label)));
    }

    public ProviderMode provider() {
        ProviderMode m = ProviderMode.parse(Pkcs11ConfigText.metadata(body()).get(Pkcs11ConfigText.META_PROVIDER));
        return m != null ? m : ProviderMode.SUN;
    }

    public void setProvider(ProviderMode mode) {
        body.set(Pkcs11ConfigText.withMetadata(body(), Pkcs11ConfigText.META_PROVIDER,
                mode == null ? null : mode.value()));
    }

    public String library() {
        return Pkcs11ConfigText.value(body(), Pkcs11ConfigText.KEY_LIBRARY);
    }

    public void setLibrary(String path) {
        String p = StringUtils.trimToNull(path);
        body.set(Pkcs11ConfigText.withValue(body(), Pkcs11ConfigText.KEY_LIBRARY,
                p == null ? null : Pkcs11ConfigText.formatLibraryValue(p)));
    }

    public Pkcs11Profiles.ProfileEdit toEdit() {
        return new Pkcs11Profiles.ProfileEdit(originalId, id(), body());
    }

    /**
     * The file name of a library path, or the value itself when it has no path separator.
     */
    public static String libraryFileName(String library) {
        if (StringUtils.isBlank(library)) {
            return "";
        }
        int i = Math.max(library.lastIndexOf('/'), library.lastIndexOf('\\'));
        return i >= 0 && i < library.length() - 1 ? library.substring(i + 1) : library;
    }

    /**
     * {@code true} for a library path with a root (absolute, or starting with a separator) that does not point to a
     * file.
     */
    public static boolean isLibraryMissing(String library) {
        if (StringUtils.isBlank(library)) {
            return false;
        }
        try {
            Path p = Path.of(library);
            return p.getRoot() != null && !p.toFile().isFile();
        } catch (InvalidPathException e) {
            return true;
        }
    }

    /**
     * Turns a label into a profile id: diacritics dropped, lower case, runs of other characters replaced by {@code -},
     * at most {@value #MAX_ID_LENGTH} characters. Returns {@value #FALLBACK_ID} when nothing usable is left.
     */
    public static String idFromLabel(String label) {
        String s = Normalizer.normalize(StringUtils.defaultString(label), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-").replaceAll("-{2,}", "-");
        s = trimSeparators(s);
        if (s.length() > MAX_ID_LENGTH) {
            s = trimSeparators(s.substring(0, MAX_ID_LENGTH));
        }
        return s.isEmpty() ? FALLBACK_ID : s;
    }

    private static String trimSeparators(String s) {
        return s.replaceAll("^[-._]+|[-._]+$", "");
    }
}
