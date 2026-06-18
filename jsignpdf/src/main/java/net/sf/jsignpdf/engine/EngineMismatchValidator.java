package net.sf.jsignpdf.engine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PadesLevel;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.AppConfig;
import net.sf.jsignpdf.utils.PKCS11Utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Checks a populated {@link BasicSignerOptions} against the capability set of the engine that will sign
 * it, before any I/O happens. Each option that is set to a non-default value while the active engine
 * lacks the matching {@link Capability} produces a {@link Mismatch}. The same table drives the CLI
 * fail-fast and (via {@link #capabilityFor} helpers) the FX control gating.
 *
 * <p>
 * The table is intentionally conservative: visible-signature and TSA sub-options are only evaluated
 * when their umbrella capability ({@link Capability#VISIBLE_SIGNATURE} / {@link Capability#TSA}) is
 * present, mirroring the section-vs-field granularity of the FX UI.
 * </p>
 *
 * @author Josef Cacek
 */
public final class EngineMismatchValidator {

    /**
     * A single option/capability mismatch.
     *
     * @param option a short, stable label for the offending option (CLI flag where one exists)
     * @param capability the capability the active engine is missing
     */
    public record Mismatch(String option, Capability capability) {
    }

    private static final Map<HashAlgorithm, Capability> HASH_CAPS = new EnumMap<>(HashAlgorithm.class);
    static {
        HASH_CAPS.put(HashAlgorithm.SHA1, Capability.HASH_SHA1);
        HASH_CAPS.put(HashAlgorithm.SHA256, Capability.HASH_SHA256);
        HASH_CAPS.put(HashAlgorithm.SHA384, Capability.HASH_SHA384);
        HASH_CAPS.put(HashAlgorithm.SHA512, Capability.HASH_SHA512);
        HASH_CAPS.put(HashAlgorithm.RIPEMD160, Capability.HASH_RIPEMD160);
    }

    private static final Map<RenderMode, Capability> RENDER_CAPS = new EnumMap<>(RenderMode.class);
    static {
        RENDER_CAPS.put(RenderMode.DESCRIPTION_ONLY, Capability.VISIBLE_RENDER_MODE_DESCRIPTION_ONLY);
        RENDER_CAPS.put(RenderMode.GRAPHIC_AND_DESCRIPTION, Capability.VISIBLE_RENDER_MODE_GRAPHIC_AND_DESCRIPTION);
        RENDER_CAPS.put(RenderMode.SIGNAME_AND_DESCRIPTION, Capability.VISIBLE_RENDER_MODE_NAME_AND_DESCRIPTION);
    }

    private static final Map<PadesLevel, Capability> PADES_CAPS = new EnumMap<>(PadesLevel.class);
    static {
        PADES_CAPS.put(PadesLevel.BASELINE_B, Capability.PADES_BASELINE_B);
        PADES_CAPS.put(PadesLevel.BASELINE_T, Capability.PADES_BASELINE_T);
        PADES_CAPS.put(PadesLevel.BASELINE_LT, Capability.PADES_BASELINE_LT);
        PADES_CAPS.put(PadesLevel.BASELINE_LTA, Capability.PADES_BASELINE_LTA);
    }

    private EngineMismatchValidator() {
    }

    /**
     * Returns the list of option/capability mismatches between the given options and engine. Empty when
     * the engine can honour every option that is set.
     *
     * @param o the populated signing options
     * @param engine the engine that would sign
     * @return the (possibly empty) list of mismatches
     */
    public static List<Mismatch> findMismatches(BasicSignerOptions o, SigningEngine engine) {
        final List<Mismatch> out = new ArrayList<>();
        final var caps = engine.capabilities();

        // hash algorithm
        final HashAlgorithm hash = o.getHashAlgorithmX();
        final Capability hashCap = HASH_CAPS.get(hash);
        if (hashCap != null && !caps.contains(hashCap)) {
            out.add(new Mismatch("--hash-algorithm", hashCap));
        }

        // PAdES baseline level
        if (o.getPadesLevel() != null) {
            final Capability padesCap = PADES_CAPS.get(o.getPadesLevel());
            if (padesCap != null && !caps.contains(padesCap)) {
                out.add(new Mismatch("--pades-level", padesCap));
            }
        }

        // overwrite (non-incremental) mode — incremental append is universal, so only a request to
        // overwrite the document (append disabled) against an engine that can't do it is a mismatch.
        if (!o.isAppendX() && !caps.contains(Capability.OVERWRITE_MODE)) {
            out.add(new Mismatch("--append", Capability.OVERWRITE_MODE));
        }

        // certification level
        if (o.getCertLevelX() != net.sf.jsignpdf.types.CertificationLevel.NOT_CERTIFIED
                && !caps.contains(Capability.CERTIFICATION_LEVEL)) {
            out.add(new Mismatch("--cert-level", Capability.CERTIFICATION_LEVEL));
        }

        // encryption + permissions
        if (o.isAdvanced() && o.getPdfEncryption() == PDFEncryption.PASSWORD
                && !caps.contains(Capability.ENCRYPTION_PASSWORD)) {
            out.add(new Mismatch("--encryption (password)", Capability.ENCRYPTION_PASSWORD));
        }
        if (o.isAdvanced() && o.getPdfEncryption() == PDFEncryption.CERTIFICATE
                && !caps.contains(Capability.ENCRYPTION_CERTIFICATE)) {
            out.add(new Mismatch("--encryption (certificate)", Capability.ENCRYPTION_CERTIFICATE));
        }
        if (o.isAdvanced() && o.getPdfEncryption() != PDFEncryption.NONE && hasNonDefaultPermissions(o)
                && !caps.contains(Capability.PERMISSIONS_BITMASK)) {
            out.add(new Mismatch("--print-right / permissions", Capability.PERMISSIONS_BITMASK));
        }

        // visible signature (umbrella + fields)
        if (o.isVisible()) {
            if (!caps.contains(Capability.VISIBLE_SIGNATURE)) {
                out.add(new Mismatch("--visible-signature", Capability.VISIBLE_SIGNATURE));
            } else {
                if (o.getImgPath() != null && !caps.contains(Capability.VISIBLE_SIGNATURE_GRAPHIC)) {
                    out.add(new Mismatch("--img-path", Capability.VISIBLE_SIGNATURE_GRAPHIC));
                }
                if (o.getBgImgPath() != null && !caps.contains(Capability.VISIBLE_BACKGROUND_IMAGE)) {
                    out.add(new Mismatch("--bg-path", Capability.VISIBLE_BACKGROUND_IMAGE));
                }
                if (o.getBgImgScale() != Constants.DEFVAL_BG_SCALE && !caps.contains(Capability.VISIBLE_BACKGROUND_IMAGE_SCALE)) {
                    out.add(new Mismatch("--bg-scale", Capability.VISIBLE_BACKGROUND_IMAGE_SCALE));
                }
                if (o.getL2Text() != null && !caps.contains(Capability.VISIBLE_LAYER2_TEXT)) {
                    out.add(new Mismatch("--l2-text", Capability.VISIBLE_LAYER2_TEXT));
                }
                if (o.getL4Text() != null && !caps.contains(Capability.VISIBLE_LAYER4_TEXT)) {
                    out.add(new Mismatch("--l4-text", Capability.VISIBLE_LAYER4_TEXT));
                }
                final Capability renderCap = RENDER_CAPS.get(o.getRenderMode());
                if (renderCap != null && !caps.contains(renderCap)) {
                    out.add(new Mismatch("--render-mode", renderCap));
                }
                if (AppConfig.fontPath() != null && !caps.contains(Capability.VISIBLE_CUSTOM_FONT)) {
                    out.add(new Mismatch("font.path (advanced.properties)", Capability.VISIBLE_CUSTOM_FONT));
                }
                if (!o.isAcro6Layers() && !caps.contains(Capability.ACRO6_LAYERS)) {
                    out.add(new Mismatch("--disable-acro6layers", Capability.ACRO6_LAYERS));
                }
            }
        }

        // TSA (umbrella + fields)
        if (o.isTimestampX()) {
            if (!caps.contains(Capability.TSA)) {
                out.add(new Mismatch("--tsa-url", Capability.TSA));
            } else {
                if (StringUtils.isNotEmpty(o.getTsaPolicy()) && !caps.contains(Capability.TSA_POLICY_OID)) {
                    out.add(new Mismatch("--tsa-policy-oid", Capability.TSA_POLICY_OID));
                }
                if (o.getTsaServerAuthn() == ServerAuthentication.PASSWORD && !caps.contains(Capability.TSA_BASIC_AUTH)) {
                    out.add(new Mismatch("--tsa-authentication", Capability.TSA_BASIC_AUTH));
                }
            }
        }

        // OCSP / CRL
        if (o.isOcspEnabledX() && !caps.contains(Capability.OCSP_EMBED)) {
            out.add(new Mismatch("--ocsp", Capability.OCSP_EMBED));
        }
        if (o.isCrlEnabledX() && !caps.contains(Capability.CRL_EMBED)) {
            out.add(new Mismatch("--crl", Capability.CRL_EMBED));
        }

        // proxy
        if (o.isAdvanced() && o.getProxyType() != java.net.Proxy.Type.DIRECT && !caps.contains(Capability.PROXY_SUPPORT)) {
            out.add(new Mismatch("--proxy-type", Capability.PROXY_SUPPORT));
        }

        // keystore type: external (CloudFoxy) / pkcs11
        final String ksType = o.getKsType();
        if (StringUtils.equalsIgnoreCase(ksType, Constants.KEYSTORE_TYPE_CLOUDFOXY)
                && !caps.contains(Capability.EXTERNAL_DIGEST)) {
            out.add(new Mismatch("--key-store-type " + Constants.KEYSTORE_TYPE_CLOUDFOXY, Capability.EXTERNAL_DIGEST));
        } else if (PKCS11Utils.getProviderNameForKeystoreType(ksType) != null
                && !caps.contains(Capability.PKCS11_PROVIDER)) {
            out.add(new Mismatch("--key-store-type " + ksType, Capability.PKCS11_PROVIDER));
        }

        return out;
    }

    private static boolean hasNonDefaultPermissions(BasicSignerOptions o) {
        return o.getRightPrinting() != PrintRight.ALLOW_PRINTING || o.isRightCopy() || o.isRightAssembly()
                || o.isRightFillIn() || o.isRightScreanReaders() || o.isRightModifyAnnotations()
                || o.isRightModifyContents();
    }
}
