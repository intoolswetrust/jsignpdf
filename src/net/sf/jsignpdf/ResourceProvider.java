package net.sf.jsignpdf;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Entry point to internationalization.
 * Resource bundles has base "translation/messages".
 *
 * @author Josef Cacek [josef.cacek (at) gmail.com]
 * @author $Author: kwart $
 * @version $Revision: 1.1 $
 * @created $Date: 2008/02/08 13:33:29 $
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
	public String get(String aKey) {
		return bundle.getString(aKey);
	}

	/**
	 * Returns message for given key from active ResourceBundle and replaces
	 * parameters with values given in array.
	 * @param aKey key in resource bundle
	 * @param anArgs array of parameters to replace in message
	 * @return message for given key with given arguments
	 */
	public String get(String aKey, String anArgs[]) {
		String tmpResource = get(aKey);
		if (tmpResource == null) {
			return aKey;
		} else if (anArgs == null || anArgs.length == 0) {
			return tmpResource;
		}
		final MessageFormat tmpFormat = new MessageFormat(tmpResource);
		return tmpFormat.format(anArgs);
	}

}
