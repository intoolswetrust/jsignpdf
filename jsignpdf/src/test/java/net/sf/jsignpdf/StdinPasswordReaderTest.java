package net.sf.jsignpdf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import org.junit.Test;

/**
 * Unit tests for {@link StdinPasswordReader}.
 */
public class StdinPasswordReaderTest {

    @Test
    public void readsSinglePassword() throws Exception {
        StdinPasswordReader r = reader("secret\n", false);
        assertArrayEquals("secret".toCharArray(), r.readNext("keystore-password", 1, 1));
    }

    @Test
    public void readsMultiplePasswordsInOrder() throws Exception {
        StdinPasswordReader r = reader("a\nb\nc\n", false);
        assertArrayEquals("a".toCharArray(), r.readNext("keystore-password", 1, 3));
        assertArrayEquals("b".toCharArray(), r.readNext("key-password", 2, 3));
        assertArrayEquals("c".toCharArray(), r.readNext("tsa-password", 3, 3));
    }

    @Test
    public void handlesCrlfLineEndings() throws Exception {
        StdinPasswordReader r = reader("a\r\nb\r\n", false);
        assertArrayEquals("a".toCharArray(), r.readNext("keystore-password", 1, 2));
        assertArrayEquals("b".toCharArray(), r.readNext("key-password", 2, 2));
    }

    @Test
    public void handlesMissingTrailingNewline() throws Exception {
        StdinPasswordReader r = reader("tail", false);
        assertArrayEquals("tail".toCharArray(), r.readNext("keystore-password", 1, 1));
    }

    @Test
    public void emitsProgressLineWithOptionAndIndex() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StdinPasswordReader r = new StdinPasswordReader(new BufferedReader(new StringReader("x\n")), null,
                new PrintStream(baos), false);
        r.readNext("tsa-cert-password", 2, 3);
        String out = baos.toString();
        assertTrue("progress line missing prefix, was: " + out, out.contains("[jsignpdf] "));
        assertTrue("progress line missing long option name, was: " + out, out.contains("--tsa-cert-password"));
        assertTrue("progress line missing index/total, was: " + out, out.contains("(2/3)"));
        assertTrue("progress line missing source marker, was: " + out, out.contains("stdin"));
    }

    @Test
    public void quietSuppressesProgressLine() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StdinPasswordReader r = new StdinPasswordReader(new BufferedReader(new StringReader("x\n")), null,
                new PrintStream(baos), true);
        r.readNext("keystore-password", 1, 1);
        assertEquals("quiet mode must not write progress", "", baos.toString());
    }

    @Test
    public void eofBeforeReadThrows() {
        StdinPasswordReader r = reader("", false);
        try {
            r.readNext("keystore-password", 1, 1);
            fail("expected EOFException");
        } catch (IOException e) {
            assertTrue(e instanceof EOFException);
            assertTrue("message should name the option, was: " + e.getMessage(),
                    e.getMessage().contains("--keystore-password"));
        }
    }

    @Test
    public void ioExceptionDuringReadIsWrapped() {
        BufferedReader throwing = new BufferedReader(new StringReader("")) {
            @Override
            public String readLine() throws IOException {
                throw new IOException("boom");
            }
        };
        StdinPasswordReader r = new StdinPasswordReader(throwing, null, new PrintStream(new ByteArrayOutputStream()),
                true);
        try {
            r.readNext("user-password", 1, 1);
            fail("expected IOException");
        } catch (IOException e) {
            assertFalse("EOFException should be reserved for null reads", e instanceof EOFException);
            assertTrue("message should name the option, was: " + e.getMessage(),
                    e.getMessage().contains("--user-password"));
            assertTrue("message should include the underlying cause text, was: " + e.getMessage(),
                    e.getMessage().contains("boom"));
        }
    }

    private static StdinPasswordReader reader(String stdin, boolean quiet) {
        return new StdinPasswordReader(new BufferedReader(new StringReader(stdin)), null,
                new PrintStream(new ByteArrayOutputStream()), quiet);
    }
}
