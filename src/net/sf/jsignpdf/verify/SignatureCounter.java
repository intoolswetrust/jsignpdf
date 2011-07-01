package net.sf.jsignpdf.verify;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
 * @version $Revision: 1.1 $
 * @created $Date: 2011/07/01 11:15:30 $
 */
public class SignatureCounter {

  /**
   * Main program.
   * 
   * @param args
   */
  public static void main(String[] args) {

    // create the Options
    Option optHelp = new Option("h", "help", false, "print this message");
    Option optDebug = new Option("d", "debug", false, "enables debug output");
    Option optPasswd = new Option("p", "password", true, "set password for opening PDF");
    optPasswd.setArgName("password");

    final Options options = new Options();
    options.addOption(optHelp);
    options.addOption(optDebug);
    options.addOption(optPasswd);

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

    final String[] tmpArgs = line.getArgs();
    if (line.hasOption("h") || args == null || args.length == 0) {
      // automatically generate the help statement
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(70, "java -jar SignatureCounter.jar [file1.pdf [file2.pdf ...]]",
          "JSignpdf SignatureCounter is a command line tool which prints count of signatures in given PDF document.",
          options, null, true);
    } else {
      byte[] tmpPasswd = null;
      if (line.hasOption("p")) {
        tmpPasswd = line.getOptionValue("p").getBytes();
      }
      boolean debug = line.hasOption("d");

      for (String tmpFilePath : tmpArgs) {
        if (debug) {
          System.out.print("Counting signatures in " + tmpFilePath + ": ");
        }
        final File tmpFile = new File(tmpFilePath);
        if (!tmpFile.canRead()) {
          System.err.println("Couldn't read the file. Check the path and permissions.");
          System.exit(-1);
        }
        try {
          final PdfReader pdfReader = PdfUtils.getPdfReader(tmpFilePath, tmpPasswd);
          @SuppressWarnings("unchecked")
          final List<String> sigNames = pdfReader.getAcroFields().getSignatureNames();
          System.out.println(sigNames.size());
          if (debug) {
            System.out.println("Signature names: " + sigNames);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
