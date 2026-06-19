package net.sf.jsignpdf.types;

import java.util.Locale;

/**
 * PAdES baseline signature levels (ETSI EN 319 142). This is the single new per-document field the
 * DSS engine needs; it is selected by the user through {@code --pades-level} / the FX dropdown and
 * honoured by engines that declare the matching {@code PADES_BASELINE_*} capability.
 *
 * <p>
 * The type is intentionally backend-neutral &mdash; it carries no DSS (or any other signing library)
 * dependency, preserving the engine-api invariant that no engine library leaks into the shared model.
 * The DSS engine maps these constants onto {@code eu.europa.esig.dss.enumerations.SignatureLevel}
 * internally.
 * </p>
 *
 * @author Josef Cacek
 */
public enum PadesLevel {

    /** Basic signature. */
    BASELINE_B,
    /** B + signature timestamp. */
    BASELINE_T,
    /** T + validation material (certificates, OCSP/CRL) embedded for long-term validation. */
    BASELINE_LT,
    /** LT + archive timestamp. */
    BASELINE_LTA;

    /**
     * Parses a short, case-insensitive token ({@code B} / {@code T} / {@code LT} / {@code LTA}) or the
     * full enum name ({@code BASELINE_B} ...) into a {@link PadesLevel}.
     *
     * @param value the token, may be {@code null}
     * @return the matching level, or {@code null} when {@code value} is {@code null} or unrecognised
     */
    public static PadesLevel fromString(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toUpperCase(Locale.ENGLISH);
        switch (v) {
            case "B":
                return BASELINE_B;
            case "T":
                return BASELINE_T;
            case "LT":
                return BASELINE_LT;
            case "LTA":
                return BASELINE_LTA;
            default:
                try {
                    return PadesLevel.valueOf(v);
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }

    /**
     * @return the short spec token for this level ({@code B} / {@code T} / {@code LT} / {@code LTA})
     */
    public String shortName() {
        return name().substring("BASELINE_".length());
    }
}
