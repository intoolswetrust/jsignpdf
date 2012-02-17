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
import java.io.IOException;
import java.util.List;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.PdfUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.lowagie.text.pdf.PdfReader;

/**
 * Simple util to count signatures.
 * 
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.4 $
 * @created $Date: 2012/02/17 11:50:36 $
 */
public class SignatureCounter {

  /**
   * Main program.
   * 
   * @param args
   */
  public static void main(String[] args) {

    // create the Options
    final Option optHelp = new Option("h", "help", false, "print this message");
    final Option optDebug = new Option("d", "debug", false, "enables debug output");
    final Option optNames = new Option("n", "names", false,
        "print comma separated signature names instead of the count");
    final Option optPasswd = new Option("p", "password", true, "set password for opening PDF");
    optPasswd.setArgName("password");

    final Options options = new Options();
    options.addOption(optHelp);
    options.addOption(optDebug);
    options.addOption(optNames);
    options.addOption(optPasswd);

    CommandLine line = null;
    try {
      // create the command line parser
      CommandLineParser parser = new PosixParser();
      // parse the command line arguments
      line = parser.parse(options, args);
    } catch (ParseException exp) {
      System.err.println("Unable to parse command line (Use -h for the help)\n" + exp.getMessage());
      System.exit(Constants.EXIT_CODE_PARSE_ERR);
    }

    final String[] tmpArgs = line.getArgs();
    if (line.hasOption("h") || args == null || args.length == 0) {
      // automatically generate the help statement
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(70, "java -jar SignatureCounter.jar [file1.pdf [file2.pdf ...]]",
          "JSignPdf SignatureCounter is a command line tool which prints count of signatures in given PDF document.",
          options, null, true);
    } else {
      byte[] tmpPasswd = null;
      if (line.hasOption("p")) {
        tmpPasswd = line.getOptionValue("p").getBytes();
      }
      final boolean debug = line.hasOption("d");
      final boolean names = line.hasOption("n");

      for (String tmpFilePath : tmpArgs) {
        if (debug) {
          System.out.print("Counting signatures in " + tmpFilePath + ": ");
        }
        final File tmpFile = new File(tmpFilePath);
        if (!tmpFile.canRead()) {
          System.err.println("Couldn't read the file. Check the path and permissions: " + tmpFilePath);
          System.exit(Constants.EXIT_CODE_CANT_READ_FILE);
        }
        try {
          final PdfReader pdfReader = PdfUtils.getPdfReader(tmpFilePath, tmpPasswd);
          @SuppressWarnings("unchecked")
          final List<String> sigNames = pdfReader.getAcroFields().getSignatureNames();
          if (names) {
            //print comma-separated names
            boolean isNotFirst = false;
            for (String sig : sigNames) {
              if (isNotFirst) {
                System.out.println(",");
              } else {
                isNotFirst = true;
              }
              System.out.println(sig);
            }
          } else {
            //normal processing print only count of signatures
            System.out.println(sigNames.size());
            if (debug) {
              System.out.println("Signature names: " + sigNames);
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
          System.exit(Constants.EXIT_CODE_COMMON_ERROR);
        }
      }
    }
  }
}
