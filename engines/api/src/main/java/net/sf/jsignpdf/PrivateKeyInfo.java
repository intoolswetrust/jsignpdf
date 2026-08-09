package net.sf.jsignpdf;

import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.Certificate;

/**
 * Helper class (POI) which holds private key and the assigned certificates.
 *
 * @author Josef Cacek
 */
public class PrivateKeyInfo {

    private PrivateKey key;
    private Certificate[] chain;
    private Provider provider;

    public PrivateKeyInfo() {
    }

    /**
     * Creates instance and fills fields.
     *
     * @param key
     * @param chain
     */
    public PrivateKeyInfo(PrivateKey key, Certificate[] chain) {
        this(key, chain, null);
    }

    /**
     * Creates instance and fills fields, including the provider the key came from.
     *
     * @param key
     * @param chain
     * @param provider keystore provider, or {@code null} when unknown
     */
    public PrivateKeyInfo(PrivateKey key, Certificate[] chain, Provider provider) {
        super();
        this.key = key;
        this.chain = chain;
        this.provider = provider;
    }

    /**
     * The {@link Provider} of the keystore the key was loaded from, or {@code null} when unknown. For a
     * PKCS#11 token this is the provider that owns the opaque key: a signing engine must keep the key on it,
     * because no other provider can use a key that cannot be exported.
     *
     * @return the keystore provider or {@code null}
     */
    public Provider getProvider() {
        return provider;
    }

    /**
     * @param provider the keystore provider to set
     */
    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    /**
     * @return the key
     */
    public PrivateKey getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(PrivateKey key) {
        this.key = key;
    }

    /**
     * @return the chain
     */
    public Certificate[] getChain() {
        return chain;
    }

    /**
     * @param chain the chain to set
     */
    public void setChain(Certificate[] chain) {
        this.chain = chain;
    }
}
