package net.sf.jsignpdf.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.PrivateKeyInfo;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * This class provides methods for KeyStore(s) handling.
 * 
 * @author Josef Cacek
 */
public class KeyStoreUtils {

	private static final ResourceProvider res = ResourceProvider.getInstance();

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Returns array of supported KeyStores
	 * 
	 * @return String array with supported KeyStore implementation names
	 */
	public static SortedSet<String> getKeyStores() {
		final Set<String> tmpKeyStores = java.security.Security.getAlgorithms("KeyStore");
		return new TreeSet<String>(tmpKeyStores);
	}

	/**
	 * Loads key names (aliases) from the keystore
	 * 
	 * @return array of key aliases
	 */
	public static String[] getKeyAliases(final BasicSignerOptions options) {
		if (options == null) {
			throw new NullPointerException("Options are empty.");
		}
		options.log("console.getKeystoreType", options.getKsType());
		final KeyStore tmpKs = loadKeyStore(options.getKsType(), options.getKsFile(), options.getKsPasswd());
		if (tmpKs == null) {
			throw new NullPointerException(res.get("error.keystoreNull"));
		}
		final List<String> tmpResult = getAliasesList(tmpKs, options);
		return tmpResult.toArray(new String[tmpResult.size()]);
	}

	/**
	 * Returns list of key aliases in given keystore.
	 * 
	 * @param aKs
	 * @param options
	 * @return
	 */
	private static List<String> getAliasesList(final KeyStore aKs, final BasicSignerOptions options) {
		if (options == null) {
			throw new NullPointerException("Options are empty.");
		}
		if (aKs == null) {
			throw new NullPointerException(res.get("error.keystoreNull"));
		}
		final List<String> tmpResult = new ArrayList<String>();
		try {
			options.log("console.getAliases");
			final Enumeration<String> tmpAliases = aKs.aliases();
			final boolean checkValidity = ConfigProvider.getInstance().getAsBool("certificate.checkValidity", true);
			while (tmpAliases.hasMoreElements()) {
				final String tmpAlias = tmpAliases.nextElement();
				if (aKs.isKeyEntry(tmpAlias)) {
					final Certificate tmpCert = aKs.getCertificate(tmpAlias);
					if (checkValidity && tmpCert instanceof X509Certificate) {
						final X509Certificate tmpX509 = (X509Certificate) tmpCert;
						try {
							tmpX509.checkValidity();
							// check if the certificate is supposed to be used
							// for digital signatures
							final boolean keyUsage[] = tmpX509.getKeyUsage();
							if (keyUsage == null || keyUsage.length == 0 || keyUsage[0]) {
								tmpResult.add(tmpAlias);
							} else {
								options.log("console.certificateNotForSignature", tmpAlias);
							}
						} catch (CertificateExpiredException e) {
							options.log("console.certificateExpired", tmpAlias);
						} catch (CertificateNotYetValidException e) {
							options.log("console.certificateNotYetValid", tmpAlias);
						}
					} else {
						tmpResult.add(tmpAlias);
					}
				}
			}
		} catch (Exception e) {
			options.log("console.exception");
			e.printStackTrace(options.getPrintWriter());
		}
		return tmpResult;
	}

	/**
	 * Returns alias defined (either as a string or as an key index) in options
	 * 
	 * @param options
	 * @return key alias
	 */
	public static String getKeyAlias(final BasicSignerOptions options) {
		final KeyStore tmpKs = loadKeyStore(options.getKsType(), options.getKsFile(), options.getKsPasswd());

		String tmpResult = getKeyAliasInternal(options, tmpKs);
		return tmpResult;
	}

	private static String getKeyAliasInternal(final BasicSignerOptions options, final KeyStore aKs) {
		String tmpResult = null;
		try {
			if (aKs.isKeyEntry(options.getKeyAliasX())) {
				tmpResult = options.getKeyAliasX();
				options.log("console.usedKeyAlias", tmpResult);
				return tmpResult;
			}
		} catch (KeyStoreException e) {
			// nothing to do, fallback to default handling
		}
		final List<String> tmpList = getAliasesList(aKs, options);
		final String tmpAlias = options.getKeyAliasX();
		final int tmpIndex = options.getKeyIndexX();

		if (tmpAlias != null && tmpList.contains(tmpAlias)) {
			tmpResult = tmpAlias;
		} else if (tmpList.size() > tmpIndex && tmpIndex >= 0) {
			tmpResult = tmpList.get(tmpIndex);
		} else if (tmpList.size() > 0) {
			// fallback - return the first key
			tmpResult = tmpList.get(0);
		}
		options.log("console.usedKeyAlias", tmpResult);
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
	public static PrivateKeyInfo getPkInfo(BasicSignerOptions options) throws UnrecoverableKeyException,
			KeyStoreException, NoSuchAlgorithmException {
		final KeyStore tmpKs = loadKeyStore(options.getKsType(), options.getKsFile(), options.getKsPasswd());

		String tmpAlias = getKeyAliasInternal(options, tmpKs);
		options.log("console.getPrivateKey");
		final PrivateKey tmpPk = (PrivateKey) tmpKs.getKey(tmpAlias, options.getKeyPasswdX());
		options.log("console.getCertChain");
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
		} catch (Exception exception) {
			// nothing to do here
		}
	}

}
