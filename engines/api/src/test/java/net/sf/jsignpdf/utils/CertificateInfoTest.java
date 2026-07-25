package net.sf.jsignpdf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.jsignpdf.Constants;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.qualified.ETSIQCObjectIdentifiers;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the certificate description used by the FINE-level signing diagnostics (issue #452).
 */
public class CertificateInfoTest {

    private static final String OCSP_URL = "http://ocsp.example.test/ocsp";
    private static final String CA_ISSUERS_URL = "http://aia.example.test/ca.crt";
    private static final String CRL_URL = "http://crl.example.test/root.crl";

    @BeforeClass
    public static void addProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    public void tokenIdIsDssCertificateTokenId() throws Exception {
        final X509Certificate cert = selfSigned("CN=Token Id Test", true);
        final String expected = "C-" + HexFormat.of().withUpperCase()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
        assertEquals(expected, CertificateInfo.tokenId(cert));
    }

    @Test
    public void describeContainsIdentityAndExtensions() throws Exception {
        final X509Certificate cert = selfSigned("CN=Full Certificate,O=JSignPdf Test", true);
        final String description = CertificateInfo.describe(cert, "  [0] ");

        assertTrue(description, description.contains("subject=O=JSignPdf Test,CN=Full Certificate"));
        assertTrue(description, description.contains("issuer=O=JSignPdf Test,CN=Full Certificate"));
        assertTrue(description, description.contains("serial=" + cert.getSerialNumber().toString(16)));
        assertTrue(description, description.contains("id=" + CertificateInfo.tokenId(cert)));
        assertTrue(description, description.contains("digitalSignature"));
        assertTrue(description, description.contains("nonRepudiation"));
        assertTrue(description, description.contains("QcCompliance"));
        assertTrue(description, description.contains(CA_ISSUERS_URL));
        assertTrue(description, description.contains(OCSP_URL));
        assertTrue(description, description.contains(CRL_URL));
    }

    @Test
    public void describeOmitsAbsentExtensions() throws Exception {
        final String description = CertificateInfo.describe(selfSigned("CN=Bare Certificate", false), "  [0] ");

        assertTrue(description, description.contains("subject=CN=Bare Certificate"));
        assertTrue(description, !description.contains("keyUsage="));
        assertTrue(description, !description.contains("AIA "));
        assertTrue(description, !description.contains("CRL DP="));
        assertTrue(description, !description.contains("qcStatements="));
    }

    @Test
    public void extensionAccessorsReturnTheConfiguredUrls() throws Exception {
        final X509Certificate cert = selfSigned("CN=Urls", true);

        assertEquals(List.of(CA_ISSUERS_URL), CertificateInfo.caIssuersUrls(cert));
        assertEquals(List.of(OCSP_URL), CertificateInfo.ocspUrls(cert));
        assertEquals(List.of(CRL_URL), CertificateInfo.crlDistributionPointUrls(cert));
        assertEquals(List.of("QcCompliance"), CertificateInfo.qcStatements(cert));
    }

    @Test
    public void subjectAndIssuerPreferTheCommonName() throws Exception {
        final X509Certificate cert = selfSigned("CN=Just The CN,O=Ignored", false);

        assertEquals("Just The CN", CertificateInfo.subjectOf(cert));
        assertEquals("Just The CN", CertificateInfo.issuerOf(cert));
    }

    @Test
    public void expiredCertificateIsFlagged() throws Exception {
        final Date longAgo = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        final Date yesterday = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        final X509Certificate cert = build("CN=Expired", false, longAgo, yesterday);

        assertTrue(CertificateInfo.describe(cert, "").contains("(EXPIRED)"));
    }

    @Test
    public void logChainEmitsOneFineRecordForTheWholeChain() throws Exception {
        final Certificate[] chain = { selfSigned("CN=Leaf", true), selfSigned("CN=Issuer", false) };
        final List<LogRecord> records = captureFine(() -> CertificateInfo.logChain("Signing certificate chain", chain));

        assertEquals(1, records.size());
        final String message = records.get(0).getMessage();
        assertTrue(message, message.startsWith("Signing certificate chain (2 certificates)"));
        assertTrue(message, message.contains("[0] subject=CN=Leaf"));
        assertTrue(message, message.contains("[1] subject=CN=Issuer"));
    }

    @Test
    public void logChainIgnoresEmptyInput() throws Exception {
        assertTrue(captureFine(() -> CertificateInfo.logChain("Chain", (Certificate[]) null)).isEmpty());
        assertTrue(captureFine(() -> CertificateInfo.logChain("Chain", new Certificate[0])).isEmpty());
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

    private static X509Certificate selfSigned(String dn, boolean withExtensions) throws Exception {
        final Date notBefore = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        final Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);
        return build(dn, withExtensions, notBefore, notAfter);
    }

    private static X509Certificate build(String dn, boolean withExtensions, Date notBefore, Date notAfter)
            throws Exception {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        final KeyPair keyPair = kpg.generateKeyPair();
        final X500Name name = new X500Name(dn);
        final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name,
                BigInteger.valueOf(System.nanoTime()), notBefore, notAfter, name, keyPair.getPublic());
        if (withExtensions) {
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
            builder.addExtension(Extension.authorityInfoAccess, false, new AuthorityInformationAccess(
                    new AccessDescription[] {
                            new AccessDescription(AccessDescription.id_ad_caIssuers, uri(CA_ISSUERS_URL)),
                            new AccessDescription(AccessDescription.id_ad_ocsp, uri(OCSP_URL)) }));
            builder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(new DistributionPoint[] {
                    new DistributionPoint(new DistributionPointName(new GeneralNames(uri(CRL_URL))), null, null) }));
            final ASN1EncodableVector qcStatements = new ASN1EncodableVector();
            qcStatements.add(new QCStatement(ETSIQCObjectIdentifiers.id_etsi_qcs_QcCompliance));
            builder.addExtension(Extension.qCStatements, false, new DERSequence(qcStatements));
        }
        final ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate());
        final X509CertificateHolder holder = builder.build(signer);
        final X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);
        assertNotNull(certificate);
        return certificate;
    }

    private static GeneralName uri(String url) {
        return new GeneralName(GeneralName.uniformResourceIdentifier, url);
    }
}
