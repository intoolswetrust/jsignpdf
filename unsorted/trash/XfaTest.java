package net.sf.jsignpdf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.XfaForm;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class XfaTest {

	static {
		org.apache.xml.security.Init.init();
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			PdfReader pdfReader = new PdfReader("c:/Java/docs/JSignPdf-resources/XFA/samples/test.pdf");
			PdfStamper stamper = new PdfStamper(pdfReader, new FileOutputStream(
					"c:/Java/docs/JSignPdf-resources/XFA/samples/test_xfa.pdf"), '\0');
			XfaForm xfaForm = pdfReader.getAcroFields().getXfa();
			System.out.println("isXfaPresent: " + xfaForm.isXfaPresent());
			if (xfaForm.isXfaPresent()) {
				Document doc = xfaForm.getDomDocument();
				serialize(doc);
				Node datasetsNode = xfaForm.getDatasetsNode();
				System.out.println(datasetsNode);

				// Create an XML Signature object from the document, BaseURI and
				// signature algorithm (in this case DSA)
				XMLSignature sig = new XMLSignature(doc, "http://dsig.cacek.cz/", XMLSignature.ALGO_ID_SIGNATURE_RSA);

				datasetsNode.appendChild(sig.getElement());
				{
					// create the transforms object for the Document/Reference
					Transforms transforms = new Transforms(doc);
					// First we have to strip away the signature element (it's
					// not part of the
					// signature calculations). The enveloped transform can be
					// used for this.
					transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
					// Part of the signature element needs to be canonicalized.
					// It is a kind
					// of normalizing algorithm for XML. For more information
					// please take a
					// look at the W3C XML Digital Signature webpage.
					transforms.addTransform(Transforms.TRANSFORM_C14N_WITH_COMMENTS);
					// Add the above Document/Reference
					sig.addDocument("", transforms, org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1);
				}

				{
					KeyStore ks = KeyStore.getInstance("BCPKCS12");
					ks.load(new FileInputStream("k:\\E-podpis\\2009\\exportedCertificateWithPrivateKey.pfx"),
							"OpenOffice.org".toCharArray());
					String alias = ks.aliases().nextElement();
					PrivateKey pk = (PrivateKey) ks.getKey(alias, null);

					// Add in the KeyInfo for the certificate that we used the
					// private key of
					X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

					sig.addKeyInfo(cert);
					sig.addKeyInfo(cert.getPublicKey());
					System.out.println("Start signing");
					sig.sign(pk);
					System.out.println("Finished signing");
				}

				xfaForm.setDomDocument(doc);
				xfaForm.setChanged(true);
				serialize(doc);
				// FileOutputStream f = new FileOutputStream(signatureFile);
				//
				// XMLUtils.outputDOMc14nWithComments(doc, f);
				//
				// f.close();
				// System.out.println("Wrote signature to " + BaseURI);

			}
			int total = pdfReader.getNumberOfPages() + 1;
			for (int i = 1; i < total; i++) {
				pdfReader.setPageContent(i, pdfReader.getPageContent(i));
			}
			stamper.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void serialize(Document doc) throws IOException {
		OutputFormat format = new OutputFormat(doc);
		format.setLineWidth(65);
		format.setIndenting(true);
		format.setIndent(2);
		XMLSerializer serializer = new XMLSerializer(System.out, format);
		serializer.serialize(doc);
	}

}
