package net.sf.jsignpdf.engine.dss.pdfbox;

import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pdf.visible.SignatureFieldDimensionAndPosition;

/**
 * Shared font-size handling for the drawers returned by {@link JSignPdfSignatureDrawerFactory}.
 */
final class SignatureTextSize {

    private SignatureTextSize() {
    }

    /**
     * Caps the DSS-calculated text size at the user's preferred font size. With
     * {@code TextWrapping.FILL_BOX_AND_LINEBREAK} DSS grows the font until it fills the signature
     * rectangle, which produces oversized text on large boxes. Applying the configured size as an upper
     * bound keeps the auto-scaling that saves small boxes from clipped text, while honouring the font size
     * the user asked for. Both drawers apply it so the behaviour does not depend on whether a background
     * image happens to be configured.
     *
     * @param dimensionAndPosition the box computed by DSS; mutated in place
     * @param parameters the visible signature parameters holding the preferred font size
     * @return the same {@code dimensionAndPosition} instance, for call chaining
     */
    static SignatureFieldDimensionAndPosition capToPreferredFontSize(
            SignatureFieldDimensionAndPosition dimensionAndPosition, SignatureImageParameters parameters) {
        SignatureImageTextParameters textParams = parameters == null ? null : parameters.getTextParameters();
        if (textParams != null && textParams.getFont() != null) {
            float preferredSize = textParams.getFont().getSize();
            if (preferredSize > 0f && dimensionAndPosition.getTextSize() > preferredSize) {
                dimensionAndPosition.setTextSize(preferredSize);
            }
        }
        return dimensionAndPosition;
    }
}
