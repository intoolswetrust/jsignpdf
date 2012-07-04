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
package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.Constants.RES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
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

import javax.crypto.Cipher;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.PrivateKeyInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * This class provides methods for KeyStore(s) handling.
 * 
 * @author Josef Cacek
 */
public class KeyStoreUtils {

	private final static Logger LOGGER = Logger.getLogger(KeyStoreUtils.class);

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
		LOGGER.info(RES.get("console.getKeystoreType", options.getKsType()));
		final KeyStore tmpKs = loadKeyStore(options.getKsType(), options.getKsFile(), options.getKsPasswd());
		if (tmpKs == null) {
			throw new NullPointerException(RES.get("error.keystoreNull"));
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
			throw new NullPointerException(RES.get("error.keystoreNull"));
		}
		final List<String> tmpResult = new ArrayList<String>();
		try {
			LOGGER.info(RES.get("console.getAliases"));
			final Enumeration<String> tmpAliases = aKs.aliases();
			final boolean checkValidity = ConfigProvider.getInstance().getAsBool("certificate.checkValidity", true);
			final boolean checkKeyUsage = ConfigProvider.getInstance().getAsBool("certificate.checkKeyUsage", true);
			final boolean checkCriticalExtensions = ConfigProvider.getInstance().getAsBool(
					"certificate.checkCriticalExtensions", true);
			while (tmpAliases.hasMoreElements()) {
				String tmpAlias = tmpAliases.nextElement();
				if (aKs.isKeyEntry(tmpAlias)) {
					final Certificate tmpCert = aKs.getCertificate(tmpAlias);
					boolean tmpAddAlias = true;
					if (tmpCert instanceof X509Certificate) {
						final X509Certificate tmpX509 = (X509Certificate) tmpCert;
						if (checkValidity) {
							try {
								tmpX509.checkValidity();
							} catch (CertificateExpiredException e) {
								LOGGER.info(RES.get("console.certificateExpired", tmpAlias));
								tmpAddAlias = false;
							} catch (CertificateNotYetValidException e) {
								LOGGER.info(RES.get("console.certificateNotYetValid", tmpAlias));
								tmpAddAlias = false;
							}
						}
						if (checkKeyUsage) {
							// check if the certificate is supposed to be
							// used for digital signatures
							final boolean keyUsage[] = tmpX509.getKeyUsage();
							if (keyUsage != null && keyUsage.length > 0) {
								// KeyUsage ::= BIT STRING {
								// digitalSignature (0),
								// nonRepudiation (1),
								// keyEncipherment (2),
								// dataEncipherment (3),
								// keyAgreement (4),
								// keyCertSign (5),
								// cRLSign (6),
								// encipherOnly (7),
								// decipherOnly (8) }
								if (!(keyUsage[0] || keyUsage[1])) {
									LOGGER.info(RES.get("console.certificateNotForSignature", tmpAlias));
									tmpAddAlias = false;
								}
							}
						}
						// check critical extensions
						if (checkCriticalExtensions) {
							final Set<String> criticalExtensionOIDs = tmpX509.getCriticalExtensionOIDs();
							if (criticalExtensionOIDs != null) {
								for (String oid : criticalExtensionOIDs) {
									if (!Constants.SUPPORTED_CRITICAL_EXTENSION_OIDS.contains(oid)) {
										LOGGER.info(RES.get("console.criticalExtensionNotSupported", tmpAlias, oid));
										tmpAddAlias = false;
									}
								}
							}
						}
					}
					if (tmpAddAlias) {
						tmpResult.add(tmpAlias);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(RES.get("console.exception"), e);
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
		if (aKs == null) {
			final String message = RES.get("error.keystoreNull");
			LOGGER.warn(message);
			throw new NullPointerException(message);
		}
		String tmpResult = null;
		if (StringUtils.isNotEmpty(options.getKeyAliasX())) {
			try {
				if (aKs.isKeyEntry(options.getKeyAliasX())) {
					tmpResult = options.getKeyAliasX();
					LOGGER.info(RES.get("console.usedKeyAlias", tmpResult));
					return tmpResult;
				}
			} catch (KeyStoreException e) {
				// nothing to do, fallback to default handling
			}
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
		LOGGER.info(RES.get("console.usedKeyAlias", tmpResult));
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
	 * Creates empty JKS keystore..
	 * 
	 * @return new JKS keystore
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 */
	public static KeyStore createKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException {
		final KeyStore newKeyStore = KeyStore.getInstance("JKS");
		newKeyStore.load(null, null);
		return newKeyStore;
	}

	/**
	 * Copies certificates from one keystore to another (both keystore has to be
	 * initialized.
	 * 
	 * @param fromKeyStore
	 * @param toKeyStore
	 * @return
	 */
	public static boolean copyCertificates(KeyStore fromKeyStore, KeyStore toKeyStore) {
		if (fromKeyStore == null || toKeyStore == null) {
			return false;
		}

		try {
			for (String alias : getCertAliases(fromKeyStore)) {
				toKeyStore.setCertificateEntry(alias, fromKeyStore.getCertificate(alias));
			}
			return true;
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return false;
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
			if (StringUtils.isNotEmpty(aKsFile)) {
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
		LOGGER.info(RES.get("console.getPrivateKey"));
		final PrivateKey tmpPk = (PrivateKey) tmpKs.getKey(tmpAlias, options.getKeyPasswdX());
		LOGGER.info(RES.get("console.getCertChain"));
		final Certificate[] tmpChain = tmpKs.getCertificateChain(tmpAlias);
		PrivateKeyInfo tmpResult = new PrivateKeyInfo(tmpPk, tmpChain);
		return tmpResult;
	}

	/**
	 * Loads a {@link X509Certificate} from the given path. Returns null if the
	 * certificate can't be loaded.
	 * 
	 * @param filePath
	 * @return
	 */
	public static X509Certificate loadCertificate(final String filePath) {
		if (StringUtils.isEmpty(filePath)) {
			LOGGER.debug("Empty file path");
			return null;
		}
		FileInputStream inStream = null;
		X509Certificate cert = null;
		try {
			final CertificateFactory certFac = CertificateFactory.getInstance(Constants.CERT_TYPE_X509); // X.509
			inStream = FileUtils.openInputStream(new File(filePath));
			cert = (X509Certificate) certFac.generateCertificate(inStream);
		} catch (Exception e) {
			LOGGER.debug("Unable to load certificate", e);
		} finally {
			IOUtils.closeQuietly(inStream);
		}
		return cert;
	}

	/**
	 * Returns true if the given certificate can be used for encryption, false
	 * otherwise.
	 * 
	 * @param cert
	 * @return
	 */
	public static boolean isEncryptionSupported(final Certificate cert) {
		boolean result = false;
		if (cert != null) {
			try {
				Cipher.getInstance(cert.getPublicKey().getAlgorithm());
				result = true;
			} catch (Exception e) {
				LOGGER.debug("Not possible to encrypt with the certificate", e);
			}
		}
		return result;
	}

	public static KeyStore createTrustStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException {
		final KeyStore trustStore = createKeyStore();

		char SEP = File.separatorChar;
		final File dir = new File(System.getProperty("java.home") + SEP + "lib" + SEP + "security");
		final File file = new File(dir, "cacerts");
		if (file.canRead()) {
			final KeyStore ks = KeyStore.getInstance("JKS");
			final InputStream in = new FileInputStream(file);
			try {
				ks.load(in, null);
			} finally {
				in.close();
			}
			copyCertificates(ks, trustStore);
		}
		return trustStore;
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
