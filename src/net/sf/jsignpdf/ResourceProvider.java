package net.sf.jsignpdf;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Entry point to internationalization.
 * Resource bundles has base "translation/messages".
 *
 * @author Josef Cacek [josef.cacek (at) gmail.com]
 * @author $Author: kwart $
 * @version $Revision: 1.2 $
 * @created $Date: 2008/02/11 20:36:59 $
 */
public class ResourceProvider {

	private static final ResourceProvider provider = new ResourceProvider();

	private ResourceBundle bundle;

	private ResourceProvider() {
//		bundle = Utf8ResourceBundle.getBundle("translations.messages",
//			Locale.getDefault(), ResourceProvider.class.getClassLoader());
		bundle = ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE);
	}

	/**
	 * Returns singleton of ResourceProvider
	 * @return singleton
	 */
	public static ResourceProvider getInstance() {
		return provider;
	}

	/**
	 * Returns message for given key from active ResourceBundle
	 * @param aKey name of key in resource bundle
	 * @return message for given key
	 */
	public String get(final String aKey) {
		String tmpMessage = bundle.getString(aKey);
		if (tmpMessage == null) {
			tmpMessage = aKey;
		} else {
			tmpMessage = tmpMessage.replaceAll("&([^&])", "$1");
		}
		return tmpMessage;
	}

	/**
	 * Returns index of character which should be used as a mnemonic.
	 * It returns -1 if such an character doesn't exist.
	 * @param aKey resource key
	 * @return index (position) of character in translated message
	 */
	public int getMnemonicIndex(final String aKey) {
		String tmpMessage = bundle.getString(aKey);
		int tmpResult = -1;
		if (tmpMessage != null) {
			int searchFrom = 0;
			int tmpDoubles = 0;
			int tmpPos;
			final int tmpLen = tmpMessage.length();
			do {
				tmpPos = tmpMessage.indexOf('&', searchFrom);
				if (tmpPos == tmpLen-1) tmpPos = -1;
				if (tmpPos>-1) {
					if (tmpMessage.charAt(tmpPos+1) != '&') {
						tmpResult = tmpPos - tmpDoubles;
					} else {
						searchFrom = tmpPos + 2;
						tmpDoubles++;
					}
				}
			} while (tmpPos!=-1 && tmpResult==-1 && searchFrom<tmpLen);
		}
		return tmpResult;
	}

	/**
	 * Returns message for given key from active ResourceBundle and replaces
	 * parameters with values given in array.
	 * @param aKey key in resource bundle
	 * @param anArgs array of parameters to replace in message
	 * @return message for given key with given arguments
	 */
	public String get(String aKey, String anArgs[]) {
		String tmpMessage = get(aKey);
		if (aKey==tmpMessage || anArgs == null || anArgs.length == 0) {
			return tmpMessage;
		}
		final MessageFormat tmpFormat = new MessageFormat(tmpMessage);
		return tmpFormat.format(anArgs);
	}

}
