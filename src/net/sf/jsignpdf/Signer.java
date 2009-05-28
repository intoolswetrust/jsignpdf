package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.*;

import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

/**
 * Simple main class - it sets system Look&Feel and creates SignPdfForm GUI
 * @author Josef Cacek
 */
public class Signer {

	private static void printHelp() {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(70,
				"JSignPdf.bat ",
				"//TODO header",
				SignerOptionsFromCmdLine.OPTS,
				"//TODO footer",
				true);
	}

	/**
	 * Main.
	 * @param args
	 */
	public static void main(String[] args) {
		if (args!=null && args.length>0) {
			final SignerOptionsFromCmdLine tmpOpts = new SignerOptionsFromCmdLine();
			try {
				tmpOpts.loadCmdLine(args);
			} catch (ParseException exp) {
				System.out.println("Unable to parse command line, check the arguments. (Use -h for help screen.)");
				exp.printStackTrace();
				System.exit(EXIT_CODE_PARSE_ERR);
			}

			if (tmpOpts.isPrintVersion()) {
				System.out.println("JSignPdf version " + VERSION);
			}
			if (tmpOpts.isPrintHelp()) {
				printHelp();
			}
			if (tmpOpts.isListKeyStores()) {
				System.out.println("KeyStores:");
				for (String tmpKsType : KeyStoreUtils.getKeyStores()) {
					System.out.println(tmpKsType);
				}
			}
			if (tmpOpts.isListKeys()) {
				System.out.println("Keys:");
				//list certificate aliases in the keystore
				for (String tmpCert : KeyStoreUtils.getCertAliases(
					tmpOpts.getKsType(),
					tmpOpts.getKsFile(),
					tmpOpts.getKsPasswdStr())) {

					System.out.println(tmpCert);
				}
			}
			if (tmpOpts.getFiles()!=null && tmpOpts.getFiles().length>0) {
				signFiles(tmpOpts);
			} else {
				final boolean tmpCommand =
					tmpOpts.isPrintVersion() || tmpOpts.isPrintHelp() || tmpOpts.isListKeyStores() ||
					tmpOpts.isListKeys();
				if (!tmpCommand) {
					//no valid command provided - print help and exit
					printHelp();
					System.exit(EXIT_CODE_NO_COMMAND);
				}
			}
		} else {
			try {
		        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		    } catch (Exception e) {
		        System.err.println("Can't set Look&Feel.");
		    }
			SignPdfForm tmpForm = new SignPdfForm(WindowConstants.EXIT_ON_CLOSE);
			tmpForm.pack();
			tmpForm.setVisible(true);
		}
	}

	private static void signFiles(SignerOptionsFromCmdLine anOpts) {
		for (String tmpFile : anOpts.getFiles()) {
			String tmpNameBase, tmpSuffix;
			//TODO remove directory from name base
			// add argument for directory
			if (tmpFile.toUpperCase().endsWith(".pdf")) {
				tmpSuffix = tmpFile.substring(tmpFile.length() - 4);
				tmpNameBase = tmpFile.substring(0, tmpFile.length() - 4);
			} else {
				tmpSuffix = ".pdf";
				tmpNameBase = tmpFile;
			}
			final StringBuilder tmpName = new StringBuilder(anOpts.getOutPrefix());
			tmpName.append(tmpNameBase).append(anOpts.getOutSuffix()).append(tmpSuffix);
			anOpts.setOutFile(tmpName.toString());
		}

	}

}
