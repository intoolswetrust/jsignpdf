package net.sf.jsignpdf.signing.validation;

import java.io.File;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;

public class PdfSignatureValidator {

    private static final Map<String, String> DIGEST_OID_TO_NAME = new HashMap<>();
    static {
        DIGEST_OID_TO_NAME.put("1.3.14.3.2.26", "SHA-1");
        DIGEST_OID_TO_NAME.put("2.16.840.1.101.3.4.2.1", "SHA-256");
        DIGEST_OID_TO_NAME.put("2.16.840.1.101.3.4.2.2", "SHA-384");
        DIGEST_OID_TO_NAME.put("2.16.840.1.101.3.4.2.3", "SHA-512");
        DIGEST_OID_TO_NAME.put("1.3.36.3.2.1", "RIPEMD160");
    }

    public static String digestOidToName(String oid) {
        return DIGEST_OID_TO_NAME.getOrDefault(oid, oid);
    }

    public static class ValidationResult {
        public int signatureCount;
        public String subFilter;
        public int[] byteRange;
        public boolean byteRangeStartsAtZero;
        public boolean byteRangeEndsAtEof;
        public boolean byteRangeHasGap;
        public int cmsSignerCount;
        public String digestAlgorithmOid;
        public int certificateCount;
        public String signerCertificateSubject;
        public boolean signatureValid;
        public String reason;
        public String location;
        public String contactInfo;
        public Calendar signDate;
    }

    public static ValidationResult validate(File signedPdf) throws Exception {
        return validate(signedPdf, 0);
    }

    @SuppressWarnings("unchecked")
    public static ValidationResult validate(File signedPdf, int signatureIndex) throws Exception {
        byte[] fileBytes = Files.readAllBytes(signedPdf.toPath());
        PDDocument doc = PDDocument.load(fileBytes);
        try {
            List<PDSignature> signatures = doc.getSignatureDictionaries();
            ValidationResult result = new ValidationResult();
            result.signatureCount = signatures.size();

            if (signatures.isEmpty() || signatureIndex >= signatures.size()) {
                return result;
            }

            PDSignature sig = signatures.get(signatureIndex);
            result.subFilter = sig.getSubFilter();
            result.reason = sig.getReason();
            result.location = sig.getLocation();
            result.contactInfo = sig.getContactInfo();
            result.signDate = sig.getSignDate();

            int[] byteRange = sig.getByteRange();
            result.byteRange = byteRange;
            if (byteRange != null && byteRange.length == 4) {
                result.byteRangeStartsAtZero = (byteRange[0] == 0);
                result.byteRangeEndsAtEof = (byteRange[2] + byteRange[3] == fileBytes.length);
                result.byteRangeHasGap = (byteRange[1] < byteRange[2]);
            }

            byte[] contents = sig.getContents(fileBytes);
            byte[] signedContent = sig.getSignedContent(fileBytes);

            CMSSignedData cmsData = new CMSSignedData(new CMSProcessableByteArray(signedContent), contents);
            SignerInformationStore signerStore = cmsData.getSignerInfos();
            Collection<SignerInformation> signers = signerStore.getSigners();
            result.cmsSignerCount = signers.size();

            if (signers.isEmpty()) {
                return result;
            }

            SignerInformation signer = signers.iterator().next();
            result.digestAlgorithmOid = signer.getDigestAlgOID();

            Store<X509CertificateHolder> certStore = cmsData.getCertificates();
            Collection<X509CertificateHolder> certs = certStore.getMatches(signer.getSID());
            result.certificateCount = certs.size();

            if (!certs.isEmpty()) {
                X509CertificateHolder certHolder = certs.iterator().next();
                result.signerCertificateSubject = certHolder.getSubject().toString();
                result.signatureValid = signer.verify(
                        new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(certHolder));
            }

            return result;
        } finally {
            doc.close();
        }
    }

    public static int getSignatureCount(File signedPdf) throws Exception {
        PDDocument doc = PDDocument.load(signedPdf);
        try {
            return doc.getSignatureDictionaries().size();
        } finally {
            doc.close();
        }
    }
}
