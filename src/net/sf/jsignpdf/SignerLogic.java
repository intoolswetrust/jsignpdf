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
package net.sf.jsignpdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Proxy;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import net.sf.jsignpdf.crl.CRLInfo;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.utils.FontUtils;
import net.sf.jsignpdf.utils.KeyStoreUtils;
import net.sf.jsignpdf.utils.ResourceProvider;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
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
import com.lowagie.text.pdf.TSAClientBouncyCastle;

/**
 * Main logic of signer application. It uses iText to create signature in PDF.
 * 
 * @author Josef Cacek
 */
public class SignerLogic implements Runnable {

  private final static Logger logger = Logger.getLogger(SignerLogic.class);

  protected final static ResourceProvider res = ResourceProvider.getInstance();

  private final BasicSignerOptions options;

  /**
   * Constructor with all necessary parameters.
   * 
   * @param anOptions
   *          options of signer
   */
  public SignerLogic(final BasicSignerOptions anOptions) {
    if (anOptions == null) {
      throw new NullPointerException("Options has to be filled.");
    }
    options = anOptions;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Runnable#run()
   */
  public void run() {
    signFile();
  }

  /**
   * Signs a single file.
   * 
   * @return true when signing is finished succesfully, false otherwise
   */
  public boolean signFile() {
    final String outFile = options.getOutFileX();
    if (!validateInOutFiles(options.getInFile(), outFile)) {
      options.log("console.skippingSigning");
      return false;
    }

    boolean finished = false;
    Throwable tmpException = null;
    FileOutputStream fout = null;
    try {
      final PrivateKeyInfo pkInfo = KeyStoreUtils.getPkInfo(options);
      final PrivateKey key = pkInfo.getKey();
      final Certificate[] chain = pkInfo.getChain();
      if (ArrayUtils.isEmpty(chain)) {
        //the certificate was not found
        options.log("console.certificateChainEmpty");
        return false;
      }
      options.log("console.createPdfReader", options.getInFile());
      PdfReader reader;
      try {
        // try to read without password
        reader = new PdfReader(options.getInFile());
      } catch (Exception e) {
        try {
          reader = new PdfReader(options.getInFile(), new byte[0]);
        } catch (Exception e2) {
          reader = new PdfReader(options.getInFile(), options.getPdfOwnerPwdStr().getBytes());
        }
      }

      options.log("console.createOutPdf", outFile);
      fout = new FileOutputStream(outFile);

      final HashAlgorithm hashAlgorithm = options.getHashAlgorithmX();

      options.log("console.createSignature");
      char tmpPdfVersion = '\0'; // default version - the same as input
      if (reader.getPdfVersion() < hashAlgorithm.getPdfVersion()) {
        //this covers also problems with visible signatures (embedded fonts) in PDF 1.2, because the minimal version
        //for hash algorithms is 1.3 (for SHA1)
        if (options.isAppendX()) {
          //if we are in append mode and version should be updated then return false (not possible)
          options.log("console.updateVersionNotPossibleInAppendMode");
          return false;
        }
        tmpPdfVersion = hashAlgorithm.getPdfVersion();
        options.log("console.updateVersion",
            new String[] { String.valueOf(reader.getPdfVersion()), String.valueOf(tmpPdfVersion) });
      }

      final PdfStamper stp = PdfStamper.createSignature(reader, fout, tmpPdfVersion, null, options.isAppendX());
      if (!options.isAppendX()) {
        // we are not in append mode, let's remove existing signatures
        // (otherwise we're getting to troubles)
        final AcroFields acroFields = stp.getAcroFields();
        final List<String> sigNames = acroFields.getSignatureNames();
        for (String sigName : sigNames) {
          acroFields.removeField(sigName);
        }
      }
      if (options.isEncryptedX()) {
        options.log("console.setEncryption");
        final int tmpRight = options.getRightPrinting().getRight() | (options.isRightCopy() ? PdfWriter.ALLOW_COPY : 0)
            | (options.isRightAssembly() ? PdfWriter.ALLOW_ASSEMBLY : 0)
            | (options.isRightFillIn() ? PdfWriter.ALLOW_FILL_IN : 0)
            | (options.isRightScreanReaders() ? PdfWriter.ALLOW_SCREENREADERS : 0)
            | (options.isRightModifyAnnotations() ? PdfWriter.ALLOW_MODIFY_ANNOTATIONS : 0)
            | (options.isRightModifyContents() ? PdfWriter.ALLOW_MODIFY_CONTENTS : 0);
        stp.setEncryption(true, options.getPdfUserPwdStr(), options.getPdfOwnerPwdStr(), tmpRight);
      }

      final PdfSignatureAppearance sap = stp.getSignatureAppearance();
      sap.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
      if (StringUtils.isNotEmpty(options.getReason())) {
        options.log("console.setReason", options.getReason());
        sap.setReason(options.getReason());
      }
      if (StringUtils.isNotEmpty(options.getLocation())) {
        options.log("console.setLocation", options.getLocation());
        sap.setLocation(options.getLocation());
      }
      if (StringUtils.isNotEmpty(options.getContact())) {
        options.log("console.setContact", options.getContact());
        sap.setContact(options.getContact());
      }
      options.log("console.setCertificationLevel");
      sap.setCertificationLevel(options.getCertLevelX().getLevel());

      if (options.isVisible()) {
        // visible signature is enabled
        options.log("console.configureVisible");
        options.log("console.setAcro6Layers", Boolean.toString(options.isAcro6Layers()));
        sap.setAcro6Layers(options.isAcro6Layers());

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
        if (options.getL2Text() != null) {
          sap.setLayer2Text(options.getL2Text());
        } else {
          final StringBuilder buf = new StringBuilder();
          buf.append(res.get("default.l2text.signedBy")).append(" ");
          buf.append(PdfPKCS7.getSubjectFields((X509Certificate) chain[0]).getField("CN")).append('\n');
          final SimpleDateFormat sd = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
          buf.append(res.get("default.l2text.date")).append(" ").append(sd.format(sap.getSignDate().getTime()));
          if (StringUtils.isNotEmpty(options.getReason()))
            buf.append('\n').append(res.get("default.l2text.reason")).append(" ").append(options.getReason());
          if (StringUtils.isNotEmpty(options.getLocation()))
            buf.append('\n').append(res.get("default.l2text.location")).append(" ").append(options.getLocation());
          sap.setLayer2Text(buf.toString());
          ;
        }
        if (FontUtils.getL2BaseFont() != null) {
          sap.setLayer2Font(new Font(FontUtils.getL2BaseFont(), options.getL2TextFontSize()));
        }
        options.log("console.setL4Text");
        sap.setLayer4Text(options.getL4Text());
        options.log("console.setRender");
        RenderMode renderMode = options.getRenderMode();
        if (renderMode == RenderMode.GRAPHIC_AND_DESCRIPTION && sap.getSignatureGraphic() == null) {
          logger
              .warn("Render mode of visible signature is set to GRAPHIC_AND_DESCRIPTION, but no image is loaded. Fallback to DESCRIPTION_ONLY.");
          options.log("console.renderModeFallback");
          renderMode = RenderMode.DESCRIPTION_ONLY;
        }
        sap.setRender(renderMode.getRender());
        options.log("console.setVisibleSignature");
        sap.setVisibleSignature(
            new Rectangle(options.getPositionLLX(), options.getPositionLLY(), options.getPositionURX(), options
                .getPositionURY()), options.getPage(), null);
      }

      options.log("console.processing");
      final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, new PdfName("adbe.pkcs7.detached"));
      if (!StringUtils.isEmpty(options.getReason())) {
        dic.setReason(sap.getReason());
      }
      if (!StringUtils.isEmpty(options.getLocation())) {
        dic.setLocation(sap.getLocation());
      }
      if (!StringUtils.isEmpty(options.getContact())) {
        dic.setContact(sap.getContact());
      }
      dic.setDate(new PdfDate(sap.getSignDate()));
      sap.setCryptoDictionary(dic);

      final Proxy tmpProxy = options.createProxy();

      final CRLInfo crlInfo = new CRLInfo(options, chain);

      // CRLs are stored twice in PDF c.f.
      // PdfPKCS7.getAuthenticatedAttributeBytes
      final int contentEstimated = (int) (Constants.DEFVAL_SIG_SIZE + 2L * crlInfo.getByteCount());
      HashMap exc = new HashMap();
      exc.put(PdfName.CONTENTS, new Integer(contentEstimated * 2 + 2));
      sap.preClose(exc);

      PdfPKCS7 sgn = new PdfPKCS7(key, chain, crlInfo.getCrls(), hashAlgorithm.getAlgorithmName(), null, false);
      InputStream data = sap.getRangeStream();
      final MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm.getAlgorithmName());
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
        if (StringUtils.isEmpty(url)) {
          //get from options
          options.log("console.noOCSPURL");
          url = options.getOcspServerUrl();
        }
        if (!StringUtils.isEmpty(url)) {
          options.log("console.readingOCSP", url);
          final OcspClientBouncyCastle ocspClient = new OcspClientBouncyCastle((X509Certificate) chain[0],
              (X509Certificate) chain[1], url);
          ocspClient.setProxy(tmpProxy);
          ocsp = ocspClient.getEncoded();
        }
      }
      byte sh[] = sgn.getAuthenticatedAttributeBytes(hash, cal, ocsp);
      sgn.update(sh, 0, sh.length);

