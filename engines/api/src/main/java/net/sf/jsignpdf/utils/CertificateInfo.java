package net.sf.jsignpdf.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Level;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import net.sf.jsignpdf.Constants;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

/**
 * Read-only description of an X.509 certificate for the diagnostic log. Signing failures caused by
 * certificates &mdash; a chain that is not anchored, revocation data that cannot be fetched, an expired
 * intermediate &mdash; are reported by the underlying libraries with identifiers the user cannot act on (most
 * notably DSS's {@code C-<SHA-256 hex>} token id). Logging the identity, validity, key usage, QC statements and
 * the AIA / CRL distribution-point URLs of every certificate in the chain lets those reports be matched to a
 * real certificate without reaching for {@code openssl} (issue #452).
 *
 * <p>
 * Everything here is best-effort: an extension that cannot be parsed is omitted rather than failing the
 * signing operation. All output is diagnostic, hence English-only (like the rest of the FINE-level tracing).
 * </p>
 *
 * @author Josef Cacek
 */
public class CertificateInfo {

    /** Timestamp layout for validity bounds &mdash; UTC, so logs from different machines compare directly. */
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    /** Key-usage bit names in the order of the {@code KeyUsage} BIT STRING (RFC 5280 &sect;4.2.1.3). */
    private static final String[] KEY_USAGE_NAMES = { "digitalSignature", "nonRepudiation", "keyEncipherment",
            "dataEncipherment", "keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly" };

    /** Well-known ETSI QC statement OIDs, mapped to the names used in EN 319 412-5. */
    private static final String[][] QC_STATEMENT_NAMES = {
            { "0.4.0.1862.1.1", "QcCompliance" },
            { "0.4.0.1862.1.2", "QcLimitValue" },
            { "0.4.0.1862.1.3", "QcRetentionPeriod" },
            { "0.4.0.1862.1.4", "QcSSCD" },
            { "0.4.0.1862.1.5", "QcPDS" },
            { "0.4.0.1862.1.6", "QcType" },
            { "0.4.0.1862.1.7", "QcCClegislation" },
            { "0.4.0.19495.2", "PSD2QcType" },
            { "1.3.6.1.5.5.7.11.2", "QcSemanticsId-eIDAS" } };

    private CertificateInfo() {
    }

    /**
     * Logs the identity and revocation-relevant extensions of a whole certificate chain at {@link Level#FINE}.
     * Nothing is computed when FINE is not loggable.
     *
     * @param label a short name for the chain, e.g. {@code "Signing certificate chain"}
     * @param chain the chain, leaf first; {@code null} / empty is silently ignored
     */
    public static void logChain(String label, Certificate[] chain) {
        if (chain == null || chain.length == 0) {
            return;
        }
        logChain(label, Arrays.asList(chain));
    }

    /**
     * Logs the identity and revocation-relevant extensions of a certificate collection at {@link Level#FINE}.
     * Non-X.509 entries are skipped.
     *
     * @param label a short name for the chain, e.g. {@code "Timestamp certificate chain"}
     * @param chain the certificates, leaf first; {@code null} / empty is silently ignored
     */
    public static void logChain(String label, Iterable<? extends Certificate> chain) {
        if (chain == null || !Constants.LOGGER.isLoggable(Level.FINE)) {
            return;
        }
        final List<X509Certificate> certs = new ArrayList<>();
        for (Certificate cert : chain) {
            if (cert instanceof X509Certificate) {
                certs.add((X509Certificate) cert);
            }
        }
        if (certs.isEmpty()) {
            return;
        }
        final StringBuilder buf = new StringBuilder(label).append(" (").append(certs.size())
                .append(certs.size() == 1 ? " certificate)" : " certificates)");
        for (int i = 0; i < certs.size(); i++) {
            buf.append(System.lineSeparator()).append(describe(certs.get(i), "  [" + i + "] "));
        }
        Constants.LOGGER.fine(buf.toString());
    }

