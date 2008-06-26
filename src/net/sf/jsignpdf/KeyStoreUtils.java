package net.sf.jsignpdf;

import java.io.File;
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

	/**
	 * Returns array of supported KeyStores
	 * @return String array with supported KeyStore implementation names
	 */
	public static String[] getKeyStores() {
		final Set<String> tmpKeyStores = java.security.Security.getAlgorithms("KeyStore");
		final List<String> tmpResult = new ArrayList<String>(tmpKeyStores);
		return tmpResult.toArray(new String[tmpResult.size()]);
	}

	/**
	 * Loads key names (aliases) from the keystore
	 * @return array of key aliases
	 */
	public static String[] getKeyAliases(final SignerOptions options) {
		if (options==null) {
			throw new NullPointerException("Options are empty.");
		}
		final List<String> tmpResult = new ArrayList<String>();
		try {
			options.log("console.getKeystoreType", options.getKsType());
			final KeyStore tmpKs = loadKeyStore(options.getKsType(),
				options.getKsFile(), options.getKsPasswd());

			options.log("console.getAliases");
//			tmpResult.addAll(Collections.list(tmpKs.aliases()));
			final Enumeration<String> tmpAliases = tmpKs.aliases();
			while (tmpAliases.hasMoreElements()) {
				final String tmpAlias = tmpAliases.nextElement();
				if (tmpKs.isKeyEntry(tmpAlias)) {
					tmpResult.add(tmpAlias);
				}
			}			
			options.fireSignerFinishedEvent(true);
		} catch (Exception e) {
			options.log("console.exception");
			e.printStackTrace(options.getPrintWriter());
			options.fireSignerFinishedEvent(false);
		}
		return tmpResult.toArray(new String[tmpResult.size()]);
	}

	/**
	 * Loads certificate names (aliases) from the given keystore
	 * @return array of certificate aliases
	 */
	public static String[] getCertAliases(KeyStore tmpKs) {
		if (tmpKs==null) return null;
		final List<String> tmpResult = new ArrayList<String>();
		try {
			final Enumeration<String> tmpAliases = tmpKs.aliases();
			while (tmpAliases.hasMoreElements()) {
				final String tmpAlias = tmpAliases.nextElement();
				if (tmpKs.isCertificateEntry(tmpAlias)) {
					tmpResult.add(tmpAlias);
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return tmpResult.toArray(new String[tmpResult.size()]);
	}	

	/**
	 * Loads certificate names (aliases) from the given keystore
	 * @param aKsType
	 * @param aKsFile
	 * @param aKsPasswd
	 * @return array of certificate aliases
	 */
	public static String[] getCertAliases(String aKsType, String aKsFile, String aKsPasswd) {
		return getCertAliases(loadKeyStore(aKsType, aKsFile, aKsPasswd));
	}

	/**
	 * Opens given keystore.
	 * @param aKsType
	 * @param aKsFile
	 * @param aKsPasswd
	 * @return
	 */
	public static KeyStore loadKeyStore(String aKsType, 
		final String aKsFile, final String aKsPasswd) {
		char[] tmpPass = null;
		if (aKsPasswd!=null) {
			tmpPass = aKsPasswd.toCharArray();
		}			
		return loadKeyStore(aKsType, aKsFile, tmpPass);
	}

	/**
	 * Opens given keystore.
	 * @param aKsType
	 * @param aKsFile
	 * @param aKsPasswd
	 * @return
	 */
	public static KeyStore loadKeyStore(String aKsType, 
		final String aKsFile, final char[] aKsPasswd) {

		if (StringUtils.isEmpty(aKsType) && StringUtils.isEmpty(aKsFile)) {
			return loadCacertsKeyStore(null);
		}

		if (StringUtils.isEmpty(aKsType)) {
			aKsType = KeyStore.getDefaultType();
		}

		KeyStore tmpKs = null;
		InputStream tmpIS = null;
		try {
			tmpKs = KeyStore.getInstance(aKsType);
			if (!StringUtils.isEmpty(aKsFile)) {
				tmpIS = new FileInputStream(aKsFile);
			}
			tmpKs.load(tmpIS, aKsPasswd);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (tmpIS!=null) try { tmpIS.close(); } catch (Exception e) {}
		}
		return tmpKs;
	}


	/**
	 * Loads the default root certificates at &lt;java.home&gt;/lib/security/cacerts.
	 * @param provider the provider or <code>null</code> for the default provider
	 * @return a <CODE>KeyStore</CODE>
	 */    
	public static KeyStore loadCacertsKeyStore(String provider) {
		File file = new File(System.getProperty("java.home"), "lib");
		file = new File(file, "security");
		file = new File(file, "cacerts");
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(file);
			KeyStore k;
			if (provider == null)
				k = KeyStore.getInstance("JKS");
			else
				k = KeyStore.getInstance("JKS", provider);
			k.load(fin, null);
			return k;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try{if (fin != null) {fin.close();}}catch(Exception ex){}
		}
	}

}
