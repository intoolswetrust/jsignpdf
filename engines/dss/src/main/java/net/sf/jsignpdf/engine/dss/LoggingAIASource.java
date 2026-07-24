package net.sf.jsignpdf.engine.dss;

import java.util.Set;
import java.util.logging.Level;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.CertificateInfo;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.x509.aia.AIASource;

/**
 * An {@link AIASource} decorator that reports the issuer-certificate downloads DSS performs to complete a
 * chain. A chain the keystore delivers incomplete is silently completed over AIA, so a failure to reach the
 * {@code caIssuers} URL surfaces much later as an unanchored chain; logging the lookup and how many
 * certificates it yielded makes the two cases distinguishable (issue #452).
 *
 * @author Josef Cacek
 */
final class LoggingAIASource implements AIASource {

    private static final long serialVersionUID = 1L;

    private final AIASource delegate;

    LoggingAIASource(AIASource delegate) {
        this.delegate = delegate;
    }

    /**
     * Wraps {@code delegate} only when FINE logging is active, so a normal run keeps DSS's original source.
     *
     * @param delegate the AIA source to trace
     * @return the decorated source, or {@code delegate} itself when FINE is off
     */
    static AIASource wrap(AIASource delegate) {
        return Constants.LOGGER.isLoggable(Level.FINE) ? new LoggingAIASource(delegate) : delegate;
    }

    @Override
    public Set<CertificateToken> getCertificatesByAIA(CertificateToken certificateToken) {
        final long startNanos = System.nanoTime();
        try {
            final Set<CertificateToken> certificates = delegate.getCertificatesByAIA(certificateToken);
            logResult(certificateToken, certificates != null ? certificates.size() : 0, startNanos, null);
            return certificates;
        } catch (RuntimeException e) {
            logResult(certificateToken, 0, startNanos, e);
            throw e;
        }
    }

    private void logResult(CertificateToken certificateToken, int count, long startNanos, RuntimeException failure) {
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        final StringBuilder buf = new StringBuilder("AIA lookup for ");
        if (certificateToken.getCertificate() != null) {
            buf.append(CertificateInfo.subjectOf(certificateToken.getCertificate()));
        } else {
            buf.append(certificateToken.getSubject().getCanonical());
        }
        buf.append(" (").append(certificateToken.getDSSIdAsString()).append("): ");
        if (failure != null) {
            buf.append("FAILED (").append(failure.getClass().getSimpleName()).append(": ")
                    .append(failure.getMessage()).append(')');
        } else {
            buf.append(count).append(" issuer certificate(s)");
        }
        buf.append(", elapsed=").append(elapsedMs).append("ms");
        Constants.LOGGER.fine(buf.toString());
    }
}
