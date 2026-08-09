package net.sf.jsignpdf.engine.dss;

import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_CONTACT;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_LOCATION;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_REASON;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_SIGNER;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_TIMESTAMP;
import static net.sf.jsignpdf.Constants.L2TEXT_PLACEHOLDER_CERTIFICATE;
import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.PrivateKeyInfo;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.types.BufferingMode;
import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PadesLevel;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.AppConfig;
import net.sf.jsignpdf.utils.KeyStoreUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessStreamCache.StreamCacheCreateFunction;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import eu.europa.esig.dss.alert.LogOnStatusAlert;
import eu.europa.esig.dss.enumerations.CertificationPermission;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.ImageScaling;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.TextWrapping;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.DSSFont;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pdf.PdfMemoryUsageSetting;
import eu.europa.esig.dss.pdf.PdfSignatureFieldPositionChecker;
import eu.europa.esig.dss.signature.resources.TempFileResourcesHandlerBuilder;
import eu.europa.esig.dss.service.SecureRandomNonceSource;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.http.proxy.ProxyProperties;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;

import net.sf.jsignpdf.engine.dss.pdfbox.JSignPdfPdfObjFactory;
import net.sf.jsignpdf.engine.dss.pdfbox.JSignPdfSignatureImageParameters;

/**
 * The EU-DSS-based signing engine. It produces PAdES (ETSI.CAdES.detached) signatures at the baseline
 * levels B / T / LT / LTA, the levels the OpenPDF engine cannot create. The signing flow is lifted from
 * the standalone {@code jsignpdf-pades} project and adapted to read JSignPdf's {@link BasicSignerOptions}
 * model, obtain key material through JSignPdf's shared {@link KeyStoreUtils#getPkInfo(BasicSignerOptions)},
 * and read DSS-specific trust knobs from the engine-scoped {@link EngineConfig}.
 *
 * @author Josef Cacek
 */
public class DssSigningEngine implements SigningEngine {

    /** Stable identifier used in config files and CLI args. */
    public static final String ID = "dss";

    /**
     * Config key ({@code engine.dss.contentSize}): explicit number of bytes to reserve in the PDF
     * {@code /Contents} for the CMS signature. A positive value overrides the automatic estimate; {@code 0}
     * (the default / absent) means the size is estimated from the certificate chain and signing options.
     */
    static final String KEY_CONTENT_SIZE = "contentSize";

    /**
     * Config key ({@code engine.dss.retryOnUndersize}): when {@code true} (the default), the signature is
     * re-created with a larger reserved {@code /Contents} if DSS reports the reserved size was too small.
     * The retry repeats the signing operation (and, for timestamped levels, fetches a fresh TSA token).
     */
    static final String KEY_RETRY_ON_UNDERSIZE = "retryOnUndersize";

    /**
     * Config key ({@code engine.dss.relaxFieldOverlap}): when {@code true}, a visible signature rectangle
     * that overlaps an existing PDF annotation logs a warning instead of throwing an error. Defaults to
     * {@code false} — DSS's strict behaviour is safer and only users who knowingly sign over annotations
     * should enable this.
     */
    static final String KEY_RELAX_FIELD_OVERLAP = "relaxFieldOverlap";

    /**
     * Config key ({@code engine.dss.tsa.nonce}): when {@code true} (the default), a random nonce is sent in
     * the RFC 3161 timestamp request and the TSA is required to echo it back, which protects against replayed
     * timestamp responses. Set to {@code false} only for a TSA that rejects or mishandles nonced requests.
     * The openpdf engine always sends a nonce and ignores this key.
     */
    static final String KEY_TSA_NONCE = "tsa.nonce";

    /** Lower bound for the reserved {@code /Contents} size, matching DSS's own default; never estimate below it. */
    private static final int MIN_CONTENT_SIZE = 9472;

    /** Per-certificate fallback used only when a chain certificate cannot be DER-encoded for measurement. */
    private static final int CERT_SIZE_FALLBACK = 2048;

    /**
     * Headroom added on top of the certificate-chain bytes for the signer info, signed attributes, the
     * signature value (comfortably covers RSA-4096) and the ASN.1 framing of the CMS SignedData.
     */
    private static final int CMS_OVERHEAD = 4096;

    /**
     * Extra space reserved when a signature timestamp is embedded (PAdES level T and above). The timestamp
     * token carries its own TSA certificate chain and is by far the largest single variable in {@code /Contents}.
     */
    private static final int TSA_ALLOWANCE = 16384;

    /** Slack added on top of the exact size DSS reports when growing after an undersize failure. */
    private static final int RETRY_MARGIN = 2048;

    /** Cap on undersize retries; DSS reports the exact required size, so a single retry normally suffices. */
    private static final int MAX_CONTENT_SIZE_RETRIES = 3;

    /**
     * Per-document spill threshold used in {@link BufferingMode#TEMP}: documents below this stay in memory,
     * larger ones spill to disk. Keeps the common case fast when a user leaves temp mode enabled
     * permanently. This is a per-document budget, not a global one, and the disk side is unrestricted.
     */
    private static final long MIXED_THRESHOLD_BYTES = 64L * 1024 * 1024;

    /** Extracts the actual CMS length from the DSS "signature size too small" message; see {@code assertContentSizeSufficient}. */
    private static final Pattern UNDERSIZE_LENGTH_PATTERN = Pattern.compile("with a length \\[(\\d+)\\]");

