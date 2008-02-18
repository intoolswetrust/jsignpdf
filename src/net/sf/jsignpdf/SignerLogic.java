package net.sf.jsignpdf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;

/**
 * Main logic of signer application. It uses iText to create signature in PDF.
 * @author Josef Cacek
 */
public class SignerLogic implements Runnable {

	protected final ResourceProvider res = ResourceProvider.getInstance();

	private SignerOptions options;

	/**
	 * Constructor with all necessary parameters.
	 * @param anOptions options of signer
	 */
	public SignerLogic(final SignerOptions anOptions) {
		if (anOptions==null) {
			throw new NullPointerException("Options has to be filled.");
		}
		options = anOptions;
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
		options.getOutWriter().println(res.get(aKey, anArgs));
	}

	public void run() {
		boolean tmpResult = false;
		try {
			log("console.getKeystoreType", options.getKsType());
			final KeyStore ks = KeyStore.getInstance(options.getKsType());
			log("console.loadKeystore", options.getKsFile());
			ks.load(new FileInputStream(options.getKsFile()),options.getKsPasswd());
			log("console.getAliases");
			final String alias = (String) ks.aliases().nextElement();
			log("console.getPrivateKey");
			final PrivateKey key = (PrivateKey) ks.getKey(alias, options.getKeyPasswd());
			log("console.getCertChain");
			final Certificate[] chain = ks.getCertificateChain(alias);
			log("console.createPdfReader", options.getInFile());
			final PdfReader reader = new PdfReader(options.getInFile());
			log("console.createOutPdf", options.getOutFile());
			final FileOutputStream fout = new FileOutputStream(options.getOutFile());

			log("console.createSignature");
			final PdfStamper stp =
				PdfStamper.createSignature(reader, fout, '\0');
			final PdfSignatureAppearance sap = stp.getSignatureAppearance();

			sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
			log("console.setReason", options.getReason());
			sap.setReason(options.getReason());
			log("console.setLocation", options.getLocation());
			sap.setLocation(options.getLocation());

			log("console.processing");
			stp.close();
			fout.close();

			tmpResult = true;
		} catch (Exception e) {
			log("console.exception");
			e.printStackTrace(options.getOutWriter());
		} catch (OutOfMemoryError e) {
			e.printStackTrace(options.getOutWriter());
			log("console.memoryError");
		}
		log("console.finished." + (tmpResult?"ok":"error"));
		if (options.getListener() != null) {
			options.getListener().signerFinishedEvent(tmpResult);
		}
	}

	public SignerOptions getOptions() {
		return options;
	}

	public void setOptions(SignerOptions options) {
		this.options = options;
	}

}
