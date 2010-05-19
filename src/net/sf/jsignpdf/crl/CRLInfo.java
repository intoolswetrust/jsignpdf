package net.sf.jsignpdf.crl;

import java.security.cert.CRL;

/**
 * Helper bean for holding CRL related data.
 * 
 * @author Josef Cacek
 * 
 */
public class CRLInfo {
	private CRL[] crls;
	private long byteCount;

	/**
	 * Default ctor
	 */
	public CRLInfo() {
		crls = null;
		byteCount = 0L;
	}

	/**
	 * Full ctor
	 * 
	 * @param crls
	 * @param byteCount
	 */
	public CRLInfo(final CRL[] crls, final long byteCount) {
		super();
		this.crls = crls;
		this.byteCount = byteCount;
	}

	public CRL[] getCrls() {
		return crls;
	}

	public void setCrls(CRL[] crls) {
		this.crls = crls;
	}

	public long getByteCount() {
		return byteCount;
	}

	public void setByteCount(long byteCount) {
		this.byteCount = byteCount;
	}
}
