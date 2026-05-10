package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.EXIT_CODE_NO_COMMAND;
import static net.sf.jsignpdf.Constants.EXIT_CODE_PARSE_ERR;
import static net.sf.jsignpdf.Constants.NEW_LINE;
import static net.sf.jsignpdf.Constants.RES;
import static net.sf.jsignpdf.Constants.VERSION;
import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.File;
import java.io.FileFilter;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import net.sf.jsignpdf.fx.FxLauncher;
import net.sf.jsignpdf.ssl.SSLInitializer;
import net.sf.jsignpdf.utils.GuiUtils;
import net.sf.jsignpdf.utils.KeyStoreUtils;
import net.sf.jsignpdf.utils.PKCS11Utils;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * JSignPdf main class - it either process command line or if no argument is given, sets system Look&Feel and creates
 * SignPdfForm GUI.
 *
 * @author Josef Cacek
 */
public class Signer {

    /** GitHub issues URL shown in the JavaFX fallback warning dialog. */
    static final String GITHUB_ISSUES_URL = "https://github.com/intoolswetrust/jsignpdf/issues";

    // Package-private launch hooks. Tests replace these to drive launchGui without opening real windows.
    static Consumer<BasicSignerOptions> fxLauncher = FxLauncher::launch;
    static Consumer<BasicSignerOptions> swingLauncher = Signer::doLaunchSwing;
    static Consumer<Throwable> fxFallbackNotifier = Signer::showFxFallbackWarning;

