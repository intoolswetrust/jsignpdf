package net.sf.jsignpdf.pkcs11;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * One parsed PKCS#11 profile: a SunPKCS11 config file plus the {@code #jsignpdf:} metadata it carries.
 */
public final class Pkcs11Profile {

    private final String id;
    private final Path file;
    private final boolean legacy;
    private final String body;
    private final String label;
    private final ProviderMode mode;
    private final boolean modeExplicit;
    private final String catalogRef;
    private final String cfgName;
    private final String library;

    public Pkcs11Profile(String id, Path file, boolean legacy, String body) {
        this.id = id;
        this.file = file;
        this.legacy = legacy;
        this.body = body == null ? "" : body;
        Map<String, String> meta = Pkcs11ConfigText.metadata(this.body);
        this.label = StringUtils.trimToNull(meta.get(Pkcs11ConfigText.META_LABEL));
        ProviderMode parsed = ProviderMode.parse(meta.get(Pkcs11ConfigText.META_PROVIDER));
        this.modeExplicit = parsed != null;
        this.mode = parsed != null ? parsed : ProviderMode.SUN;
        this.catalogRef = StringUtils.trimToNull(meta.get(Pkcs11ConfigText.META_CATALOG));
        this.cfgName = StringUtils.trimToNull(Pkcs11ConfigText.value(this.body, Pkcs11ConfigText.KEY_NAME));
        this.library = StringUtils.trimToNull(Pkcs11ConfigText.value(this.body, Pkcs11ConfigText.KEY_LIBRARY));
    }

    public String id() {
        return id;
    }

    public Path file() {
        return file;
    }

    /** {@code true} for {@code <cfg>/pkcs11.cfg}, exposed as the profile {@code default}. */
    public boolean isLegacy() {
        return legacy;
    }

    public String body() {
        return body;
    }

    public String label() {
        return label;
    }

    public String displayLabel() {
        return label != null ? label : id;
    }

    public ProviderMode mode() {
        return mode;
    }

    public boolean isModeExplicit() {
        return modeExplicit;
    }

    public String catalogRef() {
        return catalogRef;
    }

    /** The {@code name=} value, or {@code null}. */
    public String cfgName() {
        return cfgName;
    }

    /** The {@code library=} value, or {@code null}. */
    public String library() {
        return library;
    }

    /**
     * Keystore types listed for this profile.
     */
    public Set<String> offeredTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Pkcs11Backend b : mode.backends()) {
            types.add(b.keyStoreType());
        }
        return types;
    }

    /**
     * Backends this profile can register for the given keystore type. A legacy file without a {@code provider}
     * metadata line also accepts {@code JSIGNPKCS11}, so a 3.2 configuration that selected it keeps working.
     */
    public Set<Pkcs11Backend> backendsFor(String keyStoreType) {
        Pkcs11Backend b = Pkcs11Backend.forKeyStoreType(keyStoreType);
        if (b == null) {
            return EnumSet.noneOf(Pkcs11Backend.class);
        }
        if (mode.backends().contains(b) || (legacy && !modeExplicit && b == Pkcs11Backend.JSIGN)) {
            return EnumSet.of(b);
        }
        return EnumSet.noneOf(Pkcs11Backend.class);
    }

    public boolean accepts(String keyStoreType) {
        return !backendsFor(keyStoreType).isEmpty();
    }

    @Override
    public String toString() {
        return id;
    }
}
