package net.sf.jsignpdf.verify;

import java.security.cert.X509Certificate;

/**
 * Helper class to hold together X509Certificate and alias used in keystore.
 * 
 * @author Josef Cacek
 */
public class CertificateWithAlias {

	private String alias;
	private X509Certificate certificate;

	public CertificateWithAlias(String alias, X509Certificate certificate) {
		super();
		this.alias = alias;
		this.certificate = certificate;
	}

	/**
	 * Returns alias of certificate
	 * 
	 * @return
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Returns X509Certificate instance
	 * 
	 * @return
	 */
	public X509Certificate getCertificate() {
		return certificate;
	}

}