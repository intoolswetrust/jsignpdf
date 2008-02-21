package net.sf.jsignpdf;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Encryptor
 * @author Josef Cacek
 */
public class JSignEncryptor {

	private BufferedBlockCipher cipher;
	private KeyParameter key;

	/**
	 * Initialize the cryptographic engine.
	 * @param aKey
	 */
	public JSignEncryptor(final byte[] aKey) {
		cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new BlowfishEngine()));
		key = new KeyParameter(aKey);
	}

	/**
	 * Initialize the cryptographic engine.
	 * The string should be at least 8 chars long.
	 * @param aKey
	 */
	public JSignEncryptor(final String aKey) {
		this(aKey.getBytes());
	}

	/**
	 * Encryptor with a default key
	 */
	public JSignEncryptor() {
		this(Constants.USER_HOME + "Conan, premozitel hobitu.");
	}

	/**
	 * Private routine that does the gritty work.
	 * @param data
	 * @return
	 * @throws CryptoException
	 */
	private byte[] callCipher(byte[] data) throws CryptoException {
		final int size = cipher.getOutputSize(data.length);
		byte[] result = new byte[size];
		int olen = cipher.processBytes(data, 0, data.length, result, 0);
		olen += cipher.doFinal(result, olen);

		if (olen < size) {
			byte[] tmp = new byte[olen];
			System.arraycopy(result, 0, tmp, 0, olen);
			result = tmp;
		}

		return result;
	}

	/**
	 * Encrypt arbitrary byte array, returning the encrypted data in a different byte array.
	 * @param data
	 * @return
	 * @throws CryptoException
	 */
	private synchronized byte[] encrypt(byte[] data) throws CryptoException {
		if (data == null || data.length == 0) {
			return new byte[0];
		}

		cipher.init(true, key);
		return callCipher(data);
	}

	/**
	 * Encrypts a string.
	 * @param data
	 * @return
	 * @throws CryptoException
	 */
	public String encryptString(String data) throws CryptoException {
		if (data == null || data.length() == 0) {
			return null;
		}

		return StringUtils.toHexString(encrypt(data.getBytes()));
	}

	/**
	 * Decrypts arbitrary data.
	 * @param data
	 * @return
	 * @throws CryptoException
	 */
	private synchronized byte[] decrypt(byte[] data) throws CryptoException {
		if (data == null || data.length == 0) {
			return new byte[0];
		}

		cipher.init(false, key);
		return callCipher(data);
	}

	/**
	 * Decrypts a string that was previously encoded using encryptString.
	 * @param data
	 * @return
	 * @throws CryptoException
	 */
	public String decryptString(final String data) throws CryptoException {
		if (data == null || data.length() == 0) {
			return "";
		}

		return new String(decrypt(StringUtils.fromHexString(data)));
	}
}
