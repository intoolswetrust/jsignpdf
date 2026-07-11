package net.sf.jsignpdf.engine.dss;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.utils.ConfigLocationResolver;

import org.apache.commons.lang3.StringUtils;

import eu.europa.esig.dss.model.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;

/**
 * Builds and configures the {@link CommonCertificateVerifier} that drives DSS revocation handling and
 * trust-anchor resolution for the LT / LTA baseline levels. Trust material is read through the phase-1
 * {@link EngineConfig} view ({@code engine.dss.*}); the mapping mirrors {@code jsignpdf-pades}'
 * {@code TrustConfig} / {@code TrustedCertSourcesProvider}.
 *
 * <p>
 * B and T need no trust configuration, so the verifier returned for them is the bare
 * {@link CommonCertificateVerifier}. LT/LTA additionally need reachable revocation data; when
 * {@code engine.dss.online.enabled=true} the AIA / OCSP / CRL online sources are wired in.
 * </p>
 *
 * @author Josef Cacek
 */
final class DssTrustConfigurer {

    static final String KEY_ONLINE_ENABLED = "online.enabled";

    // --- EU LOTL machinery (trust.eu.* sub-namespace) ---
    /** Enable the bundled European LOTL (with the bundled OJ keystore). */
    static final String KEY_EU_ENABLED = "trust.eu.enabled";
    /** Override the default EU LOTL URL (only effective when {@link #KEY_EU_ENABLED} is set). */
    static final String KEY_EU_LOTL_URL = "trust.eu.lotlUrl";
    /** Override the Official Journal scheme-information URL used by the announcement predicate. */
    static final String KEY_EU_OJ_URL = "trust.eu.ojUrl";
    /** Override the bundled OJ keystore with an external file. */
    static final String KEY_EU_OJ_KEYSTORE_FILE = "trust.eu.ojKeystoreFile";
    /** Password for the OJ keystore override file. */
    static final String KEY_EU_OJ_KEYSTORE_PASSWORD = "trust.eu.ojKeystorePassword";

    // --- generic / advanced trust material ---
    static final String KEY_LOTL_URLS = "trust.lotlUrls";
    /**
     * Enable Mutual Recognition Agreement processing for the {@link #KEY_LOTL_URLS} sources, so DSS grants
     * trust to third-country trust services recognised via an MRA LOTL (e.g. the eIDAS international pilot's
     * {@code mra_lotl.xml}). Off by default; only MRA LOTLs need it.
     */
    static final String KEY_LOTL_MRA_SUPPORT = "trust.lotlMraSupport";
    static final String KEY_CERT_FILES = "trust.certFiles";
    static final String KEY_CERT_URLS = "trust.certUrls";
    static final String KEY_TRUSTSTORE_FILE = "trust.truststoreFile";
    static final String KEY_TRUSTSTORE_TYPE = "trust.truststoreType";
    static final String KEY_TRUSTSTORE_PASSWORD = "trust.truststorePassword";
    /**
     * Boolean flag: when on, seed trust anchors from every OS / JVM certificate store available on the current
     * platform (issue #447) &mdash; the portable {@code cacerts} everywhere, plus {@code Windows-ROOT} /
     * {@code Windows-MY} on Windows and {@code KeychainStore} on macOS. Off by default; these are public-CA / OS
     * roots, not eIDAS trusted-list anchors.
     */
    static final String KEY_SYSTEM_STORE = "trust.systemStore";

    /** Pseudo store name for the JVM's default CA truststore (a file, not a {@link KeyStore} provider type). */
    private static final String CACERTS_STORE = "cacerts";
    /** Default password for the JVM {@code cacerts} store when {@code javax.net.ssl.trustStorePassword} is unset. */
    private static final String CACERTS_DEFAULT_PASSWORD = "changeit";

    /** Canonical EU List of Trusted Lists location. */
    static final String DEFAULT_EU_LOTL_URL = "https://ec.europa.eu/tools/lotl/eu-lotl.xml";

