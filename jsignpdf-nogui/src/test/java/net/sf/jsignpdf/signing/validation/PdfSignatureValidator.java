package net.sf.jsignpdf.signing.validation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.util.Store;

/**
 * Validates PDF signatures independently from OpenPDF by using Apache PDFBox to extract
 * signature dictionaries and BouncyCastle CMS to parse and cryptographically verify the
 * PKCS#7 containers.
 */
public class PdfSignatureValidator {

    private static final Map<String, String> DIGEST_OID_TO_NAME = new HashMap<>();
    static {
        DIGEST_OID_TO_NAME.put("1.3.14.3.2.26", "SHA-1");
        DIGEST_OID_TO_NAME.put("2.16.840.1.101.3.4.2.1", "SHA-256");
        DIGEST_OID_TO_NAME.put("2.16.840.1.101.3.4.2.2", "SHA-384");
        DIGEST_OID_TO_NAME.put("2.16.840.1.101.3.4.2.3", "SHA-512");
        DIGEST_OID_TO_NAME.put("1.3.36.3.2.1", "RIPEMD160");
    }

    /**
     * Maps a digest algorithm OID to its human-readable name (e.g. "2.16.840.1.101.3.4.2.1" to "SHA-256").
     */
    public static String digestOidToName(String oid) {
        return DIGEST_OID_TO_NAME.getOrDefault(oid, oid);
    }

    /**
     * Holds all properties extracted from a PDF signature for test assertions.
     */
    public static class ValidationResult {
        // CMS / cryptographic properties
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
        // Signature dictionary metadata
        public String reason;
        public String location;
        public String contactInfo;
        public Calendar signDate;
        // Timestamp properties
        public boolean hasTimestamp;
        public String timestampDigestAlgorithmOid;
        public String timestampPolicyOid;
        public Date timestampDate;
        // Visual / widget annotation properties
        public boolean hasVisibleRect;
        public int signaturePage = -1;
        public float rectLLX;
        public float rectLLY;
        public float rectURX;
        public float rectURY;
        public String appearanceText;
    }

    /**
     * Validates the first signature in the given signed PDF.
     */
    public static ValidationResult validate(File signedPdf) throws Exception {
        return validate(signedPdf, 0);
    }

    /**
     * Validates the signature at the given index. Extracts ByteRange, parses the CMS/PKCS#7
     * container, verifies cryptographic integrity, extracts visual properties from the widget
     * annotation, and populates a {@link ValidationResult}.
     */
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

            // Extract timestamp token from CMS unsigned attributes
            extractTimestampInfo(signer, result);

            // Extract visual signature properties from the widget annotation
            extractWidgetProperties(doc, sig, result);

