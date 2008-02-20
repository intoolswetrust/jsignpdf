package net.sf.jsignpdf;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Enumeration;
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
	 * @param aType KeyStore type
	 * @return array of key aliasis in given keystore
	 */
	public String[] getAliases(final String aType) {
		//jaka je performance? nemel by to byt take specialni thread?
		if (options==null) {
			throw new NullPointerException("Options are empty.");
		}
		final List<String> tmpResult = new ArrayList<String>();
		try {
			options.log("console.getKeystoreType", options.getKsType());
			final KeyStore tmpKs = KeyStore.getInstance(aType);
			InputStream tmpIS = null;
			char[] tmpPass = null;
			if (!StringUtils.isEmpty(options.ksFile)) {
				tmpIS = new FileInputStream(options.ksFile);
			}
			if (options.ksPasswd!=null && options.ksPasswd.length>0) {
				tmpPass = options.ksPasswd;
			}
			options.log("console.loadKeystore", options.getKsFile());
			tmpKs.load(tmpIS, tmpPass);
			options.log("console.getAliases");
			Enumeration<String> tmpAliases = tmpKs.aliases();
			while (tmpAliases.hasMoreElements()) {
				tmpResult.add(tmpAliases.nextElement());
			}
			options.fireSignerFinishedEvent(true);
		} catch (Exception e) {
			options.log("console.exception");
			e.printStackTrace(options.getPrintWriter());
			options.fireSignerFinishedEvent(false);
		}
		return tmpResult.toArray(new String[tmpResult.size()]);
	}

	public SignerOptions getOptions() {
		return options;
	}

	public void setOptions(SignerOptions options) {
		this.options = options;
	}

}
