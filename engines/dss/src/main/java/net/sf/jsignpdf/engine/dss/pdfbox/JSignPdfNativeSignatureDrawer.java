package net.sf.jsignpdf.engine.dss.pdfbox;

import eu.europa.esig.dss.pdf.pdfbox.visible.nativedrawer.NativePdfBoxVisibleSignatureDrawer;
import eu.europa.esig.dss.pdf.visible.SignatureFieldDimensionAndPosition;

/**
 * DSS's stock native drawer with the JSignPdf font-size cap applied. Used for every visible signature
 * that does not need background-image layering, so signatures without a background image stay on DSS's
 * own rendering path while still honouring the configured font size as an upper bound.
 *
 * @see SignatureTextSize#capToPreferredFontSize
 */
public class JSignPdfNativeSignatureDrawer extends NativePdfBoxVisibleSignatureDrawer {

    @Override
    public SignatureFieldDimensionAndPosition buildSignatureFieldBox() {
        return SignatureTextSize.capToPreferredFontSize(super.buildSignatureFieldBox(), parameters);
    }
}