    /**
     * Default Official Journal scheme-information URL announcing the certificates allowed to sign the EU LOTL.
     * Must stay in sync with the bundled OJ keystore ({@link #OJ_KEYSTORE_RESOURCE}); both were rotated by
     * OJ C/2026/1944 (April 2026). Override via {@link #KEY_EU_OJ_URL} when pointing at a newer OJ notice.
     */
    static final String DEFAULT_OJ_URL =
            "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=OJ:C_202601944";

    /** Classpath location of the bundled OJ keystore (validates the EU LOTL's own signature). */
    static final String OJ_KEYSTORE_RESOURCE = "/net/sf/jsignpdf/engine/dss/eu-oj-keystore.p12";

    /** Keystore type / password of the bundled OJ keystore (matches the DSS demonstrations keystore). */
    private static final String OJ_KEYSTORE_TYPE = "PKCS12";
    private static final String OJ_KEYSTORE_PASSWORD = "dss-password";

    /** Separator for the list-valued keys (lotlUrls / certFiles / certUrls). */
    private static final String LIST_SEPARATOR = "[,;]+";

    /** Subdirectory (under the JSignPdf config dir, else the system temp dir) holding the cached trusted lists. */
    private static final String TL_CACHE_DIR_NAME = "dss-tl-cache";

    /**
     * How long a cached trusted-list / LOTL download stays fresh before DSS re-fetches it (24h). Trusted lists
     * change infrequently, so this lets repeat / batch LT/LTA signing reuse the on-disk cache instead of
     * re-downloading the LOTL on every {@code sign()} call.
     */
    private static final long TL_CACHE_EXPIRATION_MS = 24L * 60 * 60 * 1000;

    private final EngineConfig config;

    DssTrustConfigurer(EngineConfig config) {
        this.config = config;
    }

    /**
     * @return {@code true} when online fetching of revocation/AIA data is enabled
     */
    boolean isOnlineEnabled() {
        return config.getBoolean(KEY_ONLINE_ENABLED, false);
    }

    /**
     * Builds a verifier configured with the trusted certificate sources and (when online is enabled) the
     * AIA / OCSP / CRL online sources required to embed validation material for LT/LTA.
     *
     * @param proxyConfig the HTTP proxy configuration to route AIA / OCSP / CRL traffic through, or
     *                    {@code null} for a direct connection
     * @return the configured certificate verifier
     * @throws Exception if a configured trust source (truststore / cert file / cert URL / LOTL) cannot be
     *                   loaded; the caller fails fast rather than signing without the intended trust anchors
     */
    CommonCertificateVerifier buildVerifier(ProxyConfig proxyConfig) throws Exception {
        CommonCertificateVerifier verifier = new CommonCertificateVerifier();
        CertificateSource[] trustedSources = createTrustedCertSources();
        if (trustedSources.length > 0) {
            verifier.setTrustedCertSources(trustedSources);
        }
        if (isOnlineEnabled()) {
            OCSPDataLoader ocspDataLoader = new OCSPDataLoader();
            ocspDataLoader.setProxyConfig(proxyConfig);
            CommonsDataLoader dataLoader = new CommonsDataLoader();
            dataLoader.setProxyConfig(proxyConfig);
            verifier.setAIASource(new DefaultAIASource(dataLoader));
            verifier.setOcspSource(new OnlineOCSPSource(ocspDataLoader));
            verifier.setCrlSource(new OnlineCRLSource(dataLoader));
        }
        return verifier;
    }

