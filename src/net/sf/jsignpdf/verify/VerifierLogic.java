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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.KeyStoreUtils;
import net.sf.jsignpdf.utils.PdfUtils;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.SingleResp;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.OcspClientBouncyCastle;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfPKCS7.X509Name;
import com.lowagie.text.pdf.PdfReader;

/**
 * Class VerifierLogic contains all logic for PDF signatures verification. It
 * uses only system default keystore by default, but you can add additional
 * certificates from external files using {@link #addX509CertFile(String)}
 * method.
 * 
 * @author Josef Cacek
 * @author Aleksandar Stojsavljevic
 * @version $Revision: 1.21 $
 * @created $Date: 2012/02/17 11:50:36 $
 */
public class VerifierLogic {

	private KeyStore kall;
	private boolean failFast;

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
	public VerificationResult verify(final String aFileName, byte[] aPassword) {

		try {
			return verify(PdfUtils.getPdfReader(aFileName, aPassword));
		} catch (Exception e) {
			final VerificationResult tmpResult = new VerificationResult();
			tmpResult.setException(e);
			return tmpResult;
		}
	}

	/**
	 * Verifies signature(s) in PDF document.
	 * 
	 * @param content
	 *            content of PDF
	 * @param aPassword
	 *            password
	 * @return
	 */
	public VerificationResult verify(final byte[] content, byte[] aPassword) {

		try {
			return verify(PdfUtils.getPdfReader(content, aPassword));
		} catch (Exception e) {
			final VerificationResult tmpResult = new VerificationResult();
			tmpResult.setException(e);
			return tmpResult;
		}
	}

	/**
	 * Verifies signature(s) in PDF document.
	 * 
	 * @param tmpReader
	 *            PdfReader for given PDF
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private VerificationResult verify(final PdfReader tmpReader) {
		final VerificationResult tmpResult = new VerificationResult();
		try {
			final AcroFields tmpAcroFields = tmpReader.getAcroFields();
			final List<String> tmpNames = tmpAcroFields.getSignatureNames();
			tmpResult.setTotalRevisions(tmpAcroFields.getTotalRevisions());

			final int lastSignatureIdx = tmpNames.size() - 1;
			for (int i = lastSignatureIdx; i >= 0; i--) {
				final String name = tmpNames.get(i);
				final SignatureVerification tmpVerif = new SignatureVerification(name);
				tmpVerif.setLastSignature(i == lastSignatureIdx);
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
				tmpVerif.setCrlPresent(pk.getCRLs() != null && pk.getCRLs().size() > 0);
				tmpVerif.setFails(PdfPKCS7.verifyCertificates(pkc, kall, pk.getCRLs(), tmpVerif.getDate()));
				tmpVerif.setSigningCertificate(pk.getSigningCertificate());

				// generate CertPath
				List<Certificate> certList = Arrays.asList(pkc);
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				CertPath cp = cf.generateCertPath(certList);
				tmpVerif.setCertPath(cp);

				// to save time - check OCSP in certificate only if document's OCSP is not present and valid
				if (!tmpVerif.isOcspValid()) {
					// try to get OCSP url from signing certificate 
					String url = PdfPKCS7.getOCSPURL((X509Certificate) pk.getSigningCertificate());
					tmpVerif.setOcspInCertPresent(url != null);

					if (url != null) {
						// OCSP url is found in signing certificate - verify certificate with that url
						tmpVerif.setOcspInCertValid(validateCertificateOCSP(pkc, url));
					}
				}

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
				if (failFast && tmpVerif.containsError()) {
					return tmpResult;
				}
			}
		} catch (Exception e) {
			tmpResult.setException(e);
		}
		return tmpResult;
	}

	/**
	 * @return the failFast
	 */
	public boolean isFailFast() {
		return failFast;
	}

	/**
	 * @param failFast
	 *            the failFast to set
	 */
	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
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
		final PdfReader tmpReader = PdfUtils.getPdfReader(aFileName, aPassword);
		final AcroFields tmpAcroFields = tmpReader.getAcroFields();
		return tmpAcroFields.extractRevision(aSignatureName);
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

			// check TS token's certificate against keystore
			if (certs.size() == 1) {
				boolean verifyTimestampCertificates = PdfPKCS7.verifyTimestampCertificates(token, kall, null);
				if (!verifyTimestampCertificates) {
					throw new Exception("Certificate can't be verified agains keystore.");
				}
			} else {
				int certSize = certs.size();
				Certificate[] array = certs.toArray(new Certificate[certSize]);
				Certificate[] certArray = new Certificate[certSize];
				// reverse order
				for (int i = 0; i < certSize; i++) {
					certArray[i] = array[certSize - 1 - i];
				}
				// token.validate(SignerInformationVerifier) will check if certificate has been valid at the time the timestamp was created
				Object[] verifyCertificates = PdfPKCS7.verifyCertificates(certArray, kall, null, null);
				if (verifyCertificates != null) {
					throw new Exception("Certificate can't be verified agains keystore.");
				}
			}

			SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder().build(certificate);
			token.validate(verifier);
		} catch (Exception e) {
			return e;
		}
		return null;
	}

	/**
	 * Validates certificate (chain) using OCSP.
	 * 
	 * @param pkc
	 *            certificate chain, 3rd certificate will be validated (pkc[2])
	 * @param url
	 *            OCSP url for validation
	 * @return
	 */
	private static boolean validateCertificateOCSP(Certificate pkc[], String url) {
		if (pkc.length < 2) {
			return false;
		}

		try {
			OcspClientBouncyCastle ocspClient = new OcspClientBouncyCastle((X509Certificate) pkc[2],
					(X509Certificate) pkc[1], url);
			// TODO implement proxy support
//			ocspClient.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888)));

			byte[] encoded = ocspClient.getEncoded();

			ASN1InputStream inp = new ASN1InputStream(encoded);
			BasicOCSPResponse resp = BasicOCSPResponse.getInstance(inp.readObject());
			org.bouncycastle.ocsp.BasicOCSPResp basicResp = new org.bouncycastle.ocsp.BasicOCSPResp(resp);

			SingleResp sr = basicResp.getResponses()[0];
			CertificateID cid = sr.getCertID();
			X509Certificate sigcer = (X509Certificate) pkc[2];
			X509Certificate isscer = (X509Certificate) pkc[1];
			CertificateID tis = new CertificateID(CertificateID.HASH_SHA1, isscer, sigcer.getSerialNumber());
			return tis.equals(cid);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Gets validation codes for all signatures in document.
	 * 
	 * @param verResult
	 * @return
	 */
	public static Map<String, Integer> getValidationCodes(VerificationResult verResult) {
		final Map<String, Integer> validationCodes = new HashMap<String, Integer>();

		for (SignatureVerification verification : verResult.getVerifications()) {
			validationCodes.put(verification.getName(), verification.getValidationCode());
		}

		return validationCodes;
	}

}
