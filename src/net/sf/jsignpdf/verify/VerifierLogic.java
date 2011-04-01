package net.sf.jsignpdf.verify;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.KeyStoreUtils;

import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfPKCS7.X509Name;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;

/**
 * Class VerifierLogic contains all logic for PDF signatures verification. It
 * uses only system default keystore by default, but you can add additional
 * certificates from external files using {@link #addX509CertFile(String)}
 * method.
 * 
 * @author Josef Cacek
 * @author $Author: stojsavljevic $
 * @version $Revision: 1.12 $
 * @created $Date: 2011/04/01 12:17:04 $
 */
public class VerifierLogic {

	private KeyStore kall;

	/**
	 * Constructor. It initializes default keystore.
	 */
	public VerifierLogic(final String aType, final String aKeyStore, final String aPasswd) {
		reinitKeystore(aType, aKeyStore, aPasswd);
	}

	/**
	 * Adds X.509 certificates from given file. If any Exception occures, it's
	 * not throwed but returned as a result of this method.
	 * 
	 * @param aPath
	 *            full path to the file with certificate(s)
	 * @return Exception if any throwed during adding.
	 */
	@SuppressWarnings("unchecked")
	public Exception addX509CertFile(final String aPath) {
		try {
			final CertificateFactory tmpCertFac = CertificateFactory.getInstance(Constants.CERT_TYPE_X509); // X.509
			// ?
			final Collection<X509Certificate> tmpCertCol = (Collection<X509Certificate>) tmpCertFac
					.generateCertificates(new FileInputStream(aPath));
			for (X509Certificate tmpCert : tmpCertCol) {
				kall.setCertificateEntry(tmpCert.getSerialNumber().toString(Character.MAX_RADIX), tmpCert);
			}
		} catch (Exception e) {
			return e;
		}
		return null;
	}

