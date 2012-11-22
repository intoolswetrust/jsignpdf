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
package net.sf.jsignpdf.preview;

import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import net.sf.jsignpdf.BasicSignerOptions;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFParseException;
import com.sun.pdfview.decrypt.PDFPassword;

/**
 * Helper class for converting a page in PDF to a {@link BufferedImage} object.
 * 
 * @author Josef Cacek
 */
public class Pdf2Image {

	private BasicSignerOptions options;

	/**
	 * Constructor - gets an options object with configured input PDF and
	 * possibly deconding (owner) password.
	 * 
	 * @param anOpts
	 */
	public Pdf2Image(BasicSignerOptions anOpts) {
		if (anOpts == null)
			throw new NullPointerException("Options have to be not-null");
		options = anOpts;
	}

	/**
	 * Returns an image preview of given page.
	 * 
	 * @param aPage
	 *            Page to preview (counted from 1)
	 * @return image or null if error occures.
	 */
	public BufferedImage getImageForPage(final int aPage) {
		BufferedImage tmpResult = getImageUsingPdfRenderer(aPage);
		if (tmpResult == null) {
			tmpResult = getImageUsingPdfBox(aPage);
		}
		return tmpResult;
	}

	/**
	 * Returns image (or null if failed) generated from given page in PDF using
	 * PDFBox tool.
	 * 
	 * @param aPage
	 *            page in PDF (1 based)
	 * @return image or null
	 */
	public BufferedImage getImageUsingPdfRenderer(final int aPage) {
		BufferedImage tmpResult = null;
		try {
			// load a pdf from a byte buffer
			File file = new File(options.getInFile());
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			FileChannel channel = raf.getChannel();
			ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
			PDFFile pdffile = null;
			try {
				// try to read PDF with owner password
				pdffile = new PDFFile(buf, new PDFPassword(options.getPdfOwnerPwdStrX()));
			} catch (PDFParseException ppe) {
				try {
					// try to read PDF with empty password
					pdffile = new PDFFile(buf, new PDFPassword(""));
				} catch (PDFParseException ppe2) {
					// try to read PDF without password
					pdffile = new PDFFile(buf);
				}
			}

			// draw the page to an image
			PDFPage page = pdffile.getPage(aPage);

			// get the width and height for the doc at the default zoom
			Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());

			// generate the image
			tmpResult = (BufferedImage) page.getImage(rect.width, rect.height, rect, // clip
					// rect
					null, // null for the ImageObserver
					true, // fill background with white
					true // block until drawing is done
					);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tmpResult;
	}

	/**
	 * Returns image (or null if failed) generated from given page in PDF using
	 * PDFBox tool.
	 * 
	 * @param aPage
	 *            page in PDF (1 based)
	 * @return image or null
	 */
	public BufferedImage getImageUsingPdfBox(final int aPage) {
		BufferedImage tmpResult = null;
		PDDocument tmpDoc = null;

		try {
			tmpDoc = PDDocument.load(options.getInFile());
			if (tmpDoc.isEncrypted()) {
				tmpDoc.decrypt(options.getPdfOwnerPwdStrX());
			}
			int resolution;
			try {
				resolution = Toolkit.getDefaultToolkit().getScreenResolution();
			} catch (HeadlessException e) {
				resolution = 96;
			}

			final PDPage page = (PDPage) tmpDoc.getDocumentCatalog().getAllPages().get(aPage - 1);
			tmpResult = page.convertToImage(BufferedImage.TYPE_INT_RGB, resolution);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (tmpDoc != null) {
				try {
					tmpDoc.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return tmpResult;
	}
}
