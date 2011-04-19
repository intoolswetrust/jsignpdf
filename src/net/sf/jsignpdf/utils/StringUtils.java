package net.sf.jsignpdf.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Some methods for string handling. Originally based on StringUtils class from
 * Apache Derby project
 * 
 * @author Josef Cacek
 * @deprecated Use StringUtils from commons-lang where possible.
 */
@Deprecated
public class StringUtils {

	/**
	 * Used to print out a string for error messages, chops is off at 60 chars
	 * for historical reasons.
	 */
	public final static String formatForPrint(String input) {
		if (input.length() > 60) {
			StringBuffer tmp = new StringBuffer(input.substring(0, 60));
			tmp.append("&");
			input = tmp.toString();
		}
		return input;
	}

	/**
	 * A method that receive an array of Objects and return a String array
	 * representation of that array.
	 */
	public static String[] toStringArray(Object[] objArray) {
		int idx;
		int len = objArray.length;
		String[] strArray = new String[len];

		for (idx = 0; idx < len; idx++) {
			strArray[idx] = objArray[idx].toString();
		}

		return strArray;
	}

	/**
	 * Get 7-bit ASCII character array from input String. The lower 7 bits of
	 * each character in the input string is assumed to be the ASCII character
	 * value.
	 * 
	 * Hexadecimal - Character | 00 NUL| 01 SOH| 02 STX| 03 ETX| 04 EOT| 05 ENQ|
	 * 06 ACK| 07 BEL| | 08 BS | 09 HT | 0A NL | 0B VT | 0C NP | 0D CR | 0E SO |
	 * 0F SI | | 10 DLE| 11 DC1| 12 DC2| 13 DC3| 14 DC4| 15 NAK| 16 SYN| 17 ETB|
	 * | 18 CAN| 19 EM | 1A SUB| 1B ESC| 1C FS | 1D GS | 1E RS | 1F US | | 20 SP
	 * | 21 ! | 22 " | 23 # | 24 $ | 25 % | 26 & | 27 ' | | 28 ( | 29 ) | 2A * |
	 * 2B + | 2C , | 2D - | 2E . | 2F / | | 30 0 | 31 1 | 32 2 | 33 3 | 34 4 |
	 * 35 5 | 36 6 | 37 7 | | 38 8 | 39 9 | 3A : | 3B ; | 3C < | 3D = | 3E > |
	 * 3F ? | | 40 @ | 41 A | 42 B | 43 C | 44 D | 45 E | 46 F | 47 G | | 48 H |
	 * 49 I | 4A J | 4B K | 4C L | 4D M | 4E N | 4F O | | 50 P | 51 Q | 52 R |
	 * 53 S | 54 T | 55 U | 56 V | 57 W | | 58 X | 59 Y | 5A Z | 5B [ | 5C \ |
	 * 5D ] | 5E ^ | 5F _ | | 60 ` | 61 a | 62 b | 63 c | 64 d | 65 e | 66 f |
	 * 67 g | | 68 h | 69 i | 6A j | 6B k | 6C l | 6D m | 6E n | 6F o | | 70 p |
	 * 71 q | 72 r | 73 s | 74 t | 75 u | 76 v | 77 w | | 78 x | 79 y | 7A z |
	 * 7B { | 7C | | 7D } | 7E ~ | 7F DEL|
	 * 
	 */
	public static byte[] getAsciiBytes(String input) {
		char[] c = input.toCharArray();
		byte[] b = new byte[c.length];
		for (int i = 0; i < c.length; i++)
			b[i] = (byte) (c[i] & 0x007F);

		return b;
	}

	/**
	 * Trim off trailing blanks but not leading blanks
	 * 
	 * @param str
	 * 
	 * @return The input with trailing blanks stipped off
	 */
	public static String trimTrailing(String str) {
		if (str == null)
			return null;
		int len = str.length();
		for (; len > 0; len--) {
			if (!Character.isWhitespace(str.charAt(len - 1)))
				break;
		}
		return str.substring(0, len);
	} // end of trimTrailing

