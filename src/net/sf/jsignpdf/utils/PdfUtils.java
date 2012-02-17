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
