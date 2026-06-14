package net.sf.jsignpdf.extcsp;

import net.sf.jsignpdf.BasicSignerOptions;

import java.security.cert.Certificate;
import java.util.LinkedList;

public interface IExternalCryptoProvider {

    /**
     * Get the CSP name for GUI
     * 
     * @return String - the CSP name
     */
    String getName();

    /**
     * The method returns a certificate chain for the provided alias
     * 
     * @param options - command line / GUI provided options like keystore, PIN/password, alias, ...
     * @return Certificate[] - a list of certificates, or null if there was an error
     */
    Certificate[] getChain(BasicSignerOptions options);

    /**
     * The methods takes an initial fingerprint of the document, and creates and external signature, which can be used for the
     * 'setExternalDigest' method.
     * 
     * @param options - command line / GUI provided options like keystore, PIN/password, alias, ...
     * @param fingerprint - byte array containing the document fingerprint (only SHA1 and SHA256 are supported)
     * @return byte[] with the signature, null if there was an error
     */
    byte[] getSignature(BasicSignerOptions options, byte[] fingerprint);

    /**
     * Query the crypto provider and return a list of aliases available.
     * 
     * @param options - command line / GUI provided options like keystore, PIN/password, alias, ...
     * @return LinkedList<String> - a list of names
     * @throws NullPointerException - when the list can't be created
     */
    public LinkedList<String> getAliasesList(BasicSignerOptions options) throws NullPointerException;
}
