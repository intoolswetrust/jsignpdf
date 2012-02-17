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
package net.sf.jsignpdf.verify;

import java.util.ArrayList;
import java.util.List;

/**
 * VerificaionResult class is a simple JavaBean for holding results of PDF
 * verification;
 * 
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.4 $
 * @created $Date: 2012/02/17 11:50:35 $
 */
public class VerificationResult {

	/**
	 * Variable for storing Exception if it occures during verification.
	 */
	private Exception exception;
	/**
	 * Number of revisions of PDF document
	 */
	private int totalRevisions;
	/**
	 * List of verification results. One for each signature.
	 */
	private List<SignatureVerification> verifications;

	/**
	 * Most serious warning/error code found in singatures verification.
	 */
	private int verificationResultCode;

	/**
	 * Constructor - initializes fields.
	 */
	public VerificationResult() {
		exception = null;
		totalRevisions = 0;
		verifications = new ArrayList<SignatureVerification>();
		verificationResultCode = SignatureVerification.SIG_STAT_CODE_INFO_SIGNATURE_VALID;
	}

	/**
	 * Adds one signature verification result to the list of results.
	 * 
	 * @param aVerification
	 */
	public void addVerification(final SignatureVerification aVerification) {
		verifications.add(aVerification);
		final int code = aVerification.getValidationCode();
		if (code != SignatureVerification.SIG_STAT_CODE_INFO_SIGNATURE_VALID) {
			if (verificationResultCode < code) {
				verificationResultCode = code;
			}
		}
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public int getTotalRevisions() {
		return totalRevisions;
	}

	public void setTotalRevisions(int totalRevisions) {
		this.totalRevisions = totalRevisions;
	}

	public List<SignatureVerification> getVerifications() {
		return verifications;
	}

	/**
	 * @return the verificationResultCode
	 */
	public int getVerificationResultCode() {
		return verificationResultCode;
	}

}
