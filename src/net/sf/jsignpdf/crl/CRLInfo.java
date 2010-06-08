package net.sf.jsignpdf.crl;

import java.security.cert.CRL;

import net.sf.jsignpdf.Constants;

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
		byteCount = Constants.DEFVAL_SIG_SIZE;
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