    /**
     * Builds the multi-line description of a single certificate: subject / issuer / serial / validity /
     * fingerprint on the first lines, then only the extensions that are actually present.
     *
     * @param cert   the certificate to describe
     * @param prefix indentation for the first line; continuation lines are aligned under it
     * @return the description, without a trailing line separator
     */
    public static String describe(X509Certificate cert, String prefix) {
        final String indent = " ".repeat(prefix.length());
        final StringBuilder buf = new StringBuilder();
        buf.append(prefix).append("subject=").append(cert.getSubjectX500Principal().getName());
        buf.append(System.lineSeparator()).append(indent).append("issuer=")
                .append(cert.getIssuerX500Principal().getName());
        buf.append(System.lineSeparator()).append(indent).append("serial=")
                .append(cert.getSerialNumber().toString(16)).append(", validity=")
                .append(formatTimestamp(cert.getNotBefore())).append(" .. ")
                .append(formatTimestamp(cert.getNotAfter()))
                .append(expiryNote(cert));
        appendIfNotEmpty(buf, indent, "id=", StringUtils.defaultString(tokenId(cert)));
        appendIfNotEmpty(buf, indent, "keyUsage=", String.join(",", keyUsages(cert)));
        appendIfNotEmpty(buf, indent, "qcStatements=", String.join(",", qcStatements(cert)));
        appendIfNotEmpty(buf, indent, "AIA caIssuers=", String.join(", ", caIssuersUrls(cert)));
        appendIfNotEmpty(buf, indent, "AIA OCSP=", String.join(", ", ocspUrls(cert)));
        appendIfNotEmpty(buf, indent, "CRL DP=", String.join(", ", crlDistributionPointUrls(cert)));
        return buf.toString();
    }