    private static final Set<Capability> CAPABILITIES = Set.copyOf(EnumSet.of(
            Capability.SUBFILTER_ETSI_CADES_DETACHED,
            Capability.PADES_BASELINE_B, Capability.PADES_BASELINE_T,
            Capability.PADES_BASELINE_LT, Capability.PADES_BASELINE_LTA,
            Capability.DSS_DICTIONARY,

            Capability.HASH_SHA256, Capability.HASH_SHA384, Capability.HASH_SHA512,

            // No OVERWRITE_MODE: DSS always signs incrementally (PAdES requires it). Append is universal.
            Capability.CERTIFICATION_LEVEL,
            Capability.ENCRYPTION_PASSWORD, Capability.PERMISSIONS_BITMASK,

            Capability.VISIBLE_SIGNATURE, Capability.VISIBLE_LAYER2_TEXT,
            Capability.VISIBLE_BACKGROUND_IMAGE, Capability.VISIBLE_BACKGROUND_IMAGE_SCALE,
            Capability.VISIBLE_SIGNATURE_GRAPHIC,
            Capability.VISIBLE_CUSTOM_FONT,
            Capability.VISIBLE_RENDER_MODE_DESCRIPTION_ONLY,
            Capability.VISIBLE_RENDER_MODE_GRAPHIC_AND_DESCRIPTION,
            Capability.SIGN_EXISTING_FIELD,

            Capability.TSA, Capability.TSA_POLICY_OID, Capability.TSA_BASIC_AUTH,
            Capability.OCSP_EMBED, Capability.CRL_EMBED,

            Capability.PROXY_SUPPORT,
            Capability.PKCS11_PROVIDER));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "EU DSS (PAdES)";
    }

    @Override
    public Set<Capability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public boolean sign(final BasicSignerOptions options, final EngineConfig engineConfig) {
        final String outFile = options.getOutFileX();
        boolean finished = false;
        File encryptedTempFile = null;
        TempFileResourcesHandlerBuilder resourcesHandlerBuilder = null;
        // Hoisted out of the try so the untrusted-chain handler can name the offending certificates: the signer
        // chain, and the timestamp chain captured by the wrapping TSP source (issue #448).
        Certificate[] chain = null;
        CapturingTspSource tspSource = null;
        try {
            // Resolved up front so an unusable buffering.tempDir aborts before any work is done. The
            // directory is only read in TEMP mode, so a stale path cannot break a memory-mode sign.
            final BufferingMode bufferingMode = AppConfig.bufferingMode();
            final File bufferingTempDir;
            if (bufferingMode == BufferingMode.TEMP) {
                try {
                    bufferingTempDir = AppConfig.bufferingTempDir();
                } catch (IOException e) {
                    LOGGER.severe(e.getMessage());
                    return false;
                }
                LOGGER.info(RES.get("console.buffering.temp", bufferingTempDir != null
                        ? bufferingTempDir.getAbsolutePath() : System.getProperty("java.io.tmpdir")));
            } else {
                bufferingTempDir = null;
            }

            final PrivateKeyInfo pkInfo = KeyStoreUtils.getPkInfo(options);
            if (pkInfo == null) {
                LOGGER.info(RES.get("console.certificateChainEmpty"));
                return false;
            }
            final PrivateKey key = pkInfo.getKey();
            chain = pkInfo.getChain();
            if (ArrayUtils.isEmpty(chain)) {
                LOGGER.info(RES.get("console.certificateChainEmpty"));
                return false;
            }

            final HashAlgorithm hashAlgorithm = options.getHashAlgorithmX();
            final DigestAlgorithm digestAlgorithm = DssMappings.toDigestAlgorithm(hashAlgorithm);
            if (digestAlgorithm == null) {
                // SHA-1 / RIPEMD-160 are not valid PAdES digests. The default (SHA-256) maps fine, so
                // reaching here means a non-PAdES digest was explicitly selected in advanced mode.
                // EngineMismatchValidator normally rejects this earlier; fail fast here too rather than
                // silently substituting a digest.
                LOGGER.severe(RES.get("console.dss.unsupportedHash", hashAlgorithm.getAlgorithmName()));
                return false;
            }

            try (PrivateKeySignatureToken token = new PrivateKeySignatureToken(key, chain, pkInfo.getProvider())) {
                final PAdESSignatureParameters parameters = new PAdESSignatureParameters();
                parameters.setDigestAlgorithm(digestAlgorithm);
                parameters.setSigningCertificate(token.getKeyEntry().getCertificate());
                parameters.setCertificateChain(token.getKeyEntry().getCertificateChain());

                // PAdES level (+ TSA auto-upgrade B->T).
                final boolean useTsa = options.isTimestampX() && StringUtils.isNotEmpty(options.getTsaUrl());
                final PadesLevel padesLevel = options.getPadesLevel();
                if (useTsa && (padesLevel == null || padesLevel == PadesLevel.BASELINE_B)) {
                    LOGGER.info(RES.get("console.dss.tsaUpgrade"));
                    parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_T);
                } else {
                    parameters.setSignatureLevel(DssMappings.toSignatureLevel(padesLevel));
                }

                final Calendar signingCal = Calendar.getInstance();
                parameters.bLevel().setSigningDate(signingCal.getTime());

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

                // Certification level (DocMDP).
                LOGGER.info(RES.get("console.setCertificationLevel"));
                final CertificationLevel certLevel = options.getCertLevelX();
                final CertificationPermission permission = DssMappings.toCertificationPermission(certLevel);
                if (permission != null) {
                    parameters.setPermission(permission);
                }

                // Owner password to open an encrypted input PDF.
                final char[] ownerPwd = options.getPdfOwnerPwd();
                if (ownerPwd != null && ownerPwd.length > 0) {
                    parameters.setPasswordProtection(ownerPwd);
                }

                // Encrypt-before-sign (password-only).
                File effectiveInFile = new File(options.getInFile());
                if (options.isAdvanced() && options.getPdfEncryption() == net.sf.jsignpdf.types.PDFEncryption.PASSWORD) {
                    LOGGER.info(RES.get("console.setEncryption"));
                    encryptedTempFile = encryptPdf(effectiveInFile, options,
                            streamCache(bufferingMode, bufferingTempDir), bufferingTempDir);
                    if (encryptedTempFile == null) {
                        return false;
                    }
                    effectiveInFile = encryptedTempFile;
                    // DSS must open the temp with the password encryptPdf just used (not necessarily the
                    // input-decrypt password set above). PDFBox treats an empty owner password as "owner =
                    // user password", so when the owner password is empty fall back to the user password;
                    // otherwise DSS cannot decrypt the temp at all (the owner-empty / user-password-only case).
                    final String encOwnerPwd = options.getPdfOwnerPwdStrX();
                    final String openPwd = StringUtils.isNotEmpty(encOwnerPwd) ? encOwnerPwd : options.getPdfUserPwdStr();
                    if (StringUtils.isNotEmpty(openPwd)) {
                        parameters.setPasswordProtection(openPwd.toCharArray());
                    }
                }

                final DSSDocument document = new FileDocument(effectiveInFile);

                if (options.isVisible()) {
                    LOGGER.info(RES.get("console.configureVisible"));
                    configureVisibleSignature(parameters, options, chain, signingCal, effectiveInFile,
                            streamCache(bufferingMode, bufferingTempDir));
                }

                // Certificate verifier + trust material (LT/LTA).
                final DssTrustConfigurer trustConfigurer = new DssTrustConfigurer(engineConfig);
                final boolean ltOrLta = parameters.getSignatureLevel() == SignatureLevel.PAdES_BASELINE_LT
                        || parameters.getSignatureLevel() == SignatureLevel.PAdES_BASELINE_LTA;
                if (ltOrLta) {
                    // LT/LTA build on a signature timestamp (level T); without a TSA, DSS would fail deep in
                    // signDocument(). Fail fast here with a clear message instead.
                    if (!useTsa) {
                        LOGGER.severe(RES.get("console.dss.ltNoTsa"));
                        return false;
                    }
                    if (!trustConfigurer.isOnlineEnabled()) {
                        LOGGER.severe(RES.get("console.dss.ltNoRevocation"));
                        return false;
                    }
                }
                final ProxyConfig proxyConfig = buildProxyConfig(options);
                final CommonCertificateVerifier verifier;
                try {
                    verifier = trustConfigurer.buildVerifier(proxyConfig);
                } catch (Exception e) {
                    // A configured trust source could not be loaded (bad truststore path/password, unreadable
                    // cert file/URL, ...). Fail fast with a clear message rather than signing without the
                    // intended trust anchors and surfacing an opaque DSS error later.
                    LOGGER.log(Level.SEVERE, RES.get("console.dss.trustConfigFailed"), e);
                    return false;
                }
                final PAdESService service = new PAdESService(verifier);

                // Use custom PDF object factory with background-image layering
                JSignPdfPdfObjFactory pdfObjFactory = new JSignPdfPdfObjFactory();

                // Opt-in overlap relaxation: when enabled, a visible signature that overlaps
                // an existing PDF annotation logs a warning instead of throwing an error.
                if (engineConfig.getBoolean(KEY_RELAX_FIELD_OVERLAP, false)) {
                    PdfSignatureFieldPositionChecker positionChecker = new PdfSignatureFieldPositionChecker();
                    positionChecker.setAlertOnSignatureFieldOverlap(new LogOnStatusAlert());
                    pdfObjFactory.setPdfSignatureFieldPositionChecker(positionChecker);
                }

                if (bufferingMode == BufferingMode.TEMP) {
                    // Kept local on purpose: clear() deletes every temp file the builder ever created, so a
                    // shared instance would let one sign delete another concurrent sign's output document.
                    resourcesHandlerBuilder = new TempFileResourcesHandlerBuilder();
                    resourcesHandlerBuilder.setFileNamePrefix("jsignpdf-dss-");
                    if (bufferingTempDir != null) {
                        resourcesHandlerBuilder.setTempFileDirectory(bufferingTempDir);
                    }
                    pdfObjFactory.setResourcesHandlerBuilder(resourcesHandlerBuilder);
                    // Note: this one cannot honour buffering.tempDir. DSS's PdfMemoryUsageSetting carries no
                    // directory and PdfBoxUtils.getMemoryUsageSetting never calls MemoryUsageSetting
                    // .setTempDir, so DSS's internal PDFBox scratch always lands in java.io.tmpdir. Users who
                    // need it elsewhere have to point java.io.tmpdir there; see JSignPdf.adoc.
                    pdfObjFactory.setPdfMemoryUsageSetting(PdfMemoryUsageSetting.mixed(MIXED_THRESHOLD_BYTES));
                }

                service.setPdfObjFactory(pdfObjFactory);

                if (useTsa) {
                    LOGGER.info(RES.get("console.creatingTsaClient"));
                    // Wrap the TSA source so the timestamp chain is captured for diagnostics: if DSS later
                    // rejects the signature because that chain is not anchored, the untrusted-chain report can
                    // name the timestamp certificate instead of a bare fingerprint (issue #448).
                    tspSource = new CapturingTspSource(options.getTsaUrl(),
                            buildTspSource(options, parameters, digestAlgorithm, proxyConfig, engineConfig));
                    service.setTspSource(tspSource);
                }

                LOGGER.info(RES.get("console.processing"));
                LOGGER.info(RES.get("console.createSignature"));
                final int configuredContentSize = engineConfig.getInt(KEY_CONTENT_SIZE, 0);
                final int initialContentSize = configuredContentSize > 0
                        ? configuredContentSize
                        : estimateContentSize(chain, useTsa);
                final boolean retryOnUndersize = engineConfig.getBoolean(KEY_RETRY_ON_UNDERSIZE, true);
                final DSSDocument signedDocument = signWithContentSize(service, document, parameters, token,
                        initialContentSize, retryOnUndersize, resourcesHandlerBuilder);

                LOGGER.info(RES.get("console.createOutPdf", outFile));
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    signedDocument.writeTo(fos);
                }
                LOGGER.info(RES.get("console.closeStream"));
            }
            finished = true;
        } catch (eu.europa.esig.dss.alert.exception.AlertException e) {
            // LT/LTA: DSS refused because revocation data could not be collected for the signer or timestamp
            // chain — it is not anchored by the configured trust material (its CA is not in the truststore /
            // cert files / LOTL, or an MRA LOTL needs engine.dss.trust.lotlMraSupport=true). Surface that
            // instead of an opaque stack trace, and name the offending certificate(s) rather than leaving the
            // user to map a C-<fingerprint> back to a CA (issue #448).
            final Collection<X509Certificate> tsaChain = tspSource != null
                    ? tspSource.getCapturedCertificates() : null;
            final String details = DssUntrustedChainReporter.describe(e, chain, tsaChain);
            String message = RES.get("console.dss.untrustedChain");
            if (!details.isEmpty()) {
                message = message + System.lineSeparator() + details;
            }
            LOGGER.log(Level.SEVERE, message, e);
        } catch (Exception e) {
            final String httpHint = remoteHttpErrorHint(e);
            LOGGER.log(Level.SEVERE, httpHint != null ? httpHint : RES.get("console.exception"), e);
        } catch (OutOfMemoryError e) {
            LOGGER.log(Level.SEVERE, RES.get("console.memoryError"), e);
        } finally {
            if (encryptedTempFile != null) {
                encryptedTempFile.delete();
            }
            if (resourcesHandlerBuilder != null) {
                // Must run after writeTo(): the signed DSSDocument is a FileDocument backed by one of
                // these temp files.
                resourcesHandlerBuilder.clear();
            }
        }
        return finished;
    }

    /**
     * Estimates how many bytes to reserve in the PDF {@code /Contents} for the CMS signature. DSS uses a
     * fixed reservation (default {@value #MIN_CONTENT_SIZE}) that is too small for large certificate chains
     * (e.g. eID / qualified certificates) combined with an embedded signature timestamp, which is exactly the
     * case reported in issue #430. Sizing from the actual chain (plus a fixed timestamp allowance) covers
     * those in a single pass; {@link #signWithContentSize} is the safety net when even this is too small.
     *
     * <p>
     * The estimate cannot be exact for timestamped signatures: the TSA token (with its own certificate chain)
     * is only known after the TSA responds inside {@code signDocument()}, so a fixed {@link #TSA_ALLOWANCE} is
     * reserved instead.
     * </p>
     *
     * @param chain         the signer's certificate chain (all of it is encapsulated in the CMS)
     * @param withTimestamp whether a signature timestamp will be embedded (PAdES level T and above)
     * @return the number of bytes to reserve, never below {@link #MIN_CONTENT_SIZE}
     */
    private static int estimateContentSize(Certificate[] chain, boolean withTimestamp) {
        int chainBytes = 0;
        for (Certificate cert : chain) {
            try {
                chainBytes += cert.getEncoded().length;
            } catch (CertificateEncodingException e) {
                chainBytes += CERT_SIZE_FALLBACK;
            }
        }
        int estimate = chainBytes + CMS_OVERHEAD;
        if (withTimestamp) {
            estimate += TSA_ALLOWANCE;
        }
        return Math.max(estimate, MIN_CONTENT_SIZE);
    }

    /**
     * Signs the document, reserving {@code initialContentSize} bytes for the CMS {@code /Contents}. The
     * reserved size is fixed before the byte ranges are digested, so it cannot be derived from the produced
     * signature; when {@code retryOnUndersize} is enabled and DSS reports the reservation was too small, this
     * re-runs the whole signing operation with the exact size DSS reported (plus {@link #RETRY_MARGIN}). For
     * timestamped levels each retry fetches a fresh TSA token, hence the {@link #MAX_CONTENT_SIZE_RETRIES} cap.
     */
    private DSSDocument signWithContentSize(PAdESService service, DSSDocument document,
            PAdESSignatureParameters parameters, PrivateKeySignatureToken token,
            int initialContentSize, boolean retryOnUndersize, TempFileResourcesHandlerBuilder resourcesHandlerBuilder) {
        int contentSize = initialContentSize;
        for (int attempt = 0;; attempt++) {
            parameters.setContentSize(contentSize);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Signing attempt " + attempt + " reserving " + contentSize + " bytes for /Contents");
            }
            final ToBeSigned dataToSign = service.getDataToSign(document, parameters);
            // From the parameters, not the key: they disagree when a PKCS#11 key reports "RSA" under an
            // id-RSASSA-PSS certificate, and DSS rejects a SignatureValue that does not match.
            final SignatureValue signatureValue = token.sign(dataToSign, parameters.getSignatureAlgorithm(),
                    token.getKeyEntry());
            try {
                return service.signDocument(document, parameters, signatureValue);
            } catch (IllegalArgumentException e) {
                final Integer required = parseRequiredContentSize(e.getMessage());
                // Doubling is the fallback when the required size cannot be parsed; the guard below stops a
                // non-growing loop in that case.
                final int grown = required != null ? required + RETRY_MARGIN : contentSize * 2;
                if (!retryOnUndersize || attempt >= MAX_CONTENT_SIZE_RETRIES || grown <= contentSize) {
                    logUndersizeGuidance(contentSize, required, retryOnUndersize);
                    throw e;
                }
                LOGGER.info(RES.get("console.dss.contentSizeRetry", String.valueOf(contentSize),
                        String.valueOf(grown)));
                if (resourcesHandlerBuilder != null) {
                    // Each attempt stages a full copy of the document in getDataToSign() and another in
                    // signDocument(); nothing from a failed attempt is reachable, so release it before
                    // retrying rather than letting temp files pile up across the loop.
                    resourcesHandlerBuilder.clear();
                }
                contentSize = grown;
            }
        }
    }

    /**
     * Logs an actionable message when an undersized {@code /Contents} cannot be recovered, pointing the user at
     * the two knobs that control it: the automatic {@code engine.dss.retryOnUndersize} growth (when it is
     * switched off) and the explicit {@code engine.dss.contentSize} reservation. Without this the only feedback
     * is the raw DSS {@link IllegalArgumentException}, which names {@code setContentSize(...)} (a DSS API call)
     * rather than the JSignPdf setting.
     *
     * @param contentSize the reservation that proved too small
     * @param required    the size DSS reported as needed, or {@code null} if it could not be parsed
     */
    private static void logUndersizeGuidance(int contentSize, Integer required, boolean retryOnUndersize) {
        final String suggested = String.valueOf((required != null ? required : contentSize * 2) + RETRY_MARGIN);
        if (retryOnUndersize) {
            // Retry was enabled but exhausted/non-progressing: only the explicit override is left.
            LOGGER.severe(RES.get("console.dss.contentSizeExhausted", String.valueOf(contentSize), suggested));
        } else {
            LOGGER.severe(RES.get("console.dss.contentSizeTooSmall", String.valueOf(contentSize), suggested));
        }
    }

    /**
     * Parses the required CMS length out of the DSS "signature size is too small" message, so the retry can
     * reserve exactly that (plus a margin). Returns {@code null} when the message does not match, in which case
     * the caller falls back to doubling.
     */
    private static Integer parseRequiredContentSize(String message) {
        if (message == null) {
            return null;
        }
        final Matcher matcher = UNDERSIZE_LENGTH_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                return Integer.valueOf(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static final Pattern HTTP_STATUS_CODE = Pattern.compile("HTTP status code\\s*:\\s*(\\d{3})");

    private static String remoteHttpErrorHint(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            final String msg = t.getMessage();
            if (msg != null) {
                final Matcher m = HTTP_STATUS_CODE.matcher(msg);
                if (m.find() && ("400".equals(m.group(1)) || "429".equals(m.group(1)))) {
                    return RES.get("console.dss.tsaHttpError", m.group(1));
                }
            }
        }
        return null;
    }

    private OnlineTSPSource buildTspSource(BasicSignerOptions options, PAdESSignatureParameters parameters,
            DigestAlgorithm digestAlgorithm, ProxyConfig proxyConfig, EngineConfig engineConfig) {
        final String tsaUrl = options.getTsaUrl();
        final TimestampDataLoader tsDataLoader = new TimestampDataLoader();
        tsDataLoader.setProxyConfig(proxyConfig);
        if (options.getTsaServerAuthn() == ServerAuthentication.PASSWORD) {
            final URI tsaUri = URI.create(tsaUrl);
            tsDataLoader.addAuthentication(tsaUri.getHost(), resolvePort(tsaUri), null,
                    StringUtils.defaultString(options.getTsaUser()),
                    StringUtils.defaultString(options.getTsaPasswd()).toCharArray());
        }
        final OnlineTSPSource tspSource = new OnlineTSPSource(tsaUrl, tsDataLoader);
        if (engineConfig.getBoolean(KEY_TSA_NONCE, true)) {
            tspSource.setNonceSource(new SecureRandomNonceSource());
        }
        final String policyOid = options.getTsaPolicy();
        if (StringUtils.isNotEmpty(policyOid)) {
            LOGGER.info(RES.get("console.settingTsaPolicy", policyOid));
            tspSource.setPolicyOid(policyOid);
        }
        final String tsaHashAlg = options.getTsaHashAlgWithFallback();
        if (StringUtils.isNotEmpty(tsaHashAlg)) {
            LOGGER.info(RES.get("console.settingTsaHashAlg", tsaHashAlg));
            final DigestAlgorithm tsaDigest = DigestAlgorithm.forJavaName(tsaHashAlg);
            parameters.getContentTimestampParameters().setDigestAlgorithm(tsaDigest);
            parameters.getSignatureTimestampParameters().setDigestAlgorithm(tsaDigest);
            parameters.getArchiveTimestampParameters().setDigestAlgorithm(tsaDigest);
        }
        return tspSource;
    }

    /** Resolves the port for basic-auth registration, defaulting from the scheme when none is given. */
    private static int resolvePort(URI uri) {
        final int port = uri.getPort();
        if (port >= 0) {
            return port;
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    /**
     * Translates JSignPdf's proxy settings into a DSS {@link ProxyConfig} so OCSP / CRL / AIA / TSA
     * traffic honours the configured proxy (mirroring the OpenPDF engine's {@code createProxy()} use).
     * DSS routes only HTTP-style proxies; SOCKS is unsupported, so it is reported rather than silently
     * ignored.
     *
     * @return the proxy configuration, or {@code null} for a direct connection
     */
    private static ProxyConfig buildProxyConfig(BasicSignerOptions options) {
        if (!options.isAdvanced() || options.getProxyType() == Proxy.Type.DIRECT) {
            return null;
        }
        if (options.getProxyType() == Proxy.Type.SOCKS) {
            LOGGER.warning(RES.get("console.dss.socksProxyUnsupported"));
            return null;
        }
        final String host = options.getProxyHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }
        final int port = options.getProxyPort();
        final ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setHttpProperties(proxyProperties("http", host, port));
        // An HTTP proxy is reached over http even for https targets (CONNECT tunnelling), hence "http".
        proxyConfig.setHttpsProperties(proxyProperties("http", host, port));
        return proxyConfig;
    }

    private static ProxyProperties proxyProperties(String scheme, String host, int port) {
        final ProxyProperties props = new ProxyProperties();
        props.setScheme(scheme);
        props.setHost(host);
        props.setPort(port);
        return props;
    }

    /**
     * PDFBox stream cache for the engine's direct {@code Loader.loadPDF} calls. These bypass DSS entirely,
     * so {@code IPdfObjFactory.setPdfMemoryUsageSetting} does not reach them &mdash; without this their
     * decoded stream data would be cached on the heap before DSS is involved. (The raw file bytes are read
     * lazily either way; it is the stream cache that defaults to memory-only.)
     *
     * <p>
     * Unlike the DSS-internal setting, this one honours {@code buffering.tempDir}: PDFBox's
     * {@code MemoryUsageSetting} takes a directory, whereas DSS's {@code PdfMemoryUsageSetting} has no
     * equivalent and always lands in {@code java.io.tmpdir}.
     * </p>
     *
     * @return the temp-file-backed cache function, or {@code null} to keep PDFBox's memory-only default
     */
    private static StreamCacheCreateFunction streamCache(BufferingMode mode, File tempDir) {
        if (mode != BufferingMode.TEMP) {
            return null;
        }
        final MemoryUsageSetting setting = MemoryUsageSetting.setupMixed(MIXED_THRESHOLD_BYTES);
        if (tempDir != null) {
            setting.setTempDir(tempDir);
        }
        return setting.streamCache;
    }

    private static PDDocument loadPdf(File file, StreamCacheCreateFunction streamCache) throws IOException {
        return streamCache != null ? Loader.loadPDF(file, streamCache) : Loader.loadPDF(file);
    }

    private File encryptPdf(File inFile, BasicSignerOptions options, StreamCacheCreateFunction streamCache,
            File tempDir) throws Exception {
        try (PDDocument doc = loadPdf(inFile, streamCache)) {
            if (!doc.getSignatureDictionaries().isEmpty()) {
                LOGGER.info(RES.get("console.dss.cannotEncryptSigned"));
                return null;
            }
            final AccessPermission ap = buildAccessPermission(options);
            final String encOwnerPwd = StringUtils.defaultString(options.getPdfOwnerPwdStrX());
            final String encUserPwd = StringUtils.defaultString(options.getPdfUserPwdStr());
            final StandardProtectionPolicy policy = new StandardProtectionPolicy(encOwnerPwd, encUserPwd, ap);
            // 128-bit matches the OpenPDF engine's password encryption (PdfStamper.setEncryption(true, ...)
            // i.e. STANDARD_ENCRYPTION_128), so switching engines is not an encryption-strength downgrade.
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);

            final File tempFile = File.createTempFile("jsignpdf-dss-enc-", ".pdf", tempDir);
            try {
                doc.save(tempFile);
            } catch (Exception e) {
                // Only the returned file reaches the caller's finally, so a failed save has to clean up
                // after itself rather than leave a document-sized file behind.
                tempFile.delete();
                throw e;
            }
            return tempFile;
        }
    }

    private AccessPermission buildAccessPermission(BasicSignerOptions options) {
        final AccessPermission ap = new AccessPermission();
        PrintRight printing = options.getRightPrinting();
        if (printing == null) {
            printing = PrintRight.ALLOW_PRINTING;
        }
        ap.setCanPrint(printing != PrintRight.DISALLOW_PRINTING);
        ap.setCanPrintFaithful(printing == PrintRight.ALLOW_PRINTING);
        ap.setCanExtractContent(options.isRightCopy());
        ap.setCanAssembleDocument(options.isRightAssembly());
        ap.setCanFillInForm(options.isRightFillIn());
        ap.setCanExtractForAccessibility(options.isRightScreanReaders());
        ap.setCanModifyAnnotations(options.isRightModifyAnnotations());
        ap.setCanModify(options.isRightModifyContents());
        return ap;
    }

    private void configureVisibleSignature(PAdESSignatureParameters parameters, BasicSignerOptions options,
            Certificate[] chain, Calendar signingCal, File inFile, StreamCacheCreateFunction streamCache)
            throws Exception {
        final JSignPdfSignatureImageParameters imageParams = new JSignPdfSignatureImageParameters();
        final SignatureFieldParameters fieldParams = new SignatureFieldParameters();
        // Size of the box the appearance is drawn into, whatever its source - used for the text wrapping mode.
        final float boxWidth;
        final float boxHeight;

        final String sigFieldName = options.getSigFieldNameX();
        if (sigFieldName != null) {
            final PDRectangle fieldRect;
            final int fieldPage;
            try (PDDocument pdDoc = loadPdf(inFile, streamCache)) {
                final PDAnnotationWidget widget = findSignatureFieldWidget(pdDoc, sigFieldName);
                fieldRect = widget.getRectangle();
                fieldPage = pageNumberOf(pdDoc, widget);
            }
            LOGGER.info(RES.get("console.sigField.placing", sigFieldName));
            fieldParams.setFieldId(sigFieldName);
            // No coordinates: DSS reads the appearance rectangle off the existing widget. The page is set so the
            // parameters describe the field actually being filled rather than their page-1 default - DSS passes
            // it to PDFBox' SignatureOptions, which only consults it when it has to create a widget itself, so
            // this does not change the output for a pre-existing field (verified in ExistingFieldSigningTest).
            fieldParams.setPage(fieldPage);
            boxWidth = fieldRect == null ? 0f : Math.abs(fieldRect.getWidth());
            boxHeight = fieldRect == null ? 0f : Math.abs(fieldRect.getHeight());
        } else {
            int page = options.getPage();
            float pageWidth;
            float pageHeight;
            try (PDDocument pdDoc = loadPdf(inFile, streamCache)) {
                final int totalPages = pdDoc.getNumberOfPages();
                if (page < 1 || page > totalPages) {
                    page = totalPages;
                }
                final PDPage pdPage = pdDoc.getPage(page - 1);
                final PDRectangle mediaBox = pdPage.getMediaBox();
                final int rotation = pdPage.getRotation();
                if (rotation == 90 || rotation == 270) {
                    pageWidth = mediaBox.getHeight();
                    pageHeight = mediaBox.getWidth();
                } else {
                    pageWidth = mediaBox.getWidth();
                    pageHeight = mediaBox.getHeight();
                }
            }

            final float llx = fixPosition(options.getPositionLLX(), pageWidth);
            final float lly = fixPosition(options.getPositionLLY(), pageHeight);
            final float urx = fixPosition(options.getPositionURX(), pageWidth);
            final float ury = fixPosition(options.getPositionURY(), pageHeight);

            fieldParams.setPage(page);
            fieldParams.setOriginX(llx);
            // DSS uses a top-left origin (PDF uses bottom-left), so flip the Y coordinate.
            fieldParams.setOriginY(pageHeight - ury);
            fieldParams.setWidth(urx - llx);
            fieldParams.setHeight(ury - lly);
            boxWidth = urx - llx;
            boxHeight = ury - lly;
        }
        imageParams.setFieldParameters(fieldParams);

        final RenderMode renderMode = options.getRenderMode();
        final boolean withGraphic = renderMode == RenderMode.GRAPHIC_AND_DESCRIPTION;

        // Background image (drawn first, behind everything) — available in all modes. Scaling follows the
        // OpenPDF semantics of bgImgScale (0 = stretch, <0 = best fit, >0 = multiplier) and is applied by
        // JSignPdfOverlaySignatureDrawer, which is the drawer selected whenever a background image is set.
        final String bgImgPath = options.getBgImgPath();
        if (bgImgPath != null) {
            LOGGER.info(RES.get("console.createImage", bgImgPath));
            imageParams.setBackgroundImage(new FileDocument(bgImgPath));
            imageParams.setBackgroundScale(options.getBgImgScale());
        }

        // Foreground signature graphic (drawn above background, below text) — used in GRAPHIC_AND_DESCRIPTION mode
        if (withGraphic) {
            final String imgPath = options.getImgPath();
            if (imgPath != null) {
                LOGGER.info(RES.get("console.createImage", imgPath));
                imageParams.setImage(new FileDocument(imgPath));
            }
        }

        LOGGER.info(RES.get("console.setL2Text"));
        final SignatureImageTextParameters textParams = new SignatureImageTextParameters();
        textParams.setText(buildSignatureText(options, chain, signingCal));
        // FILL_BOX_AND_LINEBREAK: DSS auto-calculates the largest font that fits the signature rectangle,
        // wrapping lines as needed, so text is never clipped on a small box. The configured font size stays
        // an upper bound — both drawers returned by JSignPdfSignatureDrawerFactory cap the computed size at
        // it. DSS rejects this mode when the field has no explicit size, so keep its default there.
        if (boxWidth > 0f && boxHeight > 0f) {
            textParams.setTextWrapping(TextWrapping.FILL_BOX_AND_LINEBREAK);
        }
        // Transparent text background so the background image shows through.
        // (DSS defaults to solid white which would paint over the image.)
        textParams.setBackgroundColor(null);
        final DSSFont font = DssFontUtils.getVisibleSignatureFont();
        if (font != null) {
            float fontSize = options.getL2TextFontSize();
            if (fontSize <= 0f) {
                fontSize = net.sf.jsignpdf.Constants.DEFVAL_L2_FONT_SIZE;
            }
            font.setSize(fontSize);
            textParams.setFont(font);
        }
        imageParams.setTextParameters(textParams);

        // The foreground graphic has no scale option of its own, so it always keeps its aspect ratio.
        // STRETCH would additionally be rejected by DSS 6.4+ in combination with FILL_BOX_AND_LINEBREAK.
        imageParams.setImageScaling(ImageScaling.ZOOM_AND_CENTER);

        parameters.setImageParameters(imageParams);
    }

    /**
     * Returns the first widget of the named signature field. DSS itself resolves the field by id and rejects a
     * missing, already signed or non-signature field, but it does so only while signing - and the widget is
     * needed here to feed the field's page and box size into the parameters.
     */
    private PDAnnotationWidget findSignatureFieldWidget(PDDocument doc, String fieldName) throws Exception {
        final PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
        final PDField field = acroForm == null ? null : acroForm.getField(fieldName);
        if (!(field instanceof PDSignatureField sigField)) {
            throw new IllegalArgumentException(RES.get("console.sigField.notASignatureField", fieldName));
        }
        final List<PDAnnotationWidget> widgets = sigField.getWidgets();
        if (widgets == null || widgets.isEmpty() || widgets.get(0) == null) {
            throw new IllegalArgumentException(RES.get("console.sigField.noWidget", fieldName));
        }
        return widgets.get(0);
    }

    /**
     * Returns the 1-based page number the widget sits on. {@code /P} is optional, so fall back to scanning the
     * pages for the annotation.
     */
    private int pageNumberOf(PDDocument doc, PDAnnotationWidget widget) {
        final PDPage widgetPage = widget.getPage();
        int pageNumber = 1;
        for (PDPage page : doc.getPages()) {
            if (widgetPage != null) {
                if (page.getCOSObject().equals(widgetPage.getCOSObject())) {
                    return pageNumber;
                }
            } else {
                try {
                    for (PDAnnotation annotation : page.getAnnotations()) {
                        if (annotation != null && annotation.getCOSObject().equals(widget.getCOSObject())) {
                            return pageNumber;
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Unable to read annotations of page " + pageNumber, e);
                }
            }
            pageNumber++;
        }
        LOGGER.log(Level.FINE, "Signature field widget not found on any page, assuming page 1");
        return 1;
    }

    private String buildSignatureText(BasicSignerOptions options, Certificate[] chain, Calendar signingCal) {
        final X509Certificate signerCert = (X509Certificate) chain[0];
        String signer = extractCN(signerCert);
        if (StringUtils.isNotEmpty(options.getSignerName())) {
            signer = options.getSignerName();
        }
        final String certificate = signerCert.getSubjectX500Principal().toString();
        final String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(signingCal.getTime());
        final String reason = options.getReason();
        final String location = options.getLocation();

        if (options.getL2Text() == null) {
            final StringBuilder buf = new StringBuilder();
            buf.append(RES.get("default.l2text.signedBy")).append(' ').append(signer).append('\n');
            buf.append(RES.get("default.l2text.date")).append(' ').append(timestamp);
            if (StringUtils.isNotEmpty(reason)) {
                buf.append('\n').append(RES.get("default.l2text.reason")).append(' ').append(reason);
            }
            if (StringUtils.isNotEmpty(location)) {
                buf.append('\n').append(RES.get("default.l2text.location")).append(' ').append(location);
            }
            return buf.toString();
        }
        // Same placeholder template language (${...}) as the OpenPDF engine, so a single --l2-text works
        // across engines.
        final Map<String, String> replacements = new HashMap<>();
        replacements.put(L2TEXT_PLACEHOLDER_SIGNER, StringUtils.defaultString(signer));
        replacements.put(L2TEXT_PLACEHOLDER_CERTIFICATE, certificate);
        replacements.put(L2TEXT_PLACEHOLDER_TIMESTAMP, timestamp);
        replacements.put(L2TEXT_PLACEHOLDER_LOCATION, StringUtils.defaultString(location));
        replacements.put(L2TEXT_PLACEHOLDER_REASON, StringUtils.defaultString(reason));
        replacements.put(L2TEXT_PLACEHOLDER_CONTACT, StringUtils.defaultString(options.getContact()));
        return StrSubstitutor.replace(options.getL2Text(), replacements);
    }

    private String extractCN(X509Certificate cert) {
        try {
            final LdapName ldapName = new LdapName(cert.getSubjectX500Principal().getName());
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (Exception e) {
            // fall through to the full DN
        }
        return cert.getSubjectX500Principal().toString();
    }

    private float fixPosition(float origPos, float base) {
        return origPos >= 0 ? origPos : base + origPos;
    }
}
