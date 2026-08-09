package net.sf.jsignpdf.engine.openpdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.openpdf.text.Document;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.AcroFields;
import org.openpdf.text.pdf.PdfArray;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfPKCS7;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Regression coverage for issue #23 on the {@code openpdf} engine: EC (non-RSA) private keys must sign.
 * The iText-2.1-era {@code PdfPKCS7} JSignPdf carried before the OpenPDF 3 migration rejected anything that
 * was not an {@code RSAPrivate(Crt)Key}; OpenPDF 3 derives the algorithm from the key instead.
 */
public class OpenPdfEcSigningTest {

    private static final char[] PWD = "ecpass".toCharArray();
    private static final String ALIAS = "ec256";

    /** id-ecPublicKey - what OpenPDF's {@code PdfPKCS7} writes as the SignerInfo signatureAlgorithm. */
    private static final String ID_EC_PUBLIC_KEY = "1.2.840.10045.2.1";

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
        try (FileOutputStream fos = new FileOutputStream(inputFile)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();
            document.add(new Paragraph("EC signing test"));
            document.close();
        }
    }

    @Test
    public void ecKeySignsAndVerifies() throws Exception {
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

        assertTrue("EC signing should succeed on the openpdf engine",
                new OpenPdfSigningEngine().sign(o, EMPTY_CONFIG));
        assertTrue("output PDF should exist", outputFile.isFile());

        PdfReader reader = new PdfReader(outputFile.getAbsolutePath());
        try {
            AcroFields fields = reader.getAcroFields();
            List<String> names = fields.getSignedFieldNames();
            assertEquals("expected exactly one signature", 1, names.size());
            PdfPKCS7 pkcs7 = fields.verifySignature(names.get(0));
            assertNotNull(pkcs7);
            assertTrue("signature must verify", pkcs7.verify());
            assertEquals("EC", pkcs7.getSigningCertificate().getPublicKey().getAlgorithm());
        } finally {
            reader.close();
        }

        assertEquals("SignerInfo signatureAlgorithm OID", ID_EC_PUBLIC_KEY, signerInfoAlgOid(outputFile));
    }

    /** Reads the SignerInfo signatureAlgorithm OID out of the signed PDF's CMS blob. */
    private static String signerInfoAlgOid(File pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf.getAbsolutePath());
        try {
            AcroFields fields = reader.getAcroFields();
            String name = fields.getSignedFieldNames().get(0);
            byte[] pdfBytes = Files.readAllBytes(pdf.toPath());
            PdfArray range = fields.getSignatureDictionary(name).getAsArray(PdfName.BYTERANGE);
            // /Contents sits between the two ByteRange gaps, hex-encoded and wrapped in < >
            int start = range.getAsNumber(1).intValue() + 1;
            int end = range.getAsNumber(2).intValue() - 1;
            byte[] hex = new byte[end - start];
            System.arraycopy(pdfBytes, start, hex, 0, hex.length);
            byte[] der = hexToBytes(new String(hex, "ISO-8859-1").trim());
            CMSSignedData cms = new CMSSignedData(der);
            SignerInformation si = cms.getSignerInfos().getSigners().iterator().next();
            return si.getEncryptionAlgOID();
        } finally {
            reader.close();
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length() / 2 * 2;
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
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
