package net.sf.jsignpdf.engine.dss;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.CertificateInfo;

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

    private final String tsaUrl;

    private final TSPSource delegate;

    /** Captured timestamp certificates, keyed by DSS token id to de-duplicate across signature/archive tokens. */
    private final Map<String, X509Certificate> capturedCerts = new LinkedHashMap<>();

    CapturingTspSource(String tsaUrl, TSPSource delegate) {
        this.tsaUrl = tsaUrl;
        this.delegate = delegate;
    }

    @Override
    public TimestampBinary getTimeStampResponse(DigestAlgorithm digestAlgorithm, byte[] digest) throws DSSException {
        final boolean debug = Constants.LOGGER.isLoggable(Level.FINE);
        if (debug) {
            Constants.LOGGER.fine("TSA request -> " + tsaUrl + ": digestAlgorithm=" + digestAlgorithm + ", digest="
                    + (digest != null ? HexFormat.of().formatHex(digest) : "null"));
        }
        final long startNanos = System.nanoTime();
        TimestampBinary token = delegate.getTimeStampResponse(digestAlgorithm, digest);
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        final int knownCerts = capturedCerts.size();
        capture(token);
        if (debug) {
            logResponse(token, elapsedMs);
            if (capturedCerts.size() > knownCerts) {
                CertificateInfo.logChain("Timestamp certificate chain", capturedCerts.values());
            }
        }
        return token;
    }

    private void logResponse(TimestampBinary token, long elapsedMs) {
        if (token == null) {
            Constants.LOGGER.fine("TSA response <- " + tsaUrl + ": empty response, elapsed=" + elapsedMs + "ms");
            return;
        }
        String genTime = "?";
        int certs = 0;
        try {
            TimestampToken parsed = new TimestampToken(token.getBytes(), TimestampType.SIGNATURE_TIMESTAMP);
            genTime = String.valueOf(parsed.getGenerationTime());
            certs = parsed.getCertificates().size();
        } catch (Exception e) {
            genTime = "unparseable";
        }
        Constants.LOGGER.fine("TSA response <- " + tsaUrl + ": " + token.getBytes().length + " bytes, genTime="
                + genTime + ", certs=" + certs + ", elapsed=" + elapsedMs + "ms");
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
