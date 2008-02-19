package net.sf.jsignpdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class provides methods for KeyStore(s) handling.
 * @author Josef Cacek
 */
public class KeyStoreUtils {

	private SignerOptions options;

	/**
	 * Returns array of supported KeyStores
	 * @return String array with supported KeyStore implementation names
	 */
	public String[] getKeyStrores() {
		final List<String> tmpResult = new ArrayList<String>();
		tmpResult.add(Constants.KS_TYPE_PKCS12.toUpperCase());
		final Set<String> tmpKeyStores = java.security.Security.getAlgorithms("KeyStore");
		for (String tmpKs : tmpKeyStores) {
			final String tmpUpperName = tmpKs.toUpperCase();
			if (!tmpResult.contains(tmpUpperName)) {
				tmpResult.add(tmpUpperName);
			}
		}
		return tmpResult.toArray(new String[tmpResult.size()]);
	}

	/**
	 * Loads key names (aliases) from the keystore
	 * @return
	 */
	public String[] getAliases() {
		if (options==null) {
			throw new NullPointerException("Options are empty.");
		}
		//TODO implement
		return null;
	}

	public SignerOptions getOptions() {
		return options;
	}

	public void setOptions(SignerOptions options) {
		this.options = options;
	}

}
