package net.sf.jsignpdf.preview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link JpxCodecInstaller}: download, SHA-256 verification, atomic install, and failure/cancel cleanup. The
 * download is driven against an in-JVM {@link HttpServer} on a loopback port, so no external network is required.
 */
public class JpxCodecInstallerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private HttpServer server;
    private String baseUrl;
    private final Map<String, byte[]> content = new HashMap<>();

    /** A progress handler that records the last reported values and can be told to cancel. */
    private static final class RecordingHandler implements JpxCodecInstaller.ProgressHandler {
        long lastDone;
        long lastTotal;
        boolean cancel;

        @Override
        public void onProgress(long done, long total) {
            lastDone = done;
            lastTotal = total;
        }

        @Override
        public boolean isCancelled() {
            return cancel;
        }
    }

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @After
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] body = content.get(exchange.getRequestURI().getPath());
        if (body == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private JpxCodecInstaller.Artifact serve(String path, byte[] body, String sha256) {
        content.put("/" + path, body);
        return new JpxCodecInstaller.Artifact(path, sha256, body.length);
    }

    private static byte[] payload(int size, int seed) {
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = (byte) (i + seed);
        }
        return b;
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte x : digest) {
            sb.append(String.format(Locale.ROOT, "%02x", x));
        }
        return sb.toString();
    }

    @Test
    public void install_downloadsVerifiesAndInstallsAllArtifacts() throws Exception {
        Path pluginsDir = tmp.newFolder("plugins").toPath();
        byte[] a = payload(200_000, 1);
        byte[] b = payload(150_000, 2);
        List<JpxCodecInstaller.Artifact> artifacts = List.of(
                serve("repo/a.jar", a, sha256(a)),
                serve("repo/b.jar", b, sha256(b)));

        RecordingHandler handler = new RecordingHandler();
        new JpxCodecInstaller(pluginsDir).install(handler, baseUrl, artifacts);

        assertTrue(Files.isRegularFile(pluginsDir.resolve("a.jar")));
        assertTrue(Files.isRegularFile(pluginsDir.resolve("b.jar")));
        assertEquals(a.length, Files.size(pluginsDir.resolve("a.jar")));
        assertEquals(b.length, Files.size(pluginsDir.resolve("b.jar")));
        assertNoPartFiles(pluginsDir);
        // Progress reaches the combined total.
        assertEquals((long) a.length + b.length, handler.lastTotal);
        assertEquals(handler.lastTotal, handler.lastDone);
    }

    @Test
    public void install_rejectsArtifactWithWrongHash_andLeavesNothingBehind() throws Exception {
        Path pluginsDir = tmp.newFolder("plugins").toPath();
        byte[] a = payload(50_000, 7);
        List<JpxCodecInstaller.Artifact> artifacts = List.of(
                serve("repo/bad.jar", a, "0000000000000000000000000000000000000000000000000000000000000000"));

        try {
            new JpxCodecInstaller(pluginsDir).install(new RecordingHandler(), baseUrl, artifacts);
            fail("Expected IOException for SHA-256 mismatch");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("SHA-256 mismatch"));
        }
        assertFalse(Files.exists(pluginsDir.resolve("bad.jar")));
        assertNoPartFiles(pluginsDir);
    }

    @Test
    public void install_failsOnHttpError() throws Exception {
        Path pluginsDir = tmp.newFolder("plugins").toPath();
        // Not registered with the server -> 404.
        List<JpxCodecInstaller.Artifact> artifacts = List.of(
                new JpxCodecInstaller.Artifact("repo/missing.jar", sha256(new byte[0]), 0));

        try {
            new JpxCodecInstaller(pluginsDir).install(new RecordingHandler(), baseUrl, artifacts);
            fail("Expected IOException for HTTP 404");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("404"));
        }
        assertFalse(Files.exists(pluginsDir.resolve("missing.jar")));
        assertNoPartFiles(pluginsDir);
    }

    @Test
    public void install_cancels_andCleansUp() throws Exception {
        Path pluginsDir = tmp.newFolder("plugins").toPath();
        byte[] a = payload(300_000, 3);
        List<JpxCodecInstaller.Artifact> artifacts = List.of(serve("repo/a.jar", a, sha256(a)));

        RecordingHandler handler = new RecordingHandler();
        handler.cancel = true;

        try {
            new JpxCodecInstaller(pluginsDir).install(handler, baseUrl, artifacts);
            fail("Expected InterruptedException on cancel");
        } catch (InterruptedException expected) {
            // ok
        }
        assertFalse(Files.exists(pluginsDir.resolve("a.jar")));
        assertNoPartFiles(pluginsDir);
    }

    private static void assertNoPartFiles(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            assertFalse("No .part files should remain",
                    files.anyMatch(p -> p.getFileName().toString().endsWith(".part")));
        }
    }
}
