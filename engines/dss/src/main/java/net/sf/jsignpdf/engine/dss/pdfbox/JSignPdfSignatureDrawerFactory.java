package net.sf.jsignpdf.engine.dss.pdfbox;

import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxSignatureDrawer;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxSignatureDrawerFactory;

public class JSignPdfSignatureDrawerFactory implements PdfBoxSignatureDrawerFactory {

    @Override
    public PdfBoxSignatureDrawer getSignatureDrawer(SignatureImageParameters imageParameters) {
        return new JSignPdfOverlaySignatureDrawer();
    }
}
