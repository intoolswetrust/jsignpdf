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

import net.sf.jsignpdf.types.FloatPoint;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;

/**
 * Provides additional information for selected input PDF file.
 * 
 * @author Josef Cacek
 */
public class PdfExtraInfo {

	private BasicSignerOptions options;

	/**
	 * @param anOptions
	 */
	public PdfExtraInfo(BasicSignerOptions anOptions) {
		options = anOptions;
	}

	/**
	 * Returns number of pages in PDF document. If error occures (file not found
	 * or sth. similar) -1 is returned.
	 * 
	 * @return number of pages (or -1 if error occures)
	 */
	public int getNumberOfPages() {
		int tmpResult = 0;
		PdfReader reader = null;
		try {
			try {
				reader = new PdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
			} catch (Exception e) {
				try {
					reader = new PdfReader(options.getInFile(), new byte[0]);
				} catch (Exception e2) {
					// try to read without password
					reader = new PdfReader(options.getInFile());
				}
			}
			tmpResult = reader.getNumberOfPages();
		} catch (Exception e) {
			tmpResult = -1;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
				}
			}
		}

		return tmpResult;
	}

	/**
	 * Returns page size identified by upper rigth corner position. If no error
	 * occures FloatPoint with x as a width and y as a height is returned, null
	 * otherwise.
	 * 
	 * @param aPage
	 *            number of page for which size should be returned
	 * @return FloatPoint or null
	 */
	public FloatPoint getPageSize(int aPage) {
		FloatPoint tmpResult = null;
		PdfReader reader = null;
		try {
			try {
				reader = new PdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
			} catch (Exception e) {
				try {
					reader = new PdfReader(options.getInFile(), new byte[0]);
				} catch (Exception e2) {
					// try to read without password
					reader = new PdfReader(options.getInFile());
				}
			}
			final Rectangle tmpRect = reader.getPageSize(aPage);
			if (tmpRect != null) {
				tmpResult = new FloatPoint(tmpRect.getRight(), tmpRect.getTop());
			}
		} catch (Exception e) {
			// nothing to do
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
				}
			}
		}

		return tmpResult;
	}
}
