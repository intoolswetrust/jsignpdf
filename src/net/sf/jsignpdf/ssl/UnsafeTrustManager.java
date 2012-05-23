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
package net.sf.jsignpdf.ssl;

import static net.sf.jsignpdf.Constants.RES;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

/**
 * 
 * @author Josef Cacek
 */
public class UnsafeTrustManager implements X509TrustManager {

	private final static Logger LOGGER = Logger.getLogger(UnsafeTrustManager.class);

	private X509TrustManager tm;
	private X509Certificate[] chain;

	public UnsafeTrustManager(X509TrustManager tm) {
		this.tm = tm;
	}

	/**
	 * @return the chain
	 */
	public X509Certificate[] getChain() {
		return chain;
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
	 */
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
	 */
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		this.chain = chain;
		if (tm != null) {
			try {
				tm.checkServerTrusted(chain, authType);
			} catch (CertificateException e) {
				LOGGER.warn(RES.get("console.serverNotTrusted"));
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
	 */
	public X509Certificate[] getAcceptedIssuers() {
		// TODO What shall we return?
		return null;
	}

}