    /**
     * The DSS certificate token id ({@code C-} + uppercase SHA-256 hex of the DER encoding) &mdash; the
     * identifier DSS uses when it reports a missing trust anchor or missing revocation data.
     *
     * @param cert the certificate
     * @return the token id, or {@code null} when the certificate cannot be re-encoded
     */
    public static String tokenId(X509Certificate cert) {
        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return "C-" + HexFormat.of().withUpperCase().formatHex(sha256.digest(cert.getEncoded()));
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            return null;
        }
    }

    /** @return the principal's CN when present, otherwise its full RFC 2253 DN */
    public static String commonNameOrDn(X500Principal principal) {
        try {
            final LdapName ldapName = new LdapName(principal.getName());
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (Exception e) {
            Constants.LOGGER.log(Level.FINEST, "Unparseable distinguished name", e);
        }
        return principal.getName();
    }

    /** @return the certificate's subject CN, falling back to the full DN */
    public static String subjectOf(X509Certificate cert) {
        return commonNameOrDn(cert.getSubjectX500Principal());
    }

    /** @return the certificate's issuer CN, falling back to the full DN */
    public static String issuerOf(X509Certificate cert) {
        return commonNameOrDn(cert.getIssuerX500Principal());
    }

    /** @return the names of the asserted key-usage bits, empty when the extension is absent */
    public static List<String> keyUsages(X509Certificate cert) {
        final List<String> usages = new ArrayList<>();
        final boolean[] bits = cert.getKeyUsage();
        if (bits != null) {
            for (int i = 0; i < bits.length && i < KEY_USAGE_NAMES.length; i++) {
                if (bits[i]) {
                    usages.add(KEY_USAGE_NAMES[i]);
                }
            }
        }
        return usages;
    }

    /**
     * @return the QC statements asserted by the certificate, by their EN 319 412-5 name where known and by raw
     *         OID otherwise; empty when the certificate carries no QCStatements extension
     */
    public static List<String> qcStatements(X509Certificate cert) {
        final List<String> statements = new ArrayList<>();
        final ASN1Primitive value = extensionValue(cert, Extension.qCStatements);
        if (!(value instanceof ASN1Sequence)) {
            return statements;
        }
        for (ASN1Encodable element : (ASN1Sequence) value) {
            try {
                final ASN1ObjectIdentifier oid = QCStatement.getInstance(element).getStatementId();
                statements.add(qcStatementName(oid.getId()));
            } catch (Exception e) {
                Constants.LOGGER.log(Level.FINEST, "Unparseable QCStatement", e);
            }
        }
        return statements;
    }

    /** @return the {@code caIssuers} access URLs from the Authority Information Access extension */
    public static List<String> caIssuersUrls(X509Certificate cert) {
        return accessUrls(cert, AccessDescription.id_ad_caIssuers);
    }

    /** @return the {@code OCSP} access URLs from the Authority Information Access extension */
    public static List<String> ocspUrls(X509Certificate cert) {
        return accessUrls(cert, AccessDescription.id_ad_ocsp);
    }

    /** @return the URLs listed in the CRL Distribution Points extension */
    public static List<String> crlDistributionPointUrls(X509Certificate cert) {
        final List<String> urls = new ArrayList<>();
        final ASN1Primitive value = extensionValue(cert, Extension.cRLDistributionPoints);
        if (value == null) {
            return urls;
        }
        try {
            for (DistributionPoint point : CRLDistPoint.getInstance(value).getDistributionPoints()) {
                final DistributionPointName name = point.getDistributionPoint();
                if (name == null || name.getType() != DistributionPointName.FULL_NAME) {
                    continue;
                }
                for (GeneralName generalName : GeneralNames.getInstance(name.getName()).getNames()) {
                    addUriName(urls, generalName);
                }
            }
        } catch (Exception e) {
            Constants.LOGGER.log(Level.FINEST, "Unparseable CRL distribution points", e);
        }
        return urls;
    }

    private static List<String> accessUrls(X509Certificate cert, ASN1ObjectIdentifier method) {
        final List<String> urls = new ArrayList<>();
        final ASN1Primitive value = extensionValue(cert, Extension.authorityInfoAccess);
        if (value == null) {
            return urls;
        }
        try {
            for (AccessDescription description : AuthorityInformationAccess.getInstance(value).getAccessDescriptions()) {
                if (method.equals(description.getAccessMethod())) {
                    addUriName(urls, description.getAccessLocation());
                }
            }
        } catch (Exception e) {
            Constants.LOGGER.log(Level.FINEST, "Unparseable authority information access", e);
        }
        return urls;
    }

    private static void addUriName(List<String> urls, GeneralName name) {
        if (name != null && name.getTagNo() == GeneralName.uniformResourceIdentifier
                && name.getName() instanceof ASN1String) {
            urls.add(((ASN1String) name.getName()).getString());
        }
    }

    private static ASN1Primitive extensionValue(X509Certificate cert, ASN1ObjectIdentifier oid) {
        final byte[] encoded = cert.getExtensionValue(oid.getId());
        if (encoded == null) {
            return null;
        }
        try {
            return JcaX509ExtensionUtils.parseExtensionValue(encoded);
        } catch (Exception e) {
            Constants.LOGGER.log(Level.FINEST, "Unparseable certificate extension " + oid, e);
            return null;
        }
    }

    private static String qcStatementName(String oid) {
        for (String[] mapping : QC_STATEMENT_NAMES) {
            if (mapping[0].equals(oid)) {
                return mapping[1];
            }
        }
        return oid;
    }

    /**
     * Renders a point in time the way the signing diagnostics do: UTC, so log lines from different machines
     * (and from the certificate, CRL and OCSP sides of the same run) compare directly.
     *
     * @param date the instant, may be {@code null}
     * @return the formatted timestamp, or {@code "?"} for {@code null}
     */
    public static String formatTimestamp(Date date) {
        return date == null ? "?" : TIMESTAMP.format(date.toInstant());
    }

    private static void appendIfNotEmpty(StringBuilder buf, String indent, String caption, String value) {
        if (!value.isEmpty()) {
            buf.append(System.lineSeparator()).append(indent).append(caption).append(value);
        }
    }

    /** Flags the two states that make a chain fail validation for a reason unrelated to trust or revocation. */
    private static String expiryNote(X509Certificate cert) {
        final Instant now = Instant.now();
        if (now.isAfter(cert.getNotAfter().toInstant())) {
            return " (EXPIRED)";
        }
        if (now.isBefore(cert.getNotBefore().toInstant())) {
            return " (NOT YET VALID)";
        }
        return "";
    }
}
