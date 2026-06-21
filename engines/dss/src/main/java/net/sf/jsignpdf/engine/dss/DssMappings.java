package net.sf.jsignpdf.engine.dss;

import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PadesLevel;

import eu.europa.esig.dss.enumerations.CertificationPermission;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;

/**
 * Translates JSignPdf's backend-neutral model enums onto their DSS counterparts. Keeping these maps in
 * the DSS engine (rather than on the shared model types) preserves the engine-api invariant that no
 * engine library leaks into {@code net.sf.jsignpdf.types.*}.
 *
 * @author Josef Cacek
 */
final class DssMappings {

    private DssMappings() {
    }

    /**
     * Maps a JSignPdf {@link PadesLevel} to a DSS {@link SignatureLevel}. {@code null} defaults to
     * {@link SignatureLevel#PAdES_BASELINE_B}.
     *
     * @param level the JSignPdf level (may be {@code null})
     * @return the DSS signature level
     */
    static SignatureLevel toSignatureLevel(PadesLevel level) {
        if (level == null) {
            return SignatureLevel.PAdES_BASELINE_B;
        }
        switch (level) {
            case BASELINE_B:
                return SignatureLevel.PAdES_BASELINE_B;
            case BASELINE_T:
                return SignatureLevel.PAdES_BASELINE_T;
            case BASELINE_LT:
                return SignatureLevel.PAdES_BASELINE_LT;
            case BASELINE_LTA:
                return SignatureLevel.PAdES_BASELINE_LTA;
            default:
                return SignatureLevel.PAdES_BASELINE_B;
        }
    }

    /**
     * Maps a JSignPdf {@link HashAlgorithm} to a DSS {@link DigestAlgorithm}.
     *
     * @param hash the JSignPdf hash algorithm
     * @return the DSS digest algorithm, or {@code null} when the algorithm is not a valid PAdES digest
     *         (SHA-1 / RIPEMD-160 are rejected by the capability validator before signing, so the engine
     *         should never actually see them)
     */
    static DigestAlgorithm toDigestAlgorithm(HashAlgorithm hash) {
        if (hash == null) {
            return DigestAlgorithm.SHA256;
        }
        switch (hash) {
            case SHA256:
                return DigestAlgorithm.SHA256;
            case SHA384:
                return DigestAlgorithm.SHA384;
            case SHA512:
                return DigestAlgorithm.SHA512;
            default:
                return null; // SHA1 / RIPEMD160 are not PAdES digests
        }
    }

    /**
     * Maps a JSignPdf {@link CertificationLevel} (DocMDP) to a DSS {@link CertificationPermission}.
     *
     * @param level the certification level
     * @return the DSS permission, or {@code null} for a non-certifying (approval) signature
     */
    static CertificationPermission toCertificationPermission(CertificationLevel level) {
        if (level == null) {
            return null;
        }
        switch (level) {
            case CERTIFIED_NO_CHANGES_ALLOWED:
                return CertificationPermission.NO_CHANGE_PERMITTED;
            case CERTIFIED_FORM_FILLING:
                return CertificationPermission.MINIMAL_CHANGES_PERMITTED;
            case CERTIFIED_FORM_FILLING_AND_ANNOTATIONS:
                return CertificationPermission.CHANGES_PERMITTED;
            default:
                return null;
        }
    }
}
