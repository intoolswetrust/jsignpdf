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

	private SignerOptions options;

	public SignerLogic() {}

	/**
	 * Constructor with all necessary parameters.
	 * @param anOptions options of signer
	 */
	public SignerLogic(final SignerOptions anOptions) {
		options = anOptions;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (options==null) {
			throw new NullPointerException("Options has to be filled.");
		}

		boolean tmpResult = false;
		try {

			options.log("console.getKeystoreType", options.getKsType());
			final KeyStore ks = KeyStore.getInstance(options.getKsType());
			options.log("console.loadKeystore", options.getKsFile());
			ks.load(new FileInputStream(options.getKsFile()),options.getKsPasswd());
			options.log("console.getAliases");
			final String alias = (String) ks.aliases().nextElement();
			options.log("console.getPrivateKey");
			final PrivateKey key = (PrivateKey) ks.getKey(alias, options.getKeyPasswd());
			options.log("console.getCertChain");
			final Certificate[] chain = ks.getCertificateChain(alias);
			options.log("console.createPdfReader", options.getInFile());
			final PdfReader reader = new PdfReader(options.getInFile());
			options.log("console.createOutPdf", options.getOutFile());
			final FileOutputStream fout = new FileOutputStream(options.getOutFile());

			options.log("console.createSignature");
			final PdfStamper stp =
				PdfStamper.createSignature(reader, fout, '\0');
			final PdfSignatureAppearance sap = stp.getSignatureAppearance();

			sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
			options.log("console.setReason", options.getReason());
			sap.setReason(options.getReason());
			options.log("console.setLocation", options.getLocation());
			sap.setLocation(options.getLocation());

			options.log("console.processing");
			stp.close();
			fout.close();

			tmpResult = true;
		} catch (Exception e) {
			options.log("console.exception");
			e.printStackTrace(options.getPrintWriter());
		} catch (OutOfMemoryError e) {
			e.printStackTrace(options.getPrintWriter());
			options.log("console.memoryError");
		}
		options.log("console.finished." + (tmpResult?"ok":"error"));
		options.fireSignerFinishedEvent(tmpResult);
	}

	public SignerOptions getOptions() {
		return options;
	}

	public void setOptions(SignerOptions options) {
		this.options = options;
	}

}
