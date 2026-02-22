package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.signing.tsa.EmbeddedTsaServer;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;

/**
 * Integration tests for PDF signing with RFC 3161 timestamping. Uses an
 * embedded TSA server backed by BouncyCastle TSP.
 */
public class TimestampSigningTest extends SigningTestBase {

    private static EmbeddedTsaServer tsaServer;

    @BeforeClass
    public static void startTsa() throws Exception {
        tsaServer = new EmbeddedTsaServer();
        tsaServer.start();
    }

    @AfterClass
    public static void stopTsa() {
        if (tsaServer != null) {
            tsaServer.stop();
        }
    }

    private BasicSignerOptions createTimestampOptions() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setTimestamp(true);
        options.setTsaUrl(tsaServer.getUrl());
        options.setTsaHashAlg("SHA-256");
        return options;
    }

    @Test
    public void testSigningWithTimestamp() throws Exception {
        BasicSignerOptions options = createTimestampOptions();
        ValidationResult result = signAndValidate(options);

        assertEquals("Should have 1 signature", 1, result.signatureCount);
        assertTrue("Signature should be valid", result.signatureValid);
        assertTrue("Signature should contain a timestamp token", result.hasTimestamp);
    }

    @Test
    public void testTimestampDigestAlgorithm() throws Exception {
        BasicSignerOptions options = createTimestampOptions();
        ValidationResult result = signAndValidate(options);

        assertTrue("Should have timestamp", result.hasTimestamp);
        assertNotNull("Timestamp digest algorithm OID should be present", result.timestampDigestAlgorithmOid);
        // We requested SHA-256; its OID is 2.16.840.1.101.3.4.2.1
        assertEquals("Timestamp digest algorithm should be SHA-256",
                "2.16.840.1.101.3.4.2.1", result.timestampDigestAlgorithmOid);
    }

    @Test
    public void testTimestampHasDate() throws Exception {
        BasicSignerOptions options = createTimestampOptions();
        ValidationResult result = signAndValidate(options);

        assertTrue("Should have timestamp", result.hasTimestamp);
        assertNotNull("Timestamp date should be present", result.timestampDate);
    }

    @Test
    public void testTimestampWithSha1HashAlg() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setTimestamp(true);
        options.setTsaUrl(tsaServer.getUrl());
        options.setTsaHashAlg("SHA-1");

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertTrue("Should have timestamp", result.hasTimestamp);
        // SHA-1 OID is 1.3.14.3.2.26
        assertEquals("Timestamp digest should be SHA-1",
                "1.3.14.3.2.26", result.timestampDigestAlgorithmOid);
    }

    @Test
    public void testSignatureWithoutTimestampHasNoToken() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        // Timestamp NOT enabled
        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertFalse("Signature without TSA should have no timestamp token", result.hasTimestamp);
    }
}
