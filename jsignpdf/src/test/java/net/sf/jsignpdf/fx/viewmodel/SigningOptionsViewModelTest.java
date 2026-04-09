package net.sf.jsignpdf.fx.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PDFEncryption;

/**
 * Tests for {@link SigningOptionsViewModel}, verifying that the advanced flag
 * is correctly set and that all advanced-gated options are properly synced.
 */
public class SigningOptionsViewModelTest {

    /**
     * syncToOptions must set advanced=true so that the advanced-gated getters
     * in BasicSignerOptions return the configured values instead of defaults.
     */
    @Test
    public void testSyncToOptions_setsAdvancedTrue() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        BasicSignerOptions opts = new BasicSignerOptions();
        assertFalse("advanced should be false by default", opts.isAdvanced());

        vm.syncToOptions(opts);
        assertTrue("syncToOptions must set advanced=true", opts.isAdvanced());
    }

    @Test
    public void testSyncToOptions_pdfEncryptionPreserved() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        vm.pdfEncryptionProperty().set(PDFEncryption.PASSWORD);
        vm.pdfOwnerPasswordProperty().set("owner123");
        vm.pdfUserPasswordProperty().set("user456");

        BasicSignerOptions opts = new BasicSignerOptions();
        vm.syncToOptions(opts);

        assertEquals(PDFEncryption.PASSWORD, opts.getPdfEncryption());
        assertEquals("owner123", opts.getPdfOwnerPwdStr());
        assertEquals("owner123", opts.getPdfOwnerPwdStrX());
        assertEquals("user456", opts.getPdfUserPwdStr());
    }

    @Test
    public void testSyncToOptions_encryptionWithEmptyUserPassword() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        vm.pdfEncryptionProperty().set(PDFEncryption.PASSWORD);
        vm.pdfOwnerPasswordProperty().set("owner-only");
        vm.pdfUserPasswordProperty().set("");

        BasicSignerOptions opts = new BasicSignerOptions();
        vm.syncToOptions(opts);

        assertTrue("advanced must be true for encryption to work", opts.isAdvanced());
        assertEquals(PDFEncryption.PASSWORD, opts.getPdfEncryption());
        assertEquals("owner-only", opts.getPdfOwnerPwdStrX());
    }

    @Test
    public void testSyncToOptions_certLevelPreserved() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        vm.certLevelProperty().set(CertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED);

        BasicSignerOptions opts = new BasicSignerOptions();
        vm.syncToOptions(opts);

        assertEquals("getCertLevelX must return the configured value when advanced=true",
                CertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED, opts.getCertLevelX());
    }

    @Test
    public void testSyncToOptions_hashAlgorithmPreserved() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        vm.hashAlgorithmProperty().set(HashAlgorithm.SHA512);

        BasicSignerOptions opts = new BasicSignerOptions();
        vm.syncToOptions(opts);

        assertEquals("getHashAlgorithmX must return the configured value when advanced=true",
                HashAlgorithm.SHA512, opts.getHashAlgorithmX());
    }

    @Test
    public void testSyncToOptions_timestampPreserved() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        vm.tsaEnabledProperty().set(true);
        vm.tsaUrlProperty().set("http://tsa.example.com");

        BasicSignerOptions opts = new BasicSignerOptions();
        vm.syncToOptions(opts);

        assertTrue("isTimestampX must return true when advanced=true and timestamp enabled",
                opts.isTimestampX());
    }

    @Test
    public void testSyncToOptions_ocspPreserved() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        vm.ocspEnabledProperty().set(true);
        vm.ocspServerUrlProperty().set("http://ocsp.example.com");

        BasicSignerOptions opts = new BasicSignerOptions();
        vm.syncToOptions(opts);

        assertTrue("isOcspEnabledX must return true when advanced=true",
                opts.isOcspEnabledX());
    }

    @Test
    public void testSyncToOptions_crlPreserved() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        vm.crlEnabledProperty().set(true);

        BasicSignerOptions opts = new BasicSignerOptions();
        vm.syncToOptions(opts);

        assertTrue("isCrlEnabledX must return true when advanced=true",
                opts.isCrlEnabledX());
    }

    /**
     * Verifies that without the fix (advanced=false), the advanced-gated
     * getters would return defaults. This documents the contract.
     */
    @Test
    public void testAdvancedFalse_gettersReturnDefaults() {
        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setAdvanced(false);
        opts.setPdfOwnerPwd("secret".toCharArray());
        opts.setPdfEncryption(PDFEncryption.PASSWORD);
        opts.setCertLevel(CertificationLevel.CERTIFIED_NO_CHANGES_ALLOWED);
        opts.setHashAlgorithm(HashAlgorithm.SHA512);
        opts.setTimestamp(true);
        opts.setOcspEnabled(true);
        opts.setCrlEnabled(true);

        assertEquals("getPdfOwnerPwdStrX should be empty when advanced=false",
                "", opts.getPdfOwnerPwdStrX());
        assertEquals("getCertLevelX should return NOT_CERTIFIED when advanced=false",
                CertificationLevel.NOT_CERTIFIED, opts.getCertLevelX());
        assertEquals("getHashAlgorithmX should return default when advanced=false",
                Constants.DEFVAL_HASH_ALGORITHM, opts.getHashAlgorithmX());
        assertFalse("isTimestampX should be false when advanced=false",
                opts.isTimestampX());
        assertFalse("isOcspEnabledX should be false when advanced=false",
                opts.isOcspEnabledX());
        assertFalse("isCrlEnabledX should be false when advanced=false",
                opts.isCrlEnabledX());
    }

    @Test
    public void testSyncRoundTrip() {
        SigningOptionsViewModel vm = new SigningOptionsViewModel();
        vm.pdfEncryptionProperty().set(PDFEncryption.PASSWORD);
        vm.pdfOwnerPasswordProperty().set("owner");
        vm.pdfUserPasswordProperty().set("user");
        vm.certLevelProperty().set(CertificationLevel.CERTIFIED_FORM_FILLING);
        vm.hashAlgorithmProperty().set(HashAlgorithm.SHA384);
        vm.tsaEnabledProperty().set(true);

        BasicSignerOptions opts = new BasicSignerOptions();
        vm.syncToOptions(opts);

        SigningOptionsViewModel vm2 = new SigningOptionsViewModel();
        vm2.syncFromOptions(opts);

        assertEquals(PDFEncryption.PASSWORD, vm2.pdfEncryptionProperty().get());
        assertEquals("owner", vm2.pdfOwnerPasswordProperty().get());
        assertEquals("user", vm2.pdfUserPasswordProperty().get());
        assertEquals(CertificationLevel.CERTIFIED_FORM_FILLING, vm2.certLevelProperty().get());
        assertEquals(HashAlgorithm.SHA384, vm2.hashAlgorithmProperty().get());
        assertTrue(vm2.tsaEnabledProperty().get());
    }
}
