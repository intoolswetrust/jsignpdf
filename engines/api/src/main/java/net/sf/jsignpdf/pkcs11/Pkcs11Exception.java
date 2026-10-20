package net.sf.jsignpdf.pkcs11;

/**
 * A PKCS#11 profile cannot be resolved or registered. The message is user-facing.
 */
public class Pkcs11Exception extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Pkcs11Exception(String message) {
        super(message);
    }

    public Pkcs11Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
