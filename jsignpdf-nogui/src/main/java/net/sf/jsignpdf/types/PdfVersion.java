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

import com.lowagie.text.pdf.PdfWriter;

/**
 * Enum of PDF versions
 * 
 * @author Josef Cacek
 */
public enum PdfVersion {
    PDF_1_2("PDF-1.2", PdfWriter.VERSION_1_2), PDF_1_3("PDF-1.3", PdfWriter.VERSION_1_3), PDF_1_4("PDF-1.4",
            PdfWriter.VERSION_1_4), PDF_1_5("PDF-1.5", PdfWriter.VERSION_1_5), PDF_1_6("PDF-1.6",
                    PdfWriter.VERSION_1_6), PDF_1_7("PDF-1.7", PdfWriter.VERSION_1_7);

    private final String name;
    private final char charVersion;

    private PdfVersion(final String aName, char aVersion) {
        name = aName;
        charVersion = aVersion;
    }

    /**
     * Gets version name.
     */
    public String getVersionName() {
        return name;
    }

    /**
     * Gets version as char (representation in PdfReader and PdfWriter).
     */
    public char getCharVersion() {
        return charVersion;
    }

    public static PdfVersion fromCharVersion(char ver) {
        for (PdfVersion pdfVer: PdfVersion.values()) {
            if (pdfVer.getCharVersion() == ver) {
                return pdfVer;
            }
        }
        return null;
    }
}
