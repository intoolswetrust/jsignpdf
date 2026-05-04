package net.sf.jsignpdf.types;

import static net.sf.jsignpdf.Constants.RES;

/**
 * A server authentication methods enum.
 * 
 * @author Josef Cacek
 */
public enum ServerAuthentication {
    NONE("serverAuthn.none"), PASSWORD("serverAuthn.password"), CERTIFICATE("serverAuthn.certificate");

    private String msgKey;

    ServerAuthentication(final String aMsgKey) {
        msgKey = aMsgKey;
    }

    /**
     * Returns internationalized description of a level.
     */
    @Override
    public String toString() {
        return RES.get(msgKey);
    }

}
