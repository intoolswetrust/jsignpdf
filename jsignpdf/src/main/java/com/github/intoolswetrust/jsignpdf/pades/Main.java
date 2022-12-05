package com.github.intoolswetrust.jsignpdf.pades;

import static com.github.intoolswetrust.jsignpdf.pades.Constants.LOGGER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore.PasswordProtection;
import java.security.Security;
import java.util.TreeSet;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.JCommander;
import com.github.intoolswetrust.jsignpdf.pades.config.BasicConfig;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;

public class Main {

    public static void main(String[] args) {
        BasicConfig config = new BasicConfig();
        JCommander jcmd = JCommander.newBuilder().addObject(config).build();
        jcmd.parse(args);

        boolean cmdUsed = false;
        if (config.isPrintHelp()) {
            cmdUsed = true;
            jcmd.usage();
        }
        if (config.isListKeyStores()) {
            cmdUsed = true;
            TreeSet<String> ksts = new TreeSet<String>(Security.getAlgorithms("KeyStore"));
            for (String kst : ksts) {
                System.out.println(kst);
            }
        }
        if (config.isListKeys()) {
            cmdUsed = true;
            try {
                for (String kst : KeyStoreUtils.getKeyAliases(config)) {
                    System.out.println(kst);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to list keystore content", e);
            }
        }
        if (config.getFiles().isEmpty()) {
            if (!cmdUsed) {
                jcmd.usage();
            }
            System.exit(cmdUsed ? 0 : 1);
        }

        try (KeyStoreSignatureTokenConnection signingToken = KeyStoreUtils.createKeyStoreSignatureTokenConnection(config)) {

            DSSPrivateKeyEntry privateKey = StringUtils.isEmpty(config.getKeyAlias()) ? signingToken.getKeys().get(0)
                    : signingToken.getKey(config.getKeyAlias(), new PasswordProtection(config.getKeyPasswordAsChars()));

            // Preparing parameters for the PAdES signature
            PAdESSignatureParameters parameters = new PAdESSignatureParameters();
            // We choose the level of the signature (-B, -T, -LT, -LTA).
            parameters.setSignatureLevel(config.getPadesLevel().getSignatureLevel());

            // We set the digest algorithm to use with the signature algorithm. You must use the
            // same parameter when you invoke the method sign on the token. The default value is
            // SHA256
            parameters.setDigestAlgorithm(config.getDigestAlgorithm());

            // We set the signing certificate
            parameters.setSigningCertificate(privateKey.getCertificate());
            // We set the certificate chain
            parameters.setCertificateChain(privateKey.getCertificateChain());

            // Create common certificate verifier
            CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
            commonCertificateVerifier.setAIASource(new DefaultAIASource());
            commonCertificateVerifier.setCrlSource(new OnlineCRLSource());
            commonCertificateVerifier.setOcspSource(new OnlineOCSPSource());
            
            // Create PAdESService for signature
            PAdESService service = new PAdESService(commonCertificateVerifier);
            // Set the Timestamp source
            String tspServer = config.getTsaServerUrl();
            if (StringUtils.isNotEmpty(tspServer)) {
                OnlineTSPSource onlineTSPSource = new OnlineTSPSource(tspServer);
                TimestampDataLoader dataLoader = new TimestampDataLoader();
                //dataLoader.addAuthentication(...)
                onlineTSPSource.setDataLoader(dataLoader); // uses the specific content-type
                service.setTspSource(onlineTSPSource);
            }

            for (File pdfFile : config.getFiles()) {
                FileDocument toSignDocument = new FileDocument(pdfFile);
                // Get the SignedInfo segment that need to be signed.
                ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);

                // This function obtains the signature value for signed information using the
                // private key and specified algorithm
                DigestAlgorithm digestAlgorithm = parameters.getDigestAlgorithm();
                SignatureValue signatureValue = signingToken.sign(dataToSign, digestAlgorithm, privateKey);

                // Optionally or for debug purpose :
                // Validate the signature value against the original dataToSign
                System.out.println(
                        "Valid: " + service.isValidSignatureValue(dataToSign, signatureValue, privateKey.getCertificate()));

                // We invoke the padesService to sign the document with the signature value obtained in
                // the previous step.
                DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
                File outFile = getOutputFile(pdfFile, config);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    signedDocument.writeTo(fos);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Unable to write the signed document to a file", e);
                    System.exit(3);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to sign", e);
            System.exit(2);
        }
    }

    private static File getOutputFile(File pdfFile, BasicConfig config) {
        String tmpNameBase = pdfFile.getName();
        String tmpSuffix = ".pdf";
        if (StringUtils.endsWithIgnoreCase(tmpNameBase, tmpSuffix)) {
            tmpSuffix = StringUtils.right(tmpNameBase, 4);
            tmpNameBase = StringUtils.left(tmpNameBase, tmpNameBase.length() - 4);
        }
        File outputDir = config.getOutDirectory();
        if (null == outputDir) {
            outputDir = pdfFile.getAbsoluteFile().getParentFile();
        } else {
            outputDir.mkdirs();
        }
        final StringBuilder tmpName = new StringBuilder();
        tmpName.append(tmpNameBase).append(config.getOutSuffix()).append(tmpSuffix);
        return new File(outputDir, tmpName.toString());
    }

}