	/**
	 * Initializes keystore (load certificates from default keystore). All
	 * previously added certificates from external files are forgotten.
	 */
	public void reinitKeystore(String aKsType, final String aKeyStore, final String aPasswd) {
		try {
			kall = KeyStoreUtils.createKeyStore();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		final KeyStore ksToImport = KeyStoreUtils.loadKeyStore(aKsType, aKeyStore, aPasswd);
		if (ksToImport != null) {
			KeyStoreUtils.copyCertificates(ksToImport, kall);
		}
	}

	/**
	 * Verifies signature(s) in PDF document.
	 * 
	 * @param aFileName
	 *            path to a verified PDF file
	 * @param aPassword
	 *            PDF password - used if PDF is encrypted
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public VerificationResult verify(final String aFileName, byte[] aPassword) {
		final VerificationResult tmpResult = new VerificationResult();
		try {
			final PdfReader tmpReader = getPdfReader(aFileName, aPassword);

			final AcroFields tmpAcroFields = tmpReader.getAcroFields();
			final List<String> tmpNames = tmpAcroFields.getSignatureNames();
			tmpResult.setTotalRevisions(tmpAcroFields.getTotalRevisions());

			for (int i = tmpNames.size() - 1; i >= 0; i--) {
				final String name = tmpNames.get(i);
				final SignatureVerification tmpVerif = new SignatureVerification(name);
				tmpVerif.setWholeDocument(tmpAcroFields.signatureCoversWholeDocument(name));
				tmpVerif.setRevision(tmpAcroFields.getRevision(name));
				final PdfPKCS7 pk = tmpAcroFields.verifySignature(name);
				final TimeStampToken tst = pk.getTimeStampToken();
				tmpVerif.setTsTokenPresent(tst != null);
				tmpVerif.setTsTokenValidationResult(validateTimeStampToken(tst));
				tmpVerif.setDate(pk.getTimeStampDate() != null ? pk.getTimeStampDate() : pk.getSignDate());
				tmpVerif.setLocation(pk.getLocation());
				tmpVerif.setReason(pk.getReason());
				tmpVerif.setSignName(pk.getSignName());
				final Certificate pkc[] = pk.getCertificates();
				final X509Name tmpX509Name = PdfPKCS7.getSubjectFields(pk.getSigningCertificate());
				tmpVerif.setSubject(tmpX509Name.toString());
				tmpVerif.setModified(!pk.verify());
				tmpVerif.setOcspPresent(pk.getOcsp() != null);
				tmpVerif.setOcspValid(pk.isRevocationValid());
				tmpVerif.setFails(PdfPKCS7.verifyCertificates(pkc, kall, pk.getCRLs(), tmpVerif.getDate()));

				String certificateAlias = kall.getCertificateAlias(pk.getSigningCertificate());
				if (certificateAlias != null) {
					// this means that signing certificate is directly trusted

					String verifyCertificate = PdfPKCS7.verifyCertificate(pk.getSigningCertificate(), pk.getCRLs(),
							tmpVerif.getDate());
					if (verifyCertificate == null) {
						// this means that signing certificate is valid
						tmpVerif.setSignCertTrustedAndValid(true);
					}
				}

				final InputStream revision = tmpAcroFields.extractRevision(name);
				try {
					final PdfReader revisionReader = new PdfReader(revision);
					tmpVerif.setCertLevelCode(revisionReader.getCertificationLevel());
				} finally {
					if (revision != null) {
						revision.close();
					}
				}
				tmpResult.addVerification(tmpVerif);
			}
		} catch (Exception e) {
			tmpResult.setException(e);
		}
		return tmpResult;
	}

	/**
	 * Returns InputStream which contains extracted revision (PDF) which was
	 * signed with signature of given name.
	 * 
	 * @param aFileName
	 * @param aPassword
	 * @param aSignatureName
	 * @return
	 * @throws IOException
	 */
	public InputStream extractRevision(String aFileName, byte[] aPassword, String aSignatureName) throws IOException {
		final PdfReader tmpReader = getPdfReader(aFileName, aPassword);
		final AcroFields tmpAcroFields = tmpReader.getAcroFields();
		return tmpAcroFields.extractRevision(aSignatureName);
	}

	/**
	 * It tries to create PDF reader in 3 steps:
	 * <ul>
	 * <li>without password</li>
	 * <li>with empty password</li>
	 * <li>with given password</li>
	 * </ul>
	 * 
	 * @param aFileName
	 *            file name of PDF
	 * @param aPassword
	 *            password
	 * @return
	 * @throws IOException
	 */
	public static PdfReader getPdfReader(final String aFileName, byte[] aPassword) throws IOException {
		PdfReader tmpReader = null;
		try {
			// try to read without password
			tmpReader = new PdfReader(aFileName);
		} catch (Exception e) {
			try {
				tmpReader = new PdfReader(aFileName, new byte[0]);
			} catch (Exception e2) {
				tmpReader = new PdfReader(aFileName, aPassword);
			}
		}
		return tmpReader;
	}

	/**
	 * Returns keystore used in verifier.
	 * 
	 * @return used keystore
	 */
	public KeyStore getKeyStore() {
		return kall;
	}

	public Exception validateTimeStampToken(TimeStampToken token) {
		if (token == null) {
			return null;
		}
		try {
			SignerId signer = token.getSID();

			X509Certificate certificate = null;
			X500Principal sign_cert_issuer = signer.getIssuer();
			BigInteger sign_cert_serial = signer.getSerialNumber();

			CertStore store = token.getCertificatesAndCRLs("Collection", "BC");

			// Iterate CertStore to find a signing certificate
			Collection<? extends Certificate> certs = store.getCertificates(null);
			Iterator<? extends Certificate> iter = certs.iterator();
			while (iter.hasNext()) {
				X509Certificate cert = (X509Certificate) iter.next();
				if (cert.getIssuerX500Principal().equals(sign_cert_issuer)
						&& cert.getSerialNumber().equals(sign_cert_serial)) {
					certificate = cert;
					break;
				}
			}

			if (certificate == null) {
				throw new TSPException("Missing signing certificate for TSA.");
			}

			SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder().build(certificate);
			token.validate(verifier);
		} catch (Exception e) {
			return e;
		}
		return null;
	}

	/**
	 * Gets validation codes for all signatures in document.
	 * 
	 * @param verResult
	 * @return
	 */
	public static Map<String, Integer> getValidationCodes(VerificationResult verResult) {
		Map<String, Integer> validationCodes = new HashMap<String, Integer>();
		List<SignatureVerification> verifications = verResult.getVerifications();
		int i = verifications.size();

		for (SignatureVerification verification : verifications) {
			Integer code = getValidationCode(verification, i == verifications.size());
			validationCodes.put(verification.getName(), code);
			i--;
		}

		return validationCodes;
	}

	/**
	 * Gets validation code for provided <code>SignatureVerification</code>
	 * (single revision).
	 * 
	 * @param verification
	 * @param isLastSignature
	 * @return
	 */
	public static Integer getValidationCode(SignatureVerification verification, boolean isLastSignature) {
		Integer code = null;

		// TODO Handle case when OCRL checking fails
		if (verification.isModified()) {
			// ERROR: signed revision is altered
			code = SignatureVerification.SIG_STAT_CODE_ERROR_REVISION_MODIFIED;
		} else if (!isLastSignature && verification.getCertLevelCode() != PdfSignatureAppearance.NOT_CERTIFIED) {
			// ERROR: some signature has certification level set but document is changed (at least with some additional signatures)
			code = SignatureVerification.SIG_STAT_CODE_ERROR_CERTIFICATION_BROKEN;
		} else if (isLastSignature && !verification.isWholeDocument()
				&& verification.getCertLevelCode() != PdfSignatureAppearance.NOT_CERTIFIED) {
			// ERROR: last signature doesn't cover whole document (document is changed) and certification level set
			code = SignatureVerification.SIG_STAT_CODE_ERROR_CERTIFICATION_BROKEN;
		} else if (isLastSignature && !verification.isWholeDocument()) {
			// WARNING: last signature doesn't cover whole document - there is some unsigned content in the document
			code = SignatureVerification.SIG_STAT_CODE_WARNING_UNSIGNED_CONTENT;
		} else if (!verification.isSignCertTrustedAndValid() && verification.getFails() != null) {
			// WARNING: certificate is not trusted (can't be verified against keystore)
			code = SignatureVerification.SIG_STAT_CODE_WARNING_SIGNATURE_VALIDITY_UNKNOWN;
		} else if (!verification.isSignCertTrustedAndValid() && verification.isOcspPresent()
				&& !verification.isOcspValid()) {
			// WARNING: OCSP validation fails
			code = SignatureVerification.SIG_STAT_CODE_WARNING_SIGNATURE_OCSP_INVALID;
		} else if (!verification.isTsTokenPresent()) {
			// WARNING: signature date/time are from the clock on the signer's computer
			code = SignatureVerification.SIG_STAT_CODE_WARNING_NO_TIMESTAMP_TOKEN;
		} else if (verification.isTsTokenPresent() && verification.getTsTokenValidationResult() != null) {
			// WARNING: signature is timestamped but the timestamp could not be verified
			code = SignatureVerification.SIG_STAT_CODE_WARNING_TIMESTAMP_INVALID;
		} else {
			// INFO: signature is valid
			code = SignatureVerification.SIG_STAT_CODE_INFO_SIGNATURE_VALID;
		}

		return code;
	}

}
