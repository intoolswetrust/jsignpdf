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
package net.sf.jsignpdf;

import java.io.FileOutputStream;
import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

/**
 * Simple small programm to uncompress PDFs.
 * 
 * @author Josef Cacek
 */
public class UncompressPdf {

	/**
	 * The main 'main'.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			System.out.println("Usage:\njava " + UncompressPdf.class.getName() + " file.pdf [file2.pdf [...]]");
			return;
		}
		Document.compress = false;
		for (String tmpFile : args) {
			String newFileName = null;
			if (tmpFile.toLowerCase().endsWith(".pdf")) {
				newFileName = tmpFile.substring(0, tmpFile.length() - 4) + "_uncompressed.pdf";
			} else {
				newFileName = tmpFile + "_uncompressed.pdf";
			}
			System.out.println("Uncompressing " + tmpFile + " to " + newFileName);
			try {
				PdfReader reader = new PdfReader(tmpFile);
				PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(newFileName), '\0');
				int total = reader.getNumberOfPages() + 1;
				for (int i = 1; i < total; i++) {
					reader.setPageContent(i, reader.getPageContent(i));
				}
				stamper.close();
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			} catch (DocumentException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
