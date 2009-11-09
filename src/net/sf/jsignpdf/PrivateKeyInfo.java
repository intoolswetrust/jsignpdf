package net.sf.jsignpdf;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Helper class (POI) which holds private key and the assigned certificates.
 * @author Josef Cacek
 */
public class PrivateKeyInfo {

	private PrivateKey key;
	private Certificate[] chain;

	public PrivateKeyInfo() {}

	/**
	 * Creates instance and fills fields.
	 * @param key
	 * @param chain
	 */
	public PrivateKeyInfo(PrivateKey key, Certificate[] chain) {
		super();
		this.key = key;
		this.chain = chain;
	}

	/**
	 * @return the key
	 */
	public PrivateKey getKey() {
		return key;
	}
	/**
	 * @param key the key to set
	 */
	public void setKey(PrivateKey key) {
		this.key = key;
	}
	/**
	 * @return the chain
	 */
	public Certificate[] getChain() {
		return chain;
	}
	/**
	 * @param chain the chain to set
	 */
	public void setChain(Certificate[] chain) {
		this.chain = chain;
	}
}
