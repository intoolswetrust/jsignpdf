package net.sf.jsignpdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration tests for {@link SignerOptionsFromCmdLine} that exercise the stdin-password feature end-to-end:
 * CLI parsing + canonical ordering + warning emission.
 */
public class SignerOptionsFromCmdLineTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void flagAndSentinel_singlePasswordReadFromStdin() throws Exception {
        Fixture f = new Fixture("stdin-ks\n");
        f.opts.setCmdLine(new String[] { "--enable-stdin-passwords", "-ksp", "-" });
        f.opts.loadCmdLine();
        assertEquals("stdin-ks", new String(f.opts.getKsPasswd()));
        assertTrue(f.warnings().isEmpty());
    }

    @Test
    public void flagAndSentinel_canonicalOrderRegardlessOfCliOrder() throws Exception {
        // CLI order: tsp, ksp. Expected stdin order: ksp first, tsp second.
        Fixture f = new Fixture("first-ks\nsecond-tsa\n");
        f.opts.setCmdLine(new String[] {
                "--enable-stdin-passwords",
                "-tsp", "-",
                "-ksp", "-",
        });
        f.opts.loadCmdLine();
        assertEquals("first-ks", new String(f.opts.getKsPasswd()));
        assertEquals("second-tsa", f.opts.getTsaPasswd());
    }

    @Test
    public void flagAndSentinel_threePasswordsInCanonicalOrder() throws Exception {
        // Canonical order: ksp, kp, opwd, upwd, tscp, tsp.
        Fixture f = new Fixture("a\nb\nc\n");
        f.opts.setCmdLine(new String[] {
                "--enable-stdin-passwords",
                "-tsp", "-",
                "-ksp", "-",
                "-kp", "-",
        });
        f.opts.loadCmdLine();
        assertEquals("a", new String(f.opts.getKsPasswd()));
        assertEquals("b", new String(f.opts.getKeyPasswd()));
        assertEquals("c", f.opts.getTsaPasswd());
    }

    @Test
    public void flagSet_literalNonDashValuePassesThrough() throws Exception {
        Fixture f = new Fixture("should-not-be-read\n");
        f.opts.setCmdLine(new String[] {
                "--enable-stdin-passwords",
                "-ksp", "secret",
        });
        f.opts.loadCmdLine();
        assertEquals("secret", new String(f.opts.getKsPasswd()));
    }

    @Test
    public void flagSet_dashInsideLongerStringIsLiteral() throws Exception {
        // Only exactly "-" is the sentinel. A value containing a dash must pass through literally.
        // Use --long-opt=value form because commons-cli would otherwise treat a leading-dash value
        // as a new option rather than as the argument of the previous one.
        Fixture f = new Fixture("should-not-be-read\n");
        f.opts.setCmdLine(new String[] {
                "--enable-stdin-passwords",
                "--keystore-password=-abc",
        });
        f.opts.loadCmdLine();
        assertEquals("-abc", new String(f.opts.getKsPasswd()));
    }

    @Test
    public void flagSet_trailingDashIsLiteral() throws Exception {
        // "abc-" ends with a dash but is not the sentinel.
        Fixture f = new Fixture("should-not-be-read\n");
        f.opts.setCmdLine(new String[] {
                "--enable-stdin-passwords",
                "-ksp", "abc-",
        });
        f.opts.loadCmdLine();
        assertEquals("abc-", new String(f.opts.getKsPasswd()));
    }

    @Test
    public void sentinelWithoutFlag_warnsAndUsesLiteralDash() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksp", "-" });
        f.opts.loadCmdLine();
        assertEquals("-", new String(f.opts.getKsPasswd()));
        String w = f.warnings();
        assertTrue("warning should name the option, was: " + w, w.contains("--keystore-password"));
        assertTrue("warning should name the flag, was: " + w, w.contains("--enable-stdin-passwords"));
    }

    @Test
    public void sentinelWithoutFlag_warnsPerOccurrence() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksp", "-", "-kp", "-" });
        f.opts.loadCmdLine();
        assertEquals("-", new String(f.opts.getKsPasswd()));
        assertEquals("-", new String(f.opts.getKeyPasswd()));
        String w = f.warnings();
        assertTrue("warning should mention keystore-password, was: " + w, w.contains("--keystore-password"));
        assertTrue("warning should mention key-password, was: " + w, w.contains("--key-password"));
    }

    /**
     * {@code -q} silences the loggers for the whole process, which is what the flag is for but outlives this
     * test in a shared surefire JVM - every later test that asserts on a log record would see nothing. The
     * levels are restored here; the class order that decides who runs after this is platform-dependent.
     */
    @Test
    public void sentinelWithoutFlag_quietSuppressesWarning() throws Exception {
        Level appLevel = Constants.LOGGER.getLevel();
        Level globalLevel = Logger.getGlobal().getLevel();
        try {
            Fixture f = new Fixture("");
            f.opts.setCmdLine(new String[] { "-q", "-ksp", "-" });
            f.opts.loadCmdLine();
            assertEquals("-", new String(f.opts.getKsPasswd()));
            assertTrue("quiet mode must suppress the warning, was: " + f.warnings(), f.warnings().isEmpty());
        } finally {
            Constants.LOGGER.setLevel(appLevel);
            Logger.getGlobal().setLevel(globalLevel);
        }
    }

    @Test
    public void flagSetWithoutSentinel_readerIsNotUsed() throws Exception {
        // If no password option uses "-", the reader must never be consulted.
        Fixture f = new Fixture("POISON\n");
        f.opts.setCmdLine(new String[] { "--enable-stdin-passwords", "-ksp", "real" });
        f.opts.loadCmdLine();
        assertEquals("real", new String(f.opts.getKsPasswd()));
    }

    @Test
    public void noPasswordOptions_noReaderNoWarnings() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksf", "/tmp/some.p12" });
        f.opts.loadCmdLine();
        assertNull(f.opts.getKsPasswd());
        assertTrue(f.warnings().isEmpty());
    }

    @Test
    public void eofDuringStdinReadThrowsParseException() {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "--enable-stdin-passwords", "-ksp", "-" });
        try {
            f.opts.loadCmdLine();
            fail("expected ParseException");
        } catch (ParseException e) {
            assertTrue("message should name the option, was: " + e.getMessage(),
                    e.getMessage().contains("--keystore-password"));
        }
    }

    @Test
    public void allSixPasswordsCanBeReadFromStdin() throws Exception {
        // Canonical order: ksp, kp, opwd, upwd, tscp, tsp.
        Fixture f = new Fixture("one\ntwo\nthree\nfour\nfive\nsix\n");
        f.opts.setCmdLine(new String[] {
                "--enable-stdin-passwords",
                "-tsp", "-",
                "-tscp", "-",
                "-upwd", "-",
                "-opwd", "-",
                "-kp", "-",
                "-ksp", "-",
        });
        f.opts.loadCmdLine();
        assertEquals("one", new String(f.opts.getKsPasswd()));
        assertEquals("two", new String(f.opts.getKeyPasswd()));
        assertEquals("three", new String(f.opts.getPdfOwnerPwd()));
        assertEquals("four", new String(f.opts.getPdfUserPwd()));
        assertEquals("five", f.opts.getTsaCertFilePwd());
        assertEquals("six", f.opts.getTsaPasswd());
    }

    @Test
    public void progressLinesReportCorrectIndexAndTotal() throws Exception {
        ByteArrayOutputStream progress = new ByteArrayOutputStream();
        SignerOptionsFromCmdLine opts = new SignerOptionsFromCmdLine();
        opts.setPasswordReader(new StdinPasswordReader(new BufferedReader(new StringReader("a\nb\n")), null,
                new PrintStream(progress), false));
        opts.setWarningOut(new PrintStream(new ByteArrayOutputStream()));
        opts.setCmdLine(new String[] { "--enable-stdin-passwords", "-tsp", "-", "-ksp", "-" });
        opts.loadCmdLine();

        String out = progress.toString();
        assertTrue("missing ksp progress line, was: " + out,
                out.contains("--keystore-password (1/2)"));
        assertTrue("missing tsp progress line, was: " + out,
                out.contains("--tsa-password (2/2)"));
        // Ordering: ksp line must appear before tsp line regardless of CLI order.
        assertTrue("ksp progress must precede tsp progress, was: " + out,
                out.indexOf("--keystore-password") < out.indexOf("--tsa-password"));
    }

    @Test
    public void dashFromPropertiesFile_isNotReinterpretedAsSentinel() throws Exception {
        // Per design-doc §3.5: a '-' set from a properties file is not a sentinel.
        // Simulate loadOptions() having set the password before CLI parse. With no matching
        // password option on the CLI, the resolver must leave the props-loaded value alone —
        // even when --enable-stdin-passwords is set, and the reader must not be consulted.
        Fixture f = new Fixture("POISON\n");
        f.opts.setKsPasswd("-");
        f.opts.setCmdLine(new String[] { "--enable-stdin-passwords" });
        f.opts.loadCmdLine();
        assertEquals("-", new String(f.opts.getKsPasswd()));
        assertTrue(f.warnings().isEmpty());
    }

    @Test
    public void cliSentinelOverridesPropertiesFileValue() throws Exception {
        // Per design-doc §3.5: when a password is set by a properties file and the same option
        // is also given on the CLI as the stdin sentinel, the CLI wins and stdin is consulted.
        Fixture f = new Fixture("from-stdin\n");
        f.opts.setKsPasswd("from-props");
        f.opts.setCmdLine(new String[] { "--enable-stdin-passwords", "-ksp", "-" });
        f.opts.loadCmdLine();
        assertEquals("from-stdin", new String(f.opts.getKsPasswd()));
    }

    @Test
    public void parseFailureForUnknownOption_doesNotTouchReader() {
        // Sanity check: commons-cli throws before we reach the resolver, reader is untouched.
        Fixture f = new Fixture("POISON\n");
        f.opts.setCmdLine(new String[] { "--no-such-flag" });
        try {
            f.opts.loadCmdLine();
            fail("expected ParseException for unknown option");
        } catch (ParseException expected) {
            // ok
        }
        assertFalse(f.warnings().contains("[jsignpdf]"));
    }

    @Test
    public void engineOption_setsEngineId() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "--engine", "openpdf" });
        f.opts.loadCmdLine();
        assertEquals("openpdf", f.opts.getEngine());
        assertFalse(f.opts.isListEngines());
    }

    @Test
    public void engineOption_shortForm() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-eng", "pdfbox" });
        f.opts.loadCmdLine();
        assertEquals("pdfbox", f.opts.getEngine());
    }

    @Test
    public void engineOption_absentLeavesNull() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksf", "/tmp/x.p12" });
        f.opts.loadCmdLine();
        assertNull(f.opts.getEngine());
    }

    @Test
    public void listEnginesOption_setsFlag() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "--list-engines" });
        f.opts.loadCmdLine();
        assertTrue(f.opts.isListEngines());
    }

    @Test
    public void listEnginesOption_shortForm() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-le" });
        f.opts.loadCmdLine();
        assertTrue(f.opts.isListEngines());
    }

    @Test
    public void padesLevelOption_longForm() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "--pades-level", "LTA" });
        f.opts.loadCmdLine();
        assertEquals(net.sf.jsignpdf.types.PadesLevel.BASELINE_LTA, f.opts.getPadesLevel());
    }

    @Test
    public void padesLevelOption_shortFormCaseInsensitive() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-pl", "lt" });
        f.opts.loadCmdLine();
        assertEquals(net.sf.jsignpdf.types.PadesLevel.BASELINE_LT, f.opts.getPadesLevel());
    }

    @Test
    public void padesLevelOption_absentLeavesNull() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksf", "/tmp/x.p12" });
        f.opts.loadCmdLine();
        assertNull(f.opts.getPadesLevel());
    }

    @Test
    public void append_isDefaultWhenNeitherFlagGiven() throws Exception {
        // Incremental append is the safe default and matches the GUI; omitting both flags must not request
        // an overwrite (which a PAdES engine like DSS cannot honour).
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksf", "/tmp/x.p12" });
        f.opts.loadCmdLine();
        assertTrue(f.opts.isAppend());
    }

    @Test
    public void overwriteFlag_disablesAppend() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksf", "/tmp/x.p12", "--overwrite" });
        f.opts.loadCmdLine();
        assertFalse(f.opts.isAppend());
    }

    @Test
    public void legacyAppendFlag_stillKeepsAppendOn() throws Exception {
        // --append is now redundant (append is the default) but must remain a harmless no-op for scripts.
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksf", "/tmp/x.p12", "--append" });
        f.opts.loadCmdLine();
        assertTrue(f.opts.isAppend());
    }

    @Test
    public void optionOverride_multiplePairsFlowToAdvancedConfig() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] {
                "-o", "engine.cliovr.online.enabled=true",
                "-o", "engine.cliovr.trust.certFiles=/path/ca.pem",
        });
        f.opts.loadCmdLine();
        net.sf.jsignpdf.engine.EngineConfig cfg = net.sf.jsignpdf.utils.AppConfig.engineConfigFor("cliovr");
        assertTrue(cfg.getBoolean("online.enabled", false));
        assertEquals("/path/ca.pem", cfg.getString("trust.certFiles"));
    }

    @Test
    public void optionOverride_valueMayContainEquals() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-o", "engine.cliovr.eq.value=a=b=c" });
        f.opts.loadCmdLine();
        assertEquals("a=b=c", net.sf.jsignpdf.utils.AppConfig.engineConfigFor("cliovr").getString("eq.value"));
    }

    @Test
    public void optionOverride_missingEqualsIsRejected() {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-o", "engine.cliovr.no-separator" });
        try {
            f.opts.loadCmdLine();
            fail("expected ParseException for a value without '='");
        } catch (ParseException e) {
            assertTrue("message should echo the bad value, was: " + e.getMessage(),
                    e.getMessage().contains("engine.cliovr.no-separator"));
        }
    }

    @Test
    public void optionOverride_emptyKeyIsRejected() {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-o", "=orphan" });
        try {
            f.opts.loadCmdLine();
            fail("expected ParseException for an empty key");
        } catch (ParseException expected) {
            // ok
        }
    }

    @Test
    public void outSuffix_withoutFlagStaysUnsetAndResolvesLazily() throws Exception {
        net.sf.jsignpdf.utils.AdvancedConfig cfg = net.sf.jsignpdf.utils.PropertyStoreFactory.getInstance()
                .advancedConfig();
        Fixture f = new Fixture("");
        try {
            f.opts.setCmdLine(new String[] { "-ksf", "/tmp/x.p12" }); // no -os
            f.opts.loadCmdLine();
            assertNull("No -os must leave the suffix unset so lower precedence levels apply", f.opts.getOutSuffix());
            cfg.setProperty("output.suffix", "_resolvedlate"); // changed after parsing
            assertEquals("_resolvedlate", f.opts.getOutSuffixX());
        } finally {
            cfg.removeProperty("output.suffix");
        }
    }

    @Test
    public void outSuffix_explicitFlagWinsOverConfiguredDefault() throws Exception {
        net.sf.jsignpdf.utils.AdvancedConfig cfg = net.sf.jsignpdf.utils.PropertyStoreFactory.getInstance()
                .advancedConfig();
        cfg.setProperty("output.suffix", "_fromcfg");
        try {
            Fixture f = new Fixture("");
            f.opts.setCmdLine(new String[] { "--out-suffix", "_explicit" });
            f.opts.loadCmdLine();
            assertEquals("_explicit", f.opts.getOutSuffix());
        } finally {
            cfg.removeProperty("output.suffix");
        }
    }

    @Test
    public void timestampOnly_isNotSwallowedAsTsaUrlAndImpliesTheTsaPlumbing() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-tso", "-ts", "http://tsa.example/tsa" });
        f.opts.loadCmdLine();
        assertTrue(f.opts.isTimestampOnly());
        assertEquals("http://tsa.example/tsa", f.opts.getTsaUrl());
        assertTrue("the flag must turn on the TSA plumbing", f.opts.isTimestampX());
    }

    @Test
    public void timestampOnly_tsaUrlFromPropertiesFileIsEnough() throws Exception {
        File props = tempFolder.newFile("tsa.properties");
        Files.writeString(props.toPath(), "tsa.enabled=false\ntsa.url=http\\://tsa.example/tsa\n");
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-lpf", props.getAbsolutePath(), "--timestamp-only" });
        f.opts.loadCmdLine();
        assertTrue(f.opts.isTimestampOnly());
        assertTrue("a TSA URL loaded from a properties file must still enable the TSA plumbing",
                f.opts.isTimestampX());
    }

    @Test
    public void timestampOnly_withoutTsaUrlIsRejected() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "--timestamp-only" });
        try {
            f.opts.loadCmdLine();
            fail("expected ParseException for a missing TSA URL");
        } catch (ParseException expected) {
            assertTrue(expected.getMessage().contains("--tsa-server-url"));
        }
    }

    @Test
    public void timestampOnly_rejectsTheOptionsItCannotHonour() throws Exception {
        assertTimestampOnlyRejects("--overwrite", "--overwrite");
        assertTimestampOnlyRejects("--sig-field", "-sf", "Signature1");
        assertTimestampOnlyRejects("--certification-level", "-cl", "CERTIFIED_NO_CHANGES_ALLOWED");
    }

    @Test
    public void timestampOnly_certLevelFromAPropertiesFileIsNotRejected() throws Exception {
        File props = tempFolder.newFile("certlevel.properties");
        Files.writeString(props.toPath(), "certification.level=CERTIFIED_NO_CHANGES_ALLOWED\n");
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-lpf", props.getAbsolutePath(), "-tso", "-ts", "http://tsa.example/tsa" });
        f.opts.loadCmdLine();
        assertTrue("only a typed flag is rejected, a reused properties file stays usable",
                f.opts.isTimestampOnly());
    }

    @Test
    public void timestampOnly_outSuffixFallsBackToTheTimestampKey() throws Exception {
        net.sf.jsignpdf.utils.AdvancedConfig cfg = net.sf.jsignpdf.utils.PropertyStoreFactory.getInstance()
                .advancedConfig();
        cfg.setProperty("output.suffix", "_signedcfg");
        cfg.setProperty("output.suffix.timestamp", "_stampedcfg");
        try {
            Fixture f = new Fixture("");
            f.opts.setCmdLine(new String[] { "-tso", "-ts", "http://tsa.example/tsa" });
            f.opts.loadCmdLine();
            assertNull(f.opts.getOutSuffix());
            assertEquals("output.suffix must not be consulted in this mode", "_stampedcfg", f.opts.getOutSuffixX());

            f = new Fixture("");
            f.opts.setCmdLine(new String[] { "-tso", "-ts", "http://tsa.example/tsa", "-os", "_explicit" });
            f.opts.loadCmdLine();
            assertEquals("an explicit suffix wins", "_explicit", f.opts.getOutSuffixX());
        } finally {
            cfg.removeProperty("output.suffix");
            cfg.removeProperty("output.suffix.timestamp");
        }
    }

    private void assertTimestampOnlyRejects(String expectedInMessage, String... offendingOption) throws Exception {
        Fixture f = new Fixture("");
        String[] base = { "-tso", "-ts", "http://tsa.example/tsa" };
        String[] args = new String[base.length + offendingOption.length];
        System.arraycopy(base, 0, args, 0, base.length);
        System.arraycopy(offendingOption, 0, args, base.length, offendingOption.length);
        f.opts.setCmdLine(args);
        try {
            f.opts.loadCmdLine();
            fail("expected ParseException for " + expectedInMessage);
        } catch (ParseException expected) {
            assertTrue("the message must name the typed flag: " + expected.getMessage(),
                    expected.getMessage().contains(expectedInMessage));
        }
    }

    @Test
    public void sigFieldOptionIsParsed() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-sf", "Signature2" });
        f.opts.loadCmdLine();
        assertEquals("Signature2", f.opts.getSigFieldName());
        assertTrue(f.opts.isSigFieldSet());
    }

    @Test
    public void sigFieldSelectorsArePassedThroughUnresolved() throws Exception {
        // The selectors are resolved per input file at signing time, so parsing must keep them verbatim.
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "--sig-field", "#2" });
        f.opts.loadCmdLine();
        assertEquals("#2", f.opts.getSigFieldName());

        Fixture auto = new Fixture("");
        auto.opts.setCmdLine(new String[] { "--sig-field", "auto" });
        auto.opts.loadCmdLine();
        assertEquals("auto", auto.opts.getSigFieldName());
    }

    @Test
    public void noSigFieldOptionMeansNewField() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-ksf", "/tmp/x.p12" });
        f.opts.loadCmdLine();
        assertNull(f.opts.getSigFieldName());
        assertFalse(f.opts.isSigFieldSet());
    }

    @Test
    public void listSigFieldsIsACommand() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-lsf", "document.pdf" });
        f.opts.loadCmdLine();
        assertTrue(f.opts.isListSigFields());
        assertEquals("document.pdf", f.opts.getInFile());

        Fixture without = new Fixture("");
        without.opts.setCmdLine(new String[] { "document.pdf" });
        without.opts.loadCmdLine();
        assertFalse(without.opts.isListSigFields());
    }

    /**
     * The command line has no basic mode, so a properties file must not be able to turn the advanced flag off:
     * it gates whether the key password is used at all ({@code getKeyPasswdX()} falls back to the keystore
     * password without it), and signing then dies with an UnrecoverableKeyException. Same for the PDF
     * passwords and the permissions.
     */
    @Test
    public void loadedPropertiesCanNotTurnOffAdvancedMode() throws Exception {
        File props = tempFolder.newFile("basic-mode.properties");
        Files.writeString(props.toPath(), "view.advanced=false\n");

        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-lpf", props.getAbsolutePath(), "-ksp", "storepass", "-kp", "keypass",
                "-opwd", "ownerpass", "document.pdf" });
        f.opts.loadCmdLine();

        assertTrue("the command line is always advanced", f.opts.isAdvanced());
        assertEquals("keypass", new String(f.opts.getKeyPasswdX()));
        assertEquals("ownerpass", f.opts.getPdfOwnerPwdStrX());
    }

    /** Convenience wiring: captures warnings and feeds a canned stdin reader with no Console. */
    private static final class Fixture {
        final SignerOptionsFromCmdLine opts = new SignerOptionsFromCmdLine();
        final ByteArrayOutputStream warningBytes = new ByteArrayOutputStream();

        Fixture(String stdin) {
            opts.setPasswordReader(new StdinPasswordReader(new BufferedReader(new StringReader(stdin)), null,
                    new PrintStream(new ByteArrayOutputStream()), true));
            opts.setWarningOut(new PrintStream(warningBytes));
        }

        String warnings() {
            return warningBytes.toString();
        }
    }
}
