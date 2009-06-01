package net.sf.jsignpdf;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Main logic of signer application. It uses iText to create signature in PDF.
 * @author Josef Cacek
 */
public class SignerLogic implements Runnable {

	private BasicSignerOptions options;

	public SignerLogic() {}

	/**
	 * Constructor with all necessary parameters.
	 * @param anOptions options of signer
	 */
	public SignerLogic(final BasicSignerOptions anOptions) {
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
			InputStream ksInputStream = null;
			if (!StringUtils.isEmpty(options.getKsFile())) {
				options.log("console.loadKeystore", options.getKsFile());
				ksInputStream = new FileInputStream(options.getKsFile());
			}
			ks.load(ksInputStream,options.getKsPasswd());
			options.log("console.getAliases");
			String tmpAlias = options.getKeyAliasX();
			if (tmpAlias==null || tmpAlias.length()==0) {
				final String tmpAliases[] = KeyStoreUtils.getKeyAliases(options);
				if (tmpAliases!=null && tmpAliases.length>0) {
					tmpAlias = tmpAliases[0];
				}
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
				final int tmpRight = options.getRightPrinting().getRight()
					| (options.isRightCopy() ? PdfWriter.ALLOW_COPY: 0)
					| (options.isRightAssembly() ? PdfWriter.ALLOW_ASSEMBLY: 0)
					| (options.isRightFillIn() ? PdfWriter.ALLOW_FILL_IN: 0)
					| (options.isRightScreanReaders() ? PdfWriter.ALLOW_SCREENREADERS: 0)
					| (options.isRightModifyAnnotations() ? PdfWriter.ALLOW_MODIFY_ANNOTATIONS: 0)
					| (options.isRightModifyContents() ? PdfWriter.ALLOW_MODIFY_CONTENTS: 0)
					;
				stp.setEncryption(true,
					options.getPdfUserPwdStr(),
					options.getPdfOwnerPwdStr(),
					tmpRight
					);
			}

			final PdfSignatureAppearance sap = stp.getSignatureAppearance();
			sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
			if (!StringUtils.isEmpty(options.getReason())) {
				options.log("console.setReason", options.getReason());
				sap.setReason(options.getReason());
			}
			if (!StringUtils.isEmpty(options.getLocation())) {
				options.log("console.setLocation", options.getLocation());
				sap.setLocation(options.getLocation());
			}
			options.log("console.setCertificationLevel");
			sap.setCertificationLevel(options.getCertLevelX().getLevel());

			if (options.isVisible()) {
				//visible signature is enabled
				options.log("console.configureVisible");
				options.log("console.setAcro6Layers");
				sap.setAcro6Layers(true);

				final String tmpImgPath = options.getImgPath();
				if (tmpImgPath != null) {
					options.log("console.createImage", tmpImgPath);
					final Image img = Image.getInstance(tmpImgPath);
					options.log("console.setSignatureGraphic");
					sap.setSignatureGraphic(img);
				}
				final String tmpBgImgPath = options.getBgImgPath();
				if (tmpBgImgPath != null) {
					options.log("console.createImage", tmpBgImgPath);
					final Image img = Image.getInstance(tmpBgImgPath);
					options.log("console.setImage");
					sap.setImage(img);
				}
				options.log("console.setImageScale");
				sap.setImageScale(options.getBgImgScale());
				options.log("console.setL2Text");
				sap.setLayer2Text(options.getL2Text());
				options.log("console.setL4Text");
				sap.setLayer4Text(options.getL4Text());
				options.log("console.setRender");
				sap.setRender(options.getRenderMode().getRender());
				options.log("console.setVisibleSignature");
				sap.setVisibleSignature(
					new Rectangle(
						options.getPositionLLX(),
						options.getPositionLLY(),
						options.getPositionURX(),
						options.getPositionURY()),
					options.getPage(),
					null);
			}
			options.log("console.processing");
			stp.close();
			options.log("console.closeStream");
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

	public BasicSignerOptions getOptions() {
		return options;
	}

	public void setOptions(BasicSignerOptions options) {
		this.options = options;
	}

}
