package net.sf.jsignpdf.utils;

import java.io.IOException;

import com.lowagie.text.pdf.PdfReader;

/**
 * Utilities to handle PDFs.
 * 
 * @author Josef Cacek
 */
public class PdfUtils {

  /**
   * It tries to create PDF reader in 3 steps:
   * <ul>
   * <li>without password</li>
   * <li>with empty password</li>
   * <li>with given password</li>
   * </ul>
   * 
   * @param aFileName
   *          file name of PDF
   * @param aPassword
   *          password
   * @return
   * @throws IOException
   */
  public static PdfReader getPdfReader(final String aFileName, byte[] aPassword) throws IOException {
    PdfReader tmpReader = null;
    try {
      // try to read without password
      tmpReader = new PdfReader(aFileName);
    } catch (Exception e) {
      try {
        tmpReader = new PdfReader(aFileName, new byte[0]);
      } catch (Exception e2) {
        tmpReader = new PdfReader(aFileName, aPassword);
      }
    }
    return tmpReader;
  }

  /**
   * It tries to create PDF reader in 3 steps:
   * <ul>
   * <li>without password</li>
   * <li>with empty password</li>
   * <li>with given password</li>
   * </ul>
   * 
   * @param content
   *          content of PDF
   * @param aPassword
   *          password
   * @return
   * @throws IOException
   */
  public static PdfReader getPdfReader(final byte[] content, byte[] aPassword) throws IOException {
    PdfReader tmpReader = null;
    try {
      // try to read without password
      tmpReader = new PdfReader(content);
    } catch (Exception e) {
      try {
        tmpReader = new PdfReader(content, new byte[0]);
      } catch (Exception e2) {
        tmpReader = new PdfReader(content, aPassword);
      }
    }
    return tmpReader;
  }

}
