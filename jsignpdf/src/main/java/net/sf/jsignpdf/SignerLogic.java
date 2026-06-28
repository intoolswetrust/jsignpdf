package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.RES;
import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import net.sf.jsignpdf.engine.DssLtTrustPreflight;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.engine.EngineMismatchValidator;
import net.sf.jsignpdf.engine.EngineMismatchValidator.Mismatch;
import net.sf.jsignpdf.engine.EngineRegistry;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.utils.AppConfig;

import org.apache.commons.lang3.StringUtils;

/**
 * Signing dispatcher. Resolves the {@link SigningEngine} for the invocation, validates the options
 * against the engine's capabilities, then delegates the actual signing to the engine. The concrete
 * signing implementations live in the {@code jsignpdf-engine-*} modules and are discovered at runtime
 * via {@link EngineRegistry}.
 *
 * @author Josef Cacek
 */
public class SignerLogic implements Runnable {

    private final BasicSignerOptions options;

    /**
     * Constructor with all necessary parameters.
     *
     * @param anOptions options of signer
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
    @Override
    public void run() {
        signFile();
    }

    /**
     * Signs a single file: validates the input/output files, resolves and capability-checks the active
     * signing engine, and delegates the signing to it.
     *
     * @return true when signing is finished successfully, false otherwise
     */
    public boolean signFile() {
        final String outFile = options.getOutFileX();
        if (!validateInOutFiles(options.getInFile(), outFile)) {
            LOGGER.info(RES.get("console.skippingSigning"));
            return false;
        }

        boolean finished = false;
        try {
            final SigningEngine engine;
            try {
                engine = EngineRegistry.getInstance().resolve(options);
            } catch (RuntimeException e) {
                LOGGER.severe(RES.get("console.engineNotFound", StringUtils.defaultString(options.getEngine())));
                return false;
            }

            final List<Mismatch> mismatches = EngineMismatchValidator.findMismatches(options, engine);
            if (!mismatches.isEmpty()) {
                LOGGER.severe(RES.get("console.engineMismatch", engine.id()));
                for (Mismatch m : mismatches) {
                    LOGGER.severe(RES.get("console.engineMismatch.option", m.option(), m.capability().name()));
                }
                return false;
            }

            final EngineConfig engineConfig = AppConfig.engineConfigFor(engine.id());

            // Fail fast on an LT/LTA request the engine isn't configured to satisfy (issue #432), before any
            // key/PIN access or network round-trip, with the exact keys to set.
            final DssLtTrustPreflight.Result preflight =
                    DssLtTrustPreflight.check(options, engine, engineConfig);
            if (preflight.hasIssues()) {
                LOGGER.severe(RES.get("console.dss.ltPreflightFailed"));
                if (preflight.onlineMissing()) {
                    LOGGER.severe(RES.get("console.dss.ltPreflight.online"));
                }
                if (preflight.trustSourceMissing()) {
                    LOGGER.severe(RES.get("console.dss.ltPreflight.trust"));
                }
                return false;
            }

            finished = engine.sign(options, engineConfig);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, RES.get("console.exception"), e);
        } finally {
            LOGGER.info(RES.get("console.finished." + (finished ? "ok" : "error")));
            options.fireSignerFinishedEvent(null);
        }
        return finished;
    }

    /**
     * Validates if input and output files are valid for signing.
     *
     * @param inFile input file
     * @param outFile output file
     * @return true if valid, false otherwise
     */
    private boolean validateInOutFiles(final String inFile, final String outFile) {
        LOGGER.info(RES.get("console.validatingFiles"));
        if (StringUtils.isEmpty(inFile) || StringUtils.isEmpty(outFile)) {
            LOGGER.info(RES.get("console.fileNotFilled.error"));
            return false;
        }
        final File tmpInFile = new File(inFile);
        final File tmpOutFile = new File(outFile);
        if (!(tmpInFile.exists() && tmpInFile.isFile() && tmpInFile.canRead())) {
            LOGGER.info(RES.get("console.inFileNotFound.error"));
            return false;
        }
        if (tmpInFile.getAbsolutePath().equals(tmpOutFile.getAbsolutePath())) {
            LOGGER.info(RES.get("console.filesAreEqual.error"));
            return false;
        }
        return true;
    }

}
