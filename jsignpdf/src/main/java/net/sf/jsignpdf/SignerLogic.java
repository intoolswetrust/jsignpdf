package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.RES;
import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import net.sf.jsignpdf.engine.DssLtTrustPreflight;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.engine.EngineMismatchValidator;
import net.sf.jsignpdf.engine.EngineMismatchValidator.Mismatch;
import net.sf.jsignpdf.engine.EngineRegistry;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.SignatureFieldInfo;
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

            if (!validateSigField()) {
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

            if (!resolveSigField()) {
                return false;
            }

            finished = engine.sign(options, engineConfig);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, RES.get("console.exception"), e);
        } finally {
            options.setResolvedSigFieldName(null);
            LOGGER.info(RES.get("console.finished." + (finished ? "ok" : "error")));
            options.fireSignerFinishedEvent(null);
        }
        return finished;
    }

    /**
     * Fail-fast checks for {@code --sig-field} that need no I/O at all, so a wrong combination is reported
     * before any keystore or PIN access.
     *
     * <p>Signing into a pre-placed field means preserving the document as its author built it, so an effective
     * overwrite (non-incremental) mode is refused outright - not warned about, and not conditioned on whether
     * the input happens to be signed yet. Position options are only ignored, with a warning: the field's own
     * {@code /Rect} decides where the signature goes and neither library can relocate an existing widget.
     *
     * <p>A selected field also implies a visible signature: the whole appearance handling in both engines -
     * including the call that attaches the signature to the field - hangs off {@code isVisible()}, and an
     * author who drew a signature box wants something drawn in it. A zero-size field rectangle still yields an
     * invisible signature; that is the field's decision, not the flag's. The flag is set here rather than
     * together with the field resolution, because {@link EngineMismatchValidator} gates every
     * visible-signature capability check behind {@code isVisible()} and runs in between - setting it later
     * would leave {@code --img-path} and friends unvalidated for a field-placed appearance.
     *
     * @return true when signing can continue
     */
    private boolean validateSigField() {
        if (!options.isSigFieldSet()) {
            return true;
        }
        if (!options.isAppendX()) {
            // isAppendX() is false for an explicit --overwrite and whenever PDF encryption is requested, which
            // overrides the flag. Name which one applies - the encryption path is invisible in the command line.
            LOGGER.severe(RES.get(options.getPdfEncryption() == PDFEncryption.NONE
                    ? "console.sigField.overwriteNotSupported"
                    : "console.sigField.encryptionNotSupported", options.getSigFieldName()));
            return false;
        }
        if (isPositionSet()) {
            LOGGER.warning(RES.get("console.sigField.positionIgnored", options.getSigFieldName()));
        }
        options.setVisible(true);
        return true;
    }

    private boolean isPositionSet() {
        return options.getPage() != Constants.DEFVAL_PAGE || options.getPositionLLX() != Constants.DEFVAL_LLX
                || options.getPositionLLY() != Constants.DEFVAL_LLY || options.getPositionURX() != Constants.DEFVAL_URX
                || options.getPositionURY() != Constants.DEFVAL_URY;
    }

    /**
     * Resolves the configured {@code --sig-field} selector against the input file. The resolution is kept
     * separate from the configured value so that {@code #N} / {@code auto} are re-resolved for every file of a
     * batch run. The visible flag the selection implies is set earlier, in {@link #validateSigField()}.
     *
     * @return true when signing can continue
     */
    private boolean resolveSigField() {
        if (!options.isSigFieldSet()) {
            return true;
        }
        final String selector = options.getSigFieldName();
        try {
            final SignatureFieldInfo field = new PdfExtraInfo(options).resolveSignatureField(selector);
            options.setResolvedSigFieldName(field.name());
            LOGGER.info(RES.get("console.sigField.using", field.name(), String.valueOf(field.page())));
            if (!field.hasVisibleRect()) {
                LOGGER.info(RES.get("console.sigField.zeroSizeRect", field.name()));
            }
        } catch (SignatureFieldException e) {
            LOGGER.severe(e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.severe(RES.get("console.sigField.cantReadFields", StringUtils.defaultString(options.getInFile()),
                    StringUtils.defaultString(e.getMessage())));
            return false;
        }
        return true;
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
