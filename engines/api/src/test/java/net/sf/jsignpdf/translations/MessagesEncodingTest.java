package net.sf.jsignpdf.translations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Verifies that every {@code messages*.properties} translation file is stored in
 * raw UTF-8 (the default for {@link java.util.ResourceBundle#getBundle(String)}
 * on Java 9+) and does not fall back to the legacy {@code \\uXXXX} native-ascii
 * escape form.
 *
 * <p>A {@code \\u} preceded by an odd number of backslashes is a literal
 * backslash followed by the letter {@code u} and is accepted; a {@code \\u}
 * preceded by an even number of backslashes is a real unicode escape and fails
 * the test.
 */
public class MessagesEncodingTest {

    private static final String DEFAULT_BUNDLE = "/net/sf/jsignpdf/translations/messages.properties";

    @Test
    public void allMessagesFilesAreValidUtf8() throws Exception {
        List<Path> files = listMessagesFiles();
        assertFalse("No messages*.properties files found", files.isEmpty());
        for (Path file : files) {
            byte[] bytes = Files.readAllBytes(file);
            assertStrictUtf8(file, bytes);
        }
    }

    @Test
    public void allMessagesFilesAreFreeOfUnicodeEscapes() throws Exception {
        List<Path> files = listMessagesFiles();
        assertFalse("No messages*.properties files found", files.isEmpty());
        for (Path file : files) {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            int offending = countRealUnicodeEscapes(content);
            assertTrue(file.getFileName() + " still contains " + offending
                    + " real \\uXXXX escape(s); expected raw UTF-8 characters instead",
                    offending == 0);
        }
    }

    private static void assertStrictUtf8(Path file, byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            fail(file.getFileName() + " is not valid UTF-8: " + e.getMessage());
        }
    }

    /**
     * Count occurrences of {@code \\uXXXX} whose preceding run of backslashes
     * has even length — i.e. the backslash introducing the escape is itself
     * unescaped. Literal sequences like {@code \\\\u2026} (file bytes
     * {@code \\\\\\\\u2026}) are intentionally excluded.
     */
    private static int countRealUnicodeEscapes(String content) {
        int count = 0;
        int i = 0;
        while ((i = content.indexOf("\\u", i)) != -1) {
            if (i + 6 <= content.length() && isHex4(content, i + 2)) {
                int leading = 0;
                int j = i - 1;
                while (j >= 0 && content.charAt(j) == '\\') {
                    leading++;
                    j--;
                }
                if (leading % 2 == 0) {
                    count++;
                }
            }
            i += 2;
        }
        return count;
    }

    private static boolean isHex4(String s, int start) {
        for (int k = 0; k < 4; k++) {
            char c = s.charAt(start + k);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static List<Path> listMessagesFiles() throws Exception {
        // Resolve the translations directory via a known resource file. Looking
        // up the directory itself is unreliable here because this test class
        // lives in the same package, which shadows the directory on the test
        // classpath.
        URL url = MessagesEncodingTest.class.getResource(DEFAULT_BUNDLE);
        assertNotNull("Default bundle not on classpath: " + DEFAULT_BUNDLE, url);
        Path dir = Paths.get(url.toURI()).getParent();
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "messages*.properties")) {
            for (Path p : stream) {
                files.add(p);
            }
        }
        return files;
    }
}
