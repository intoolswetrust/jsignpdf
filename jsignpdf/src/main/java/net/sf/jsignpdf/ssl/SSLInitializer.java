package net.sf.jsignpdf.ssl;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.AppConfig;
import net.sf.jsignpdf.utils.KeyStoreUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper class for handling default SSL connections settings (HTTPS).
 *
 * @author Josef Cacek
 */
public class SSLInitializer {

    private static final TrustManager[] TRUST_MANAGERS = new TrustManager[] { new DynamicX509TrustManager() };

    public static final void init()
            throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, IOException {
        if (AppConfig.relaxSslSecurity()) {
            LOGGER.fine("Relaxing SSL security.");

            // Details for the properties -
            // http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html
            // Workaround for
            // http://sourceforge.net/tracker/?func=detail&atid=1037906&aid=3491269&group_id=216921
            System.setProperty("jsse.enableSNIExtension", "false");

            // just in case...
            System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
            System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "true");

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, TRUST_MANAGERS, null);

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }

    /**
     * @param options
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    public static void init(BasicSignerOptions options) throws NoSuchAlgorithmException, KeyManagementException,
            KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
        KeyManager[] km = null;
        if (options != null && options.getTsaServerAuthn() == ServerAuthentication.CERTIFICATE) {
            char[] pwd = null;
            if (StringUtils.isNotEmpty(options.getTsaCertFilePwd())) {
                pwd = options.getTsaCertFilePwd().toCharArray();
            }
            LOGGER.info(Constants.RES.get("ssl.keymanager.init", options.getTsaCertFile()));
            final String ksType = StringUtils.defaultIfBlank(options.getTsaCertFileType(), "PKCS12");
            KeyStore keyStore = KeyStoreUtils.loadKeyStore(ksType, options.getTsaCertFile(), pwd);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, pwd);
            km = keyManagerFactory.getKeyManagers();
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(km, TRUST_MANAGERS, null);

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }
}
