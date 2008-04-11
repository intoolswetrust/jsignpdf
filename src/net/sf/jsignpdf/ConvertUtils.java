package net.sf.jsignpdf;

/**
 * Type conversion routines.
 * @author Josef Cacek
 */
public class ConvertUtils {

	/**
	 * Converts given value to String. If null is provided as parameter null is returned.
	 * @param anObj value
	 * @return String
	 */
	public static String toString(Object anObj) {
		return anObj==null?null:anObj.toString();
	}

	/**
	 * Converts given value to String.
	 * @param aValue value
	 * @return String
	 */
	public static String toString(int aValue) {
		return String.valueOf(aValue);
	}

	/**
	 * Converts given value to String.
	 * @param aValue value
	 * @return String
	 */
	public static String toString(long aValue) {
		return String.valueOf(aValue);
	}

	/**
	 * Converts given value to String.
	 * @param aValue value
	 * @return String
	 */
	public static String toString(float aValue) {
		return String.valueOf(aValue);
	}

	/**
	 * Converts given value to String.
	 * @param aValue value
	 * @return String
	 */
	public static String toString(double aValue) {
		return String.valueOf(aValue);
	}

	/**
	 * Converts given value to String.
	 * @param aValue value
	 * @return String
	 */
	public static String toString(boolean aValue) {
		return String.valueOf(aValue);
	}

	/**
	 * Converts given value to Integer. If conversion fails null is returned.
	 * @param anObj value
	 * @return Integer
	 */
	public static Integer toInteger(Object anObj) {
		if (anObj==null) return null;
		Integer tmpResult = null;
		if (anObj instanceof Integer) {
			tmpResult = (Integer) anObj;
		} else if (anObj instanceof Number) {
			tmpResult = new Integer(((Number) anObj).intValue());
		} else {
			try {
				tmpResult = new Integer(toString(anObj));
			} catch (NumberFormatException nfe) {}
		}
		return tmpResult;
	}

	/**
	 * Converts given value to int, if it is possible. When the conversion fails (null value)
	 * the given default value is returned
	 * @param anObj value to convert
	 * @param aDefault default value
	 * @return converted value (or default)
	 */
	public static int toInt(Object anObj, int aDefault) {
		final Integer tmpInteger = toInteger(anObj);
		return tmpInteger==null?aDefault:tmpInteger.intValue();
	}

	/**
	 * Converts given value to Float. If conversion fails null is returned.
	 * @param anObj value
	 * @return Float
	 */
	public static Float toFloat(Object anObj) {
		if (anObj==null) return null;
		Float tmpResult = null;
		if (anObj instanceof Float) {
			tmpResult = (Float) anObj;
		} else if (anObj instanceof Number) {
			tmpResult = new Float(((Number) anObj).floatValue());
		} else {
			try {
				tmpResult = new Float(toString(anObj));
			} catch (NumberFormatException nfe) {}
		}
		return tmpResult;
	}

	/**
	 * Converts given value to float, if it is possible. When the conversion fails (null value)
	 * the given default value is returned
	 * @param anObj value to convert
	 * @param aDefault default value
	 * @return converted value (or default)
	 */
	public static float toFloat(Object anObj, float aDefault) {
		final Float tmpVal = toFloat(anObj);
		return tmpVal==null?aDefault:tmpVal.floatValue();
	}

	/**
	 * Converts parameter to Boolean.
	 * <ol>
	 * <li>If given value is null, null is returned back</li>
	 * <li>If given value is Boolean instance, it is retured unchanged</li>
	 * <li>In all other cases is value converted to string and compared (ignoring case) to following:
	 * <ul>
	 * <li>"true", "yes", "on" - Boolean.TRUE is returned</li>
	 * <li>"false", "no", "off" - Boolean.FALSE is returned</li>
	 * <li>anything else - null is returned</li>
	 * </ul>
	 * </li>
	 * </ol>
	 * @param anObj object to convert
	 * @return boolean
	 */
	public static Boolean toBoolean(Object anObj) {
		Boolean tmpResult = null;
		if (anObj==null) {
			//nothing to do
		} else if (anObj instanceof Boolean) {
			tmpResult = (Boolean) anObj;
		} else {
			final String tmpStr = toString(anObj);
			if ("true".equalsIgnoreCase(tmpStr)
					|| "yes".equalsIgnoreCase(tmpStr)
					|| "on".equalsIgnoreCase(tmpStr)) {
				tmpResult = Boolean.TRUE;
			} else if ("false".equalsIgnoreCase(tmpStr)
					|| "no".equalsIgnoreCase(tmpStr)
					|| "off".equalsIgnoreCase(tmpStr)) {
				tmpResult = Boolean.FALSE;
			}
		}
		return tmpResult;
	}

	/**
	 * Converts given value to boolean, if it is possible. When the conversion fails (null value)
	 * the given default value is returned
	 * @param anObj value to convert
	 * @param aDefault default value
	 * @return converted value (or default)
	 * @see #toBoolean(Object)
	 */
 	public static boolean toBoolean(Object anObj, boolean aDefault) {
		final Boolean tmpBool = toBoolean(anObj);
		return tmpBool==null?aDefault:tmpBool.booleanValue();
	}

}
