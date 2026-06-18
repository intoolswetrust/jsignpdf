package net.sf.jsignpdf.engine;

/**
 * Fine-grained capabilities a {@link SigningEngine} may support. An engine returns the subset it
 * implements from {@link SigningEngine#capabilities()}; a capability that is absent from that set is
 * considered unsupported. The CLI uses this to fail fast on a mismatch and the JavaFX UI uses it to
 * disable controls the active engine cannot honour.
 *
 * @author Josef Cacek
 */
public enum Capability {

    // signature container / format
    SUBFILTER_ADBE_PKCS7_DETACHED,
    SUBFILTER_ETSI_CADES_DETACHED, // PAdES baseline subfilter
    PADES_BASELINE_B,
    PADES_BASELINE_T,
    PADES_BASELINE_LT,
    PADES_BASELINE_LTA,

    // hashes
    HASH_SHA1,
    HASH_SHA256,
    HASH_SHA384,
    HASH_SHA512,
    HASH_RIPEMD160,

    // document-level
    OVERWRITE_MODE, // rewrite the document non-incrementally; incremental append is universal and needs no capability
    CERTIFICATION_LEVEL, // DocMDP: all four levels
    ENCRYPTION_PASSWORD,
    ENCRYPTION_CERTIFICATE,
    PERMISSIONS_BITMASK, // print / copy / assembly / ...

    // visible signature
    VISIBLE_SIGNATURE,
    VISIBLE_LAYER2_TEXT, // %signer / %timestamp / ...
    VISIBLE_LAYER4_TEXT,
    VISIBLE_RENDER_MODE_DESCRIPTION_ONLY,
    VISIBLE_RENDER_MODE_GRAPHIC_AND_DESCRIPTION,
    VISIBLE_RENDER_MODE_NAME_AND_DESCRIPTION,
    VISIBLE_BACKGROUND_IMAGE,
    VISIBLE_BACKGROUND_IMAGE_SCALE,
    VISIBLE_SIGNATURE_GRAPHIC,
    VISIBLE_CUSTOM_FONT,
    ACRO6_LAYERS,

    // revocation / timestamp
    TSA,
    TSA_POLICY_OID,
    TSA_BASIC_AUTH,
    OCSP_EMBED,
    CRL_EMBED,
    DSS_DICTIONARY, // PAdES-LT/LTA prerequisite

    // transport
    PROXY_SUPPORT,

    // keystores / external signing
    EXTERNAL_DIGEST, // CloudFoxy-style externally-produced signature
    PKCS11_PROVIDER
}
