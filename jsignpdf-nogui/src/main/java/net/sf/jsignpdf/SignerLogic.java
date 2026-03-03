/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_CERTIFICATE;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_CONTACT;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_LOCATION;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_REASON;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_SIGNER;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_TIMESTAMP;
import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import eu.europa.esig.dss.enumerations.CertificationPermission;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.DSSFont;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import net.sf.jsignpdf.extcsp.CloudFoxy;
import net.sf.jsignpdf.ssl.SSLInitializer;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.FontUtils;
import net.sf.jsignpdf.utils.KeyStoreUtils;
import net.sf.jsignpdf.utils.PrivateKeySignatureToken;

/**
 * Main logic of signer application. It uses DSS PAdES for creating signatures in PDF.
 *
 * @author Josef Cacek
 */
public class SignerLogic implements Runnable {

    private final BasicSignerOptions options;

    /**
     * Constructor with all necessary parameters.
     *
     * @param anOptions options of signer
     */
    public SignerLogic(final BasicSignerOptions anOptions) {
        if (anOptions == null) {
            throw new NullPointerException("Options has to be filled.");
        }
        options = anOptions;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        signFile();
    }

    /**
     * Signs a single file.
     *
     * @return true when signing is finished successfully, false otherwise
     */
    public boolean signFile() {
        final String outFile = options.getEffectiveOutFile();
        if (!validateInOutFiles(options.getInFile(), outFile)) {
            LOGGER.info(RES.get("console.skippingSigning"));
            return false;
        }

        boolean finished = false;
        File encryptedTempFile = null;
        try {
            SSLInitializer.init(options);

            final PrivateKeyInfo pkInfo;
            final PrivateKey key;
            final Certificate[] chain;
            if (StringUtils.equalsIgnoreCase(options.getKsType(), Constants.KEYSTORE_TYPE_CLOUDFOXY)) {
                key = null;
                chain = CloudFoxy.getInstance().getChain(this.options);
                if (chain == null) {
                    return false;
                }
                LOGGER.severe("CloudFoxy is not yet supported with DSS PAdES signing");
                return false;
            } else {
                pkInfo = KeyStoreUtils.getPkInfo(options);
                key = pkInfo.getKey();
                chain = pkInfo.getChain();
            }

            if (ArrayUtils.isEmpty(chain)) {
                LOGGER.info(RES.get("console.certificateChainEmpty"));
                return false;
            }

            // Create DSS token from the existing key + chain
            PrivateKeySignatureToken token = new PrivateKeySignatureToken(key, chain);
            DSSPrivateKeyEntry keyEntry = token.getKeyEntry();

            // Build PAdES signature parameters
            PAdESSignatureParameters parameters = new PAdESSignatureParameters();

            final HashAlgorithm hashAlgorithm = options.getHashAlgorithm();
            DigestAlgorithm digestAlgorithm = hashAlgorithm.toDssDigestAlgorithm();

            parameters.setDigestAlgorithm(digestAlgorithm);
            parameters.setSigningCertificate(keyEntry.getCertificate());
            parameters.setCertificateChain(keyEntry.getCertificateChain());

            // Signature level: BASELINE_T if TSA is configured, otherwise BASELINE_B
            boolean useTsa = options.isTimestamp() && StringUtils.isNotEmpty(options.getTsaUrl());
            parameters.setSignatureLevel(useTsa ? SignatureLevel.PAdES_BASELINE_T : SignatureLevel.PAdES_BASELINE_B);

            // Signing date
            Calendar signingCal = Calendar.getInstance();
            parameters.bLevel().setSigningDate(signingCal.getTime());

            // Metadata
            final String reason = options.getReason();
            if (StringUtils.isNotEmpty(reason)) {
                LOGGER.info(RES.get("console.setReason", reason));
                parameters.setReason(reason);
            }
            final String location = options.getLocation();
            if (StringUtils.isNotEmpty(location)) {
                LOGGER.info(RES.get("console.setLocation", location));
                parameters.setLocation(location);
            }
            final String contact = options.getContact();
            if (StringUtils.isNotEmpty(contact)) {
                LOGGER.info(RES.get("console.setContact", contact));
                parameters.setContactInfo(contact);
            }

            // Certification level
            LOGGER.info(RES.get("console.setCertificationLevel"));
            CertificationPermission permission = options.getCertLevel().toDssCertificationPermission();
            if (permission != null) {
                parameters.setPermission(permission);
            }

            // Password for encrypted PDFs
            String ownerPwd = options.getPdfOwnerPwdStr();
            if (StringUtils.isNotEmpty(ownerPwd)) {
                parameters.setPasswordProtection(ownerPwd.toCharArray());
            }

            // Signature size estimation
            parameters.setContentSize(30000);

            // Encrypt PDF if requested (encrypt-before-sign)
            final PDFEncryption pdfEncryption = options.getPdfEncryption();
            if (pdfEncryption == PDFEncryption.PASSWORD) {
                LOGGER.info(RES.get("console.setEncryption"));
                encryptedTempFile = encryptPdf(options, pdfEncryption);
                if (encryptedTempFile == null) {
                    return false;
                }
                // Set password protection so DSS can read the encrypted file
                String encOwnerPwd = options.getPdfOwnerPwdStr();
                if (StringUtils.isNotEmpty(encOwnerPwd)) {
                    parameters.setPasswordProtection(encOwnerPwd.toCharArray());
                }
            }

            // Load input document (encrypted temp file if password encryption was applied)
            DSSDocument document = new FileDocument(
                    encryptedTempFile != null ? encryptedTempFile.getAbsolutePath() : options.getInFile());

            // Handle visible signature
            if (options.isVisible()) {
                LOGGER.info(RES.get("console.configureVisible"));
                configureVisibleSignature(parameters, chain, signingCal);
            }

            // Create certificate verifier
            CommonCertificateVerifier verifier = new CommonCertificateVerifier();

            // Create PAdES service
            PAdESService service = new PAdESService(verifier);

            // Configure TSA
            if (useTsa) {
                LOGGER.info(RES.get("console.creatingTsaClient"));
                TimestampDataLoader tsDataLoader = new TimestampDataLoader();
                if (options.getTsaServerAuthn() == ServerAuthentication.PASSWORD) {
                    URI tsaUri = URI.create(options.getTsaUrl());
                    tsDataLoader.addAuthentication(tsaUri.getHost(), tsaUri.getPort(), null, options.getTsaUser(),
                            options.getTsaPasswd().toCharArray());
                }
                OnlineTSPSource tspSource = new OnlineTSPSource(options.getTsaUrl(), tsDataLoader);

                final String policyOid = options.getTsaPolicy();
                if (StringUtils.isNotEmpty(policyOid)) {
                    LOGGER.info(RES.get("console.settingTsaPolicy", policyOid));
                    tspSource.setPolicyOid(policyOid);
                }

                service.setTspSource(tspSource);
            }

            LOGGER.info(RES.get("console.processing"));

            // 3-step DSS signing
            LOGGER.info(RES.get("console.createSignature"));
            ToBeSigned dataToSign = service.getDataToSign(document, parameters);
            SignatureValue signatureValue = token.sign(dataToSign, digestAlgorithm, keyEntry);
            DSSDocument signedDocument = service.signDocument(document, parameters, signatureValue);

            // Write output
            LOGGER.info(RES.get("console.createOutPdf", outFile));
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                signedDocument.writeTo(fos);
            }
            LOGGER.info(RES.get("console.closeStream"));

            finished = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, RES.get("console.exception"), e);
        } catch (OutOfMemoryError e) {
            LOGGER.log(Level.SEVERE, RES.get("console.memoryError"), e);
        } finally {
            if (encryptedTempFile != null) {
                encryptedTempFile.delete();
            }
            LOGGER.info(RES.get("console.finished." + (finished ? "ok" : "error")));
        }
        return finished;
    }

    /**
     * Builds an {@link AccessPermission} from the options' rights fields.
     */
    private AccessPermission buildAccessPermission(BasicSignerOptions options) {
        AccessPermission ap = new AccessPermission();
        PrintRight printing = options.getRightPrinting();
        ap.setCanPrint(printing == PrintRight.ALLOW_PRINTING);
        ap.setCanPrintDegraded(printing != PrintRight.DISALLOW_PRINTING);
        ap.setCanExtractContent(options.isRightCopy());
        ap.setCanAssembleDocument(options.isRightAssembly());
        ap.setCanFillInForm(options.isRightFillIn());
        ap.setCanExtractForAccessibility(options.isRightScreanReaders());
        ap.setCanModifyAnnotations(options.isRightModifyAnnotations());
        ap.setCanModify(options.isRightModifyContents());
        return ap;
    }

    /**
     * Encrypts the input PDF with password-based encryption (encrypt-before-sign)
     * and saves the result to a temp file.
     *
     * @return the encrypted temp file, or null if encryption failed
     */
    private File encryptPdf(BasicSignerOptions options, PDFEncryption pdfEncryption) throws Exception {
        File inFile = new File(options.getInFile());

        try (PDDocument doc = PDDocument.load(inFile)) {
            if (!doc.getSignatureDictionaries().isEmpty()) {
                LOGGER.info(RES.get("console.encryptionExistingSignatures"));
                return null;
            }

            AccessPermission ap = buildAccessPermission(options);
            StandardProtectionPolicy passwordPolicy = new StandardProtectionPolicy(
                    options.getPdfOwnerPwdStr(), options.getPdfUserPwdStr(), ap);
            passwordPolicy.setEncryptionKeyLength(128);
            doc.protect(passwordPolicy);

            File tempFile = File.createTempFile("jsignpdf-enc-", ".pdf");
            tempFile.deleteOnExit();
            doc.save(tempFile);
            return tempFile;
        }
    }

    /**
     * Configures visible signature parameters (field position, text, image).
     */
    private void configureVisibleSignature(PAdESSignatureParameters parameters,
            Certificate[] chain, Calendar signingCal) throws Exception {

        SignatureImageParameters imageParams = new SignatureImageParameters();

        // Determine page and page dimensions
        int page = options.getPage();
        float pageWidth;
        float pageHeight;
        try (PDDocument pdDoc = PDDocument.load(new File(options.getInFile()))) {
            int totalPages = pdDoc.getNumberOfPages();
            if (page < 1 || page > totalPages) {
                page = totalPages;
            }
            PDPage pdPage = pdDoc.getPage(page - 1);
            PDRectangle mediaBox = pdPage.getMediaBox();
            int rotation = pdPage.getRotation();
            if (rotation == 90 || rotation == 270) {
                pageWidth = mediaBox.getHeight();
                pageHeight = mediaBox.getWidth();
            } else {
                pageWidth = mediaBox.getWidth();
                pageHeight = mediaBox.getHeight();
            }
        }

        // Field position parameters
        // User provides LLX/LLY/URX/URY in PDF bottom-left coordinate system
        // DSS uses top-left origin
        float llx = fixPosition(options.getPositionLLX(), pageWidth);
        float lly = fixPosition(options.getPositionLLY(), pageHeight);
        float urx = fixPosition(options.getPositionURX(), pageWidth);
        float ury = fixPosition(options.getPositionURY(), pageHeight);
        float width = urx - llx;
        float height = ury - lly;

        SignatureFieldParameters fieldParams = new SignatureFieldParameters();
        fieldParams.setPage(page);
        fieldParams.setOriginX(llx);
        fieldParams.setOriginY(pageHeight - ury); // flip Y axis
        fieldParams.setWidth(width);
        fieldParams.setHeight(height);
        imageParams.setFieldParameters(fieldParams);

        // Build L2 text
        LOGGER.info(RES.get("console.setL2Text"));
        X509Certificate signerCert = (X509Certificate) chain[0];
        String signer = extractCN(signerCert);
        if (StringUtils.isNotEmpty(options.getSignerName())) {
            signer = options.getSignerName();
        }
        final String certificate = signerCert.getSubjectX500Principal().toString();
        final String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(signingCal.getTime());

        String l2text;
        if (options.getL2Text() != null) {
            final Map<String, String> replacements = new HashMap<String, String>();
            replacements.put(L2TEXT_PLACEHOLDER_SIGNER, StringUtils.defaultString(signer));
            replacements.put(L2TEXT_PLACEHOLDER_CERTIFICATE, certificate);
            replacements.put(L2TEXT_PLACEHOLDER_TIMESTAMP, timestamp);
            replacements.put(L2TEXT_PLACEHOLDER_LOCATION, StringUtils.defaultString(location(options)));
            replacements.put(L2TEXT_PLACEHOLDER_REASON, StringUtils.defaultString(reason(options)));
            replacements.put(L2TEXT_PLACEHOLDER_CONTACT, StringUtils.defaultString(options.getContact()));
            l2text = StrSubstitutor.replace(options.getL2Text(), replacements);
        } else {
            final StringBuilder buf = new StringBuilder();
            buf.append(RES.get("default.l2text.signedBy")).append(" ").append(signer).append('\n');
            buf.append(RES.get("default.l2text.date")).append(" ").append(timestamp);
            if (StringUtils.isNotEmpty(reason(options)))
                buf.append('\n').append(RES.get("default.l2text.reason")).append(" ").append(reason(options));
            if (StringUtils.isNotEmpty(location(options)))
                buf.append('\n').append(RES.get("default.l2text.location")).append(" ").append(location(options));
            l2text = buf.toString();
        }

        SignatureImageTextParameters textParams = new SignatureImageTextParameters();
        textParams.setText(l2text);

        DSSFont font = FontUtils.getL2BaseFont();
        if (font != null) {
            font.setSize(options.getL2TextFontSize());
            textParams.setFont(font);
        }
        imageParams.setTextParameters(textParams);

        // Background image
        final String bgImgPath = options.getBgImgPath();
        if (bgImgPath != null) {
            LOGGER.info(RES.get("console.createImage", bgImgPath));
            LOGGER.info(RES.get("console.setImage"));
            imageParams.setImage(new FileDocument(bgImgPath));
        }

        LOGGER.info(RES.get("console.setVisibleSignature"));
        parameters.setImageParameters(imageParams);
    }

    private static String reason(BasicSignerOptions options) {
        return options.getReason();
    }

    private static String location(BasicSignerOptions options) {
        return options.getLocation();
    }

    /**
     * Extracts the CN (Common Name) from a certificate's subject DN.
     */
    private String extractCN(X509Certificate cert) {
        try {
            String dn = cert.getSubjectX500Principal().getName();
            LdapName ldapName = new LdapName(dn);
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return cert.getSubjectX500Principal().toString();
    }

    private float fixPosition(float origPos, float base) {
        return origPos >= 0 ? origPos : base + origPos;
    }

    /**
     * Validates if input and output files are valid for signing.
     *
     * @param inFile input file
     * @param outFile output file
     * @return true if valid, false otherwise
     */
    private boolean validateInOutFiles(final String inFile, final String outFile) {
        LOGGER.info(RES.get("console.validatingFiles"));
        if (StringUtils.isEmpty(inFile) || StringUtils.isEmpty(outFile)) {
            LOGGER.info(RES.get("console.fileNotFilled.error"));
            return false;
        }
        final File tmpInFile = new File(inFile);
        final File tmpOutFile = new File(outFile);
        if (!(tmpInFile.exists() && tmpInFile.isFile() && tmpInFile.canRead())) {
            LOGGER.info(RES.get("console.inFileNotFound.error"));
            return false;
        }
        if (tmpInFile.getAbsolutePath().equals(tmpOutFile.getAbsolutePath())) {
            LOGGER.info(RES.get("console.filesAreEqual.error"));
            return false;
        }
        return true;
    }

}
