package net.sf.jsignpdf.engine;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.types.PadesLevel;

import org.apache.commons.lang3.StringUtils;

/**
 * Config preflight for the PAdES LT / LTA levels: those embed revocation data, which DSS only fetches for a
 * <em>trusted</em> signer chain reachable <em>online</em>. Without both, signing fails deep inside DSS with an
 * opaque {@code AlertException} ("Revocation data is skipped for untrusted certificate chain") &mdash; the
 * defect behind issue #432. This validator detects the misconfiguration up front so the CLI can fail fast and
 * the GUI can offer to fix it, before any expensive work (PDF load, key/PIN access, TSA round-trip).
 *
 * <p>
 * It is a config-level check, not a connectivity test: a configuration that passes can still fail at runtime
 * (LOTL unreachable, proxy, stale OJ keystore), which the engine surfaces via {@code console.dss.trustConfigFailed}.
 * </p>
 *
 * @author Josef Cacek
 */
public final class DssLtTrustPreflight {

    // Engine-relative keys (resolved under engine.<id>.* by EngineConfig); mirror DssTrustConfigurer's keys.
    static final String KEY_ONLINE_ENABLED = "online.enabled";
    static final String KEY_EU_ENABLED = "trust.eu.enabled";
    static final String KEY_TRUSTSTORE_FILE = "trust.truststoreFile";
    static final String KEY_CERT_FILES = "trust.certFiles";
    static final String KEY_CERT_URLS = "trust.certUrls";
    static final String KEY_LOTL_URLS = "trust.lotlUrls";

    /**
     * Outcome of the preflight.
     *
     * @param applicable         whether the check applied at all (LT/LTA selected on an LT-capable engine)
     * @param onlineMissing      LT/LTA needs online fetching but {@code online.enabled} is off
     * @param trustSourceMissing LT/LTA needs a trust anchor but none of the trust sources is configured
     * @param customLotlConfigured a custom List-of-Trusted-Lists ({@code trust.lotlUrls}) is set. This is the
     *                           only source that <em>replaces</em> the bundled EU LOTL, so the GUI auto-fix
     *                           enables the EU LOTL ({@code trust.eu.enabled}) unless a custom LOTL is present.
     *                           Extra anchor material ({@code trust.truststoreFile} / {@code certFiles} /
     *                           {@code certUrls}) is additive (e.g. a TSA CA) and does not suppress the EU LOTL.
     */
    public record Result(boolean applicable, boolean onlineMissing, boolean trustSourceMissing,
            boolean customLotlConfigured) {
        /** @return {@code true} when the configuration would make LT/LTA signing fail. */
        public boolean hasIssues() {
            return applicable && (onlineMissing || trustSourceMissing);
        }
    }

    private DssLtTrustPreflight() {
    }

    /**
     * Checks whether the engine is configured to satisfy an LT/LTA request.
     *
     * @param o      the populated signing options
     * @param engine the engine that would sign
     * @param config the engine-scoped configuration view
     * @return the preflight result; {@link Result#hasIssues()} is {@code false} when nothing is wrong or the
     *         check does not apply (non-LT/LTA level, or an engine that cannot produce LT)
     */
    public static Result check(BasicSignerOptions o, SigningEngine engine, EngineConfig config) {
        if (o == null || engine == null || config == null) {
            return new Result(false, false, false, false);
        }
        final PadesLevel level = o.getPadesLevel();
        final boolean ltOrLta = level == PadesLevel.BASELINE_LT || level == PadesLevel.BASELINE_LTA;
        if (!ltOrLta || !engine.capabilities().contains(Capability.PADES_BASELINE_LT)) {
            return new Result(false, false, false, false);
        }
        final boolean online = config.getBoolean(KEY_ONLINE_ENABLED, false);
        final boolean customLotl = StringUtils.isNotBlank(config.getString(KEY_LOTL_URLS));
        final boolean trustSource = config.getBoolean(KEY_EU_ENABLED, false)
                || StringUtils.isNotBlank(config.getString(KEY_TRUSTSTORE_FILE))
                || StringUtils.isNotBlank(config.getString(KEY_CERT_FILES))
                || StringUtils.isNotBlank(config.getString(KEY_CERT_URLS))
                || customLotl;
        return new Result(true, !online, !trustSource, customLotl);
    }
}
