package net.sf.jsignpdf.engine.dss;

import static net.sf.jsignpdf.Constants.RES;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jsignpdf.utils.CertificateInfo;

/**
 * Turns the opaque untrusted-chain {@code AlertException} DSS raises for the LT / LTA levels into an
 * actionable message. When revocation data cannot be collected because a certificate chain is not anchored,
 * DSS names the offending certificate(s) only by their DSS token id ({@code C-<SHA-256 hex>}), which is not
 * something a user can map back to a CA to add via {@code engine.dss.trust.certFiles} / {@code truststoreFile}.
 *
 * <p>
 * This reporter re-derives those token ids from the certificates JSignPdf already holds &mdash; the signer
 * chain and the (best-effort captured) timestamp chain &mdash; and, for each id mentioned in the exception,
 * emits its subject / issuer identity and whether it belongs to the signer or the timestamp chain. The
 * fingerprint is kept as a secondary identifier so the enriched line still cross-references the raw DSS error
 * (issue #448).
 * </p>
 *
 * @author Josef Cacek
 */
final class DssUntrustedChainReporter {

    /** DSS certificate token id as it appears in the alert message: {@code C-} + uppercase SHA-256 hex (64 chars). */
    private static final Pattern TOKEN_ID = Pattern.compile("C-[0-9A-Fa-f]{64}");

    private DssUntrustedChainReporter() {
    }

    /**
     * Builds the enriched, human-readable detail block for an untrusted-chain failure.
     *
     * @param error       the failure thrown by DSS (its message, and those of its causes, carry the
     *                    {@code C-<hex>} token ids)
     * @param signerChain the signer's certificate chain (leaf first), or {@code null}
     * @param tsaChain    the timestamp certificate chain captured while signing, or {@code null} if none was
     *                    captured (e.g. the failure happened before the TSA responded)
     * @return the detail block (header plus one line per identified certificate), or an empty string when no
     *         mentioned token id could be matched to a known certificate
     */
    static String describe(Throwable error, Certificate[] signerChain, Collection<? extends X509Certificate> tsaChain) {
        Set<String> tokenIds = collectTokenIds(error);
        if (tokenIds.isEmpty()) {
            return "";
        }
        Map<String, X509Certificate> signerIndex = index(toX509List(signerChain));
        Map<String, X509Certificate> tsaIndex = index(tsaChain);

        StringBuilder lines = new StringBuilder();
        for (String tokenId : tokenIds) {
            final String role;
            X509Certificate cert = signerIndex.get(tokenId);
            if (cert != null) {
                role = RES.get("console.dss.untrustedChainRoleSigner");
            } else {
                cert = tsaIndex.get(tokenId);
                if (cert == null) {
                    // An intermediate that is in neither chain we hold (e.g. an AIA-fetched CA); leave it to
                    // the raw DSS message rather than emitting a name-less line that repeats the fingerprint.
                    continue;
                }
                role = RES.get("console.dss.untrustedChainRoleTsa");
            }
            if (lines.length() > 0) {
                lines.append(System.lineSeparator());
            }
            lines.append(RES.get("console.dss.untrustedChainCert", role, subjectOf(cert), issuerOf(cert), tokenId));
        }
        if (lines.length() == 0) {
            return "";
        }
        return RES.get("console.dss.untrustedChainDetailHeader") + System.lineSeparator() + lines;
    }

    /** Gathers the distinct {@code C-<hex>} token ids from the throwable and its cause chain, preserving order. */
    private static Set<String> collectTokenIds(Throwable error) {
        Set<String> ids = new LinkedHashSet<>();
        for (Throwable t = error; t != null; t = t.getCause()) {
            final String message = t.getMessage();
            if (message != null) {
                Matcher matcher = TOKEN_ID.matcher(message);
                while (matcher.find()) {
                    // DSS emits the hex uppercase; normalise so matching against our own ids is case-insensitive.
                    ids.add(matcher.group().toUpperCase());
                }
            }
        }
        return ids;
    }

    /** Indexes certificates by their DSS token id ({@code C-} + uppercase SHA-256 hex of the DER encoding). */
    private static Map<String, X509Certificate> index(Collection<? extends X509Certificate> certs) {
        Map<String, X509Certificate> index = new LinkedHashMap<>();
        if (certs == null) {
            return index;
        }
        for (X509Certificate cert : certs) {
            if (cert == null) {
                continue;
            }
            // A certificate that cannot be re-encoded has no id and simply stays unidentified in the report.
            final String tokenId = CertificateInfo.tokenId(cert);
            if (tokenId != null) {
                index.putIfAbsent(tokenId, cert);
            }
        }
        return index;
    }

    private static List<X509Certificate> toX509List(Certificate[] chain) {
        List<X509Certificate> out = new ArrayList<>();
        if (chain != null) {
            for (Certificate cert : chain) {
                if (cert instanceof X509Certificate) {
                    out.add((X509Certificate) cert);
                }
            }
        }
        return out;
    }

    private static String subjectOf(X509Certificate cert) {
        return CertificateInfo.subjectOf(cert);
    }

    private static String issuerOf(X509Certificate cert) {
        return CertificateInfo.issuerOf(cert);
    }
}
