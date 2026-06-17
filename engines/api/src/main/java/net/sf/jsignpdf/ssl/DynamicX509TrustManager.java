package net.sf.jsignpdf.ssl;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import net.sf.jsignpdf.utils.AppConfig;
import net.sf.jsignpdf.utils.KeyStoreUtils;

/**
 * TrustManager which works with in-memory copy of cacerts truststore. If {@link AppConfig#relaxSslSecurity()} is true then it
 * adds missing server certificates to the truststore.
 *
 * @author Josef Cacek
 */
public class DynamicX509TrustManager implements X509TrustManager {

    private final KeyStore trustStore;
    private final TrustManagerFactory trustManagerFactory;

    private X509TrustManager trustManager;

    /**
     * Constructor.
     * 
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public DynamicX509TrustManager() {
        try {
            this.trustStore = KeyStoreUtils.createTrustStore();
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            reloadTrustStore();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create TrustManager.", e);
        }
    }

    /**
     * Checks client's cert-chain - no extra step here.
     * 
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        trustManager.checkClientTrusted(chain, authType);
    }

    /**
     * Checks server's cert-chain. If check fails and {@link AppConfig#relaxSslSecurity()} is true then the first certificate
     * from the chain is added to the truststore and the check is repeated.
     *
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (AppConfig.relaxSslSecurity()) {
            try {
                trustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException cx) {
                try {
                    trustStore.setCertificateEntry(UUID.randomUUID().toString(), chain[0]);
                    reloadTrustStore();
                } catch (Exception e) {
                    throw new CertificateException("Unable to recreate TrustManager", e);
                }
                trustManager.checkServerTrusted(chain, authType);
            }
        } else {
            trustManager.checkServerTrusted(chain, authType);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers() {
        return trustManager.getAcceptedIssuers();
    }

    /**
     * Reloads the in-memory trustore.
     * 
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    private void reloadTrustStore() throws KeyStoreException, NoSuchAlgorithmException {
        trustManagerFactory.init(trustStore);
        // acquire X509 trust manager from factory
        TrustManager tms[] = trustManagerFactory.getTrustManagers();
        for (int i = 0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager x509Tm) {
                trustManager = x509Tm;
                return;
            }
        }

        throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");
    }

}
