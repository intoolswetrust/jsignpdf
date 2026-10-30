package net.sf.jsignpdf.pkcs11;

/**
 * The two PKCS#11 provider implementations JSignPdf can register for a profile.
 */
public enum Pkcs11Backend {

    SUN("PKCS11", "SunPKCS11-", "sun.security.pkcs11.SunPKCS11"),
    JSIGN("JSIGNPKCS11", "JSignPKCS11-", "com.github.kwart.jsign.pkcs11.JSignPKCS11");

    private final String keyStoreType;
    private final String providerNamePrefix;
    private final String providerClass;

    Pkcs11Backend(String keyStoreType, String providerNamePrefix, String providerClass) {
        this.keyStoreType = keyStoreType;
        this.providerNamePrefix = providerNamePrefix;
        this.providerClass = providerClass;
    }

    public String keyStoreType() {
        return keyStoreType;
    }

    public String providerNamePrefix() {
        return providerNamePrefix;
    }

    public String providerClass() {
        return providerClass;
    }

    /**
     * Returns the backend registering the given keystore type (case-insensitive), or {@code null}.
     */
    public static Pkcs11Backend forKeyStoreType(String type) {
        if (type == null) {
            return null;
        }
        for (Pkcs11Backend b : values()) {
            if (b.keyStoreType.equalsIgnoreCase(type.trim())) {
                return b;
            }
        }
        return null;
    }
}
