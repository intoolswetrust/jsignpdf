package net.sf.jsignpdf.engine.openpdf;

import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_CONTACT;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_LOCATION;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_REASON;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_SIGNER;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_TIMESTAMP;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_CERTIFICATE;
import static net.sf.jsignpdf.Constants.RES;
import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Proxy;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.PrivateKeyInfo;
import net.sf.jsignpdf.crl.CRLInfo;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.extcsp.CloudFoxy;
import net.sf.jsignpdf.ssl.SSLInitializer;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PdfVersion;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.KeyStoreUtils;
import net.sf.jsignpdf.utils.PKCS11Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.AcroFields;
import org.openpdf.text.pdf.OcspClientBouncyCastle;
import org.openpdf.text.pdf.PdfDate;
import org.openpdf.text.pdf.PdfDictionary;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfPKCS7;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfSignature;
import org.openpdf.text.pdf.PdfSignatureAppearance;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.PdfString;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.TSAClientBouncyCastle;

/**
 * The default JSignPdf signing engine. It uses OpenPDF (iText 2.1 lineage) to create the signature
 * in the PDF &mdash; this is the exact code path JSignPdf used before the engine abstraction was
 * introduced, so its output is unchanged.
 *
 * @author Josef Cacek
 */
public class OpenPdfSigningEngine implements SigningEngine {

    /** Stable identifier used in config files and CLI args. */
    public static final String ID = "openpdf";

