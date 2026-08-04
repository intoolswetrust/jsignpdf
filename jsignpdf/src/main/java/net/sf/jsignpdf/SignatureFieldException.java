package net.sf.jsignpdf;

import java.io.Serial;

/**
 * Thrown when a {@code --sig-field} selector does not resolve to a signature field that can be signed. The
 * message is already localized and is meant to be shown to the user as-is.
 *
 * @author Josef Cacek
 */
public class SignatureFieldException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public SignatureFieldException(String message) {
        super(message);
    }
}
