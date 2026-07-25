package net.sf.jsignpdf.engine.dss;

import java.util.List;
import java.util.logging.Level;

import net.sf.jsignpdf.Constants;

import eu.europa.esig.dss.spi.client.http.DataLoader;

/**
 * A {@link DataLoader} decorator that traces every HTTP call DSS makes while collecting validation material.
 * DSS's own logging goes through SLF4J, which JSignPdf does not wire to its {@code java.util.logging} console,
 * so without this the AIA / CRL / OCSP traffic is invisible: a signature that fails for want of revocation data
 * shows <em>that</em> it is missing but not which server was contacted, whether it answered, or how long it took
 * (issue #452).
 *
 * <p>
 * One line is logged per request at {@link Level#FINE}: the role ({@code AIA} / {@code CRL} / {@code OCSP}), the
 * URL, the response size and the elapsed time &mdash; or the failure reason when the call did not return data.
 * The delegate's result is passed through unchanged and exceptions are rethrown, so the decorator cannot alter
 * the outcome of a fetch.
 * </p>
 *
 * @author Josef Cacek
 */
final class LoggingDataLoader implements DataLoader {

    private static final long serialVersionUID = 1L;

    /** Short name of what this loader fetches, used as the log prefix: {@code AIA}, {@code CRL} or {@code OCSP}. */
    private final String role;

    private final DataLoader delegate;

    LoggingDataLoader(String role, DataLoader delegate) {
        this.role = role;
        this.delegate = delegate;
    }

    /**
     * Wraps {@code delegate} only when FINE logging is active, so a normal run keeps DSS's original loader and
     * pays nothing for the tracing.
     *
     * @param role     the log prefix ({@code AIA} / {@code CRL} / {@code OCSP})
     * @param delegate the loader to trace
     * @return the decorated loader, or {@code delegate} itself when FINE is off
     */
    static DataLoader wrap(String role, DataLoader delegate) {
        return Constants.LOGGER.isLoggable(Level.FINE) ? new LoggingDataLoader(role, delegate) : delegate;
    }

    @Override
    public byte[] get(String url) {
        final long startNanos = System.nanoTime();
        try {
            final byte[] data = delegate.get(url);
            logResult("GET", url, data != null ? data.length : -1, startNanos, null);
            return data;
        } catch (RuntimeException e) {
            logResult("GET", url, -1, startNanos, e);
            throw e;
        }
    }

    @Override
    public DataAndUrl get(List<String> urlStrings) {
        final long startNanos = System.nanoTime();
        try {
            final DataAndUrl result = delegate.get(urlStrings);
            final String url = result != null ? result.getUrlString() : String.valueOf(urlStrings);
            logResult("GET", url, result != null && result.getData() != null ? result.getData().length : -1,
                    startNanos, null);
            return result;
        } catch (RuntimeException e) {
            logResult("GET", String.valueOf(urlStrings), -1, startNanos, e);
            throw e;
        }
    }

    @Override
    public byte[] post(String url, byte[] content) {
        final long startNanos = System.nanoTime();
        try {
            final byte[] data = delegate.post(url, content);
            logResult("POST", url, data != null ? data.length : -1, startNanos, null);
            return data;
        } catch (RuntimeException e) {
            logResult("POST", url, -1, startNanos, e);
            throw e;
        }
    }

    @Override
    public void setContentType(String contentType) {
        delegate.setContentType(contentType);
    }

    private void logResult(String method, String url, int size, long startNanos, RuntimeException failure) {
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        final StringBuilder buf = new StringBuilder(role).append(' ').append(method).append(' ').append(url)
                .append(": ");
        if (failure != null) {
            buf.append("FAILED (").append(failure.getClass().getSimpleName()).append(": ")
                    .append(failure.getMessage()).append(')');
        } else if (size < 0) {
            buf.append("no data");
        } else {
            buf.append(size).append(" bytes");
        }
        buf.append(", elapsed=").append(elapsedMs).append("ms");
        Constants.LOGGER.fine(buf.toString());
    }
}
