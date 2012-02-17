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