            return result;
        } finally {
            doc.close();
        }
    }

    /**
     * Extracts timestamp token information from the CMS signer's unsigned attributes.
     * RFC 3161 timestamps are embedded as {@code id-smime-aa-timeStampToken} attributes.
     */
    private static void extractTimestampInfo(SignerInformation signer, ValidationResult result) {
        try {
            if (signer.getUnsignedAttributes() == null) {
                return;
            }
            org.bouncycastle.asn1.cms.AttributeTable unsignedAttrs = signer.getUnsignedAttributes();
            org.bouncycastle.asn1.cms.Attribute tsAttr = unsignedAttrs.get(
                    org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if (tsAttr == null) {
                return;
            }
            byte[] tokenBytes = tsAttr.getAttrValues().getObjectAt(0).toASN1Primitive().getEncoded();
            TimeStampToken tsToken = new TimeStampToken(
                    new org.bouncycastle.cms.CMSSignedData(tokenBytes));
            TimeStampTokenInfo tsInfo = tsToken.getTimeStampInfo();
            result.hasTimestamp = true;
            result.timestampDigestAlgorithmOid = tsInfo.getHashAlgorithm().getAlgorithm().getId();
            result.timestampPolicyOid = tsInfo.getPolicy().getId();
            result.timestampDate = tsInfo.getGenTime();
        } catch (Exception e) {
            // timestamp extraction failed, leave hasTimestamp as false
        }
    }

    /**
     * Finds the {@link PDSignatureField} matching the given {@link PDSignature} and extracts
     * widget annotation properties (rectangle, page, appearance text).
     */
    private static void extractWidgetProperties(PDDocument doc, PDSignature sig, ValidationResult result)
            throws IOException {
        PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            return;
        }
        for (PDField field : acroForm.getFields()) {
            if (!(field instanceof PDSignatureField)) {
                continue;
            }
            PDSignatureField sigField = (PDSignatureField) field;
            PDSignature fieldSig = sigField.getSignature();
            if (fieldSig == null || fieldSig.getCOSObject() != sig.getCOSObject()) {
                continue;
            }
            List<PDAnnotationWidget> widgets = sigField.getWidgets();
            if (widgets.isEmpty()) {
                return;
            }
            PDAnnotationWidget widget = widgets.get(0);

            // Rectangle
            PDRectangle rect = widget.getRectangle();
            if (rect != null) {
                result.rectLLX = rect.getLowerLeftX();
                result.rectLLY = rect.getLowerLeftY();
                result.rectURX = rect.getUpperRightX();
                result.rectURY = rect.getUpperRightY();
                result.hasVisibleRect = rect.getWidth() > 0 && rect.getHeight() > 0;
            }

            // Page (iterate document pages to find the one containing this widget)
            result.signaturePage = findWidgetPage(doc, widget);

            // Appearance text (recursively extracted from the normal appearance stream)
            PDAppearanceDictionary appearance = widget.getAppearance();
            if (appearance != null) {
                PDAppearanceEntry normalEntry = appearance.getNormalAppearance();
                if (normalEntry != null && normalEntry.isStream()) {
                    result.appearanceText = extractText(normalEntry.getAppearanceStream());
                }
            }
            return;
        }
    }

    /**
     * Finds the 1-based page number that contains the given widget annotation,
     * or -1 if not found.
     */
    private static int findWidgetPage(PDDocument doc, PDAnnotationWidget widget) throws IOException {
        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            PDPage page = doc.getPage(i);
            for (PDAnnotation annot : page.getAnnotations()) {
                if (annot.getCOSObject() == widget.getCOSObject()) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    /**
     * Recursively extracts all text from a PDF form XObject (appearance stream) by parsing
     * its content stream for text-showing operators (Tj, TJ, ', ") and descending into
     * nested form XObjects. Uses font encoding to properly decode character codes to Unicode.
     */
    private static String extractText(PDFormXObject formXObject) throws IOException {
        StringBuilder text = new StringBuilder();
        PDFStreamParser parser = new PDFStreamParser(formXObject);
        parser.parse();
        List<Object> tokens = parser.getTokens();
        PDResources resources = formXObject.getResources();
        PDFont currentFont = null;

        for (int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if (!(token instanceof Operator)) {
                continue;
            }
            String opName = ((Operator) token).getName();
            if ("Tf".equals(opName) && i >= 2) {
                // /FontName fontSize Tf
                Object fontNameObj = tokens.get(i - 2);
                if (fontNameObj instanceof COSName && resources != null) {
                    currentFont = resources.getFont((COSName) fontNameObj);
                }
            } else if (("Tj".equals(opName) || "'".equals(opName) || "\"".equals(opName)) && i > 0) {
                Object prev = tokens.get(i - 1);
                if (prev instanceof COSString) {
                    text.append(decodeString((COSString) prev, currentFont));
                }
            } else if ("TJ".equals(opName) && i > 0) {
                Object prev = tokens.get(i - 1);
                if (prev instanceof COSArray) {
                    COSArray array = (COSArray) prev;
                    for (int j = 0; j < array.size(); j++) {
                        COSBase item = array.get(j);
                        if (item instanceof COSString) {
                            text.append(decodeString((COSString) item, currentFont));
                        }
                    }
                }
            }
        }

        // Recurse into nested form XObjects (e.g. iText signature layers n0..n4)
        if (resources != null) {
            for (COSName name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDFormXObject) {
                    text.append(extractText((PDFormXObject) xObject));
                }
            }
        }

        return text.toString();
    }

    /**
     * Decodes a COSString using the given font's encoding. For simple fonts (Type 1, TrueType),
     * each byte is a character code decoded via {@link PDFont#toUnicode(int)}. For composite
     * fonts (Type 0/CID), variable-length codes are read via {@link PDFont#readCode(InputStream)}.
     * Falls back to raw string value if no font is available.
     */
    private static String decodeString(COSString cosString, PDFont font) throws IOException {
        if (font == null) {
            return cosString.getString();
        }
        byte[] bytes = cosString.getBytes();
        StringBuilder sb = new StringBuilder();
        if (font instanceof PDSimpleFont) {
            for (byte b : bytes) {
                int code = b & 0xFF;
                String unicode = font.toUnicode(code);
                sb.append(unicode != null ? unicode : String.valueOf((char) code));
            }
        } else {
            InputStream input = new ByteArrayInputStream(bytes);
            while (input.available() > 0) {
                int code = font.readCode(input);
                String unicode = font.toUnicode(code);
                if (unicode != null) {
                    sb.append(unicode);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Returns the number of signature dictionaries in the given PDF.
     */
    public static int getSignatureCount(File signedPdf) throws Exception {
        PDDocument doc = PDDocument.load(signedPdf);
        try {
            return doc.getSignatureDictionaries().size();
        } finally {
            doc.close();
        }
    }
}
