package net.sf.jsignpdf.pkcs11;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Which providers a profile registers, set by the {@code #jsignpdf:provider=} metadata line.
 */
public enum ProviderMode {

    SUN("sun", EnumSet.of(Pkcs11Backend.SUN)),
    JSIGN("jsign", EnumSet.of(Pkcs11Backend.JSIGN)),
    BOTH("both", EnumSet.allOf(Pkcs11Backend.class));

    private final String value;
    private final Set<Pkcs11Backend> backends;

    ProviderMode(String value, Set<Pkcs11Backend> backends) {
        this.value = value;
        this.backends = backends;
    }

    public String value() {
        return value;
    }

    public Set<Pkcs11Backend> backends() {
        return EnumSet.copyOf(backends);
    }

    /**
     * Parses the metadata value; returns {@code null} for a blank or unknown value.
     */
    public static ProviderMode parse(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        for (ProviderMode m : values()) {
            if (m.value.equals(v)) {
                return m;
            }
        }
        return null;
    }
}
