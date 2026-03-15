/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 * 
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 * 
 * Contributor(s): Josef Cacek.
 * 
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf.types;

import static java.util.stream.Collectors.joining;
import static net.sf.jsignpdf.types.PdfVersion.PDF_1_3;
import static net.sf.jsignpdf.types.PdfVersion.PDF_1_6;
import static net.sf.jsignpdf.types.PdfVersion.PDF_1_7;

import java.util.stream.Stream;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;

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
     * Converts to DSS {@link DigestAlgorithm}.
     *
     * @return DSS DigestAlgorithm
     */
    public DigestAlgorithm toDssDigestAlgorithm() {
        switch (this) {
            case SHA1:
                return DigestAlgorithm.SHA1;
            case SHA256:
                return DigestAlgorithm.SHA256;
            case SHA384:
                return DigestAlgorithm.SHA384;
            case SHA512:
                return DigestAlgorithm.SHA512;
            case RIPEMD160:
                return DigestAlgorithm.RIPEMD160;
            default:
                throw new IllegalArgumentException("Unsupported hash algorithm: " + this);
        }
    }
}
