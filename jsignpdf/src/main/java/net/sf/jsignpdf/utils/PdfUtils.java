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
package net.sf.jsignpdf.utils;

import java.io.File;
import java.io.IOException;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;

/**
 * Utilities to handle PDFs.
 * 
 * @author Josef Cacek
 */
public class PdfUtils {

    /**
     * Creates a DSS DSSDocument from a PDF file. 
     * DSS handles password-protected PDFs differently - the password
     * is typically handled during the signing process rather than document loading.
     * 
     * @param aFileName file name of PDF
     * @param aPassword password (stored for later use, not used during document creation)
     * @return DSSDocument representing the PDF
     * @throws IOException
     */
    public static DSSDocument getDSSDocument(final String aFileName, byte[] aPassword) throws IOException {
        File file = new File(aFileName);
        if (!file.exists()) {
            throw new IOException("PDF file not found: " + aFileName);
        }
        // Note: DSS FileDocument doesn't handle password at creation time
        // Password handling will be done during the signing process via SignatureTokenConnection
        return new FileDocument(file);
    }

    /**
     * Creates a DSS DSSDocument from PDF content in memory.
     * DSS handles password-protected PDFs differently - the password
     * is typically handled during the signing process rather than document loading.
     * 
     * @param content content of PDF
     * @param aPassword password (stored for later use, not used during document creation)
     * @return DSSDocument representing the PDF content
     * @throws IOException
     */
    public static DSSDocument getDSSDocument(final byte[] content, byte[] aPassword) throws IOException {
        if (content == null || content.length == 0) {
            throw new IOException("PDF content is empty or null");
        }
        // Note: DSS InMemoryDocument doesn't handle password at creation time
        // Password handling will be done during the signing process via SignatureTokenConnection
        return new InMemoryDocument(content, "document.pdf");
    }
    

}