	/**
	 * Truncate a String to the given length with no warnings or error raised if
	 * it is bigger.
	 * 
	 * @param value
	 *            String to be truncated
	 * @param length
	 *            Maximum length of string
	 * 
	 * @return Returns value if value is null or value.length() is less or equal
	 *         to than length, otherwise a String representing value truncated
	 *         to length.
	 */
	public static String truncate(String value, int length) {
		if (value != null && value.length() > length)
			value = value.substring(0, length);
		return value;
	}

	private static char[] hex_table = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Convert a byte array to a String with a hexidecimal format. The String
	 * may be converted back to a byte array using fromHexString. <BR>
	 * For each byte (b) two characaters are generated, the first character
	 * represents the high nibble (4 bits) in hexidecimal (<code>b & 0xf0</code>
	 * ), the second character represents the low nibble (<code>b & 0x0f</code>
	 * ). <BR>
	 * The byte at <code>data[offset]</code> is represented by the first two
	 * characters in the returned String.
	 * 
	 * @param data
	 *            byte array
	 * @param offset
	 *            starting byte (zero based) to convert.
	 * @param length
	 *            number of bytes to convert.
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
	 *            byte array
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
	 *            String to convert
	 * @param offset
	 *            starting character (zero based) to convert.
	 * @param length
	 *            number of characters to convert.
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
	 * 
	 * @param s
	 * @return
	 */
	public static byte[] fromHexString(String s) {
		return fromHexString(s, 0, s.length());
	}

	/**
	 * Convert a byte array to a human-readable String for debugging purposes.
	 */
	public static String hexDump(byte[] data) {
		byte byte_value;

		StringBuffer str = new StringBuffer(data.length * 3);

		str.append("Hex dump:\n");

		for (int i = 0; i < data.length; i += 16) {
			// dump the header: 00000000:
			String offset = Integer.toHexString(i);

			// "0" left pad offset field so it is always 8 char's long.
			for (int offlen = offset.length(); offlen < 8; offlen++)
				str.append("0");
			str.append(offset);
			str.append(":");

			// dump hex version of 16 bytes per line.
			for (int j = 0; (j < 16) && ((i + j) < data.length); j++) {
				byte_value = data[i + j];

				// add spaces between every 2 bytes.
				if ((j % 2) == 0)
					str.append(" ");

				// dump a single byte.
				byte high_nibble = (byte) ((byte_value & 0xf0) >>> 4);
				byte low_nibble = (byte) (byte_value & 0x0f);

				str.append(hex_table[high_nibble]);
				str.append(hex_table[low_nibble]);
			}

			// dump ascii version of 16 bytes
			str.append("  ");

			for (int j = 0; (j < 16) && ((i + j) < data.length); j++) {
				char char_value = (char) data[i + j];

				// RESOLVE (really want isAscii() or isPrintable())
				if (Character.isLetterOrDigit(char_value))
					str.append(String.valueOf(char_value));
				else
					str.append(".");
			}

			// new line
			str.append("\n");
		}
		return (str.toString());

	}

	// The functions below are used for uppercasing SQL in a consistent manner.
	// Cloudscape will uppercase Turkish to the English locale to avoid i
	// uppercasing to an uppercase dotted i. In future versions, all
	// casing will be done in English. The result will be that we will get
	// only the 1:1 mappings in
	// http://www.unicode.org/Public/3.0-Update1/UnicodeData-3.0.1.txt
	// and avoid the 1:n mappings in
	// http://www.unicode.org/Public/3.0-Update1/SpecialCasing-3.txt
	//
	// Any SQL casing should use these functions

	/**
	 * Convert string to uppercase Always use the java.util.ENGLISH locale
	 * 
	 * @param s
	 *            string to uppercase
	 * @return uppercased string
	 */
	public static String SQLToUpperCase(String s) {
		return s.toUpperCase(Locale.ENGLISH);
	}

	/**
	 * Convert string to lowercase Return java.util.Locale.ENGLISH lowercasing
	 * 
	 * @param s
	 *            string to lowercase
	 * @return lowercased string
	 */
	public static String SQLToLowerCase(String s) {
		return s.toLowerCase(Locale.ENGLISH);

	}

