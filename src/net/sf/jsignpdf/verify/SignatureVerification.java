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

import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;

import net.sf.jsignpdf.types.CertificationLevel;

import com.lowagie.text.pdf.PdfSignatureAppearance;

/**
 * This class represents a result of a single signature verification.
 * 
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.14 $
 * @created $Date: 2012/02/17 11:50:35 $
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
	 * Certificate validity can't be verified.
	 */
	public static final int SIG_STAT_CODE_WARNING_CERTIFICATE_CANT_BE_VERIFIED = 60;

	/**
	 * Certificate expired.
	 */
	public static final int SIG_STAT_CODE_WARNING_CERTIFICATE_EXPIRED = 61;

	/**
	 * Certificate not yet valid.
	 */
	public static final int SIG_STAT_CODE_WARNING_CERTIFICATE_NOT_YET_VALID = 62;

	/**
	 * Certificate revoked.
	 */
	public static final int SIG_STAT_CODE_WARNING_CERTIFICATE_REVOKED = 63;

	/**
	 * Certificate has unsupported critical extension.
	 */
	public static final int SIG_STAT_CODE_WARNING_CERTIFICATE_UNSUPPORTED_CRITICAL_EXTENSION = 64;

	/**
	 * Invalid state. Possible circular certificate chain.
	 */
	public static final int SIG_STAT_CODE_WARNING_CERTIFICATE_INVALID_STATE = 65;

	/**
	 * All certificate errors that are not covered with 60-65 codes.
	 */
	public static final int SIG_STAT_CODE_WARNING_CERTIFICATE_PROBLEM = 66;

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

	// error messages when validating certificate
	private static final String CERT_PROBLEM_CANT_BE_VERIFIED = "Cannot be verified against the KeyStore";
	private static final String CERT_PROBLEM_EXPIRED = "certificate expired on";
	private static final String CERT_PROBLEM_NOT_YET_VALID = "certificate not valid till";
	private static final String CERT_PROBLEM_REVOKED = "Certificate revoked";
	private static final String CERT_PROBLEM_UNSUPPORTED_CRITICAL_EXTENSION = "Has unsupported critical extension";
	private static final String CERT_PROBLEM_INVALID_STATE = "Invalid state. Possible circular certificate chain";

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
	 * Certificate that signed document.
	 */
	private X509Certificate signingCertificate;

	/**
	 * Certification path for signing certificate.
	 */
	private CertPath certPath;

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
		// TODO Handle case when new content is added after signature with certification level set
		if (isModified()) {
			// ERROR: signed revision is altered
			code = SignatureVerification.SIG_STAT_CODE_ERROR_REVISION_MODIFIED;
		} else if (!isLastSignature && getCertLevelCode() == PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED) {
			// ERROR: some signature has certification level set but document is changed (at least with some additional signatures)
			code = SignatureVerification.SIG_STAT_CODE_ERROR_CERTIFICATION_BROKEN;
		} else if (isLastSignature && !isWholeDocument() && getCertLevelCode() != PdfSignatureAppearance.NOT_CERTIFIED) {
			// TODO What if e.g. annotations are added (which is allowed by cert level)?
			// ERROR: last signature doesn't cover whole document (document is changed) and certification level set
			code = SignatureVerification.SIG_STAT_CODE_ERROR_CERTIFICATION_BROKEN;
		} else if (isLastSignature && !isWholeDocument()) {
			// WARNING: last signature doesn't cover whole document - there is some unsigned content in the document
			code = SignatureVerification.SIG_STAT_CODE_WARNING_UNSIGNED_CONTENT;
		} else if (!isSignCertTrustedAndValid() && getFails() != null) {
			// WARNING: there is some problem with certificate
			String errorMessage = String.valueOf(getFails()[1]).trim().toLowerCase();
			if (errorMessage.startsWith(CERT_PROBLEM_CANT_BE_VERIFIED.trim().toLowerCase())) {
				// WARNING: certificate is not trusted (can't be verified against keystore)
				code = SignatureVerification.SIG_STAT_CODE_WARNING_CERTIFICATE_CANT_BE_VERIFIED;
			} else if (errorMessage.startsWith(CERT_PROBLEM_EXPIRED.trim().toLowerCase())) {
				// WARNING: certificate expired
				code = SignatureVerification.SIG_STAT_CODE_WARNING_CERTIFICATE_EXPIRED;
			} else if (errorMessage.startsWith(CERT_PROBLEM_NOT_YET_VALID.trim().toLowerCase())) {
				// WARNING: certificate not yet valid
				code = SignatureVerification.SIG_STAT_CODE_WARNING_CERTIFICATE_NOT_YET_VALID;
			} else if (errorMessage.startsWith(CERT_PROBLEM_REVOKED.trim().toLowerCase())) {
				// WARNING: certificate revoked
				code = SignatureVerification.SIG_STAT_CODE_WARNING_CERTIFICATE_REVOKED;
			} else if (errorMessage.startsWith(CERT_PROBLEM_UNSUPPORTED_CRITICAL_EXTENSION.trim().toLowerCase())) {
				// WARNING: certificate has unsupported critical extension
				code = SignatureVerification.SIG_STAT_CODE_WARNING_CERTIFICATE_UNSUPPORTED_CRITICAL_EXTENSION;
			} else if (errorMessage.startsWith(CERT_PROBLEM_INVALID_STATE.trim().toLowerCase())) {
				// WARNING: possible circular certificate chain
				code = SignatureVerification.SIG_STAT_CODE_WARNING_CERTIFICATE_INVALID_STATE;
			} else {
				// WARNING: some other certificate error
				code = SignatureVerification.SIG_STAT_CODE_WARNING_CERTIFICATE_PROBLEM;
			}
		} else if ((!isCrlPresent() || (isCrlPresent() && getFails() != null)) && !isSignCertTrustedAndValid() && (isOcspPresent() || isOcspInCertPresent()) && !isOcspValid()
				&& !isOcspInCertValid()) {
			// If certificate is successfully validated against CRL - don't set warning flag for OCSP (set OCSP error only if CRL doesn't exist or there are some errors)  
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

	/**
	 * @return the signingCertificate
	 */
	public X509Certificate getSigningCertificate() {
		return signingCertificate;
	}

	/**
	 * @param signingCertificate
	 *            the signingCertificate to set
	 */
	public void setSigningCertificate(X509Certificate signingCertificate) {
		this.signingCertificate = signingCertificate;
	}

	/**
	 * @return the certPath
	 */
	public CertPath getCertPath() {
		return certPath;
	}

	/**
	 * @param certPath
	 *            the certPath to set
	 */
	public void setCertPath(CertPath certPath) {
		this.certPath = certPath;
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
				+ (fails == null ? "no" : Arrays.asList(fails)) + "\n certPath="
				+ (certPath == null ? "no" : certPath.getCertificates()) + "\n signingCertificate="
				+ (signingCertificate == null ? "no" : signingCertificate.toString()) + "\n]";
	}

}
