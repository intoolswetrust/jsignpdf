package net.sf.jsignpdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;

import org.apache.commons.cli.ParseException;
import org.junit.Test;

/**
 * Integration tests for {@link SignerOptionsFromCmdLine} that exercise the stdin-password feature end-to-end:
 * CLI parsing + canonical ordering + warning emission.
 */
public class SignerOptionsFromCmdLineTest {

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

    @Test
    public void sentinelWithoutFlag_quietSuppressesWarning() throws Exception {
        Fixture f = new Fixture("");
        f.opts.setCmdLine(new String[] { "-q", "-ksp", "-" });
        f.opts.loadCmdLine();
        assertEquals("-", new String(f.opts.getKsPasswd()));
        assertTrue("quiet mode must suppress the warning, was: " + f.warnings(), f.warnings().isEmpty());
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
