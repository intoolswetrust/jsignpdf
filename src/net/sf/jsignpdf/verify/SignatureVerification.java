package net.sf.jsignpdf.verify;

import java.util.Arrays;
import java.util.Calendar;

import net.sf.jsignpdf.types.CertificationLevel;

import com.lowagie.text.pdf.PdfSignatureAppearance;

/**
 * This class represents a result of a single signature verification.
 * 
 * @author Josef Cacek
 * @author $Author: stojsavljevic $
 * @version $Revision: 1.10 $
 * @created $Date: 2011/04/18 12:40:07 $
 */
public class SignatureVerification {

	/**
	 * Signed revision has been altered.
	 */
	public static final int SIG_STAT_CODE_ERROR_REVISION_MODIFIED = 120;

	/**
	 * Revision has set certification level but document was modified later.
	 */
	public static final int SIG_STAT_CODE_ERROR_CERTIFICATION_BROKEN = 110;

	/**
	 * There is some unsigned content in document (last signature doesn't cover
	 * whole document).
	 */
	public static final int SIG_STAT_CODE_WARNING_UNSIGNED_CONTENT = 70;

	/**
	 * Signature validity can't be verified.
	 */
	public static final int SIG_STAT_CODE_WARNING_SIGNATURE_VALIDITY_UNKNOWN = 60;

	/**
	 * Signature is invalid according to OCSP.
	 */
	public static final int SIG_STAT_CODE_WARNING_SIGNATURE_OCSP_INVALID = 50;

	/**
	 * There is no timestamp token (signature date/time are from the clock on
	 * the signer's computer)
	 */
	public static final int SIG_STAT_CODE_WARNING_NO_TIMESTAMP_TOKEN = 40;

	/**
	 * Timestamp token is invalid.
	 */
	public static final int SIG_STAT_CODE_WARNING_TIMESTAMP_INVALID = 30;

	/**
	 * No revocation information (CRL or OCSP) found.
	 */
	public static final int SIG_STAT_CODE_WARNING_NO_REVOCATION_INFO = 20;

	/**
	 * Signature is valid.
	 */
	public static final int SIG_STAT_CODE_INFO_SIGNATURE_VALID = 0;

	private String signName;
	private String name;
	private String subject;
	private int revision;
	private boolean wholeDocument;
	private Calendar date;
	private boolean modified;
	private boolean ocspPresent;
	private boolean ocspValid;
	private boolean crlPresent;
	private boolean tsTokenPresent;
	private Exception tsTokenValidationResult;
	private Object[] fails;
	private String reason;
	private String location;
	private int certLevelCode;

	private boolean isLastSignature;

	/**
	 * This flag means that signing certificate is directly trusted (regarding
	 * to chosen KeyStore) and valid (it's not expired and doesn't appear in
	 * CRL).
	 */
	private boolean signCertTrustedAndValid;

	/**
	 * This flag means that OCSP url is found in signing certificate.
	 */
	private boolean ocspInCertPresent;

	/**
	 * This flag is true if OCSP url is found in signing certificate (
	 * <code>ocspInCertPresent</code> is true) and certificate is successfully
	 * validated against that url.
	 */
	private boolean ocspInCertValid;

	/**
	 * Default constructore
	 */
	public SignatureVerification() {
		//nothing to do here
	}

	/**
	 * Constructor, which fills name of signature
	 * 
	 * @param aName
	 *            name of signature
	 */
	public SignatureVerification(final String aName) {
		name = aName;
	}