    /**
     * Prints formatted help message (command line arguments).
     */
    private static void printHelp() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80, "java -jar JSignPdf.jar [file1.pdf [file2.pdf ...]]", RES.get("hlp.header"),
                SignerOptionsFromCmdLine.OPTS, NEW_LINE + RES.get("hlp.footer.exitCodes") + NEW_LINE
                        + StringUtils.repeat("-", 80) + NEW_LINE + RES.get("hlp.footer.examples"),
                true);
    }

    /**
     * Main.
     *
     * @param args
     */
    public static void main(String[] args) {
        SignerOptionsFromCmdLine tmpOpts = null;

        if (args != null && args.length > 0) {
            tmpOpts = new SignerOptionsFromCmdLine();
            parseCommandLine(args, tmpOpts);
        }

        try {
            SSLInitializer.init();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to re-configure SSL layer", e);
        }

        PKCS11Utils.registerProvidersFromDefaultLocation();

        traceInfo();
        boolean showGui = true;

        if (tmpOpts != null) {
            showGui = false;
            if (tmpOpts.isPrintVersion()) {
                System.out.println("JSignPdf version " + VERSION);
                return;
            }
            if (tmpOpts.isPrintHelp()) {
                printHelp();
                return;
            }
            if (tmpOpts.isListKeyStores()) {
                LOGGER.info(RES.get("console.keystores"));
                for (String tmpKsType : KeyStoreUtils.getKeyStores()) {
                    System.out.println(tmpKsType);
                }
                return;
            }
            if (tmpOpts.isListKeys()) {
                final String[] tmpKeyAliases = KeyStoreUtils.getKeyAliases(tmpOpts);
                LOGGER.info(RES.get("console.keys"));
                // list certificate aliases in the keystore
                for (String tmpCert : tmpKeyAliases) {
                    System.out.println(tmpCert);
                }
                return;
            }
            if (tmpOpts.isGui()) {
                showGui = true;
            } else if (ArrayUtils.isNotEmpty(tmpOpts.getFiles())
                    || (!StringUtils.isEmpty(tmpOpts.getInFile()) && !StringUtils.isEmpty(tmpOpts.getOutFile()))) {
                signFiles(tmpOpts);
                exit(0);
            } else {
                final boolean tmpCommand = tmpOpts.isPrintVersion() || tmpOpts.isPrintHelp() || tmpOpts.isListKeyStores()
                        || tmpOpts.isListKeys();
                if (!tmpCommand) {
                    // no valid command provided - print help and exit
                    printHelp();
                    exit(EXIT_CODE_NO_COMMAND);
                }
                exit(0);
            }
        }

        if (showGui) {
            launchGui(tmpOpts);
        }
    }

    /**
     * Starts the GUI. The JavaFX UI is the default; if its initialization fails (e.g. because of missing/incompatible
     * native libraries) a warning dialog is shown and the legacy Swing UI is started as a fallback.
     *
     * <p>The {@code -Djsignpdf.swing=true} system property forces the legacy Swing UI without attempting JavaFX.
     */
    static void launchGui(BasicSignerOptions opts) {
        if (Boolean.getBoolean("jsignpdf.swing")) {
            swingLauncher.accept(opts);
            return;
        }
        try {
            fxLauncher.accept(opts);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "JavaFX UI failed to initialize, falling back to Swing UI", t);
            try {
                fxFallbackNotifier.accept(t);
            } catch (Throwable ignored) {
                // Don't block the Swing fallback if the warning dialog itself can't be shown (e.g. headless).
            }
            swingLauncher.accept(opts);
        }
    }

    static String buildFxFallbackMessage(Throwable cause) {
        String reason;
        if (cause == null) {
            reason = "";
        } else if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
            reason = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        } else {
            reason = cause.getClass().getName();
        }
        return RES.get("gui.fxFallback.message", new String[] { reason, GITHUB_ISSUES_URL });
    }

    private static void showFxFallbackWarning(Throwable cause) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default L&F if system L&F can't be set.
        }
        JOptionPane.showMessageDialog(null, buildFxFallbackMessage(cause),
                RES.get("gui.fxFallback.title"), JOptionPane.WARNING_MESSAGE);
    }

    private static void doLaunchSwing(BasicSignerOptions opts) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Can't set Look&Feel.");
        }
        SignPdfForm tmpForm = new SignPdfForm(WindowConstants.EXIT_ON_CLOSE, opts);
        tmpForm.pack();
        GuiUtils.center(tmpForm);
        tmpForm.setVisible(true);
    }

    /**
     * Writes info about security providers to the {@link Logger} instance. The log-level for messages is FINER.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void traceInfo() {
        if (LOGGER.isLoggable(Level.FINER)) {
            try {
                Provider[] aProvider = Security.getProviders();
                for (int i = 0; i < aProvider.length; i++) {
                    Provider provider = aProvider[i];
                    LOGGER.finer("Provider " + (i + 1) + " : " + provider.getName() + " " + provider.getInfo() + " :");
                    List keyList = new ArrayList(provider.keySet());
                    try {
                        Collections.sort(keyList);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINER, "Provider's properties keys can't be sorted", e);
                    }
                    Iterator keyIterator = keyList.iterator();
                    while (keyIterator.hasNext()) {
                        String key = (String) keyIterator.next();
                        LOGGER.finer(key + ": " + provider.getProperty(key));
                    }
                    LOGGER.finer("------------------------------------------------");
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINER, "Listing security providers failed", e);
            }
        }
    }

    /**
     * Sign the files
     *
     * @param anOpts
     */
    private static void signFiles(SignerOptionsFromCmdLine anOpts) {
        final SignerLogic tmpLogic = new SignerLogic(anOpts);
        if (ArrayUtils.isEmpty(anOpts.getFiles())) {
            // we've used -lp (loadproperties) parameter
            if (!tmpLogic.signFile()) {
                exit(Constants.EXIT_CODE_ALL_SIG_FAILED);
            }
            return;
        }
        int successCount = 0;
        int failedCount = 0;

        for (final String wildcardPath : anOpts.getFiles()) {
            final File wildcardFile = new File(wildcardPath);

            File[] inputFiles;
            if (StringUtils.containsAny(wildcardFile.getName(), '*', '?')) {
                final File inputFolder = wildcardFile.getAbsoluteFile().getParentFile();
                final FileFilter fileFilter = new AndFileFilter(FileFileFilter.FILE,
                        new WildcardFileFilter(wildcardFile.getName()));
                inputFiles = inputFolder.listFiles(fileFilter);
                if (inputFiles == null) {
                    continue;
                }
            } else {
                inputFiles = new File[] { wildcardFile };
            }
            for (File inputFile : inputFiles) {
                final String tmpInFile = inputFile.getPath();
                if (!inputFile.canRead()) {
                    failedCount++;
                    System.err.println(RES.get("file.notReadable", new String[] { tmpInFile }));
                    continue;
                }
                anOpts.setInFile(tmpInFile);
                String tmpNameBase = inputFile.getName();
                String tmpSuffix = ".pdf";
                if (StringUtils.endsWithIgnoreCase(tmpNameBase, tmpSuffix)) {
                    tmpSuffix = StringUtils.right(tmpNameBase, 4);
                    tmpNameBase = StringUtils.left(tmpNameBase, tmpNameBase.length() - 4);
                }
                final StringBuilder tmpName = new StringBuilder(anOpts.getOutPath());
                tmpName.append(anOpts.getOutPrefix());
                tmpName.append(tmpNameBase).append(anOpts.getOutSuffix()).append(tmpSuffix);
                anOpts.setOutFile(tmpName.toString());
                if (tmpLogic.signFile()) {
                    successCount++;
                } else {
                    failedCount++;
                }

            }
        }
        if (failedCount > 0) {
            exit(successCount > 0 ? Constants.EXIT_CODE_SOME_SIG_FAILED : Constants.EXIT_CODE_ALL_SIG_FAILED);
        }
    }

    /**
     * Parses the command line. Exits with error exit code when parsing fails.
     *
     * @param args
     * @param opts
     */
    private static void parseCommandLine(String[] args, final SignerOptionsFromCmdLine opts) {
        opts.setCmdLine(args);
        try {
            opts.loadCmdLine();
        } catch (ParseException exp) {
            System.err.println("Unable to parse command line (Use -h for the help)\n" + exp.getMessage());
            exit(EXIT_CODE_PARSE_ERR);
        }
    }

    private static void exit(int exitCode) {
        PKCS11Utils.unregisterProviders();
        System.exit(exitCode);
    }
}