    CertificateSource[] createTrustedCertSources() throws Exception {
        List<CertificateSource> trustedSources = new ArrayList<>();

        LOTLSource[] lotlSources = getLotlSources();
        if (lotlSources.length > 0) {
            TLValidationJob tlValidationJob = new TLValidationJob();
            FileCacheDataLoader onlineDataLoader = new FileCacheDataLoader(new CommonsDataLoader());
            onlineDataLoader.setFileCacheDirectory(tlCacheDirectory());
            onlineDataLoader.setCacheExpirationTime(TL_CACHE_EXPIRATION_MS);
            tlValidationJob.setOnlineDataLoader(onlineDataLoader);
            tlValidationJob.setListOfTrustedListSources(lotlSources);
            TrustedListsCertificateSource trustedListsCertificateSource = new TrustedListsCertificateSource();
            tlValidationJob.setTrustedListCertificateSource(trustedListsCertificateSource);
            try {
                tlValidationJob.onlineRefresh();
            } catch (Exception e) {
                // Surface an actionable cause instead of an opaque DSS stack trace; the caller logs this via
                // console.dss.trustConfigFailed and aborts signing. Common causes: offline / proxy not
                // configured, or a stale OJ keystore that can no longer validate the LOTL signature.
                throw new IllegalStateException("Failed to refresh the EU LOTL / trusted lists (check network /"
                        + " proxy, or update the OJ keystore via " + KEY_EU_OJ_KEYSTORE_FILE + ")", e);
            }
            logTrustAnchorSummary(trustedListsCertificateSource);
            trustedSources.add(trustedListsCertificateSource);
        }

        for (String certFile : splitList(config.getString(KEY_CERT_FILES))) {
            CommonTrustedCertificateSource source = new CommonTrustedCertificateSource();
            source.addCertificate(DSSUtils.loadCertificate(new File(certFile)));
            trustedSources.add(source);
        }
        for (String certUrl : splitList(config.getString(KEY_CERT_URLS))) {
            CommonTrustedCertificateSource source = new CommonTrustedCertificateSource();
            try (InputStream is = new URL(certUrl).openStream()) {
                source.addCertificate(DSSUtils.loadCertificate(is));
            }
            trustedSources.add(source);
        }

        final String truststoreFile = config.getString(KEY_TRUSTSTORE_FILE);
        if (StringUtils.isNotEmpty(truststoreFile)) {
            final String type = StringUtils.defaultIfEmpty(config.getString(KEY_TRUSTSTORE_TYPE),
                    KeyStore.getDefaultType());
            final String pwd = config.getString(KEY_TRUSTSTORE_PASSWORD, "");
            KeyStoreCertificateSource source = new KeyStoreCertificateSource(new File(truststoreFile), type,
                    pwd != null ? pwd.toCharArray() : null);
            trustedSources.add(asTrusted(source));
        }

        if (config.getBoolean(KEY_SYSTEM_STORE, false)) {
            for (String store : systemStoreNames()) {
                CertificateSource source = loadSystemStore(store);
                if (source != null) {
                    trustedSources.add(source);
                }
            }
        }
        return trustedSources.toArray(new CertificateSource[0]);
    }

