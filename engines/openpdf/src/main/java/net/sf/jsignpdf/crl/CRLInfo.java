package net.sf.jsignpdf.crl;

import static net.sf.jsignpdf.Constants.RES;
import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.input.CountingInputStream;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;

/**
 * Helper bean for holding CRL related data.
 *
 * @author Josef Cacek
 *
 */
public class CRLInfo {

    /** Maximum number of redirects to follow when downloading a CRL. */
    private static final int MAX_CRL_REDIRECTS = 5;

    private CRL[] crls;
    private long byteCount = 0L;
    private BasicSignerOptions options;
    private Certificate[] certChain;

    /**
     * Constructor
     *
     * @param anOptions
     * @param aChain
     */
    public CRLInfo(final BasicSignerOptions anOptions, final Certificate[] aChain) {
        if (anOptions == null || aChain == null) {
            throw new NullPointerException();
        }
        options = anOptions;
        certChain = aChain;
    }

    /**
     * Returns CRLs for the certificate chain.
     *
     * @return
     */
    public CRL[] getCrls() {
        initCrls();
        return crls;
    }

    /**
     * Returns byte count, which should
     *
     * @return
     */
    public long getByteCount() {
        initCrls();
        return byteCount;
    }

    /**
     * Initialize CRLs (load URLs from certificates and download the CRLs).
     */
    private void initCrls() {
        if (!options.isCrlEnabledX() || crls != null) {
            return;
        }
        LOGGER.info(RES.get("console.readingCRLs"));
        final Set<String> urls = new HashSet<String>();
        for (Certificate cert : certChain) {
            if (cert instanceof X509Certificate x509Cert) {
                urls.addAll(getCrlUrls(x509Cert));
            }
        }
        final Set<CRL> crlSet = new HashSet<CRL>();
        for (final String urlStr : urls) {
            try {
                LOGGER.info(RES.get("console.crlinfo.loadCrl", urlStr));
                final URL tmpUrl = new URL(urlStr);
                final CountingInputStream inStream = new CountingInputStream(
                        openCrlStream(tmpUrl, options.createProxy()));
                final CertificateFactory cf = CertificateFactory.getInstance(Constants.CERT_TYPE_X509);
                final CRL crl = cf.generateCRL(inStream);
                final long tmpBytesRead = inStream.getByteCount();
                LOGGER.info(RES.get("console.crlinfo.crlSize", String.valueOf(tmpBytesRead)));
                if (!crlSet.contains(crl)) {
                    byteCount += tmpBytesRead;
                    crlSet.add(crl);
                } else {
                    LOGGER.info(RES.get("console.crlinfo.alreadyLoaded"));
                }
                inStream.close();
            } catch (MalformedURLException e) {
                LOGGER.log(Level.WARNING, "", e);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "", e);
            } catch (CertificateException e) {
                LOGGER.log(Level.WARNING, "", e);
            } catch (CRLException e) {
                LOGGER.log(Level.WARNING, "", e);
            }
        }
        crls = crlSet.toArray(new CRL[crlSet.size()]);
    }

    /**
     * Opens an input stream for the given CRL URL, following redirects manually.
     * <p>
     * {@link HttpURLConnection} only follows redirects that stay on the same protocol; a CRL
     * distribution point that redirects HTTP&nbsp;&rarr;&nbsp;HTTPS (or back) is therefore not
     * followed automatically and the caller sees an empty response. We follow the {@code Location}
     * header ourselves so cross-scheme redirects work too (#254). The configured proxy is reused
     * for every hop.
     *
     * @param url   the CRL distribution-point URL
     * @param proxy the proxy to use (never {@code null}; may be {@link Proxy#NO_PROXY})
     * @return the response input stream of the final, non-redirecting hop
     * @throws IOException on connection errors, a redirect without a {@code Location} header, or too
     *         many redirects
     */
    private InputStream openCrlStream(final URL url, final Proxy proxy) throws IOException {
        URL currentUrl = url;
        for (int hop = 0; hop <= MAX_CRL_REDIRECTS; hop++) {
            final URLConnection conn = currentUrl.openConnection(proxy);
            if (!(conn instanceof HttpURLConnection httpConn)) {
                return conn.getInputStream();
            }
            // Follow redirects ourselves so that cross-scheme hops are honoured.
            httpConn.setInstanceFollowRedirects(false);
            final int status = httpConn.getResponseCode();
            if (status < 300 || status >= 400) {
                return httpConn.getInputStream();
            }
            final String location = httpConn.getHeaderField("Location");
            httpConn.disconnect();
            if (location == null) {
                throw new IOException("CRL redirect (HTTP " + status + ") without Location header: " + currentUrl);
            }
            // Resolve against the current URL to support relative redirects.
            currentUrl = new URL(currentUrl, location);
            LOGGER.info(RES.get("console.crlinfo.loadCrl", currentUrl.toString()));
        }
        throw new IOException("Too many CRL redirects for: " + url);
    }

    /**
     * Returns (initialized, but maybe empty) set of URLs of CRLs for given certificate.
     *
     * @param aCert X509 certificate.
     * @return
     */
    private Set<String> getCrlUrls(final X509Certificate aCert) {
        final Set<String> tmpResult = new HashSet<String>();
        LOGGER.info(RES.get("console.crlinfo.retrieveCrlUrl", aCert.getSubjectX500Principal().getName()));
        final byte[] crlDPExtension = aCert.getExtensionValue(X509Extension.cRLDistributionPoints.getId());
        if (crlDPExtension != null) {
            CRLDistPoint crlDistPoints = null;
            try {
                crlDistPoints = CRLDistPoint.getInstance(X509ExtensionUtil.fromExtensionValue(crlDPExtension));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "", e);
            }
            if (crlDistPoints != null) {
                final DistributionPoint[] distPoints = crlDistPoints.getDistributionPoints();
                distPoint: for (DistributionPoint dp : distPoints) {
                    final DistributionPointName dpName = dp.getDistributionPoint();
                    final GeneralNames generalNames = (GeneralNames) dpName.getName();
                    if (generalNames != null) {
                        final GeneralName[] generalNameArr = generalNames.getNames();
                        if (generalNameArr != null) {
                            for (final GeneralName generalName : generalNameArr) {
                                if (generalName.getTagNo() == GeneralName.uniformResourceIdentifier) {
                                    final ASN1String derString = (ASN1String) generalName.getName();
                                    final String uri = derString.getString();
                                    if (uri != null && uri.startsWith("http")) {
                                        // ||uri.startsWith("ftp")
                                        LOGGER.info(RES.get("console.crlinfo.foundCrlUri", uri));
                                        tmpResult.add(uri);
                                        continue distPoint;
                                    }
                                }
                            }
                        }
                        LOGGER.info(RES.get("console.crlinfo.noUrlInDistPoint"));
                    }
                }
            }
        } else {
            LOGGER.info(RES.get("console.crlinfo.distPointNotSupported"));
        }
        return tmpResult;
    }
}
