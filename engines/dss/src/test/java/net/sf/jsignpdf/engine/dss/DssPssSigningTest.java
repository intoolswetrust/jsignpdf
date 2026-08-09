package net.sf.jsignpdf.engine.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.types.HashAlgorithm;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * RSASSA-PSS coverage for the {@code dss} engine (issue #255), pinning what each key/certificate shape
 * produces:
 * <ol>
 * <li>PSS key under a PSS-only certificate - the plain #255 case, which DSS already handled: the parameters
 * take PSS from the certificate and the key agrees.</li>
 * <li>Plain RSA key under a PSS-only certificate - the shape a PKCS#11 token presents, where the private key
 * reports {@code "RSA"} while the certificate carries {@code id-RSASSA-PSS}. This used to fail with
 * <em>"The SignatureAlgorithm within the SignatureValue 'RSA_SHA256' does not match the expected value
 * 'RSA_SSA_PSS_SHA256_MGF1'"</em>, because the token derived the algorithm from the key while DSS validated
 * against the certificate.</li>
 * <li>Plain RSA key and certificate - the regression baseline; PKCS#1 v1.5 output must not change.</li>
 * </ol>
 */
public class DssPssSigningTest {

    private static final char[] PWD = "psspass".toCharArray();
    private static final String ALIAS = "signer";

    private static final String ID_RSASSA_PSS = "1.2.840.113549.1.1.10";
    private static final String SHA256_WITH_RSA = "1.2.840.113549.1.1.11";

    private static final EngineConfig EMPTY_CONFIG = new MapEngineConfig(new HashMap<>());

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File inputFile;

    @BeforeClass
    public static void addProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Before
    public void createInputPdf() throws Exception {
        inputFile = tmp.newFile("input.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("PSS signing test");
                cs.endText();
            }
            doc.save(inputFile);
        }
    }

    @Test
    public void pssKeyUnderPssCertificateProducesPssSignature() throws Exception {
        KeyPair kp = KeyPairGenerator.getInstance("RSASSA-PSS").generateKeyPair();
        assertEquals("RSASSA-PSS", kp.getPrivate().getAlgorithm());
        assertEquals(ID_RSASSA_PSS, signAndGetSignerInfoAlgOid("pss-pss", kp.getPrivate(), selfSign(kp, true)));
    }

    @Test
    public void rsaKeyUnderPssCertificateProducesPssSignature() throws Exception {
        KeyPair kp = rsaKeyPair();
        assertEquals("RSA", kp.getPrivate().getAlgorithm());
        assertEquals(ID_RSASSA_PSS, signAndGetSignerInfoAlgOid("rsa-pss", kp.getPrivate(), selfSign(kp, true)));
    }

    @Test
    public void plainRsaKeepsPkcs1Signature() throws Exception {
        KeyPair kp = rsaKeyPair();
        assertEquals(SHA256_WITH_RSA, signAndGetSignerInfoAlgOid("rsa-rsa", kp.getPrivate(), selfSign(kp, false)));
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    /** Signs the input PDF with the given key material and returns the resulting SignerInfo algorithm OID. */
    private String signAndGetSignerInfoAlgOid(String name, PrivateKey key, X509Certificate cert) throws Exception {
        File ksFile = tmp.newFile(name + ".p12");
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(ALIAS, key, PWD, new X509Certificate[] { cert });
        try (FileOutputStream fos = new FileOutputStream(ksFile)) {
            ks.store(fos, PWD);
        }

        File outputFile = new File(tmp.getRoot(), name + "-signed.pdf");
        BasicSignerOptions o = new BasicSignerOptions();
        o.setAdvanced(true);
        o.setHashAlgorithm(HashAlgorithm.SHA256);
        o.setKsType("PKCS12");
        o.setKsFile(ksFile.getAbsolutePath());
        o.setKsPasswd(PWD);
        o.setKeyAlias(ALIAS);
        o.setKeyPasswd(PWD);
        o.setInFile(inputFile.getAbsolutePath());
        o.setOutFile(outputFile.getAbsolutePath());

        assertTrue("signing should succeed for " + name, new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertTrue("output PDF should exist", outputFile.isFile());
        return signerInfoAlgOid(outputFile);
    }

    /** Reads the SignerInfo signatureAlgorithm OID out of the signed PDF's CMS blob. */
    private static String signerInfoAlgOid(File pdf) throws Exception {
        byte[] bytes = Files.readAllBytes(pdf.toPath());
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            List<PDSignature> sigs = doc.getSignatureDictionaries();
            assertTrue("expected a signature dictionary", !sigs.isEmpty());
            CMSSignedData cms = new CMSSignedData(sigs.get(0).getContents(bytes));
            SignerInformation si = cms.getSignerInfos().getSigners().iterator().next();
            return si.getEncryptionAlgOID();
        }
    }

    /**
     * Builds a self-signed certificate for the key pair. When {@code pssOnly} is set the SubjectPublicKeyInfo
     * is re-encoded under {@code id-RSASSA-PSS}, which is the wire marker of a PSS-only certificate whatever
     * the private key itself reports.
     */
    private static X509Certificate selfSign(KeyPair kp, boolean pssOnly) throws Exception {
        X500Name name = new X500Name("CN=JSignPdf PSS Signer, O=JSignPdf Test");
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);

        SubjectPublicKeyInfo encoded = SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded());
        AlgorithmIdentifier keyAlgorithm = pssOnly
                ? new AlgorithmIdentifier(new ASN1ObjectIdentifier(ID_RSASSA_PSS))
                : new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
        SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(keyAlgorithm, encoded.parsePublicKey());

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                name, BigInteger.ONE, notBefore, notAfter, name, spki);
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));

        // The issuer's own algorithm says nothing about the subject key; use whatever this key supports.
        String sigAlg = "RSASSA-PSS".equals(kp.getPrivate().getAlgorithm()) ? "SHA256withRSAandMGF1"
                : "SHA256withRSA";
        ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(kp.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);
    }

    /** Minimal in-memory {@link EngineConfig} backed by a map (keys already engine-relative). */
    private static final class MapEngineConfig implements EngineConfig {
        private final Map<String, String> map;

        MapEngineConfig(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public String getString(String key) {
            return map.get(key);
        }

        @Override
        public String getString(String key, String fallback) {
            return map.getOrDefault(key, fallback);
        }

        @Override
        public boolean getBoolean(String key, boolean fallback) {
            String v = map.get(key);
            return v == null ? fallback : Boolean.parseBoolean(v);
        }

        @Override
        public int getInt(String key, int fallback) {
            String v = map.get(key);
            try {
                return v == null ? fallback : Integer.parseInt(v);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
