package net.sf.jsignpdf.verify;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfPKCS7.X509Name;

/**
 * Class VerifierLogic contains all logic for PDF signatures verification.
 * It uses only system default keystore by default, but you can add additional
 * certificates from external files using {@link #addX509CertFile(String)} method.
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.2 $
 * @created $Date: 2008/06/16 08:07:37 $
 */
public class VerifierLogic {

	private KeyStore kall;

	/**
	 * Constructor. It initializes default keystore.
	 */
	public VerifierLogic() {
		reinitKeystore();
	}

	/**
	 * Adds X.509 certificates from given file. If any Exception occures,
	 * it's not throwed but returned as a result of this method.
	 * @param aPath full path to the file with certificate(s)
	 * @return Exception if any throwed during adding.
	 */
	@SuppressWarnings("unchecked")
	public Exception addX509CertFile(final String aPath) {
		try {
			final CertificateFactory tmpCertFac = CertificateFactory.getInstance("X509"); //X.509 ?
			final Collection<X509Certificate> tmpCertCol = 
				(Collection<X509Certificate>) 
				tmpCertFac.generateCertificates(new FileInputStream(aPath));
			for (X509Certificate tmpCert : tmpCertCol) {
				kall.setCertificateEntry(tmpCert.getSerialNumber().toString(Character.MAX_RADIX), tmpCert);
			}
		} catch (Exception e) {
			return e;
		}
		return null;
	}

	/**
	 * Initializes keystore (load certificates from default keystore). 
	 * All previously added certificates from external files are forgotten.
	 */
	public void reinitKeystore() {
		kall = PdfPKCS7.loadCacertsKeyStore();
	}

	/**
	 * Verifies signature(s) in PDF document.
	 * @param aFileName path to a verified PDF file
	 * @param aPassword PDF password - used if PDF is encrypted 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public VerificationResult verify(final String aFileName, byte[] aPassword) {
		final VerificationResult tmpResult = new VerificationResult();
		try {
			final PdfReader tmpReader = getPdfReader(aFileName, aPassword);

			final AcroFields tmpAcroFields = tmpReader.getAcroFields();
			final ArrayList<String> tmpNames = tmpAcroFields.getSignatureNames();
			tmpResult.setTotalRevisions(tmpAcroFields.getTotalRevisions());
			
			for (String name : tmpNames) {
				final SignatureVerification tmpVerif = new SignatureVerification(name);
				tmpVerif.setWholeDocument(tmpAcroFields.signatureCoversWholeDocument(name));
				tmpVerif.setRevision(tmpAcroFields.getRevision(name));
				final PdfPKCS7 pk = tmpAcroFields.verifySignature(name);
				tmpVerif.setDate(pk.getSignDate());
				final Certificate pkc[] = pk.getCertificates();
				final X509Name tmpX509Name = PdfPKCS7.getSubjectFields(pk.getSigningCertificate());
				//TODO read more details from X509Name ?
				tmpVerif.setSubject(tmpX509Name.toString());
				tmpVerif.setModified(!pk.verify());
				//TODO revocation list and date to which should be verified?
				tmpVerif.setFails(PdfPKCS7.verifyCertificates(pkc, kall, null, tmpVerif.getDate()));
				tmpResult.addVerification(tmpVerif);
			}
		} catch (Exception e) {
			tmpResult.setException(e);
		}
		return tmpResult;
	}

	/**
	 * Returns InputStream which contains extracted revision (PDF) which was signed
	 * with signature of given name.
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
	 * @param aFileName file name of PDF
	 * @param aPassword password
	 * @return
	 * @throws IOException 
	 */
	public static PdfReader getPdfReader(final String aFileName, byte[] aPassword) throws IOException {
		PdfReader tmpReader = null;
		try {
			//try to read without password
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
	
}
