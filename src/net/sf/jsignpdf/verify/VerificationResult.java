package net.sf.jsignpdf.verify;

import java.util.ArrayList;
import java.util.List;

/**
 * VerificaionResult class is a simple JavaBean for holding results of PDF verification;
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.1 $
 * @created $Date: 2008/06/13 13:27:45 $
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
	 * Constructor - initializes fields.
	 */
	public VerificationResult() {
		exception = null;
		totalRevisions = 0;
		verifications = new ArrayList<SignatureVerification>();
	}
	
	/**
	 * Adds one signature verification result to the list of results.
	 * @param aVerification
	 */
	public void addVerification(final SignatureVerification aVerification) {
		verifications.add(aVerification);
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
}
