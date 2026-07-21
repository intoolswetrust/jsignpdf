package net.sf.jsignpdf.engine.dss.pdfbox;

import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxSignatureDrawer;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxSignatureDrawerFactory;
import eu.europa.esig.dss.pdf.pdfbox.visible.nativedrawer.NativePdfBoxVisibleSignatureDrawer;

/**
 * Factory that selects the right signature drawer based on the input parameters.
 * Only routes to the custom {@link JSignPdfOverlaySignatureDrawer} when a background
 * image is actually configured; otherwise falls back to DSS's stock
 * {@link NativePdfBoxVisibleSignatureDrawer}. This quarantines the fragile fork so
 * a DSS upgrade that breaks it cannot break the default signing path, and keeps
 * PDF/A compliance automatic for signatures without a background image.
 */
public class JSignPdfSignatureDrawerFactory implements PdfBoxSignatureDrawerFactory {

    @Override
    public PdfBoxSignatureDrawer getSignatureDrawer(SignatureImageParameters imageParameters) {
        if (imageParameters instanceof JSignPdfSignatureImageParameters jp && jp.getBackgroundImage() != null) {
            return new JSignPdfOverlaySignatureDrawer();
        }
        return new NativePdfBoxVisibleSignatureDrawer();
    }
}
