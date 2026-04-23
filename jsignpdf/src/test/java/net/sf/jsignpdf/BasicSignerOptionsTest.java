package net.sf.jsignpdf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.net.Proxy;

import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.PropertyProvider;
import net.sf.jsignpdf.utils.PropertyStoreFactory;
import org.junit.Test;

/**
 * Tests for {@link BasicSignerOptions#createCopy()}.
 */
public class BasicSignerOptionsTest {

    @Test
    public void createCopyShouldProduceEqualInstance() {
        BasicSignerOptions original = new BasicSignerOptions();

        // String fields
        original.setKsType("PKCS12");
        original.setKsFile("/path/to/keystore");
        original.setKsPasswd("ksPass".toCharArray());
        original.setKeyAlias("myAlias");
        original.setKeyIndex(3);
        original.setKeyPasswd("keyPass".toCharArray());
        original.setInFile("/input.pdf");
        original.setOutFile("/output.pdf");
        original.setSignerName("Test Signer");
        original.setReason("Testing");
        original.setLocation("Prague");
        original.setContact("test@example.com");
        original.setAppend(true);
        original.setAdvanced(true);

        // Encryption
        original.setPdfEncryption(PDFEncryption.PASSWORD);
        original.setPdfOwnerPwd("ownerPwd".toCharArray());
        original.setPdfUserPwd("userPwd".toCharArray());
        original.setPdfEncryptionCertFile("/cert.pem");
        original.setCertLevel(CertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED);
        original.setHashAlgorithm(HashAlgorithm.SHA256);
        original.setStorePasswords(true);

        // Rights
        original.setRightPrinting(PrintRight.ALLOW_DEGRADED_PRINTING);
        original.setRightCopy(true);
        original.setRightAssembly(true);
        original.setRightFillIn(true);
        original.setRightScreanReaders(true);
        original.setRightModifyAnnotations(true);
        original.setRightModifyContents(true);

        // Visible signature
        original.setVisible(true);
        original.setPage(5);
        original.setPositionLLX(10.0f);
        original.setPositionLLY(20.0f);
        original.setPositionURX(200.0f);
        original.setPositionURY(300.0f);
        original.setBgImgScale(1.5f);
        original.setRenderMode(RenderMode.GRAPHIC_AND_DESCRIPTION);
        original.setL2Text("Layer 2 text");
        original.setL4Text("Layer 4 text");
        original.setL2TextFontSize(14.0f);
        original.setImgPath("/img.png");
        original.setBgImgPath("/bg.png");
        original.setAcro6Layers(true);

        // TSA
        original.setTimestamp(true);
        original.setTsaUrl("http://tsa.example.com");
        original.setTsaServerAuthn(ServerAuthentication.CERTIFICATE);
        original.setTsaUser("tsaUser");
        original.setTsaPasswd("tsaPass");
        original.setTsaCertFileType("PKCS12");
        original.setTsaCertFile("/tsa-cert.p12");
        original.setTsaCertFilePwd("tsaCertPwd");
        original.setTsaPolicy("1.2.3.4");
        original.setTsaHashAlg("SHA-256");

        // OCSP / CRL
        original.setOcspEnabled(true);
        original.setOcspServerUrl("http://ocsp.example.com");
        original.setCrlEnabled(true);

        // Proxy
        original.setProxyType(Proxy.Type.HTTP);
        original.setProxyHost("proxy.example.com");
        original.setProxyPort(8080);

        // --- Create copy ---
        BasicSignerOptions copy = original.createCopy();

        // Must be a different object
        assertNotSame(original, copy);

        // String fields
        assertEquals(original.getKsType(), copy.getKsType());
        assertEquals(original.getKsFile(), copy.getKsFile());
        assertArrayEquals(original.getKsPasswd(), copy.getKsPasswd());
        assertEquals(original.getKeyAlias(), copy.getKeyAlias());
        assertEquals(original.getKeyIndex(), copy.getKeyIndex());
        assertArrayEquals(original.getKeyPasswd(), copy.getKeyPasswd());
        assertEquals(original.getInFile(), copy.getInFile());
        assertEquals(original.getOutFile(), copy.getOutFile());
        assertEquals(original.getSignerName(), copy.getSignerName());
        assertEquals(original.getReason(), copy.getReason());
        assertEquals(original.getLocation(), copy.getLocation());
        assertEquals(original.getContact(), copy.getContact());
        assertEquals(original.isAppend(), copy.isAppend());
        assertEquals(original.isAdvanced(), copy.isAdvanced());

        // Encryption
        assertEquals(original.getPdfEncryption(), copy.getPdfEncryption());
        assertArrayEquals(original.getPdfOwnerPwd(), copy.getPdfOwnerPwd());
        assertArrayEquals(original.getPdfUserPwd(), copy.getPdfUserPwd());
        assertEquals(original.getPdfEncryptionCertFile(), copy.getPdfEncryptionCertFile());
        assertEquals(original.getCertLevel(), copy.getCertLevel());
        assertEquals(original.getHashAlgorithm(), copy.getHashAlgorithm());
        assertEquals(original.isStorePasswords(), copy.isStorePasswords());

        // Rights
        assertEquals(original.getRightPrinting(), copy.getRightPrinting());
        assertEquals(original.isRightCopy(), copy.isRightCopy());
        assertEquals(original.isRightAssembly(), copy.isRightAssembly());
        assertEquals(original.isRightFillIn(), copy.isRightFillIn());
        assertEquals(original.isRightScreanReaders(), copy.isRightScreanReaders());
        assertEquals(original.isRightModifyAnnotations(), copy.isRightModifyAnnotations());
        assertEquals(original.isRightModifyContents(), copy.isRightModifyContents());

        // Visible signature
        assertEquals(original.isVisible(), copy.isVisible());
        assertEquals(original.getPage(), copy.getPage());
        assertEquals(original.getPositionLLX(), copy.getPositionLLX(), 0.0f);
        assertEquals(original.getPositionLLY(), copy.getPositionLLY(), 0.0f);
        assertEquals(original.getPositionURX(), copy.getPositionURX(), 0.0f);
        assertEquals(original.getPositionURY(), copy.getPositionURY(), 0.0f);
        assertEquals(original.getBgImgScale(), copy.getBgImgScale(), 0.0f);
        assertEquals(original.getRenderMode(), copy.getRenderMode());
        assertEquals(original.getL2Text(), copy.getL2Text());
        assertEquals(original.getL4Text(), copy.getL4Text());
        assertEquals(original.getL2TextFontSize(), copy.getL2TextFontSize(), 0.0f);
        assertEquals(original.getImgPath(), copy.getImgPath());
        assertEquals(original.getBgImgPath(), copy.getBgImgPath());
        assertEquals(original.isAcro6Layers(), copy.isAcro6Layers());

        // TSA
        assertEquals(original.isTimestamp(), copy.isTimestamp());
        assertEquals(original.getTsaUrl(), copy.getTsaUrl());
        assertEquals(original.getTsaServerAuthn(), copy.getTsaServerAuthn());
        assertEquals(original.getTsaUser(), copy.getTsaUser());
        assertEquals(original.getTsaPasswd(), copy.getTsaPasswd());
        assertEquals(original.getTsaCertFileType(), copy.getTsaCertFileType());
        assertEquals(original.getTsaCertFile(), copy.getTsaCertFile());
        assertEquals(original.getTsaCertFilePwd(), copy.getTsaCertFilePwd());
        assertEquals(original.getTsaPolicy(), copy.getTsaPolicy());
        assertEquals(original.getTsaHashAlg(), copy.getTsaHashAlg());

        // OCSP / CRL
        assertEquals(original.isOcspEnabled(), copy.isOcspEnabled());
        assertEquals(original.getOcspServerUrl(), copy.getOcspServerUrl());
        assertEquals(original.isCrlEnabled(), copy.isCrlEnabled());

        // Proxy
        assertEquals(original.getProxyType(), copy.getProxyType());
        assertEquals(original.getProxyHost(), copy.getProxyHost());
        assertEquals(original.getProxyPort(), copy.getProxyPort());
    }