	/**
	 * Gets validation code for this verification
	 * 
	 * @return validation code defined in {@link SignatureVerification}
	 */
	public int getValidationCode() {
		int code = SignatureVerification.SIG_STAT_CODE_INFO_SIGNATURE_VALID;

		// TODO Handle case when OCRL checking fails
		if (isModified()) {
			// ERROR: signed revision is altered
			code = SignatureVerification.SIG_STAT_CODE_ERROR_REVISION_MODIFIED;
		} else if (!isLastSignature && getCertLevelCode() != PdfSignatureAppearance.NOT_CERTIFIED) {
			// ERROR: some signature has certification level set but document is changed (at least with some additional signatures)
			code = SignatureVerification.SIG_STAT_CODE_ERROR_CERTIFICATION_BROKEN;
		} else if (isLastSignature && !isWholeDocument() && getCertLevelCode() != PdfSignatureAppearance.NOT_CERTIFIED) {
			// ERROR: last signature doesn't cover whole document (document is changed) and certification level set
			code = SignatureVerification.SIG_STAT_CODE_ERROR_CERTIFICATION_BROKEN;
		} else if (isLastSignature && !isWholeDocument()) {
			// WARNING: last signature doesn't cover whole document - there is some unsigned content in the document
			code = SignatureVerification.SIG_STAT_CODE_WARNING_UNSIGNED_CONTENT;
		} else if (!isSignCertTrustedAndValid() && getFails() != null) {
			// WARNING: certificate is not trusted (can't be verified against keystore)
			code = SignatureVerification.SIG_STAT_CODE_WARNING_SIGNATURE_VALIDITY_UNKNOWN;
		} else if (!isSignCertTrustedAndValid() && (isOcspPresent() || isOcspInCertPresent()) && !isOcspValid()
				&& !isOcspInCertValid()) {
			// WARNING: OCSP validation fails
			code = SignatureVerification.SIG_STAT_CODE_WARNING_SIGNATURE_OCSP_INVALID;
		} else if (!isSignCertTrustedAndValid() && !isOcspPresent() && !isOcspInCertPresent() && !isCrlPresent()) {
			// WARNING: No revocation information (CRL or OCSP) found
			code = SignatureVerification.SIG_STAT_CODE_WARNING_NO_REVOCATION_INFO;
		} else if (!isTsTokenPresent()) {
			// WARNING: signature date/time are from the clock on the signer's computer
			code = SignatureVerification.SIG_STAT_CODE_WARNING_NO_TIMESTAMP_TOKEN;
		} else if (isTsTokenPresent() && getTsTokenValidationResult() != null) {
			// WARNING: signature is timestamped but the timestamp could not be verified
			code = SignatureVerification.SIG_STAT_CODE_WARNING_TIMESTAMP_INVALID;
		}
		return code;
	}

	/**
	 * Returns true if given validation code means error.
	 * 
	 * @param validationCode
	 * @return
	 */
	public static boolean isError(final int validationCode) {
		return validationCode >= 100;
	}

	/**
	 * Returns true if the given {@link SignatureVerification} failed.
	 * 
	 * @return true if the validation fails
	 */
	public boolean containsError() {
		return isError(getValidationCode());
	}

	/**
	 * Returns true if given validation code means warning.
	 * 
	 * @param validationCode
	 * @return
	 */
	public static boolean isWarning(final int validationCode) {
		return validationCode >= 10 && validationCode < 100;
	}

	/**
	 * Returns true if the given {@link SignatureVerification} contains
	 * warnings.
	 * 
	 * @return true if the validation fails
	 */
	public boolean containsWarning() {
		return isWarning(getValidationCode());
	}

	/**
	 * Returns true if given validation code means "valid without warnings".
	 * 
	 * @param validationCode
	 * @return
	 */
	public static boolean isValidWithoutWarnings(final int validationCode) {
		return validationCode == SIG_STAT_CODE_INFO_SIGNATURE_VALID;
	}

	/**
	 * Returns true if the given {@link SignatureVerification} contains
	 * warnings.
	 * 
	 * @return true if the validation fails
	 */
	public boolean isValidWithoutWarnings() {
		return isValidWithoutWarnings(getValidationCode());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}

	public boolean isWholeDocument() {
		return wholeDocument;
	}

	public void setWholeDocument(boolean wholeDocument) {
		this.wholeDocument = wholeDocument;
	}

	public Calendar getDate() {
		return date;
	}

