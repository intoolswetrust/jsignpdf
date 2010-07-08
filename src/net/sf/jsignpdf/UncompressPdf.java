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
