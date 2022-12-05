package com.github.intoolswetrust.jsignpdf.pades.config;

import eu.europa.esig.dss.enumerations.SignatureLevel;

public enum PadesLevel {
    BASELINE_B(SignatureLevel.PAdES_BASELINE_B), BASELINE_T(SignatureLevel.PAdES_BASELINE_T), BASELINE_LT(
            SignatureLevel.PAdES_BASELINE_LT), BASELINE_LTA(SignatureLevel.PAdES_BASELINE_LTA);

    private final SignatureLevel signatureLevel;

    public SignatureLevel getSignatureLevel() {
        return signatureLevel;
    }

    PadesLevel(SignatureLevel signatureLevel) {
        this.signatureLevel = signatureLevel;
    }

}