	public void setDate(Calendar date) {
		this.date = date;
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public Object[] getFails() {
		return fails;
	}

	public void setFails(Object[] fails) {
		this.fails = fails;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean isOcspPresent() {
		return ocspPresent;
	}

	public void setOcspPresent(boolean ocspPresent) {
		this.ocspPresent = ocspPresent;
	}

	public boolean isOcspValid() {
		return ocspValid;
	}

	public void setOcspValid(boolean ocspValid) {
		this.ocspValid = ocspValid;
	}

	public String getSignName() {
		return signName;
	}

	public void setSignName(String signName) {
		this.signName = signName;
	}

	/**
	 * @return the tsTokenPresent
	 */
	public boolean isTsTokenPresent() {
		return tsTokenPresent;
	}

	/**
	 * @param tsTokenPresent
	 *            the tsTokenPresent to set
	 */
	public void setTsTokenPresent(boolean tsTokenPresent) {
		this.tsTokenPresent = tsTokenPresent;
	}

	/**
	 * @return the tsTokenValidationResult
	 */
	public Exception getTsTokenValidationResult() {
		return tsTokenValidationResult;
	}

	/**
	 * @param tsTokenValidationResult
	 *            the tsTokenValidationResult to set
	 */
	public void setTsTokenValidationResult(Exception tsTokenValidationResult) {
		this.tsTokenValidationResult = tsTokenValidationResult;
	}

	/**
	 * @return the certLevelCode
	 */
	public int getCertLevelCode() {
		return certLevelCode;
	}

	/**
	 * @param certLevelCode
	 *            the certLevelCode to set
	 */
	public void setCertLevelCode(int certLevelCode) {
		this.certLevelCode = certLevelCode;
	}

	/**
	 * Returns instance of {@link CertificationLevel} for the property
	 * certLevelCode.
	 * 
	 * @return
	 */
	public CertificationLevel getCertificationLevel() {
		return CertificationLevel.findCertificationLevel(certLevelCode);
	}

	/**
	 * 
	 * @return the signCertTrustedAndValid
	 */
	public boolean isSignCertTrustedAndValid() {
		return signCertTrustedAndValid;
	}

	/**
	 * @param signCertTrustedAndValid
	 *            the signCertTrustedAndValid to set
	 */
	public void setSignCertTrustedAndValid(boolean signCertTrustedAndValid) {
		this.signCertTrustedAndValid = signCertTrustedAndValid;
	}

	/**
	 * 
	 * @return the crlPresent
	 */
	public boolean isCrlPresent() {
		return crlPresent;
	}

	/**
	 * @param crlPresent
	 *            the crlPresent to set
	 */
	public void setCrlPresent(boolean crlPresent) {
		this.crlPresent = crlPresent;
	}

	/**
	 * 
	 * @return the ocspInCertPresent
	 */
	public boolean isOcspInCertPresent() {
		return ocspInCertPresent;
	}

	/**
	 * 
	 * @param ocspInCertPresent
	 *            the ocspInCertPresent to set
	 */
	public void setOcspInCertPresent(boolean ocspInCertPresent) {
		this.ocspInCertPresent = ocspInCertPresent;
	}

	/**
	 * 
	 * @return the ocspInCertValid
	 */
	public boolean isOcspInCertValid() {
		return ocspInCertValid;
	}

	/**
	 * 
	 * @param ocspInCertValid
	 *            the ocspInCertValid to set
	 */
	public void setOcspInCertValid(boolean ocspInCertValid) {
		this.ocspInCertValid = ocspInCertValid;
	}

	/**
	 * @return the isLastSignature
	 */
	public boolean isLastSignature() {
		return isLastSignature;
	}

	/**
	 * @param isLastSignature
	 *            the isLastSignature to set
	 */
	public void setLastSignature(boolean isLastSignature) {
		this.isLastSignature = isLastSignature;
	}

	public String toString() {
		return "Signature verification [" + "\n signName=" + signName + "\n name=" + name + "\n subject=" + subject
				+ "\n date=" + date.getTime() + "\n reason=" + reason + "\n location=" + location + "\n revision="
				+ revision + "\n wholeDocument=" + wholeDocument + "\n modified=" + modified + "\n certificationLevel="
				+ getCertificationLevel().name() + "\n signCertTrustedAndValid=" + signCertTrustedAndValid
				+ "\n ocspPresent=" + ocspPresent + "\n ocspValid=" + ocspValid + "\n crlPresent=" + crlPresent
				+ "\n ocspInCertPresent=" + ocspInCertPresent + "\n ocspInCertValid=" + ocspInCertValid
				+ "\n timeStampTokenPresent=" + tsTokenPresent + "\n timeStampTokenValidationFail="
				+ (tsTokenValidationResult == null ? "no" : tsTokenValidationResult.getMessage()) + "\n fails="
				+ (fails == null ? "no" : Arrays.asList(fails)) + "\n]";
	}

}
