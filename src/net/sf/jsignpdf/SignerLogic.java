package net.sf.jsignpdf;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;

import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.OcspClientBouncyCastle;
import com.lowagie.text.pdf.PdfDate;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignature;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.TSAClient;
import com.lowagie.text.pdf.TSAClientBouncyCastle;

/**
 * Main logic of signer application. It uses iText to create signature in PDF.
 * @author Josef Cacek
 */
public class SignerLogic implements Runnable {

	private BasicSignerOptions options;

	private static BaseFont l2baseFont;

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
	@SuppressWarnings("unchecked")
	public void run() {
		if (options==null) {
			throw new NullPointerException("Options has to be filled.");
		}

		boolean tmpResult = false;
		try {
			final PrivateKeyInfo pkInfo = KeyStoreUtils.getPkInfo(options);
			final PrivateKey key = pkInfo.getKey();
			final Certificate[] chain = pkInfo.getChain();
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
			if (!StringUtils.isEmpty(options.getContact())) {
				options.log("console.setContact", options.getContact());
				sap.setContact(options.getContact());
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
				if (getL2BaseFont()!=null) {
					sap.setLayer2Font(new Font(getL2BaseFont(),options.getL2TextFontSize()));
				}
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
			final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE,
					new PdfName("adbe.pkcs7.detached"));
			dic.setReason(sap.getReason());
			dic.setLocation(sap.getLocation());
			dic.setContact(sap.getContact());
			dic.setDate(new PdfDate(sap.getSignDate()));
			sap.setCryptoDictionary(dic);

			int contentEstimated = 15000;
			HashMap exc = new HashMap();
			exc.put(PdfName.CONTENTS, new Integer(contentEstimated * 2 + 2));
			sap.preClose(exc);

			PdfPKCS7 sgn = new PdfPKCS7(key, chain, null, "SHA1", null, false);
			InputStream data = sap.getRangeStream();
			MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
			byte buf[] = new byte[8192];
			int n;
			while ((n = data.read(buf)) > 0) {
				messageDigest.update(buf, 0, n);
			}
			byte hash[] = messageDigest.digest();
			Calendar cal = Calendar.getInstance();
			byte[] ocsp = null;
			if (options.isOcspEnabledX() && chain.length >= 2) {
				options.log("console.getOCSPURL");
				String url = PdfPKCS7.getOCSPURL((X509Certificate) chain[0]);
				if (url != null && url.length() > 0) {
					options.log("console.readingOCSP");
					ocsp = new OcspClientBouncyCastle(
							(X509Certificate) chain[0],
							(X509Certificate) chain[1], url).getEncoded();
				}
			}
			byte sh[] = sgn.getAuthenticatedAttributeBytes(hash, cal, ocsp);
			sgn.update(sh, 0, sh.length);

			TSAClient tsc = null;
			if (options.isTimestampX() && ! StringUtils.isEmpty(options.getTsaUrl())) {
				options.log("console.creatingTsaClient");
				tsc = new TSAClientBouncyCastle(options.getTsaUrl(),
						StringUtils.emptyNull(options.getTsaUser()),
						StringUtils.emptyNull(options.getTsaPasswd()));
			}
			byte[] encodedSig = sgn.getEncodedPKCS7(hash, cal, tsc, ocsp);

			if (contentEstimated + 2 < encodedSig.length)
				throw new Exception("Not enough space");

			byte[] paddedSig = new byte[contentEstimated];
			System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);

			PdfDictionary dic2 = new PdfDictionary();
			dic2.put(PdfName.CONTENTS, new PdfString(paddedSig)
					.setHexWriting(true));
			options.log("console.closeStream");
			sap.close(dic2);
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


	/**
	 * Returns BaseFont for text of visible signature;
	 * @return
	 */
	public static synchronized BaseFont getL2BaseFont() {
		if (l2baseFont==null) {
			try {
				final ByteArrayOutputStream tmpBaos = new ByteArrayOutputStream();
				InputStream tmpIs = SignerLogic.class.getResourceAsStream(Constants.L2TEXT_FONT_PATH);
				IOUtils.copy(tmpIs, tmpBaos);
				tmpIs.close();
				tmpBaos.close();
				l2baseFont = BaseFont.createFont(Constants.L2TEXT_FONT_NAME, BaseFont.IDENTITY_H,
						BaseFont.EMBEDDED, BaseFont.CACHED,
						tmpBaos.toByteArray(), null);
			} catch (Exception e) {
				try {
					l2baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED);
				} catch (Exception ex) {
					//where is the problem, dear Watson?
				}
			}
		}
		return l2baseFont;
	}
}