    /**
     * Both field defaults must be true so the JavaFX UI shows Store passwords
     * and Append signature as checked on a fresh install.
     */
    @Test
    public void defaultFieldValues_appendAndStorePasswordsTrue() {
        BasicSignerOptions opts = new BasicSignerOptions();
        assertTrue("append field defaults to DEFVAL_APPEND", opts.isAppend());
        assertTrue("storePasswords field defaults to DEFVAL_STOREPWD", opts.isStorePasswords());
    }

    /**
     * When loadOptions() runs against an empty properties store (fresh install
     * or after "Reset Settings"), the missing keys must fall back to the
     * DEFVAL_* constants rather than silently becoming false.
     */
    @Test
    public void loadOptions_noStoredProps_appendAndStorePasswordsDefaultTrue() {
        PropertyProvider mainConfig = PropertyStoreFactory.getInstance().mainConfig();
        mainConfig.clear();
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.loadOptions();
        assertTrue("append should default to true when no property is stored", opts.isAppend());
        assertTrue("storePasswords should default to true when no property is stored",
                opts.isStorePasswords());
    }

    /**
     * Explicit false values persisted in the properties must still be honoured
     * — the new defaults only apply when the key is absent.
     */
    @Test
    public void loadOptions_storedFalseValuesArePreserved() {
        PropertyProvider mainConfig = PropertyStoreFactory.getInstance().mainConfig();
        mainConfig.clear();
        mainConfig.setProperty(Constants.PROPERTY_APPEND, false);
        mainConfig.setProperty(Constants.PROPERTY_STOREPWD, false);
        try {
            BasicSignerOptions opts = new BasicSignerOptions();
            opts.loadOptions();
            assertEquals("append must reflect stored false", false, opts.isAppend());
            assertEquals("storePasswords must reflect stored false", false, opts.isStorePasswords());
        } finally {
            mainConfig.clear();
        }
    }

    @Test
    public void createCopyShouldDefensivelyCopyCharArrays() {
        BasicSignerOptions original = new BasicSignerOptions();
        original.setKsPasswd("secret".toCharArray());
        original.setKeyPasswd("key".toCharArray());
        original.setPdfOwnerPwd("owner".toCharArray());
        original.setPdfUserPwd("user".toCharArray());

        BasicSignerOptions copy = original.createCopy();

        // Arrays should have equal content but be different instances
        assertArrayEquals(original.getKsPasswd(), copy.getKsPasswd());
        assertNotSame(original.getKsPasswd(), copy.getKsPasswd());

        assertArrayEquals(original.getKeyPasswd(), copy.getKeyPasswd());
        assertNotSame(original.getKeyPasswd(), copy.getKeyPasswd());

        assertArrayEquals(original.getPdfOwnerPwd(), copy.getPdfOwnerPwd());
        assertNotSame(original.getPdfOwnerPwd(), copy.getPdfOwnerPwd());

        assertArrayEquals(original.getPdfUserPwd(), copy.getPdfUserPwd());
        assertNotSame(original.getPdfUserPwd(), copy.getPdfUserPwd());
    }
}