    private static final Set<Capability> CAPABILITIES = Set.copyOf(EnumSet.of(
            Capability.SUBFILTER_ADBE_PKCS7_DETACHED,
            Capability.HASH_SHA1, Capability.HASH_SHA256, Capability.HASH_SHA384, Capability.HASH_SHA512,
            Capability.HASH_RIPEMD160,
            Capability.APPEND_MODE, Capability.CERTIFICATION_LEVEL,
            Capability.ENCRYPTION_PASSWORD, Capability.ENCRYPTION_CERTIFICATE, Capability.PERMISSIONS_BITMASK,
            Capability.VISIBLE_SIGNATURE, Capability.VISIBLE_LAYER2_TEXT, Capability.VISIBLE_LAYER4_TEXT,
            Capability.VISIBLE_RENDER_MODE_DESCRIPTION_ONLY, Capability.VISIBLE_RENDER_MODE_GRAPHIC_AND_DESCRIPTION,
            Capability.VISIBLE_RENDER_MODE_NAME_AND_DESCRIPTION, Capability.VISIBLE_BACKGROUND_IMAGE,
            Capability.VISIBLE_BACKGROUND_IMAGE_SCALE, Capability.VISIBLE_SIGNATURE_GRAPHIC, Capability.VISIBLE_CUSTOM_FONT,
            Capability.ACRO6_LAYERS,
            Capability.TSA, Capability.TSA_POLICY_OID, Capability.TSA_BASIC_AUTH, Capability.OCSP_EMBED,
            Capability.CRL_EMBED,
            Capability.PROXY_SUPPORT,
            Capability.EXTERNAL_DIGEST, Capability.PKCS11_PROVIDER));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "OpenPDF";
    }

    @Override
    public Set<Capability> capabilities() {
        return CAPABILITIES;
    }

    /**
     * Signs the file described by options. The caller (dispatcher) is responsible for input/output
     * file validation, engine-capability validation and firing the finished-event lifecycle; this
     * method performs the actual OpenPDF signing and returns whether it succeeded.
     */
    @Override
    public boolean sign(final BasicSignerOptions options, final EngineConfig engineConfig) {
        final String outFile = options.getOutFileX();
        boolean finished = false;
        FileOutputStream fout = null;
        try {
            SSLInitializer.init(options);

            final PrivateKeyInfo pkInfo;
            final PrivateKey key;
            final Certificate[] chain;
            // the 'cloudfoxy' crypto provider computes signatures externally and there are
            // no
            // certificates or keys available via Java CSPs -> they have to be pulled from
            // an
            // external source in 2 steps: 1. certificate chain, 2. signature itself
            if (StringUtils.equalsIgnoreCase(options.getKsType(), Constants.KEYSTORE_TYPE_CLOUDFOXY)) {
                key = null;
                chain = CloudFoxy.getInstance().getChain(options);
                if (chain == null) {
                    return false;
                }
            } else {
                pkInfo = KeyStoreUtils.getPkInfo(options);
                if (pkInfo == null) {
                    LOGGER.info(RES.get("console.certificateChainEmpty"));
                    return false;
                }
                key = pkInfo.getKey();
                chain = pkInfo.getChain();
            }

            if (ArrayUtils.isEmpty(chain)) {
                // the certificate was not found
                LOGGER.info(RES.get("console.certificateChainEmpty"));
                return false;
            }
            LOGGER.info(RES.get("console.createPdfReader", options.getInFile()));
            PdfReader reader;
            try {
                reader = new PdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
            } catch (Exception e) {
                try {
                    reader = new PdfReader(options.getInFile(), new byte[0]);
                } catch (Exception e2) {
                    // try to read without password
                    reader = new PdfReader(options.getInFile());
                }
            }

            LOGGER.info(RES.get("console.createOutPdf", outFile));
            fout = new FileOutputStream(outFile);

            final HashAlgorithm hashAlgorithm = options.getHashAlgorithmX();

            LOGGER.info(RES.get("console.createSignature"));
            String tmpPdfVersion = null; // default version - the same as input
            String inputPdfVersion = reader.getPdfVersion();
            String requiredPdfVersionForGivenHash = hashAlgorithm.getPdfVersion().getStringVersion();
            if (inputPdfVersion != null && inputPdfVersion.compareTo(requiredPdfVersionForGivenHash) < 0) {
                // this covers also problems with visible signatures (embedded
                // fonts) in PDF 1.2, because the minimal version
                // for hash algorithms is 1.3 (for SHA1)
                if (options.isAppendX()) {
                    // if we are in append mode and version should be updated
                    // then return false (not possible)
                    LOGGER.info(RES.get("console.updateVersionNotPossibleInAppendModeForGivenHash",
                            hashAlgorithm.getAlgorithmName(), hashAlgorithm.getPdfVersion().getVersionName(),
                            PdfVersion.fromStringVersion(inputPdfVersion).getVersionName(),
                            HashAlgorithm.valuesWithPdfVersionAsString()));
                    return false;
                }
                tmpPdfVersion = requiredPdfVersionForGivenHash;
                LOGGER.info(RES.get("console.updateVersion",
                        new String[] { inputPdfVersion, tmpPdfVersion }));
            }

            final PdfStamper stp = PdfStamper.createSignature(reader, fout, tmpPdfVersion, null, options.isAppendX());
            if (!options.isAppendX()) {
                // we are not in append mode, let's remove existing signatures
                // (otherwise we're getting to troubles)
                final AcroFields acroFields = stp.getAcroFields();
                @SuppressWarnings("unchecked")
                final List<String> sigNames = acroFields.getSignedFieldNames();
                for (String sigName : sigNames) {
                    acroFields.removeField(sigName);
                }
            }
            if (options.isAdvanced() && options.getPdfEncryption() != PDFEncryption.NONE) {
                LOGGER.info(RES.get("console.setEncryption"));
                final int tmpRight = options.getRightPrinting().getRight() | (options.isRightCopy() ? PdfWriter.ALLOW_COPY : 0)
                        | (options.isRightAssembly() ? PdfWriter.ALLOW_ASSEMBLY : 0)
                        | (options.isRightFillIn() ? PdfWriter.ALLOW_FILL_IN : 0)
                        | (options.isRightScreanReaders() ? PdfWriter.ALLOW_SCREENREADERS : 0)
                        | (options.isRightModifyAnnotations() ? PdfWriter.ALLOW_MODIFY_ANNOTATIONS : 0)
                        | (options.isRightModifyContents() ? PdfWriter.ALLOW_MODIFY_CONTENTS : 0);
                switch (options.getPdfEncryption()) {
                    case PASSWORD:
                        if (StringUtils.isEmpty(options.getPdfOwnerPwdStrX())) {
                            LOGGER.severe(RES.get("console.pdfEncError.missingOwnerPassword"));
                            return false;
                        }
                        if (StringUtils.isEmpty(options.getPdfUserPwdStr())) {
                            LOGGER.severe(RES.get("console.pdfEncError.missingUserPassword"));
                            return false;
                        }
                        stp.setEncryption(true, options.getPdfUserPwdStr(), options.getPdfOwnerPwdStrX(), tmpRight);
                        break;
                    case CERTIFICATE:
                        final X509Certificate encCert = KeyStoreUtils.loadCertificate(options.getPdfEncryptionCertFile());
                        if (encCert == null) {
                            LOGGER.severe(RES.get("console.pdfEncError.wrongCertificateFile",
                                    StringUtils.defaultString(options.getPdfEncryptionCertFile())));
                            return false;
                        }
                        if (!KeyStoreUtils.isEncryptionSupported(encCert)) {
                            LOGGER.severe(RES.get("console.pdfEncError.cantUseCertificate", encCert.getSubjectDN().getName()));
                            return false;
                        }
                        stp.setEncryption(new Certificate[] { encCert }, new int[] { tmpRight }, PdfWriter.ENCRYPTION_AES_128);
                        break;
                    default:
                        LOGGER.severe(RES.get("console.unsupportedEncryptionType"));
                        return false;
                }
            }

            final PdfSignatureAppearance sap = stp.getSignatureAppearance();
            sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
            final String reason = options.getReason();
            if (StringUtils.isNotEmpty(reason)) {
                LOGGER.info(RES.get("console.setReason", reason));
                sap.setReason(reason);
            }
            final String location = options.getLocation();
            if (StringUtils.isNotEmpty(location)) {
                LOGGER.info(RES.get("console.setLocation", location));
                sap.setLocation(location);
            }
            final String contact = options.getContact();
            if (StringUtils.isNotEmpty(contact)) {
                LOGGER.info(RES.get("console.setContact", contact));
                sap.setContact(contact);
            }
            LOGGER.info(RES.get("console.setCertificationLevel"));
            sap.setCertificationLevel(options.getCertLevelX().getLevel());

            if (options.isVisible()) {
                // visible signature is enabled
                LOGGER.info(RES.get("console.configureVisible"));
                LOGGER.info(RES.get("console.setAcro6Layers", Boolean.toString(options.isAcro6Layers())));
                sap.setAcro6Layers(options.isAcro6Layers());

                final String tmpImgPath = options.getImgPath();
                if (tmpImgPath != null) {
                    LOGGER.info(RES.get("console.createImage", tmpImgPath));
                    final Image img = Image.getInstance(tmpImgPath);
                    LOGGER.info(RES.get("console.setSignatureGraphic"));
                    sap.setSignatureGraphic(img);
                }
                final String tmpBgImgPath = options.getBgImgPath();
                if (tmpBgImgPath != null) {
                    LOGGER.info(RES.get("console.createImage", tmpBgImgPath));
                    final Image img = Image.getInstance(tmpBgImgPath);
                    LOGGER.info(RES.get("console.setImage"));
                    sap.setImage(img);
                }
                LOGGER.info(RES.get("console.setImageScale"));
                sap.setImageScale(options.getBgImgScale());
                LOGGER.info(RES.get("console.setL2Text"));
                String signer = PdfPKCS7.getSubjectFields((X509Certificate) chain[0]).getField("CN");
                if (StringUtils.isNotEmpty(options.getSignerName())) {
                    signer = options.getSignerName();
                }
                final String certificate = PdfPKCS7.getSubjectFields((X509Certificate) chain[0]).toString();
                final String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(sap.getSignDateNullSafe().getTime());
                if (options.getL2Text() == null) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append(RES.get("default.l2text.signedBy")).append(" ").append(signer).append('\n');
                    buf.append(RES.get("default.l2text.date")).append(" ").append(timestamp);
                    if (StringUtils.isNotEmpty(reason))
                        buf.append('\n').append(RES.get("default.l2text.reason")).append(" ").append(reason);
                    if (StringUtils.isNotEmpty(location))
                        buf.append('\n').append(RES.get("default.l2text.location")).append(" ").append(location);
                    sap.setLayer2Text(buf.toString());
                } else {
                    final Map<String, String> replacements = new HashMap<String, String>();
                    replacements.put(L2TEXT_PLACEHOLDER_SIGNER, StringUtils.defaultString(signer));
                    replacements.put(L2TEXT_PLACEHOLDER_CERTIFICATE, certificate);
                    replacements.put(L2TEXT_PLACEHOLDER_TIMESTAMP, timestamp);
                    replacements.put(L2TEXT_PLACEHOLDER_LOCATION, StringUtils.defaultString(location));
                    replacements.put(L2TEXT_PLACEHOLDER_REASON, StringUtils.defaultString(reason));
                    replacements.put(L2TEXT_PLACEHOLDER_CONTACT, StringUtils.defaultString(contact));
                    final String l2text = StrSubstitutor.replace(options.getL2Text(), replacements);
                    sap.setLayer2Text(l2text);
                }
                final org.openpdf.text.pdf.BaseFont l2BaseFont = OpenPdfFonts.getL2BaseFont();
                if (l2BaseFont != null) {
                    sap.setLayer2Font(new Font(l2BaseFont, options.getL2TextFontSize()));
                }
                LOGGER.info(RES.get("console.setL4Text"));
                sap.setLayer4Text(options.getL4Text());
                LOGGER.info(RES.get("console.setRender"));
                RenderMode renderMode = options.getRenderMode();
                if (renderMode == RenderMode.GRAPHIC_AND_DESCRIPTION && sap.getSignatureGraphic() == null) {
                    LOGGER.warning(
                            "Render mode of visible signature is set to GRAPHIC_AND_DESCRIPTION, but no image is loaded. Fallback to DESCRIPTION_ONLY.");
                    LOGGER.info(RES.get("console.renderModeFallback"));
                    renderMode = RenderMode.DESCRIPTION_ONLY;
                }
                sap.setRender(renderMode.getRender());
                LOGGER.info(RES.get("console.setVisibleSignature"));
                int page = options.getPage();
                if (page < 1 || page > reader.getNumberOfPages()) {
                    page = reader.getNumberOfPages();
                }
                Rectangle signitureRect = computeSignatureRectangle(reader.getPageSize(page), options);
                sap.setVisibleSignature(signitureRect, page, null);
            }

            LOGGER.info(RES.get("console.processing"));
            final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, new PdfName("adbe.pkcs7.detached"));
            if (!StringUtils.isEmpty(reason)) {
                dic.setReason(sap.getReason());
            }
            if (!StringUtils.isEmpty(location)) {
                dic.setLocation(sap.getLocation());
            }
            if (!StringUtils.isEmpty(contact)) {
                dic.setContact(sap.getContact());
            }
            dic.setDate(new PdfDate(sap.getSignDateNullSafe()));
            sap.setCryptoDictionary(dic);

            final Proxy tmpProxy = options.createProxy();

            final CRLInfo crlInfo = new CRLInfo(options, chain);

            // CRLs are stored twice in PDF c.f.
            // PdfPKCS7.getAuthenticatedAttributeBytes
            final int contentEstimated = (int) (Constants.DEFVAL_SIG_SIZE + 2L * crlInfo.getByteCount());
            final Map<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
            exc.put(PdfName.CONTENTS, new Integer(contentEstimated * 2 + 2));
            sap.preClose(exc);

            String provider = PKCS11Utils.getProviderNameForKeystoreType(options.getKsType());
            PdfPKCS7 sgn = new PdfPKCS7(key, chain, crlInfo.getCrls(), hashAlgorithm.getAlgorithmName(), provider, false);
            InputStream data = sap.getRangeStream();
            final MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm.getAlgorithmName());
            byte buf[] = new byte[8192];
            int n;
            while ((n = data.read(buf)) > 0) {
                messageDigest.update(buf, 0, n);
            }
            byte hash[] = messageDigest.digest();
            Calendar cal = Calendar.getInstance();
            byte[] ocsp = null;
            if (options.isOcspEnabledX() && chain.length >= 2) {
                LOGGER.info(RES.get("console.getOCSPURL"));
                String url = PdfPKCS7.getOCSPURL((X509Certificate) chain[0]);
                if (StringUtils.isEmpty(url)) {
                    // get from options
                    LOGGER.info(RES.get("console.noOCSPURL"));
                    url = options.getOcspServerUrl();
                }
                if (!StringUtils.isEmpty(url)) {
                    LOGGER.info(RES.get("console.readingOCSP", url));
                    final OcspClientBouncyCastle ocspClient = new OcspClientBouncyCastle((X509Certificate) chain[0],
                            (X509Certificate) chain[1], url);
                    ocspClient.setProxy(tmpProxy);
                    ocsp = ocspClient.getEncoded();
                }
            }
            byte sh[] = sgn.getAuthenticatedAttributeBytes(hash, cal, ocsp);

            // THIS IS THE SIGNING, we need to have a new branch for external signers
            if (StringUtils.equalsIgnoreCase(options.getKsType(), Constants.KEYSTORE_TYPE_CLOUDFOXY)) {
                byte[] signature = CloudFoxy.getInstance().getSignature(options, sh);
                if (signature == null) {
                    return false;
                } else {
                    sgn.setExternalDigest(signature, null, "RSA");
                }
            } else {
                sgn.update(sh, 0, sh.length);
            }

            TSAClientBouncyCastle tsc = null;
            if (options.isTimestampX() && !StringUtils.isEmpty(options.getTsaUrl())) {
                LOGGER.info(RES.get("console.creatingTsaClient"));
                if (options.getTsaServerAuthn() == ServerAuthentication.PASSWORD) {
                    tsc = new TSAClientBouncyCastle(options.getTsaUrl(), StringUtils.defaultString(options.getTsaUser()),
                            StringUtils.defaultString(options.getTsaPasswd()));
                } else {
                    tsc = new TSAClientBouncyCastle(options.getTsaUrl());

                }
                final String tsaHashAlg = options.getTsaHashAlgWithFallback();
                LOGGER.info(RES.get("console.settingTsaHashAlg", tsaHashAlg));
                tsc.setDigestName(tsaHashAlg);
                tsc.setProxy(tmpProxy);
                final String policyOid = options.getTsaPolicy();
                if (StringUtils.isNotEmpty(policyOid)) {
                    LOGGER.info(RES.get("console.settingTsaPolicy", policyOid));
                    tsc.setPolicy(policyOid);
                }
            }
            byte[] encodedSig = sgn.getEncodedPKCS7(hash, cal, tsc, ocsp);

            if (contentEstimated + 2 < encodedSig.length) {
                System.err.println("SigSize - contentEstimated=" + contentEstimated + ", sigLen=" + encodedSig.length);
                throw new Exception("Not enough space");
            }

            byte[] paddedSig = new byte[contentEstimated];
            System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);

            PdfDictionary dic2 = new PdfDictionary();
            dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));
            LOGGER.info(RES.get("console.closeStream"));
            sap.close(dic2);
            fout.close();
            fout = null;
            finished = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, RES.get("console.exception"), e);
        } catch (OutOfMemoryError e) {
            LOGGER.log(Level.SEVERE, RES.get("console.memoryError"), e);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return finished;
    }

    private Rectangle computeSignatureRectangle(Rectangle pageRect, BasicSignerOptions options) {
        float pgWidth = pageRect.getWidth();
        float pgHeighth = pageRect.getHeight();
        return new Rectangle(fixPosition(options.getPositionLLX(), pgWidth), fixPosition(options.getPositionLLY(), pgHeighth),
                fixPosition(options.getPositionURX(), pgWidth), fixPosition(options.getPositionURY(), pgHeighth));
    }

    private float fixPosition(float origPos, float base) {
        return origPos >= 0 ? origPos : base + origPos;
    }
}
