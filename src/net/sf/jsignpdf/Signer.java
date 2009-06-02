package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.*;

import java.io.File;

import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

/**
 * JSignPdf main class - it either process command line or if no argument is given,
 * sets system Look&Feel and creates SignPdfForm GUI.
 * @author Josef Cacek
 */
public class Signer {

	private static void printHelp() {
		final HelpFormatter formatter = new HelpFormatter();
		final ResourceProvider res = ResourceProvider.getInstance();
		formatter.printHelp(80,
				"JSignPdf ",
				res.get("hlp.header"),
				SignerOptionsFromCmdLine.OPTS,
				res.get("hlp.footer"),
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
				final String[] tmpKeyStores = KeyStoreUtils.getKeyStores();
				tmpOpts.log("console.keystores");
				for (String tmpKsType : tmpKeyStores) {
					System.out.println(tmpKsType);
				}
			}
			if (tmpOpts.isListKeys()) {
				final String[] tmpKeyAliases = KeyStoreUtils.getKeyAliases(tmpOpts);
				tmpOpts.log("console.keys");
				//list certificate aliases in the keystore
				for (String tmpCert : tmpKeyAliases) {
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

	/**
	 * Sign the files
	 * @param anOpts
	 */
	private static void signFiles(SignerOptionsFromCmdLine anOpts) {
		final SignerLogic tmpLogic = new SignerLogic(anOpts);
		for (final String tmpInFile : anOpts.getFiles()) {
			final File tmpFile = new File(tmpInFile);
			if (! tmpFile.canRead()) {
				System.err.println(ResourceProvider.getInstance().get("file.notReadable", new String[] {tmpInFile}));
				continue;
			}
			anOpts.setInFile(tmpInFile);
			String tmpNameBase, tmpSuffix;
			if (tmpInFile.toLowerCase().endsWith(".pdf")) {
				tmpSuffix = tmpInFile.substring(tmpInFile.length() - 4);
				tmpNameBase = tmpInFile.substring(0, tmpInFile.length() - 4);
			} else {
				tmpSuffix = ".pdf";
				tmpNameBase = tmpInFile;
			}
			int tmpPos = tmpNameBase.replaceAll("\\\\", "/").lastIndexOf('/');
			if (tmpPos>-1) {
				tmpNameBase = tmpNameBase.substring(tmpPos + 1);
			}
			final StringBuilder tmpName = new StringBuilder(anOpts.getOutPath());
			tmpName.append(anOpts.getOutPrefix());
			tmpName.append(tmpNameBase).append(anOpts.getOutSuffix()).append(tmpSuffix);
			anOpts.setOutFile(tmpName.toString());
			tmpLogic.run();
		}

	}

}
