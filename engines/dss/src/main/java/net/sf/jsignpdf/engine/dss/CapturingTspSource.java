package net.sf.jsignpdf.engine.dss;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.TimestampType;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.TimestampBinary;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.spi.x509.tsp.TimestampToken;

/**
 * A {@link TSPSource} decorator that records the certificate chain embedded in every timestamp token it hands
 * back. The recorded certificates let {@link DssUntrustedChainReporter} name the timestamp chain (not just the
 * signer chain) when DSS later refuses an LT/LTA signature because that chain is not anchored &mdash; the
 * timestamp CA was exactly the anchor missing in issue #441, and a bare {@code C-<hex>} fingerprint gave no
 * hint which chain it belonged to (issue #448).
 *
 * <p>
 * Capture is best-effort and never affects signing: the delegate's token is returned unchanged, and any
 * failure to parse it for diagnostics is swallowed. DSS obtains the signature timestamp before it collects the
 * revocation data whose absence triggers the untrusted-chain alert, so by the time the reporter runs the chain
 * has normally been captured.
 * </p>
 */
final class CapturingTspSource implements TSPSource {

    private static final long serialVersionUID = 1L;

    private final TSPSource delegate;

    /** Captured timestamp certificates, keyed by DSS token id to de-duplicate across signature/archive tokens. */
    private final Map<String, X509Certificate> capturedCerts = new LinkedHashMap<>();

    CapturingTspSource(TSPSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public TimestampBinary getTimeStampResponse(DigestAlgorithm digestAlgorithm, byte[] digest) throws DSSException {
        TimestampBinary token = delegate.getTimeStampResponse(digestAlgorithm, digest);
        capture(token);
        return token;
    }

    private void capture(TimestampBinary token) {
        if (token == null) {
            return;
        }
        try {
            TimestampToken parsed = new TimestampToken(token.getBytes(), TimestampType.SIGNATURE_TIMESTAMP);
            for (CertificateToken certToken : parsed.getCertificates()) {
                if (certToken != null && certToken.getCertificate() != null) {
                    capturedCerts.putIfAbsent(certToken.getDSSIdAsString(), certToken.getCertificate());
                }
            }
        } catch (Exception e) {
            // Diagnostics only: a token we cannot parse simply leaves the timestamp chain unnamed in a later
            // untrusted-chain report; it must never interfere with the signing operation itself.
        }
    }

    /** @return the certificates seen in the issued timestamp tokens (leaf and any bundled CA certificates). */
    Collection<X509Certificate> getCapturedCertificates() {
        return capturedCerts.values();
    }
}
