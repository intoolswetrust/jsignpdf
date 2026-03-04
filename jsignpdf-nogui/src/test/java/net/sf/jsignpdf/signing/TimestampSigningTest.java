package net.sf.jsignpdf.signing;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.signing.tsa.EmbeddedTsaServer;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;
import net.sf.jsignpdf.types.ServerAuthentication;

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
    public void testTimestampWithDifferentHashAlgSetting() throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        options.setTimestamp(true);
        options.setTsaUrl(tsaServer.getUrl());
        options.setTsaHashAlg("SHA-1");

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertTrue("Should have timestamp", result.hasTimestamp);
        // DSS determines the timestamp digest algorithm internally (defaults to SHA-256)
        assertEquals("Timestamp digest should be SHA-256",
                "2.16.840.1.101.3.4.2.1", result.timestampDigestAlgorithmOid);
    }

    @Test
    public void testSigningWithTsaBasicAuthentication() throws Exception {
        EmbeddedTsaServer authTsa = new EmbeddedTsaServer();
        authTsa.requireBasicAuth("tsaUser", "tsaSecret");
        authTsa.start();
        try {
            BasicSignerOptions options = createDefaultOptions();
            options.setTimestamp(true);
            options.setTsaUrl(authTsa.getUrl());
            options.setTsaHashAlg("SHA-256");
            options.setTsaServerAuthn(ServerAuthentication.PASSWORD);
            options.setTsaUser("tsaUser");
            options.setTsaPasswd("tsaSecret");

            ValidationResult result = signAndValidate(options);

            assertEquals("Should have 1 signature", 1, result.signatureCount);
            assertTrue("Signature should be valid", result.signatureValid);
            assertTrue("Signature should contain a timestamp token", result.hasTimestamp);
        } finally {
            authTsa.stop();
        }
    }

    @Test
    public void testSigningWithTsaBasicAuthenticationWrongPassword() throws Exception {
        EmbeddedTsaServer authTsa = new EmbeddedTsaServer();
        authTsa.requireBasicAuth("tsaUser", "tsaSecret");
        authTsa.start();
        try {
            BasicSignerOptions options = createDefaultOptions();
            options.setTimestamp(true);
            options.setTsaUrl(authTsa.getUrl());
            options.setTsaHashAlg("SHA-256");
            options.setTsaServerAuthn(ServerAuthentication.PASSWORD);
            options.setTsaUser("tsaUser");
            options.setTsaPasswd("wrongPassword");

            boolean result = new SignerLogic(options).signFile();
            assertFalse("Signing should fail when TSA authentication fails", result);
        } finally {
            authTsa.stop();
        }
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
