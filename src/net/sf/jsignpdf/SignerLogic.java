package net.sf.jsignpdf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

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
			InputStream ksInputStream = null;
			if (!StringUtils.isEmpty(options.getKsFile())) {
				ksInputStream = new FileInputStream(options.getKsFile());
			}
			ks.load(ksInputStream,options.getKsPasswd());
			options.log("console.getAliases");
			String tmpAlias = options.getKeyAliasX();
			if (tmpAlias==null || tmpAlias.length()==0) {
				tmpAlias = (String) ks.aliases().nextElement();
			}
			options.log("console.getPrivateKey");
			final PrivateKey key = (PrivateKey) ks.getKey(tmpAlias, options.getKeyPasswdX());
			options.log("console.getCertChain");
			final Certificate[] chain = ks.getCertificateChain(tmpAlias);
			options.log("console.createPdfReader", options.getInFile());
			PdfReader reader;
			try {
				//try to read without password
				reader = new PdfReader(options.getInFile());
			} catch (Exception e) {
				try {
					reader = new PdfReader(options.getInFile(),
							new byte[0]);
				} catch (Exception e2) {
					reader = new PdfReader(options.getInFile(),
							options.getPdfOwnerPwdStr().getBytes());
				}
			}

			options.log("console.createOutPdf", options.getOutFile());
			final FileOutputStream fout = new FileOutputStream(options.getOutFile());

			options.log("console.createSignature");
			final PdfStamper stp =
				PdfStamper.createSignature(reader, fout, '\0', null, options.isAppendX());

			if (options.isEncryptedX()) {
				options.log("console.setEncryption");
				stp.setEncryption(true,
					options.getPdfUserPwdStr(),
					options.getPdfOwnerPwdStr(),
					PdfWriter.ALLOW_ASSEMBLY | PdfWriter.ALLOW_COPY |
					PdfWriter.ALLOW_DEGRADED_PRINTING | PdfWriter.ALLOW_FILL_IN |
					PdfWriter.ALLOW_MODIFY_ANNOTATIONS | PdfWriter.ALLOW_MODIFY_CONTENTS |
					PdfWriter.ALLOW_PRINTING | PdfWriter.ALLOW_SCREENREADERS
					);
			}

			final PdfSignatureAppearance sap = stp.getSignatureAppearance();
			sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
			options.log("console.setReason", options.getReason());
			sap.setReason(options.getReason());
			options.log("console.setLocation", options.getLocation());
			sap.setLocation(options.getLocation());
			options.log("console.setCertificationLevel");
			sap.setCertificationLevel(options.getCertLevelX().getLevel());

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
