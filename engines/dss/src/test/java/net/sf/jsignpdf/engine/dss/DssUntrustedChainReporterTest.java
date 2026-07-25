package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.europa.esig.dss.model.x509.CertificateToken;

/**
 * Unit tests for {@link DssUntrustedChainReporter}: given the {@code C-<hex>} token ids DSS puts in the
 * untrusted-chain alert, the reporter must name the offending certificate (subject / issuer / role) rather
 * than echo the bare fingerprint (issue #448). The token ids are obtained from DSS's own
 * {@link CertificateToken} so the test also pins that the reporter's fingerprint computation matches DSS.
 */
public class DssUntrustedChainReporterTest {

    private static final char[] KEY_PASSWD = "keypass".toCharArray();

    private static EmbeddedCa embeddedCa;

    @BeforeClass
    public static void setUpClass() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        embeddedCa = new EmbeddedCa();
        embeddedCa.start();
    }

    @AfterClass
    public static void tearDownClass() {
        if (embeddedCa != null) {
            embeddedCa.stop();
        }
    }

    /** DSS token id for a certificate, the authoritative value the reporter must reproduce. */
    private static String tokenId(X509Certificate cert) {
        return new CertificateToken(cert).getDSSIdAsString();
    }

    private Certificate[] signerChain() throws Exception {
        KeyStore ks = embeddedCa.issueSigningKeyStore("signer", KEY_PASSWD);
        return ks.getCertificateChain("signer");
    }

    @Test
    public void namesSignerChainCertificateBySubjectAndIssuer() throws Exception {
        Certificate[] chain = signerChain();
        X509Certificate leaf = (X509Certificate) chain[0];
        String id = tokenId(leaf);
        Throwable error = new RuntimeException(
                "Revocation data is missing for one or more certificate(s). ["
                        + id + ": Revocation data is skipped for untrusted certificate chain!]");

        String details = DssUntrustedChainReporter.describe(error, chain, null);

        assertTrue("must name the signer role", details.contains("Signer chain certificate"));
        assertTrue("must include the subject CN", details.contains("JSignPdf Test Signer"));
        assertTrue("must include the issuer CN", details.contains("JSignPdf Test Root CA"));
        assertTrue("must keep the fingerprint as a secondary identifier", details.contains(id));
    }

    @Test
    public void namesTimestampChainWhenCaptured() throws Exception {
        Certificate[] chain = signerChain();
        // A distinct leaf (not part of the signer chain) standing in for the captured timestamp certificate.
        X509Certificate tsaCert = (X509Certificate) signerChain()[0];
        String id = tokenId(tsaCert);
        Throwable error = new RuntimeException("Revocation data is missing. [" + id
                + ": Revocation data is skipped for untrusted certificate chain!]");

        String details = DssUntrustedChainReporter.describe(error, chain, List.of(tsaCert));

        assertTrue("must name the timestamp role", details.contains("Timestamp chain certificate"));
        assertTrue("must include the timestamp cert CN", details.contains("JSignPdf Test Signer"));
    }

    @Test
    public void emptyWhenNoTokenIdInMessage() throws Exception {
        Certificate[] chain = signerChain();
        String details = DssUntrustedChainReporter.describe(
                new RuntimeException("some unrelated failure"), chain, null);
        assertEquals("", details);
    }

    @Test
    public void emptyWhenTokenIdMatchesNoKnownCertificate() throws Exception {
        Certificate[] chain = signerChain();
        // A well-formed but unknown token id (not the signer's, no timestamp chain captured): the reporter
        // must not emit a name-less line that merely repeats the fingerprint already in the raw DSS message.
        String unknownId = "C-" + "0".repeat(64);
        String details = DssUntrustedChainReporter.describe(
                new RuntimeException("Revocation data is missing. [" + unknownId + ": untrusted]"), chain, null);
        assertEquals("", details);
    }

    @Test
    public void readsTokenIdFromCauseChain() throws Exception {
        Certificate[] chain = signerChain();
        X509Certificate leaf = (X509Certificate) chain[0];
        String id = tokenId(leaf);
        Throwable root = new RuntimeException("[" + id + ": untrusted]");
        Throwable wrapper = new RuntimeException("signing failed", root);

        String details = DssUntrustedChainReporter.describe(wrapper, chain, null);
        assertFalse("must find the token id nested in the cause chain", details.isEmpty());
        assertTrue(details.contains("JSignPdf Test Signer"));
    }

    /**
     * DSS renders the token id through {@code BigInteger}, so a certificate whose DER SHA-256 starts with a
     * zero byte is reported with a 62-character id. Both the id pattern and our own fingerprinting assumed a
     * fixed 64, which dropped the enrichment for about one certificate in 256 -- reproducible only as a rare
     * CI flake until this test pinned it.
     */
    @Test
    public void namesCertificateWhoseTokenIdHasALeadingZeroByte() throws Exception {
        X509Certificate cert = withLeadingZeroDigestByte();
        String id = tokenId(cert);
        assertEquals("DSS must shorten the id for this certificate", 62, id.length() - 2);

        Throwable error = new RuntimeException("Revocation data is missing for one or more certificate(s). ["
                + id + ": Revocation data is skipped for untrusted certificate chain!]");

        String details = DssUntrustedChainReporter.describe(error, new Certificate[] { cert }, null);

        assertTrue("must name the signer role", details.contains("Signer chain certificate"));
        assertTrue("must include the subject CN", details.contains("Leading Zero Digest"));
        assertTrue("must keep the fingerprint as a secondary identifier", details.contains(id));
    }

    /**
     * Mines a certificate whose DER SHA-256 starts with a {@code 0x00} byte (about one attempt in 256) by
     * re-signing with a single key pair and only varying the serial number, which keeps this cheap.
     */
    private static X509Certificate withLeadingZeroDigestByte() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        X500Name name = new X500Name("CN=Leading Zero Digest");
        Date notBefore = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate());
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        for (int serial = 1; serial < 20000; serial++) {
            X509CertificateHolder holder = new JcaX509v3CertificateBuilder(name, BigInteger.valueOf(serial),
                    notBefore, notAfter, name, keyPair.getPublic()).build(signer);
            if (sha256.digest(holder.getEncoded())[0] == 0) {
                return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .getCertificate(holder);
            }
        }
        throw new AssertionError("no certificate with a leading zero digest byte found");
    }
}
