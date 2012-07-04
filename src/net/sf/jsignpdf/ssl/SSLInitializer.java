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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import net.sf.jsignpdf.Constants;

/**
 * 
 * @author Josef Cacek
 */
public class SSLInitializer {

	public static final void init() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException,
			CertificateException, IOException {
		if (Constants.RELAX_SSL_SECURITY) {
			//Details for the properties - http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html
			//Workaround for http://sourceforge.net/tracker/?func=detail&atid=1037906&aid=3491269&group_id=216921
			System.setProperty("jsse.enableSNIExtension", "false");

			//just in case...
			System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
			System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "true");

			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
		}

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] { new DynamicX509TrustManager() }, null);

		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
	}
}
