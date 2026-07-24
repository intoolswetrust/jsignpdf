package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.jsignpdf.Constants;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Test;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.model.x509.revocation.crl.CRL;
import eu.europa.esig.dss.spi.client.http.DataLoader;
import eu.europa.esig.dss.spi.exception.DSSExternalResourceException;
import eu.europa.esig.dss.spi.x509.aia.AIASource;
import eu.europa.esig.dss.spi.x509.revocation.RevocationSource;

/**
 * Tests the FINE-level tracing decorators wired into the DSS certificate verifier (issue #452): every AIA /
 * CRL / OCSP call must be reported with its URL, outcome and timing, and the decorators must pass the
 * delegate's result (and failures) through untouched.
 */
public class DssDebugTracingTest {

    private static final String URL = "http://revocation.example.test/crl";

    @Test
    public void dataLoaderIsNotWrappedWhenFineIsOff() {
        final DataLoader delegate = new StubDataLoader(new byte[10], null);
        final Level originalLevel = Constants.LOGGER.getLevel();
        Constants.LOGGER.setLevel(Level.INFO);
        try {
            assertSame(delegate, LoggingDataLoader.wrap("CRL", delegate));
        } finally {
            Constants.LOGGER.setLevel(originalLevel);
        }
    }

    @Test
    public void dataLoaderLogsUrlAndResponseSize() {
        final DataLoader delegate = new StubDataLoader(new byte[1234], null);
        final List<LogRecord> records = captureFine(
                () -> assertEquals(1234, LoggingDataLoader.wrap("CRL", delegate).get(URL).length));

        assertEquals(1, records.size());
        final String message = records.get(0).getMessage();
        assertTrue(message, message.startsWith("CRL GET " + URL + ": 1234 bytes"));
        assertTrue(message, message.contains("elapsed="));
    }

    @Test
    public void dataLoaderLogsFailureAndRethrows() {
        final RuntimeException failure = new DSSExternalResourceException("HTTP status code : 503");
        final DataLoader delegate = new StubDataLoader(null, failure);
        final List<LogRecord> records = captureFine(() -> {
            try {
                LoggingDataLoader.wrap("OCSP", delegate).post(URL, new byte[1]);
                fail("The delegate's exception must be propagated");
            } catch (DSSExternalResourceException e) {
                assertSame(failure, e);
            }
        });

        assertEquals(1, records.size());
        final String message = records.get(0).getMessage();
        assertTrue(message, message.contains("OCSP POST " + URL + ": FAILED"));
        assertTrue(message, message.contains("HTTP status code : 503"));
    }

    @Test
    public void revocationSourceLogsTheCertificateWhenNoDataIsAvailable() throws Exception {
        final CertificateToken certificate = certificateToken();
        final List<LogRecord> records = captureFine(() -> {
            final RevocationSource<CRL> source = LoggingRevocationSource.wrap("CRL", (cert, issuer) -> null);
            assertNull(source.getRevocationToken(certificate, certificate));
        });

        assertEquals(1, records.size());
        final String message = records.get(0).getMessage();
        assertTrue(message, message.startsWith("CRL lookup for "));
        assertTrue(message, message.contains(certificate.getDSSIdAsString()));
        assertTrue(message, message.contains("no CRL data"));
    }

    @Test
    public void revocationSourceLogsFailureAndRethrows() throws Exception {
        final CertificateToken certificate = certificateToken();
        final RuntimeException failure = new DSSExternalResourceException("Unable to retrieve CRL");
        final List<LogRecord> records = captureFine(() -> {
            final RevocationSource<CRL> source = LoggingRevocationSource.wrap("CRL", (cert, issuer) -> {
                throw failure;
            });
            try {
                source.getRevocationToken(certificate, certificate);
                fail("The delegate's exception must be propagated");
            } catch (DSSExternalResourceException e) {
                assertSame(failure, e);
            }
        });

        assertEquals(1, records.size());
        assertTrue(records.get(0).getMessage(), records.get(0).getMessage().contains("FAILED"));
    }

    @Test
    public void aiaSourceLogsTheNumberOfDownloadedIssuers() throws Exception {
        final CertificateToken certificate = certificateToken();
        final List<LogRecord> records = captureFine(() -> {
            final AIASource source = LoggingAIASource.wrap(Collections::singleton);
            assertEquals(1, source.getCertificatesByAIA(certificate).size());
        });

        assertEquals(1, records.size());
        final String message = records.get(0).getMessage();
        assertTrue(message, message.startsWith("AIA lookup for "));
        assertTrue(message, message.contains("1 issuer certificate(s)"));
    }

    /** Runs {@code action} with FINE enabled on the JSignPdf logger and returns the records it produced. */
    private static List<LogRecord> captureFine(Runnable action) {
        final List<LogRecord> records = new ArrayList<>();
        final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        final Level originalLevel = Constants.LOGGER.getLevel();
        Constants.LOGGER.setLevel(Level.FINE);
        Constants.LOGGER.addHandler(handler);
        try {
            action.run();
        } finally {
            Constants.LOGGER.removeHandler(handler);
            Constants.LOGGER.setLevel(originalLevel);
        }
        return records;
    }

    private static CertificateToken certificateToken() throws Exception {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        final KeyPair keyPair = kpg.generateKeyPair();
        final X500Name name = new X500Name("CN=Revocation Trace Test");
        final Date notBefore = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        final Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);
        final ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        final X509CertificateHolder holder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, notBefore,
                notAfter, name, keyPair.getPublic()).build(signer);
        return new CertificateToken(new JcaX509CertificateConverter().getCertificate(holder));
    }

    /** A {@link DataLoader} that answers every call with a fixed payload, or fails with a fixed exception. */
    private static final class StubDataLoader implements DataLoader {

        private static final long serialVersionUID = 1L;

        private final byte[] response;
        private final RuntimeException failure;

        StubDataLoader(byte[] response, RuntimeException failure) {
            this.response = response;
            this.failure = failure;
        }

        @Override
        public byte[] get(String url) {
            return answer();
        }

        @Override
        public DataAndUrl get(List<String> urlStrings) {
            return new DataAndUrl(urlStrings.get(0), answer());
        }

        @Override
        public byte[] post(String url, byte[] content) {
            return answer();
        }

        @Override
        public void setContentType(String contentType) {
        }

        private byte[] answer() {
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
