package net.sf.jsignpdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.BadElementException;
import com.lowagie.text.DocumentException;
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
 * 
 * @author Josef Cacek
 */
public class SignerLogic implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(SignerLogic.class);
	private static final ResourceBundleBean res = ResourceProvider.getBundleBean();

	private SignerOptions options;

	private static BaseFont l2baseFont;

	/**
	 * Constructor with all necessary parameters.
	 * 
	 * @param anOptions
	 *            options of signer
	 */
	public SignerLogic(final SignerOptions anOptions) {
		options = anOptions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@SuppressWarnings("unchecked")
	public void run() {
		if (options == null) {
			throw new NullPointerException("Options has to be filled.");
		}
		if (!validateInOutFiles(options.getInFile(), options.getOutFile())) {
			logger.warn("console.skippingSigning");
			return;
		}

		boolean tmpResult = false;
		try {
			final PrivateKeyInfo pkInfo = KeyStoreUtils.getPkInfo(options);
			final PrivateKey key = pkInfo.getKey();
			final Certificate[] chain = pkInfo.getChain();
			logger.info(res.get("console.createPdfReader", options.getInFile()));
			PdfReader reader;
			try {
				// try to read without password
				final String tmpPwd = StringUtils.emptyNull(options.getInFileOwnerPwd());
				reader = new PdfReader(options.getInFile(), StringUtils.toByteArray(tmpPwd, "UTF-8"));
			} catch (Exception e) {
				logger.warn("Not a valid PDF password for input PDF.");
				try {
					logger.info("Trying to use empty password");
					reader = new PdfReader(options.getInFile(), new byte[0]);
				} catch (Exception e2) {
					logger.info("Trying to open without password");
					reader = new PdfReader(options.getInFile());
				}
			}

			logger.info(res.get("console.createOutPdf", options.getOutFile()));
			final FileOutputStream fout = new FileOutputStream(options.getOutFile());

			char tmpPdfVersion = Constants.PDF_DEFAULT_VERSION;
			if (options.isVisible() && !options.isAppend()) {
				// fix the problem with embedded font name in older PDF versions
				char tmpOldVersion = reader.getPdfVersion();
				if (tmpOldVersion < PdfWriter.VERSION_1_3) {
					tmpPdfVersion = PdfWriter.VERSION_1_3;
					logger.info(res.get("console.updateVersion", String.valueOf(tmpOldVersion), String
							.valueOf(tmpPdfVersion)));
				}
			}
			logger.info(res.get("console.createSignature"));
			final PdfStamper stp = PdfStamper.createSignature(reader, fout, tmpPdfVersion, null, options.isAppend());

			initEncryption(stp);

			final PdfSignatureAppearance sap = stp.getSignatureAppearance();
			sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);

			initProperties(sap);

			if (options.isVisible()) {
				// visible signature is enabled
				initVisibleSignature(sap);
			}

			logger.info(res.get("console.processing"));
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
			byte[] ocsp = getOcspBytes(chain);
			byte sh[] = sgn.getAuthenticatedAttributeBytes(hash, cal, ocsp);
			sgn.update(sh, 0, sh.length);
			byte[] encodedSig = sgn.getEncodedPKCS7(hash, cal, getTsaClient(), ocsp);

			if (contentEstimated + 2 < encodedSig.length)
				throw new Exception("Not enough space");

			byte[] paddedSig = new byte[contentEstimated];
			System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);

			PdfDictionary dic2 = new PdfDictionary();
			dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));
			logger.info(res.get("console.closeStream"));
			sap.close(dic2);
			fout.close();

			tmpResult = true;
		} catch (Exception e) {
			logger.warn(res.get("console.exception"), e);
		} catch (OutOfMemoryError e) {
			logger.info(res.get("console.memoryError"), e);
		}
		logger.info(res.get("console.finished." + (tmpResult ? "ok" : "error")));
	}

	/**
	 * Returns BaseFont for text of visible signature;
	 * 
	 * @return
	 */
	public static synchronized BaseFont getL2BaseFont() {
		if (l2baseFont == null) {
			try {
				final ByteArrayOutputStream tmpBaos = new ByteArrayOutputStream();
				InputStream tmpIs = SignerLogic.class.getResourceAsStream(Constants.L2TEXT_FONT_PATH);
				IOUtils.copy(tmpIs, tmpBaos);
				tmpIs.close();
				tmpBaos.close();
				l2baseFont = BaseFont.createFont(Constants.L2TEXT_FONT_NAME, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
						BaseFont.CACHED, tmpBaos.toByteArray(), null);
			} catch (Exception e) {
				try {
					l2baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED);
				} catch (Exception ex) {
					// where is the problem, dear Watson?
				}
			}
		}
		return l2baseFont;
	}

	/**
	 * Create TSA client for signing
	 * 
	 * @return
	 */
	private TSAClient getTsaClient() {
		TSAClient tsc = null;
		if (StringUtils.hasLength(options.getTsaUrl())) {
			logger.info(res.get("console.creatingTsaClient"));
			tsc = new TSAClientBouncyCastle(options.getTsaUrl(), StringUtils.emptyNull(options.getTsaUser()),
					StringUtils.emptyNull(options.getTsaPasswd()));
		}
		return tsc;
	}

	/**
	 * Returns OCSP bytes
	 * 
	 * @param chain
	 * @return
	 * @throws CertificateParsingException
	 */
	private byte[] getOcspBytes(Certificate[] chain) throws CertificateParsingException {
		byte[] ocsp = null;
		if (options.isOcspEnabled() && chain.length >= 2) {
			logger.info(res.get("console.getOCSPURL"));
			String url = PdfPKCS7.getOCSPURL((X509Certificate) chain[0]);
			if (url != null && url.length() > 0) {
				logger.info(res.get("console.readingOCSP"));
				ocsp = new OcspClientBouncyCastle((X509Certificate) chain[0], (X509Certificate) chain[1], url)
						.getEncoded();
			}
		}
		return ocsp;
	}

	/**
	 * Sets visible signature options
	 * 
	 * @param sap
	 * @throws BadElementException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void initVisibleSignature(PdfSignatureAppearance sap) throws BadElementException, MalformedURLException,
			IOException {
		logger.info(res.get("console.configureVisible"));
		logger.info(res.get("console.setAcro6Layers"));
		sap.setAcro6Layers(true);

		final String tmpImgPath = options.getImgPath();
		if (tmpImgPath != null) {
			logger.info(res.get("console.createImage", tmpImgPath));
			final Image img = Image.getInstance(tmpImgPath);
			logger.info(res.get("console.setSignatureGraphic"));
			sap.setSignatureGraphic(img);
		}
		final String tmpBgImgPath = options.getBgImgPath();
		if (tmpBgImgPath != null) {
			logger.info(res.get("console.createImage", tmpBgImgPath));
			final Image img = Image.getInstance(tmpBgImgPath);
			logger.info(res.get("console.setImage"));
			sap.setImage(img);
		}
		logger.info(res.get("console.setImageScale"));
		sap.setImageScale(options.getBgImgScale());
		logger.info(res.get("console.setL2Text"));
		sap.setLayer2Text(options.getL2Text());
		if (getL2BaseFont() != null) {
			sap.setLayer2Font(new Font(getL2BaseFont(), options.getL2TextFontSize()));
		}
		logger.info(res.get("console.setL4Text"));
		sap.setLayer4Text(options.getL4Text());
		logger.info(res.get("console.setRender"));
		sap.setRender(options.getRenderMode().getRender());
		logger.info(res.get("console.setVisibleSignature"));
		sap.setVisibleSignature(new Rectangle(options.getPositionLLX(), options.getPositionLLY(), options
				.getPositionURX(), options.getPositionURY()), options.getPage(), null);
	}

	/**
	 * Sets basic signature properties (location, reason, certification
	 * level,...)
	 * 
	 * @param sap
	 */
	private void initProperties(final PdfSignatureAppearance sap) {
		if (!StringUtils.isEmpty(options.getReason())) {
			logger.info(res.get("console.setReason", options.getReason()));
			sap.setReason(options.getReason());
		}
		if (!StringUtils.isEmpty(options.getLocation())) {
			logger.info(res.get("console.setLocation", options.getLocation()));
			sap.setLocation(options.getLocation());
		}
		if (!StringUtils.isEmpty(options.getContact())) {
			logger.info(res.get("console.setContact", options.getContact()));
			sap.setContact(options.getContact());
		}
		logger.info(res.get("console.setCertificationLevel"));
		sap.setCertificationLevel(options.getCertLevel().getLevel());

		final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, new PdfName("adbe.pkcs7.detached"));
		dic.setReason(sap.getReason());
		dic.setLocation(sap.getLocation());
		dic.setContact(sap.getContact());
		dic.setDate(new PdfDate(sap.getSignDate()));
		sap.setCryptoDictionary(dic);
	}

	/**
	 * Sets encryption for output PDF document
	 * 
	 * @param stp
	 *            stampler
	 * @throws DocumentException
	 *             calling of stampler method setEncryption fails
	 */
	private void initEncryption(PdfStamper stp) throws DocumentException {
		if (StringUtils.hasLength(options.getPdfOwnerPwd())) {
			logger.info(res.get("console.setEncryption"));
			final int tmpRight = options.getRightPrinting().getRight()
					| (options.isRightCopy() ? PdfWriter.ALLOW_COPY : 0)
					| (options.isRightAssembly() ? PdfWriter.ALLOW_ASSEMBLY : 0)
					| (options.isRightFillIn() ? PdfWriter.ALLOW_FILL_IN : 0)
					| (options.isRightScreanReaders() ? PdfWriter.ALLOW_SCREENREADERS : 0)
					| (options.isRightModifyAnnotations() ? PdfWriter.ALLOW_MODIFY_ANNOTATIONS : 0)
					| (options.isRightModifyContents() ? PdfWriter.ALLOW_MODIFY_CONTENTS : 0);
			stp.setEncryption(true, options.getPdfUserPwd(), options.getPdfOwnerPwd(), tmpRight);
		}
	}

	/**
	 * Validates if input and output files are valid for signing.
	 * 
	 * @param inFile
	 *            input file
	 * @param outFile
	 *            output file
	 * @return true if valid, false otherwise
	 */
	private boolean validateInOutFiles(final String inFile, final String outFile) {
		logger.info("console.validatingFiles");
		if (StringUtils.isEmpty(inFile) || StringUtils.isEmpty(outFile)) {
			logger.error("console.fileNotFilled.error");
			return false;
		}
		final File tmpInFile = new File(inFile);
		final File tmpOutFile = new File(outFile);
		if (!(tmpInFile.exists() && tmpInFile.isFile() && tmpInFile.canRead())) {
			logger.error("console.inFileNotFound.error");
			return false;
		}
		if (tmpInFile.getAbsolutePath().equals(tmpOutFile.getAbsolutePath())) {
			logger.error("console.filesAreEqual.error");
			return false;
		}
		return true;
	}

}