    /**
     * The OS / JVM certificate stores to seed anchors from when {@link #KEY_SYSTEM_STORE} is on: the portable
     * {@code cacerts} on every platform, plus the machine root / personal stores on Windows (via SunMSCAPI) and
     * the login keychain on macOS. A store the current JRE can't provide is simply not listed.
     */
    private static List<String> systemStoreNames() {
        List<String> stores = new ArrayList<>();
        stores.add(CACERTS_STORE);
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            stores.add("Windows-ROOT");
            stores.add("Windows-MY");
        } else if (os.contains("mac")) {
            stores.add("KeychainStore");
        }
        return stores;
    }

    /**
     * Best-effort load of one OS / JVM certificate store (issue #447): {@code cacerts} resolves the JVM's default
     * CA truststore (honouring the {@code javax.net.ssl.trustStore*} system properties, else
     * {@code $JAVA_HOME/lib/security/jssecacerts|cacerts} with password {@code changeit}); any other name is a
     * {@link KeyStore} type loaded from its provider with a null stream (e.g. {@code Windows-ROOT}). These are
     * public-CA / OS roots, not eIDAS trusted-list anchors: they let LT/LTA embed validation data for
     * commercially-issued (non-qualified) certificates but do not confer qualified status. A store that fails to
     * load is logged and skipped rather than aborting signing.
     *
     * @return the loaded source, or {@code null} if the store could not be opened
     */
    private static CertificateSource loadSystemStore(String store) {
        try {
            KeyStoreCertificateSource source;
            if (CACERTS_STORE.equalsIgnoreCase(store)) {
                final String type = System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType());
                final String pwd = System.getProperty("javax.net.ssl.trustStorePassword", CACERTS_DEFAULT_PASSWORD);
                source = new KeyStoreCertificateSource(cacertsFile(), type, pwd.toCharArray());
            } else {
                source = new KeyStoreCertificateSource(store, (char[]) null);
            }
            Constants.LOGGER.info("Loaded " + source.getNumberOfCertificates()
                    + " trust anchor(s) from system certificate store '" + store + "'");
            return asTrusted(source);
        } catch (Exception e) {
            Constants.LOGGER.log(Level.WARNING,
                    "Could not load system certificate store '" + store + "' (skipped)", e);
            return null;
        }
    }

    /**
     * Resolves the JVM's default CA truststore file following the JSSE lookup order: an explicit
     * {@code javax.net.ssl.trustStore} (unless {@code NONE}), else {@code $JAVA_HOME/lib/security/jssecacerts}
     * when present, else {@code $JAVA_HOME/lib/security/cacerts}.
     */
    private static File cacertsFile() {
        final String override = System.getProperty("javax.net.ssl.trustStore");
        if (StringUtils.isNotEmpty(override) && !"NONE".equals(override)) {
            return new File(override);
        }
        Path securityDir = Path.of(System.getProperty("java.home"), "lib", "security");
        File jssecacerts = securityDir.resolve("jssecacerts").toFile();
        return jssecacerts.exists() ? jssecacerts : securityDir.resolve("cacerts").toFile();
    }

    /**
     * Logs how many trust anchors the trusted lists produced (plus the number of TLs / LOTLs processed). Without
     * this, a LOTL that downloads but yields zero anchors (network/proxy filtering, a national TL that failed to
     * sync) is indistinguishable in the logs from one where the anchors loaded fine but the signer's / TSA's
     * particular CA simply is not listed &mdash; both surface later only as the opaque untrusted-chain
     * {@code AlertException}. The count makes that distinction visible up front (issue #441).
     */
    private static void logTrustAnchorSummary(TrustedListsCertificateSource source) {
        final String anchors = String.valueOf(source.getNumberOfCertificates());
        final TLValidationJobSummary summary = source.getSummary();
        final String tls = String.valueOf(summary != null ? summary.getNumberOfProcessedTLs() : 0);
        final String lotls = String.valueOf(summary != null ? summary.getNumberOfProcessedLOTLs() : 0);
        if (source.getNumberOfCertificates() == 0) {
            Constants.LOGGER.warning(Constants.RES.get("console.dss.trustNoAnchors", anchors, tls, lotls));
        } else {
            Constants.LOGGER.info(Constants.RES.get("console.dss.trustLoaded", anchors, tls, lotls));
        }
    }

    /**
     * Resolves the directory DSS caches the downloaded trusted lists in: {@code <configDir>/dss-tl-cache} when a
     * JSignPdf config directory is available, otherwise a stable folder under the system temp dir. A persistent
     * location (rather than {@link FileCacheDataLoader}'s default temp behaviour) lets the cache survive across
     * runs so batch LT/LTA signing reuses it.
     */
    private static File tlCacheDirectory() {
        Path base = ConfigLocationResolver.getInstance().getConfigDir();
        File cacheDir = base != null
                ? base.resolve(TL_CACHE_DIR_NAME).toFile()
                : new File(System.getProperty("java.io.tmpdir"), "jsignpdf-" + TL_CACHE_DIR_NAME);
        try {
            Files.createDirectories(cacheDir.toPath());
        } catch (Exception e) {
            // Non-fatal: DSS recreates the directory on demand; log and let signing proceed.
            Constants.LOGGER.log(Level.WARNING, "Could not create DSS trusted-list cache directory " + cacheDir, e);
        }
        return cacheDir;
    }

    LOTLSource[] getLotlSources() throws Exception {
        List<LOTLSource> lotlSources = new ArrayList<>();
        CertificateSource ojCertificateSource = null;

        if (config.getBoolean(KEY_EU_ENABLED, false)) {
            ojCertificateSource = ojKeystoreCertificateSource();
            lotlSources.add(europeanLotlSource(ojCertificateSource));
        }

        List<String> customLotlUrls = splitList(config.getString(KEY_LOTL_URLS));
        if (!customLotlUrls.isEmpty() && ojCertificateSource == null) {
            ojCertificateSource = ojKeystoreCertificateSource();
        }
        boolean mraSupport = config.getBoolean(KEY_LOTL_MRA_SUPPORT, false);
        for (String url : customLotlUrls) {
            // Advanced / "bring your own trust": signed by the (bundled or overridden) OJ certs, pivot
            // support on, but no OJ announcement predicate (a custom LOTL may not announce the EU OJ URL).
            // MRA support is opt-in for third-country mutual-recognition LOTLs.
            LOTLSource lotlSource = new LOTLSource();
            lotlSource.setUrl(url);
            lotlSource.setCertificateSource(ojCertificateSource);
            lotlSource.setPivotSupport(true);
            lotlSource.setMraSupport(mraSupport);
            lotlSources.add(lotlSource);
        }
        return lotlSources.toArray(new LOTLSource[0]);
    }

    /**
     * Builds the European LOTL source wired so DSS can validate the LOTL's own signature against the OJ
     * keystore and follow the pivot chain to the current trust anchors. The URL and OJ scheme-information URL
     * default to the canonical EU values and are overridable via {@link #KEY_EU_LOTL_URL} / {@link #KEY_EU_OJ_URL}.
     */
    private LOTLSource europeanLotlSource(CertificateSource ojCertificateSource) {
        LOTLSource lotl = new LOTLSource();
        lotl.setUrl(StringUtils.defaultIfEmpty(config.getString(KEY_EU_LOTL_URL), DEFAULT_EU_LOTL_URL));
        lotl.setCertificateSource(ojCertificateSource);
        lotl.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(
                StringUtils.defaultIfEmpty(config.getString(KEY_EU_OJ_URL), DEFAULT_OJ_URL)));
        lotl.setPivotSupport(true);
        return lotl;
    }

    /**
     * Loads the certificate source that validates the LOTL signature: an external keystore when
     * {@link #KEY_EU_OJ_KEYSTORE_FILE} is set, otherwise the keystore bundled on the classpath. Unlike
     * {@link #KEY_TRUSTSTORE_FILE} (trust anchors for the document signer), these certs are consumed only by
     * the {@link TLValidationJob} to decide whether to accept the LOTL.
     */
    private CertificateSource ojKeystoreCertificateSource() throws Exception {
        final String overrideFile = config.getString(KEY_EU_OJ_KEYSTORE_FILE);
        if (StringUtils.isNotEmpty(overrideFile)) {
            final String pwd = config.getString(KEY_EU_OJ_KEYSTORE_PASSWORD, "");
            return new KeyStoreCertificateSource(new File(overrideFile), KeyStore.getDefaultType(),
                    pwd != null ? pwd.toCharArray() : null);
        }
        try (InputStream is = DssTrustConfigurer.class.getResourceAsStream(OJ_KEYSTORE_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Bundled OJ keystore resource not found on the classpath: "
                        + OJ_KEYSTORE_RESOURCE);
            }
            return new KeyStoreCertificateSource(is, OJ_KEYSTORE_TYPE, OJ_KEYSTORE_PASSWORD.toCharArray());
        }
    }

    /**
     * Wraps a {@link KeyStoreCertificateSource} (whose source type is {@code OTHER}) in a
     * {@link CommonTrustedCertificateSource} so it qualifies as a {@code TRUSTED_STORE}. DSS's
     * {@link CommonCertificateVerifier#setTrustedCertSources} rejects anything that is not {@code TRUSTED_STORE} or
     * {@code TRUSTED_LIST}, so a raw keystore source cannot be used as a trust anchor directly.
     */
    private static CertificateSource asTrusted(KeyStoreCertificateSource source) {
        CommonTrustedCertificateSource trusted = new CommonTrustedCertificateSource();
        trusted.importAsTrusted(source);
        return trusted;
    }

    private static List<String> splitList(String value) {
        List<String> out = new ArrayList<>();
        if (StringUtils.isNotBlank(value)) {
            for (String part : value.split(LIST_SEPARATOR)) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    out.add(trimmed);
                }
            }
        }
        return out;
    }
}
