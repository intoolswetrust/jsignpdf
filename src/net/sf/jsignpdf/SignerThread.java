package net.sf.jsignpdf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;

/**
 * Main logic of signer application. It uses iText to create signature in PDF.
 * To avoid update GUI problems it's implemented as a thread.
 * @author Josef Cacek
 */
public class SignerThread extends Thread {

	protected final ResourceProvider res = ResourceProvider.getInstance();

	private final PrintWriter outWriter;
	private final String ksType;
	private final String ksFile;
	private final char[] passwd;
	private final String inFile;
	private final String outFile;
	private final String reason;
	private final String location;
	private SignResultListener listener;

	/**
	 * Contructor with all necessary parameters.
	 * @param aWriter Output writer
	 * @param aKsType key store type (e.g. "jks" or "pkcs12")
	 * @param aKsFile key store file
	 * @param aPasswd password for keystore
	 * @param aInFile input PDF file name
	 * @param aOutFile output PDF file name
	 * @param aReason signature reason
	 * @param aLocation location
	 */
	public SignerThread(final PrintWriter aWriter,
			final String aKsType, final String aKsFile, final char[] aPasswd,
			final String aInFile, final String aOutFile, final String aReason, final String aLocation) {
		outWriter = aWriter;
		ksType = aKsType;
		ksFile = aKsFile;
		passwd = aPasswd;
		inFile = aInFile;
		outFile = aOutFile;
		reason = aReason;
		location = aLocation;
	}

	/**
	 * Logs localized message to PrintWriter
	 * @param aKey message key
	 */
	void log(final String aKey) {
		log(aKey, (String[]) null);
	}

	/**
	 * Logs localized message to PrintWriter
	 * @param aKey message key
	 * @param anArg message parameter
	 */
	void log(final String aKey, final String anArg) {
		log(aKey, anArg==null? null: new String[] {anArg});
	}

	/**
	 * Logs localized message to PrintWriter
	 * @param aKey message key
	 * @param anArgs message parameters
	 */
	void log(final String aKey, final String[] anArgs) {
		outWriter.println(res.get(aKey, anArgs));
	}

	public void run() {
		boolean tmpResult = false;
		try {
			log("console.getKeystoreType", ksType);
			final KeyStore ks = KeyStore.getInstance(ksType);
			log("console.loadKeystore", ksFile);
			ks.load(new FileInputStream(ksFile),passwd);
			log("console.getAliases");
			final String alias = (String) ks.aliases().nextElement();
			log("console.getPrivateKey");
			final PrivateKey key = (PrivateKey) ks.getKey(alias, passwd);
			log("console.getCertChain");
			final Certificate[] chain = ks.getCertificateChain(alias);
			log("console.createPdfReader", inFile);
			final PdfReader reader = new PdfReader(inFile);
			log("console.createOutPdf", outFile);
			final FileOutputStream fout = new FileOutputStream(outFile);

			log("console.createSignature");
			final PdfStamper stp =
				PdfStamper.createSignature(reader, fout, '\0');
			final PdfSignatureAppearance sap = stp.getSignatureAppearance();

			sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
			log("console.setReason", reason);
			sap.setReason(reason);
			log("console.setLocation", location);
			sap.setLocation(location);

			log("console.processing");
			stp.close();
			fout.close();

			tmpResult = true;
		} catch (Exception e) {
			log("console.exception");
			e.printStackTrace(outWriter);
		} catch (OutOfMemoryError e) {
			e.printStackTrace(outWriter);
			log("console.memoryError");
		}
		log("console.finished." + (tmpResult?"ok":"error"));
		if (listener != null) {
			listener.signerFinishedEvent(tmpResult);
		}
	}

	/**
	 * @return the listener
	 */
	public SignResultListener getListener() {
		return listener;
	}

	/**
	 * @param listener the listener to set
	 */
	public void setListener(SignResultListener listener) {
		this.listener = listener;
	}
}
