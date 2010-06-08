package net.sf.jsignpdf.crl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import net.sf.jsignpdf.Constants;

import org.apache.commons.io.input.CountingInputStream;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

/**
 * CRL utils.
 * 
 * @author Josef Cacek
 */
public class CRLUtils {

	/**
	 * Returns CRLs retrieved from distribution points for the given
	 * certificate.
	 * 
	 * @param aCert
	 * @param aProxy
	 * @return
	 */
	public static CRLInfo getCRLs(final X509Certificate aCert, final Proxy aProxy) {
		final Set<CRL> crlSet = new HashSet<CRL>();
		long byteCount = 0;
		for (final String urlStr : getCrlUrls(aCert)) {
			try {
				final URL tmpUrl = new URL(urlStr);
				final CountingInputStream inStream = new CountingInputStream(tmpUrl.openConnection(aProxy)
						.getInputStream());
				final CertificateFactory cf = CertificateFactory.getInstance("X.509");
				final CRL crl = cf.generateCRL(inStream);
				if (!crlSet.contains(crl)) {
					byteCount += inStream.getByteCount();
					crlSet.add(crl);
				}
				inStream.close();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CertificateException e) {
				e.printStackTrace();
			} catch (CRLException e) {
				e.printStackTrace();
			}
		}
		// CRLs are stored twice c.f.
		// PdfPKCS7.getAuthenticatedAttributeBytes
		return new CRLInfo(crlSet.toArray(new CRL[crlSet.size()]), Constants.DEFVAL_SIG_SIZE + byteCount * 2L);
	}

	/**
	 * Returns (initialized, but maybe empty) set of URLs of CRLs for given
	 * certificate.
	 * 
	 * @param aCert
	 *            X509 certificate.
	 * @return
	 */
	public static Set<String> getCrlUrls(final X509Certificate aCert) {
		final Set<String> tmpResult = new HashSet<String>();
		final byte[] crlDPExtension = aCert.getExtensionValue(X509Extensions.CRLDistributionPoints.getId());
		if (crlDPExtension != null) {
			CRLDistPoint crlDistPoints = null;
			try {
				crlDistPoints = CRLDistPoint.getInstance(X509ExtensionUtil.fromExtensionValue(crlDPExtension));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			if (crlDistPoints != null) {
				final DistributionPoint[] distPoints = crlDistPoints.getDistributionPoints();
				for (DistributionPoint dp : distPoints) {
					final DistributionPointName dpName = dp.getDistributionPoint();

					final GeneralNames generalNames = (GeneralNames) dpName.getName();
					if (generalNames != null) {
						final GeneralName[] generalNameArr = generalNames.getNames();
						if (generalNameArr != null) {
							for (final GeneralName generalName : generalNameArr) {
								if (generalName.getTagNo() == GeneralName.uniformResourceIdentifier) {
									final DERString derString = (DERString) generalName.getName();
									final String uri = derString.getString();
									if (uri != null && (uri.startsWith("http") || uri.startsWith("ftp"))) {
										tmpResult.add(derString.getString());
									}
								}
							}
						}
					}
				}
			}
		}
		return tmpResult;
	}

}
