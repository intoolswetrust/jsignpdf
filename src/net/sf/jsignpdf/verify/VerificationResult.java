package net.sf.jsignpdf.verify;

import java.util.ArrayList;
import java.util.List;

/**
 * VerificaionResult class is a simple JavaBean for holding results of PDF
 * verification;
 * 
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.2 $
 * @created $Date: 2011/04/16 13:08:55 $
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
			if (verificationResultCode == SignatureVerification.SIG_STAT_CODE_INFO_SIGNATURE_VALID
					|| verificationResultCode > code) {
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
