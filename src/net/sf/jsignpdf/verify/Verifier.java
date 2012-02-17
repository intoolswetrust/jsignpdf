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
package net.sf.jsignpdf.verify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sf.jsignpdf.utils.KeyStoreUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;

/**
 * Entry point (i.e. main class) to PDF signature verifications.
 * 
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.9 $
 * @created $Date: 2012/02/17 11:50:35 $
 */
public class Verifier {

  /**
   * @param args
   */
  public static void main(String[] args) {

    // create the Options
    Option optHelp = new Option("h", "help", false, "print this message");
    // Option optVersion = new Option("v", "version", false,
    // "print version info");
    Option optCerts = new Option("c", "cert", true, "use external semicolon separated X.509 certificate files");
    optCerts.setArgName("certificates");
    Option optPasswd = new Option("p", "password", true, "set password for opening PDF");
    optPasswd.setArgName("password");
    Option optExtract = new Option("e", "extract", true, "extract signed PDF revisions to given folder");
    optExtract.setArgName("folder");
    Option optListKs = new Option("lk", "list-keystore-types", false, "list keystore types provided by java");
    Option optListCert = new Option("lc", "list-certificates", false, "list certificate aliases in a KeyStore");
    Option optKsType = new Option("kt", "keystore-type", true, "use keystore type with given name");
    optKsType.setArgName("keystore_type");
    Option optKsFile = new Option("kf", "keystore-file", true, "use given keystore file");
    optKsFile.setArgName("file");
    Option optKsPass = new Option("kp", "keystore-password", true, "password for keystore file (look on -kf option)");
    optKsPass.setArgName("password");
    Option optFailFast = new Option("ff", "fail-fast", true,
        "flag which sets the Verifier to exit with error code on the first validation failure");

    final Options options = new Options();
    options.addOption(optHelp);
    // options.addOption(optVersion);
    options.addOption(optCerts);
    options.addOption(optPasswd);
    options.addOption(optExtract);
    options.addOption(optListKs);
    options.addOption(optListCert);
    options.addOption(optKsType);
    options.addOption(optKsFile);
    options.addOption(optKsPass);
    options.addOption(optFailFast);

    CommandLine line = null;
    try {
      // create the command line parser
      CommandLineParser parser = new PosixParser();
      // parse the command line arguments
      line = parser.parse(options, args);
    } catch (ParseException exp) {
      System.err.println("Illegal command used: " + exp.getMessage());
      System.exit(-1);
    }

    final boolean failFast = line.hasOption("ff");
    final String[] tmpArgs = line.getArgs();
    if (line.hasOption("h") || args == null || args.length == 0) {
      // automatically generate the help statement
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(70, "java -jar Verifier.jar [file1.pdf [file2.pdf ...]]",
          "JSignPdf Verifier is a command line tool for verifying signed PDF documents.", options, null, true);
    } else if (line.hasOption("lk")) {
      // list keystores
      for (String tmpKsType : KeyStoreUtils.getKeyStores()) {
        System.out.println(tmpKsType);
      }
    } else if (line.hasOption("lc")) {
      // list certificate aliases in the keystore
      for (String tmpCert : KeyStoreUtils.getCertAliases(line.getOptionValue("kt"), line.getOptionValue("kf"),
          line.getOptionValue("kp"))) {
        System.out.println(tmpCert);
      }
    } else {
      final VerifierLogic tmpLogic = new VerifierLogic(line.getOptionValue("kt"), line.getOptionValue("kf"),
          line.getOptionValue("kp"));
      tmpLogic.setFailFast(failFast);

      if (line.hasOption("c")) {
        String tmpCertFiles = line.getOptionValue("c");
        for (String tmpCFile : tmpCertFiles.split(";")) {
          tmpLogic.addX509CertFile(tmpCFile);
        }
      }
      byte[] tmpPasswd = null;
      if (line.hasOption("p")) {
        tmpPasswd = line.getOptionValue("p").getBytes();
      }
      String tmpExtractDir = null;
      if (line.hasOption("e")) {
        tmpExtractDir = new File(line.getOptionValue("e")).getPath();
      }

      for (String tmpFilePath : tmpArgs) {
        System.out.println("Verifying " + tmpFilePath);
        final File tmpFile = new File(tmpFilePath);
        if (!tmpFile.canRead()) {
          System.err.println("Couln't read the file. Check the path and permissions.");
          if (failFast) {
            System.exit(-1);
          }
          continue;
        }
        final VerificationResult tmpResult = tmpLogic.verify(tmpFilePath, tmpPasswd);
        if (tmpResult.getException() != null) {
          tmpResult.getException().printStackTrace();
          System.exit(-1);
        } else {
          System.out.println("Total revisions: " + tmpResult.getTotalRevisions());
          for (SignatureVerification tmpSigVer : tmpResult.getVerifications()) {
            System.out.println(tmpSigVer.toString());
            if (tmpExtractDir != null) {
              try {
                File tmpExFile = new File(tmpExtractDir + "/" + tmpFile.getName() + "_" + tmpSigVer.getRevision()
                    + ".pdf");
                System.out.println("Extracting to " + tmpExFile.getCanonicalPath());
                FileOutputStream tmpFOS = new FileOutputStream(tmpExFile.getCanonicalPath());

                InputStream tmpIS = tmpLogic.extractRevision(tmpFilePath, tmpPasswd, tmpSigVer.getName());
                IOUtils.copy(tmpIS, tmpFOS);
                tmpIS.close();
                tmpFOS.close();
              } catch (IOException ioe) {
                ioe.printStackTrace();
              }
            }
          }
          if (failFast && SignatureVerification.isError(tmpResult.getVerificationResultCode())) {
            System.exit(tmpResult.getVerificationResultCode());
          }
        }
      }
    }
  }
}