      TSAClientBouncyCastle tsc = null;
      if (options.isTimestampX() && !StringUtils.isEmpty(options.getTsaUrl())) {
        options.log("console.creatingTsaClient");
        tsc = new TSAClientBouncyCastle(options.getTsaUrl(), StringUtils.defaultString(options.getTsaUser()),
            StringUtils.defaultString(options.getTsaPasswd()));
        tsc.setProxy(tmpProxy);
        final String policyOid = options.getTsaPolicy();
        if (StringUtils.isNotEmpty(policyOid)) {
          options.log("console.settingTsaPolicy", policyOid);
          tsc.setPolicy(policyOid);
        }
      }
      byte[] encodedSig = sgn.getEncodedPKCS7(hash, cal, tsc, ocsp);

      if (contentEstimated + 2 < encodedSig.length) {
        System.err.println("SigSize - contentEstimated=" + contentEstimated + ", sigLen=" + encodedSig.length);
        throw new Exception("Not enough space");
      }

      byte[] paddedSig = new byte[contentEstimated];
      System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);

      PdfDictionary dic2 = new PdfDictionary();
      dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));
      options.log("console.closeStream");
      sap.close(dic2);
      fout.close();
      fout = null;
      finished = true;
    } catch (Exception e) {
      options.log("console.exception");
      e.printStackTrace(options.getPrintWriter());
      tmpException = e;
    } catch (OutOfMemoryError e) {
      tmpException = e;
      e.printStackTrace(options.getPrintWriter());
      options.log("console.memoryError");
    } finally {
      if (fout != null) {
        try {
          fout.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      options.log("console.finished." + (finished ? "ok" : "error"));
      options.fireSignerFinishedEvent(tmpException);
    }
    return finished;
  }

  /**
   * Validates if input and output files are valid for signing.
   * 
   * @param inFile
   *          input file
   * @param outFile
   *          output file
   * @return true if valid, false otherwise
   */
  private boolean validateInOutFiles(final String inFile, final String outFile) {
    options.log("console.validatingFiles");
    if (StringUtils.isEmpty(inFile) || StringUtils.isEmpty(outFile)) {
      options.log("console.fileNotFilled.error");
      return false;
    }
    final File tmpInFile = new File(inFile);
    final File tmpOutFile = new File(outFile);
    if (!(tmpInFile.exists() && tmpInFile.isFile() && tmpInFile.canRead())) {
      options.log("console.inFileNotFound.error");
      return false;
    }
    if (tmpInFile.getAbsolutePath().equals(tmpOutFile.getAbsolutePath())) {
      options.log("console.filesAreEqual.error");
      return false;
    }
    return true;
  }

}
