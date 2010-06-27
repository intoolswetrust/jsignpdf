package net.sf.jsignpdf.types;

import com.lowagie.text.pdf.PdfWriter;

/**
 * Enum of hash algorithms supported in PDF signatures.
 * 
 * @author Josef Cacek
 */
public enum HashAlgorithm {
	SHA1("SHA-1", PdfWriter.VERSION_1_3),
	SHA256("SHA-256", PdfWriter.VERSION_1_6),
	SHA384("SHA-384", PdfWriter.VERSION_1_7),
	SHA512("SHA-512", PdfWriter.VERSION_1_7),
	RIPEMD160("RIPEMD160", PdfWriter.VERSION_1_7);

	private final char pdfVersion;
	private final String algorithmName;

	private HashAlgorithm(final String aName, char aVersion) {
		algorithmName = aName;
		pdfVersion = aVersion;
	}

	/**
	 * Gets algorithm name.
	 * 
	 * @return
	 */
	public String getAlgorithmName() {
		return algorithmName;
	}

	/**
	 * Gets minimal PDF version supporting the algorithm.
	 * 
	 * @return
	 */
	public char getPdfVersion() {
		return pdfVersion;
	}
}
