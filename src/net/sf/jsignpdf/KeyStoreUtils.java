package net.sf.jsignpdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods for KeyStore(s) handling.
 * 
 * @author Josef Cacek
 */
public class KeyStoreUtils {

	private static final Logger logger = LoggerFactory.getLogger(KeyStoreUtils.class);
	private static final ResourceBundleBean res = ResourceProvider.getBundleBean();

	/**
	 * Returns array of supported KeyStores
	 * 
	 * @return String array with supported KeyStore implementation names
	 */
	public static String[] getKeyStores() {
		final Set<String> tmpKeyStores = java.security.Security.getAlgorithms("KeyStore");
		final List<String> tmpResult = new ArrayList<String>(tmpKeyStores);
		return tmpResult.toArray(new String[tmpResult.size()]);
	}

	/**
	 * Loads key names (aliases) from the keystore
	 * 
	 * @return array of key aliases
	 */
	public static String[] getKeyAliases(final SignerOptions options) {
		if (options == null) {
			throw new NullPointerException("Options are empty.");
		}
		logger.info(res.get("console.getKeystoreType", options.getKsType()));
		final KeyStore tmpKs = loadKeyStore(options.getKsType(), options.getKsFile(), options.getKsPasswd());
		final List<String> tmpResult = getAliasesList(tmpKs, options);
		return tmpResult.toArray(new String[tmpResult.size()]);
	}

	/**
	 * Returns list of key aliases in given keystore.
	 * @param aKs not null keystore
	 * @param options not null options
	 * @return
	 */
	private static List<String> getAliasesList(KeyStore aKs, final SignerOptions options) {
		if (options == null) {
			throw new NullPointerException("Options are empty.");
		}
		if (aKs==null) {
			throw new NullPointerException("Keystore was not loaded. Check the type, path and password.");
		}
		final List<String> tmpResult = new ArrayList<String>();
		try {
			logger.info(res.get("console.getAliases"));
			final Enumeration<String> tmpAliases = aKs.aliases();
			while (tmpAliases.hasMoreElements()) {
				final String tmpAlias = tmpAliases.nextElement();
				if (aKs.isKeyEntry(tmpAlias)) {
					tmpResult.add(tmpAlias);
				}
			}
		} catch (Exception e) {
			logger.warn(res.get("console.exception"), e);
		}
		return tmpResult;
	}

	/**
	 * Returns alias defined (either as a string or as an key index) in options
	 * 
	 * @param options
	 * @return key alias
	 */
	public static String getKeyAlias(final SignerOptions options) {
		final KeyStore tmpKs = loadKeyStore(options.getKsType(), options.getKsFile(), options.getKsPasswd());

		String tmpResult = getKeyAliasInternal(options, tmpKs);
		return tmpResult;
	}

	private static String getKeyAliasInternal(final SignerOptions options, final KeyStore tmpKs) {
		String tmpResult = null;
		final List<String> tmpList = getAliasesList(tmpKs, options);
		final String tmpAlias = options.getKeyAlias();
		final int tmpIndex = options.getKeyIndex();

		if (tmpAlias != null && tmpList.contains(tmpAlias)) {
			tmpResult = tmpAlias;
		} else if (tmpList.size() > tmpIndex && tmpIndex >= 0) {
			tmpResult = tmpList.get(tmpIndex);
		} else if (tmpList.size() > 0) {
			// fallback - return the first key
			tmpResult = tmpList.get(0);
		}
		logger.info(res.get("console.usedKeyAlias", tmpResult));
		return tmpResult;
	}

	/**
	 * Loads certificate names (aliases) from the given keystore
	 * 
	 * @return array of certificate aliases
	 */
	public static String[] getCertAliases(KeyStore tmpKs) {
		if (tmpKs == null)
			return null;
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
	 * 
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
	 * 
	 * @param aKsType
	 * @param aKsFile
	 * @param aKsPasswd
	 * @return
	 */
	public static KeyStore loadKeyStore(String aKsType, final String aKsFile, final String aKsPasswd) {
		char[] tmpPass = null;
		if (aKsPasswd != null) {
			tmpPass = aKsPasswd.toCharArray();
		}
		return loadKeyStore(aKsType, aKsFile, tmpPass);
	}

	/**
	 * Opens given keystore.
	 * 
	 * @param aKsType
	 * @param aKsFile
	 * @param aKsPasswd
	 * @return
	 */
	public static KeyStore loadKeyStore(String aKsType, final String aKsFile, final char[] aKsPasswd) {

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
			fixAliases(tmpKs);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (tmpIS != null)
				try {
					tmpIS.close();
				} catch (Exception e) {
				}
		}
		return tmpKs;
	}

	/**
	 * Loads the default root certificates at
	 * &lt;java.home&gt;/lib/security/cacerts.
	 * 
	 * @param provider
	 *            the provider or <code>null</code> for the default provider
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
			try {
				if (fin != null) {
					fin.close();
				}
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Returns PrivateKey and its certificate chain
	 * 
	 * @param options
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 */
	public static PrivateKeyInfo getPkInfo(SignerOptions options) throws UnrecoverableKeyException, KeyStoreException,
			NoSuchAlgorithmException {
		final KeyStore tmpKs = loadKeyStore(options.getKsType(), options.getKsFile(), options.getKsPasswd());

		String tmpAlias = getKeyAliasInternal(options, tmpKs);
		logger.info(res.get("console.getPrivateKey"));
		final PrivateKey tmpPk = (PrivateKey) tmpKs.getKey(tmpAlias, StringUtils.toCharArray(options
				.getInFileOwnerPwd()));
		logger.info(res.get("console.getCertChain"));
		final Certificate[] tmpChain = tmpKs.getCertificateChain(tmpAlias);
		PrivateKeyInfo tmpResult = new PrivateKeyInfo(tmpPk, tmpChain);
		return tmpResult;
	}

	/**
	 * For WINDOWS-MY keystore fixes problem with non-unique aliases
	 * 
	 * @param keyStore
	 */
	@SuppressWarnings("unchecked")
	private static void fixAliases(final KeyStore keyStore) {
		Field field;
		KeyStoreSpi keyStoreVeritable;
		final Set<String> tmpAliases = new HashSet<String>();
		try {
			field = keyStore.getClass().getDeclaredField("keyStoreSpi");
			field.setAccessible(true);
			keyStoreVeritable = (KeyStoreSpi) field.get(keyStore);

			if ("sun.security.mscapi.KeyStore$MY".equals(keyStoreVeritable.getClass().getName())) {
				Collection<Object> entries;
				String alias, hashCode;
				X509Certificate[] certificates;

				field = keyStoreVeritable.getClass().getEnclosingClass().getDeclaredField("entries");
				field.setAccessible(true);
				entries = (Collection<Object>) field.get(keyStoreVeritable);

				for (Object entry : entries) {
					field = entry.getClass().getDeclaredField("certChain");
					field.setAccessible(true);
					certificates = (X509Certificate[]) field.get(entry);

					hashCode = Integer.toString(certificates[0].hashCode());

					field = entry.getClass().getDeclaredField("alias");
					field.setAccessible(true);
					alias = (String) field.get(entry);
					String tmpAlias = alias;
					int i = 0;
					while (tmpAliases.contains(tmpAlias)) {
						i++;
						tmpAlias = alias + "-" + i;
					}
					tmpAliases.add(tmpAlias);
					if (!alias.equals(hashCode)) {
						field.set(entry, tmpAlias);
					}
				}
			}
		} catch (Exception e) {
			logger.debug("Fixing aliases failed.", e);
		}
	}

}
