package net.sf.jsignpdf.fx.preferences;

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

    private final String originalId;
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty body = new SimpleStringProperty("");
    private final ProfileStatus status;

    public Pkcs11ProfileDraft(String originalId, String id, String body, ProfileStatus status) {
        this.originalId = originalId;
        this.id.set(id);
        this.body.set(body == null ? "" : body);
        this.status = status;
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

    /** Registration state when loaded; {@code null} for a new profile or after the text changed. */
    public ProfileStatus status() {
        return status;
    }

    public boolean isDefault() {
        return Pkcs11Profiles.DEFAULT_ID.equalsIgnoreCase(id());
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

    public String displayLabel() {
        String label = label();
        return StringUtils.isBlank(label) ? id() : id() + " — " + label;
    }

    public Pkcs11Profiles.ProfileEdit toEdit() {
        return new Pkcs11Profiles.ProfileEdit(originalId, id(), body());
    }
}
