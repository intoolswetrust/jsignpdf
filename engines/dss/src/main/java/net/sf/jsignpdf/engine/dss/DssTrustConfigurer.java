package net.sf.jsignpdf.engine.dss;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.sf.jsignpdf.engine.EngineConfig;

import org.apache.commons.lang3.StringUtils;

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
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
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
    static final String KEY_USE_DEFAULT_LOTL = "trust.useDefaultLotl";
    static final String KEY_LOTL_URLS = "trust.lotlUrls";
    static final String KEY_CERT_FILES = "trust.certFiles";
    static final String KEY_CERT_URLS = "trust.certUrls";
    static final String KEY_TRUSTSTORE_FILE = "trust.truststoreFile";
    static final String KEY_TRUSTSTORE_TYPE = "trust.truststoreType";
    static final String KEY_TRUSTSTORE_PASSWORD = "trust.truststorePassword";

    /** Separator for the list-valued keys (lotlUrls / certFiles / certUrls). */
    private static final String LIST_SEPARATOR = "[,;]+";

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
     */
    CommonCertificateVerifier buildVerifier(ProxyConfig proxyConfig) {
        CommonCertificateVerifier verifier = new CommonCertificateVerifier();
        try {
            CertificateSource[] trustedSources = createTrustedCertSources();
            if (trustedSources.length > 0) {
                verifier.setTrustedCertSources(trustedSources);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to configure DSS trusted certificate sources", e);
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

    private CertificateSource[] createTrustedCertSources() throws Exception {
        List<CertificateSource> trustedSources = new ArrayList<>();

        LOTLSource[] lotlSources = getLotlSources();
        if (lotlSources.length > 0) {
            TLValidationJob tlValidationJob = new TLValidationJob();
            tlValidationJob.setOnlineDataLoader(new FileCacheDataLoader(new CommonsDataLoader()));
            tlValidationJob.setListOfTrustedListSources(lotlSources);
            TrustedListsCertificateSource trustedListsCertificateSource = new TrustedListsCertificateSource();
            tlValidationJob.setTrustedListCertificateSource(trustedListsCertificateSource);
            tlValidationJob.onlineRefresh();
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
            final String type = config.getString(KEY_TRUSTSTORE_TYPE, KeyStore.getDefaultType());
            final String pwd = config.getString(KEY_TRUSTSTORE_PASSWORD, "");
            KeyStoreCertificateSource source = new KeyStoreCertificateSource(new File(truststoreFile), type,
                    pwd != null ? pwd.toCharArray() : null);
            trustedSources.add(source);
        }
        return trustedSources.toArray(new CertificateSource[0]);
    }

    private LOTLSource[] getLotlSources() {
        List<LOTLSource> lotlSources = new ArrayList<>();
        if (config.getBoolean(KEY_USE_DEFAULT_LOTL, false)) {
            lotlSources.add(new LOTLSource());
        }
        for (String url : splitList(config.getString(KEY_LOTL_URLS))) {
            LOTLSource lotlSource = new LOTLSource();
            lotlSource.setUrl(url);
            lotlSource.setCertificateSource(new CommonCertificateSource());
            lotlSources.add(lotlSource);
        }
        return lotlSources.toArray(new LOTLSource[0]);
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
