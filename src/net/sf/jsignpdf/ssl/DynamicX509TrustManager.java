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

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.KeyStoreUtils;

/**
 * TrustManager which works with in-memory copy of cacerts truststore. If
 * {@link Constants#RELAX_SSL_SECURITY} is true then it adds missing server
 * certificates to the truststore.
 * 
 * @author Josef Cacek
 */
public class DynamicX509TrustManager implements X509TrustManager {

	private final KeyStore trustStore;
	private final TrustManagerFactory trustManagerFactory;

	private X509TrustManager trustManager;

	/**
	 * Constructor.
	 * 
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 */
	public DynamicX509TrustManager() {
		try {
			this.trustStore = KeyStoreUtils.createTrustStore();
			trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			reloadTrustStore();
		} catch (Exception e) {
			throw new RuntimeException("Unable to create TrustManager.", e);
		}
	}

	/**
	 * Checks client's cert-chain - no extra step here.
	 * 
	 * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[],
	 *      java.lang.String)
	 */
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		trustManager.checkClientTrusted(chain, authType);
	}

	/**
	 * Checks server's cert-chain. If check fails and
	 * {@link Constants#RELAX_SSL_SECURITY} is true then the first certificate
	 * from the chain is added to the truststore and the check is repeated.
	 * 
	 * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[],
	 *      java.lang.String)
	 */
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (Constants.RELAX_SSL_SECURITY) {
			try {
				trustManager.checkServerTrusted(chain, authType);
			} catch (CertificateException cx) {
				try {
					trustStore.setCertificateEntry(UUID.randomUUID().toString(), chain[0]);
					reloadTrustStore();
				} catch (Exception e) {
					throw new CertificateException("Unable to recreate TrustManager", e);
				}
				trustManager.checkServerTrusted(chain, authType);
			}
		} else {
			trustManager.checkServerTrusted(chain, authType);
		}
	}

	/* (non-Javadoc)
	 * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
	 */
	public X509Certificate[] getAcceptedIssuers() {
		return trustManager.getAcceptedIssuers();
	}

	/**
	 * Reloads the in-memory trustore.
	 * 
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 */
	private void reloadTrustStore() throws KeyStoreException, NoSuchAlgorithmException {
		trustManagerFactory.init(trustStore);
		// acquire X509 trust manager from factory
		TrustManager tms[] = trustManagerFactory.getTrustManagers();
		for (int i = 0; i < tms.length; i++) {
			if (tms[i] instanceof X509TrustManager) {
				trustManager = (X509TrustManager) tms[i];
				return;
			}
		}

		throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");
	}

}
