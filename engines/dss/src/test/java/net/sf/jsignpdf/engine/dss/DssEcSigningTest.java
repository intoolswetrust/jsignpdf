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
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
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
 * Regression coverage for issue #23 on the {@code dss} engine: EC (non-RSA) private keys must sign. DSS
 * derives the algorithm from the signing certificate's key ({@code EncryptionAlgorithm.forKey}), so an EC
 * key needs no special handling - this test pins that.
 */
public class DssEcSigningTest {

    private static final char[] PWD = "ecpass".toCharArray();
    private static final String ALIAS = "ec256";

    /** ecdsa-with-SHA256 - what DSS writes as the SignerInfo signatureAlgorithm. */
    private static final String ECDSA_WITH_SHA256 = "1.2.840.10045.4.3.2";

    private static final EngineConfig EMPTY_CONFIG = new MapEngineConfig(new HashMap<>());

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File inputFile;
    private File outputFile;

    @BeforeClass
    public static void addProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Before
    public void createInputPdf() throws Exception {
        inputFile = tmp.newFile("input.pdf");
        outputFile = new File(tmp.getRoot(), "signed.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.setVersion(1.7f);
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("EC signing test");
                cs.endText();
            }
            doc.save(inputFile);
        }
    }

    @Test
    public void ecKeySignsWithEcdsaSignerInfo() throws Exception {
        File ksFile = createEcKeyStore();

        BasicSignerOptions o = new BasicSignerOptions();
        o.setAdvanced(true);
        o.setHashAlgorithm(HashAlgorithm.SHA256);
        o.setKsType("JKS");
        o.setKsFile(ksFile.getAbsolutePath());
        o.setKsPasswd(PWD);
        o.setKeyAlias(ALIAS);
        o.setKeyPasswd(PWD);
        o.setInFile(inputFile.getAbsolutePath());
        o.setOutFile(outputFile.getAbsolutePath());

        assertTrue("EC signing should succeed on the dss engine",
                new DssSigningEngine().sign(o, EMPTY_CONFIG));
        assertTrue("output PDF should exist", outputFile.isFile());
        assertEquals("SignerInfo signatureAlgorithm OID", ECDSA_WITH_SHA256, signerInfoAlgOid(outputFile));
    }

    /** Reads the SignerInfo signatureAlgorithm OID out of the signed PDF's CMS blob. */
    static String signerInfoAlgOid(File pdf) throws Exception {
        byte[] bytes = Files.readAllBytes(pdf.toPath());
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            List<PDSignature> sigs = doc.getSignatureDictionaries();
            assertTrue("expected a signature dictionary", !sigs.isEmpty());
            CMSSignedData cms = new CMSSignedData(sigs.get(0).getContents(bytes));
            SignerInformation si = cms.getSignerInfos().getSigners().iterator().next();
            return si.getEncryptionAlgOID();
        }
    }

    /** Generates a self-signed P-256 EC signing certificate and stores it in a JKS keystore file. */
    private File createEcKeyStore() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();

        X500Name name = new X500Name("CN=JSignPdf EC Signer, O=JSignPdf Test");
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE, notBefore, notAfter, name, kp.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(kp.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);

        File ksFile = tmp.newFile("ec-keystore.jks");
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry(ALIAS, kp.getPrivate(), PWD, new X509Certificate[] { cert });
        try (FileOutputStream fos = new FileOutputStream(ksFile)) {
            ks.store(fos, PWD);
        }
        return ksFile;
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
