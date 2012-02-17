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

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Encryption provider for JSignPdf.
 * 
 * @author Josef Cacek
 */
public class JSignEncryptor {

  private BufferedBlockCipher cipher;
  private KeyParameter key;

  /**
   * Initialize the cryptographic engine.
   * 
   * @param aKey
   */
  public JSignEncryptor(final byte[] aKey) {
    cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new BlowfishEngine()));
    key = new KeyParameter(aKey);
  }

  /**
   * Initialize the cryptographic engine. The string should be at least 8 chars
   * long.
   * 
   * @param aKey
   */
  public JSignEncryptor(final String aKey) {
    this(aKey.getBytes());
  }

  /**
   * Encryptor with a default key
   */
  public JSignEncryptor() {
    this(Constants.USER_HOME + "Conan, premozitel hobitu.");
  }

  /**
   * Private routine that does the gritty work.
   * 
   * @param data
   * @return
   * @throws CryptoException
   */
  private byte[] callCipher(byte[] data) throws CryptoException {
    final int size = cipher.getOutputSize(data.length);
    byte[] result = new byte[size];
    int olen = cipher.processBytes(data, 0, data.length, result, 0);
    olen += cipher.doFinal(result, olen);

    if (olen < size) {
      byte[] tmp = new byte[olen];
      System.arraycopy(result, 0, tmp, 0, olen);
      result = tmp;
    }

    return result;
  }

  /**
   * Encrypt arbitrary byte array, returning the encrypted data in a different
   * byte array.
   * 
   * @param data
   * @return
   * @throws CryptoException
   */
  private synchronized byte[] encrypt(byte[] data) throws CryptoException {
    if (data == null || data.length == 0) {
      return new byte[0];
    }

    cipher.init(true, key);
    return callCipher(data);
  }

  /**
   * Encrypts a string.
   * 
   * @param data
   * @return
   * @throws CryptoException
   */
  public String encryptString(String data) throws CryptoException {
    if (data == null || data.length() == 0) {
      return null;
    }

    return toHexString(encrypt(data.getBytes()));
  }

  /**
   * Decrypts arbitrary data.
   * 
   * @param data
   * @return
   * @throws CryptoException
   */
  private synchronized byte[] decrypt(byte[] data) throws CryptoException {
    if (data == null || data.length == 0) {
      return new byte[0];
    }

    cipher.init(false, key);
    return callCipher(data);
  }

  /**
   * Decrypts a string that was previously encoded using encryptString.
   * 
   * @param data
   * @return
   * @throws CryptoException
   */
  public String decryptString(final String data) throws CryptoException {
    if (data == null || data.length() == 0) {
      return "";
    }

    return new String(decrypt(fromHexString(data)));
  }

  private static char[] hex_table = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

  /**
   * Convert a byte array to a String with a hexidecimal format. The String may
   * be converted back to a byte array using fromHexString. <BR>
   * For each byte (b) two characaters are generated, the first character
   * represents the high nibble (4 bits) in hexidecimal (<code>b & 0xf0</code>
   * ), the second character represents the low nibble (<code>b & 0x0f</code> ). <BR>
   * The byte at <code>data[offset]</code> is represented by the first two
   * characters in the returned String.
   * 
   * @param data
   *          byte array
   * @param offset
   *          starting byte (zero based) to convert.
   * @param length
   *          number of bytes to convert.
   * 
   * @return the String (with hexidecimal format) form of the byte array
   */
  public static String toHexString(byte[] data, int offset, int length) {
    StringBuffer s = new StringBuffer(length * 2);
    int end = offset + length;

    for (int i = offset; i < end; i++) {
      int high_nibble = (data[i] & 0xf0) >>> 4;
      int low_nibble = (data[i] & 0x0f);
      s.append(hex_table[high_nibble]);
      s.append(hex_table[low_nibble]);
    }

    return s.toString();
  }

  /**
   * Returns hexadecimal view of byte array
   * 
   * @param data
   *          byte array
   * @return String representation of byte array in hexa decimals
   * @see #toHexString(byte[], int, int)
   */
  public static String toHexString(byte[] data) {
    if (data == null) {
      return null;
    }
    return toHexString(data, 0, data.length);
  }

  /**
   * Convert a hexidecimal string generated by toHexString() back into a byte
   * array.
   * 
   * @param s
   *          String to convert
   * @param offset
   *          starting character (zero based) to convert.
   * @param length
   *          number of characters to convert.
   * 
   * @return the converted byte array. Returns null if the length is not a
   *         multiple of 2.
   */
  public static byte[] fromHexString(String s, int offset, int length) {
    if ((length % 2) != 0)
      return null;

    byte[] byteArray = new byte[length / 2];

    int j = 0;
    int end = offset + length;
    for (int i = offset; i < end; i += 2) {
      int high_nibble = Character.digit(s.charAt(i), 16);
      int low_nibble = Character.digit(s.charAt(i + 1), 16);

      if (high_nibble == -1 || low_nibble == -1) {
        // illegal format
        return null;
      }

      byteArray[j++] = (byte) (((high_nibble << 4) & 0xf0) | (low_nibble & 0x0f));
    }
    return byteArray;
  }

  /**
   * Convert a hexidecimal string generated by toHexString() back into a byte
   * array
   * 
   * @param s
   * @return
   */
  public static byte[] fromHexString(String s) {
    return fromHexString(s, 0, s.length());
  }

}