	/**
	 * Compares two strings Strings will be uppercased in english and compared
	 * equivalent to s1.equalsIgnoreCase(s2) throws NPE if s1 is null
	 * 
	 * @param s1
	 *            first string to compare
	 * @param s2
	 *            second string to compare
	 * 
	 * @return true if the two upppercased ENGLISH values are equal return false
	 *         if s2 is null
	 */
	public static boolean SQLEqualsIgnoreCase(String s1, String s2) {
		if (s2 == null) {
			return false;
		}
		return s1.toUpperCase(Locale.ENGLISH).equals(s2.toUpperCase(Locale.ENGLISH));

	}

	/**
	 * Returns string to which was inserted substring on given position
	 * 
	 * @param s1
	 *            string which be returned with inserted s2
	 * @param s2
	 *            string to insert
	 * @param aPos
	 *            position of insert
	 * @return extended s1 or only s1 when ( s1 == null || s2 == null || aPos<0
	 *         ||aPos>s1.length())
	 */
	public static String insertSubString(String s1, String s2, int aPos) {
		if (s1 == null || s2 == null || aPos < 0 || aPos > s1.length()) {
			return s1;
		}
		final StringBuffer tmpSB = new StringBuffer(s1.substring(0, aPos));
		tmpSB.append(s2).append(s1.substring(aPos));
		return tmpSB.toString();
	}

	/**
	 * Returns InputStream as a String
	 * 
	 * @param aIS
	 * @param aEncoding
	 */
	public static String stream2String(InputStream aIS, String aEncoding) {
		if (aIS == null) {
			return null;
		}
		ByteArrayOutputStream tmpOS = new ByteArrayOutputStream();
		String tmpResult = null;
		try {
			IOUtils.copy(aIS, tmpOS);
			if (aEncoding != null && Charset.isSupported(aEncoding)) {
				tmpResult = tmpOS.toString(aEncoding);
			} else {
				tmpResult = tmpOS.toString();
			}
		} catch (IOException e) {
			//nothing to do
		}
		return tmpResult;
	}

	/**
	 * Returns InputStream as a String
	 * 
	 * @param aFileName
	 *            file name
	 * @param aEncoding
	 */
	public static String file2String(String aFileName, String aEncoding) {
		return stream2String(StringUtils.class.getClassLoader().getResourceAsStream(aFileName), aEncoding);
	}

	/**
	 * Converts object to normalized string (line breaks are removed), tabs and
	 * multiple spaces are replaced by normalized spaces. String is trimmed.
	 * 
	 * @param aObj
	 * @return string representation of object in normalized form
	 */
	public static String toNormalizedString(Object aObj) {
		if (aObj == null) {
			return "";
		}
		return aObj.toString().replaceAll("[\r\n]+", " ").replaceAll("[ \t]+", " ").trim();
	}

	/**
	 * Splits a string to string array. If aLine is null, empty array is
	 * returned in other cases aLine.split(aReges, -1) is used. parameter to
	 * Pattern.split
	 * 
	 * @param aLine
	 * @param aRegex
	 * @see String#split(String, int)
	 * @return array of substrings from aLine splited around aRegex
	 */
	public static String[] split(final String aLine, final String aRegex) {
		if (aLine == null) {
			return new String[] {};
		}
		return aLine.split(aRegex, -1);
	}

	/**
	 * Returns trimmed string representation of object or null if it is empty
	 * string.
	 * 
	 * @param anObj
	 *            object
	 * @return trimmed toString() or null
	 */
	public static String emptyNull(final Object anObj) {
		if (anObj == null)
			return null;
		String tmpResult = anObj.toString().trim();
		if (tmpResult.length() == 0)
			tmpResult = null;
		return tmpResult;
	}

	/**
	 * Returns true if string representation of given object is empty or null.
	 * 
	 * @param anObj
	 *            object
	 * @return true if string representation is empty
	 * @see #emptyNull(Object)
	 */
	public static boolean isEmpty(final Object anObj) {
		return emptyNull(anObj) == null;
	}

	/**
	 * Returns true if string representation of given object is not empty.
	 * 
	 * @param anObj
	 *            object
	 * @return
	 */
	public static boolean hasLength(final Object anObj) {
		return !isEmpty(anObj);
	}

	/**
	 * Returns not null String representation of given parameter.
	 * 
	 * @param anObj
	 *            object
	 * @return trimmed toString() or null
	 */
	public static String toNotNull(final Object anObj) {
		return anObj == null ? "" : anObj.toString();
	}
}
