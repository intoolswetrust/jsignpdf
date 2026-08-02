package net.sf.jsignpdf.engine.dss.pdfbox;

import eu.europa.esig.dss.pdf.AbstractPdfObjFactory;
import eu.europa.esig.dss.pdf.PDFServiceMode;
import eu.europa.esig.dss.pdf.PDFSignatureService;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxSignatureService;

public class JSignPdfPdfObjFactory extends AbstractPdfObjFactory {

    @Override
    public PDFSignatureService newPAdESSignatureService() {
        return configure(new PdfBoxSignatureService(PDFServiceMode.SIGNATURE, new JSignPdfSignatureDrawerFactory()));
    }

    @Override
    public PDFSignatureService newContentTimestampService() {
        return configure(new PdfBoxSignatureService(PDFServiceMode.CONTENT_TIMESTAMP, new JSignPdfSignatureDrawerFactory()));
    }

    @Override
    public PDFSignatureService newSignatureTimestampService() {
        return configure(new PdfBoxSignatureService(PDFServiceMode.SIGNATURE_TIMESTAMP, new JSignPdfSignatureDrawerFactory()));
    }

    @Override
    public PDFSignatureService newArchiveTimestampService() {
        return configure(new PdfBoxSignatureService(PDFServiceMode.ARCHIVE_TIMESTAMP, new JSignPdfSignatureDrawerFactory()));
    }
}
