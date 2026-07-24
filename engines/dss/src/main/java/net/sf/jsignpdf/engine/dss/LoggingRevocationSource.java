package net.sf.jsignpdf.engine.dss;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.CertificateInfo;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.model.x509.revocation.Revocation;
import eu.europa.esig.dss.spi.x509.revocation.RevocationSource;
import eu.europa.esig.dss.spi.x509.revocation.RevocationSourceAlternateUrlsSupport;
import eu.europa.esig.dss.spi.x509.revocation.RevocationToken;

/**
 * A {@link RevocationSource} decorator that names the certificate behind every CRL / OCSP lookup DSS performs
 * for the LT / LTA levels. {@link LoggingDataLoader} already traces the HTTP leg, but a URL alone does not say
 * <em>which</em> certificate the fetch was for, nor what the answer said; this fills that in with the outcome:
 * the responder URL DSS settled on, the certificate status and, for a CRL, the {@code thisUpdate} /
 * {@code nextUpdate} window of what came back (issue #452).
 *
 * <p>
 * A revocation lookup has three outcomes and all three are reported: a token, {@code null} (no CRL / OCSP
 * location on the certificate at all &mdash; the usual reason an LT signature cannot be built), or a
 * {@code DSSExternalResourceException} when every access point failed. The delegate's behaviour is preserved in
 * each case, exceptions included.
 * </p>
 *
 * @param <R> the revocation type handled by the wrapped source (CRL or OCSP)
 *
 * @author Josef Cacek
 */
final class LoggingRevocationSource<R extends Revocation> implements RevocationSourceAlternateUrlsSupport<R> {

    private static final long serialVersionUID = 1L;

    /** Short name of the revocation mechanism, used as the log prefix: {@code CRL} or {@code OCSP}. */
    private final String role;

    private final RevocationSource<R> delegate;

    LoggingRevocationSource(String role, RevocationSource<R> delegate) {
        this.role = role;
        this.delegate = delegate;
    }

    /**
     * Wraps {@code delegate} only when FINE logging is active, so a normal run keeps DSS's original source.
     *
     * @param role     the log prefix ({@code CRL} or {@code OCSP})
     * @param delegate the source to trace
     * @return the decorated source, or {@code delegate} itself when FINE is off
     */
    static <R extends Revocation> RevocationSource<R> wrap(String role, RevocationSource<R> delegate) {
        return Constants.LOGGER.isLoggable(Level.FINE) ? new LoggingRevocationSource<>(role, delegate) : delegate;
    }

    @Override
    public RevocationToken<R> getRevocationToken(CertificateToken certificateToken,
            CertificateToken issuerCertificateToken) {
        final long startNanos = System.nanoTime();
        try {
            final RevocationToken<R> token = delegate.getRevocationToken(certificateToken, issuerCertificateToken);
            logResult(certificateToken, token, startNanos, null);
            return token;
        } catch (RuntimeException e) {
            logResult(certificateToken, null, startNanos, e);
            throw e;
        }
    }

    @Override
    public RevocationToken<R> getRevocationToken(CertificateToken certificateToken,
            CertificateToken issuerCertificateToken, List<String> alternativeUrls) {
        if (!(delegate instanceof RevocationSourceAlternateUrlsSupport)) {
            return getRevocationToken(certificateToken, issuerCertificateToken);
        }
        @SuppressWarnings("unchecked")
        final RevocationSourceAlternateUrlsSupport<R> alternateUrlsDelegate =
                (RevocationSourceAlternateUrlsSupport<R>) delegate;
        final long startNanos = System.nanoTime();
        try {
            final RevocationToken<R> token = alternateUrlsDelegate.getRevocationToken(certificateToken,
                    issuerCertificateToken, alternativeUrls);
            logResult(certificateToken, token, startNanos, null);
            return token;
        } catch (RuntimeException e) {
            logResult(certificateToken, null, startNanos, e);
            throw e;
        }
    }

    private void logResult(CertificateToken certificateToken, RevocationToken<R> token, long startNanos,
            RuntimeException failure) {
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        final StringBuilder buf = new StringBuilder(role).append(" lookup for ")
                .append(subjectOf(certificateToken)).append(" (").append(certificateToken.getDSSIdAsString())
                .append("): ");
        if (failure != null) {
            buf.append("FAILED (").append(failure.getClass().getSimpleName()).append(": ")
                    .append(failure.getMessage()).append(')');
        } else if (token == null) {
            buf.append("no ").append(role).append(" data (no access point on the certificate, or none usable)");
        } else {
            buf.append("status=").append(token.getStatus());
            appendIfPresent(buf, ", source=", token.getSourceURL());
            appendDateIfPresent(buf, ", thisUpdate=", token.getThisUpdate());
            appendDateIfPresent(buf, ", nextUpdate=", token.getNextUpdate());
            appendDateIfPresent(buf, ", producedAt=", token.getProductionDate());
            appendDateIfPresent(buf, ", revokedAt=", token.getRevocationDate());
            appendIfPresent(buf, ", reason=", token.getReason());
        }
        buf.append(", elapsed=").append(elapsedMs).append("ms");
        Constants.LOGGER.fine(buf.toString());
    }

    private static String subjectOf(CertificateToken certificateToken) {
        return certificateToken.getCertificate() != null
                ? CertificateInfo.subjectOf(certificateToken.getCertificate())
                : certificateToken.getSubject().getCanonical();
    }

    private static void appendIfPresent(StringBuilder buf, String caption, Object value) {
        if (value != null) {
            buf.append(caption).append(value);
        }
    }

    private static void appendDateIfPresent(StringBuilder buf, String caption, Date value) {
        if (value != null) {
            buf.append(caption).append(CertificateInfo.formatTimestamp(value));
        }
    }
}
