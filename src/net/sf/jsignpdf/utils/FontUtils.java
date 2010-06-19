package net.sf.jsignpdf.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sf.jsignpdf.Constants;

import com.lowagie.text.pdf.BaseFont;

/**
 * Utilities for handling fonts in visible signature.
 * 
 * @author Josef Cacek
 */
public class FontUtils {

	public static BaseFont l2baseFont;

	/**
	 * Returns BaseFont for text of visible signature;
	 * 
	 * @return
	 */
	public static synchronized BaseFont getL2BaseFont() {
		if (l2baseFont == null) {
			final ConfigProvider conf = ConfigProvider.getInstance();
			try {
				final ByteArrayOutputStream tmpBaos = new ByteArrayOutputStream();
				String fontPath = conf.getNotEmptyProperty("font.path", null);
				String fontName;
				String fontEncoding;
				InputStream tmpIs;
				if (fontPath != null) {
					fontName = conf.getNotEmptyProperty("font.name", null);
					if (fontName == null) {
						fontName = new File(fontPath).getName();
					}
					fontEncoding = conf.getNotEmptyProperty("font.encoding", null);
					if (fontEncoding == null) {
						fontEncoding = BaseFont.WINANSI;
					}
					tmpIs = new FileInputStream(fontPath);
				} else {
					fontName = Constants.L2TEXT_FONT_NAME;
					fontEncoding = BaseFont.IDENTITY_H;
					tmpIs = FontUtils.class.getResourceAsStream(Constants.L2TEXT_FONT_PATH);
				}
				IOUtils.copy(tmpIs, tmpBaos);
				tmpIs.close();
				tmpBaos.close();
				l2baseFont = BaseFont.createFont(fontName, fontEncoding, BaseFont.EMBEDDED, BaseFont.CACHED, tmpBaos
						.toByteArray(), null);
			} catch (Exception e) {
				e.printStackTrace();
				try {
					l2baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
				} catch (Exception ex) {
					// where is the problem, dear Watson?
				}
			}
		}
		return l2baseFont;
	}

}
