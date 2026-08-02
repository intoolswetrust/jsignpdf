package net.sf.jsignpdf.engine.dss.pdfbox;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.pades.SignatureImageParameters;

public class JSignPdfSignatureImageParameters extends SignatureImageParameters {

    private static final long serialVersionUID = 1L;

    private DSSDocument backgroundImage;
    private float backgroundScale = -1f;

    public DSSDocument getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(DSSDocument backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public float getBackgroundScale() {
        return backgroundScale;
    }

    public void setBackgroundScale(float backgroundScale) {
        this.backgroundScale = backgroundScale;
    }

    @Override
    public boolean isEmpty() {
        return getImage() == null && backgroundImage == null && getTextParameters().isEmpty();
    }
}
