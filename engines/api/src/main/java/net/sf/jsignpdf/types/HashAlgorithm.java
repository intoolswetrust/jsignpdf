package net.sf.jsignpdf.types;

import static java.util.stream.Collectors.joining;
import static net.sf.jsignpdf.types.PdfVersion.PDF_1_3;
import static net.sf.jsignpdf.types.PdfVersion.PDF_1_6;
import static net.sf.jsignpdf.types.PdfVersion.PDF_1_7;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum of hash algorithms supported in PDF signatures.
 * 
 * @author Josef Cacek
 */
public enum HashAlgorithm {
    SHA1("SHA-1", PDF_1_3), SHA256("SHA-256", PDF_1_6), SHA384("SHA-384", PDF_1_7), SHA512("SHA-512",
            PDF_1_7), RIPEMD160("RIPEMD160", PDF_1_7);

    private final PdfVersion pdfVersion;
    private final String algorithmName;

    private HashAlgorithm(final String aName, PdfVersion aVersion) {
        algorithmName = aName;
        pdfVersion = aVersion;
    }

    /**
     * Gets algorithm name.
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Gets minimal PDF version supporting the algorithm.
     */
    public PdfVersion getPdfVersion() {
        return pdfVersion;
    }

    public String toStringWithPdfVersion() {
        return algorithmName + " (" + pdfVersion.getVersionName() + ")";
    }

    public static String valuesWithPdfVersionAsString() {
        return Stream.of(values()).map(ha -> ha.toStringWithPdfVersion()).collect(joining(", "));
    }

    /**
     * Resolves a {@link HashAlgorithm} by its algorithm name (case-insensitive,
     * e.g. {@code "SHA-256"} or {@code "sha256"}). Returns {@code null} if the
     * name is blank or does not match any supported algorithm.
     */
    public static HashAlgorithm fromAlgorithmName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.replace("-", "").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        for (HashAlgorithm ha : values()) {
            if (ha.getAlgorithmName().replace("-", "").equalsIgnoreCase(normalized)) {
                return ha;
            }
        }
        return null;
    }
}
