package net.sf.jsignpdf.types;

import net.sf.jsignpdf.ResourceProvider;

import com.lowagie.text.pdf.PdfSignatureAppearance;

/**
 * Enum for visible sign rendering configuration
 * @author Josef Cacek
 */
public enum RenderMode {

	DESCRTIPTION_ONLY ("render.descriptionOnly",
		PdfSignatureAppearance.SignatureRenderDescription),
	GRAPHIC_AND_DESCRIPTION ("render.graphicAndDescription",
		PdfSignatureAppearance.SignatureRenderGraphicAndDescription),
	SIGNAME_AND_DESCRIPTION ("render.signameAndDescription",
		PdfSignatureAppearance.SignatureRenderNameAndDescription);

	private String msgKey;
	private int render;

	RenderMode(final String aMsgKey, final int aLevel) {
		msgKey = aMsgKey;
		render = aLevel;
	}

	/**
	 * Returns internationalized description of a right.
	 */
	@Override
	public String toString() {
		return ResourceProvider.getInstance().get(msgKey);
	}

	/**
	 * Returns Visible Signature Render flag.
	 * @return integer flag
	 * @see PdfSignatureAppearance#setRender(int)
	 */
	public int getRender() {
		return render;
	}

}
