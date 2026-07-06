package net.sf.jsignpdf.preview;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Downloads the optional (non-bundled) JPEG 2000 ImageIO codec into the user's plugins directory, verifying each jar
 * against a pinned SHA-256 before it is accepted. The jars are the standard {@code jai-imageio-jpeg2000} reader plus its
 * {@code jai-imageio-core} dependency, fetched directly from Maven Central. We never redistribute them: the user opts in
 * and downloads them, so JSignPdf's shipped artifact stays free of the JJ2000 licence.
 *
 * @see JpxPluginManager for registration of the downloaded reader
 */
public final class JpxCodecInstaller {

    /** Maven Central base URL. */
    static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";

    /** URL of the codec licence, shown to the user so their consent is informed. */
    public static final String LICENSE_URL = "https://github.com/jai-imageio/jai-imageio-jpeg2000/blob/master/LICENSE-JJ2000.txt";

    /** A single pinned jar: its Maven path, expected SHA-256, and size (for a determinate progress bar). */
    record Artifact(String path, String sha256, long size) {
        String fileName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        URI uri(String baseUrl) {
            return URI.create(baseUrl + path);
        }
    }

    /** Pinned artifacts (versions and hashes verified against Maven Central). */
    static final List<Artifact> ARTIFACTS = List.of(
            new Artifact("com/github/jai-imageio/jai-imageio-jpeg2000/1.4.0/jai-imageio-jpeg2000-1.4.0.jar",
                    "07fb6e3a3040122b846c5e52520033175c3251e2ec8830df82f87cb21f388bb1", 489144L),
            new Artifact("com/github/jai-imageio/jai-imageio-core/1.4.0/jai-imageio-core-1.4.0.jar",
                    "8ad3c68e9efffb10ac87ff8bc589adf64b04a729c5194c079efd0643607fd72a", 628053L));

    /** Total number of bytes across all pinned artifacts. */
    public static final long TOTAL_BYTES = ARTIFACTS.stream().mapToLong(Artifact::size).sum();

    /** Progress/cancellation bridge so the caller can drive a UI and abort the download. */
    public interface ProgressHandler {
        /** Called as bytes arrive; {@code done} counts across all artifacts, {@code total} is {@link #TOTAL_BYTES}. */
        void onProgress(long done, long total);

        /** Polled between chunks; returning {@code true} aborts the download. */
        boolean isCancelled();
    }

    private final Path pluginsDir;

    public JpxCodecInstaller(Path pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    /**
     * Downloads and verifies every pinned artifact into the plugins directory. Each jar is streamed to a {@code .part}
     * file, checked against its pinned SHA-256, and only then atomically moved into place; on any failure the partial
     * file is removed and nothing is left half-installed.
     *
     * @throws InterruptedException if the handler requested cancellation
     * @throws IOException on network, I/O, or verification failure
     */
    public void install(ProgressHandler handler) throws IOException, InterruptedException {
        install(handler, MAVEN_CENTRAL, ARTIFACTS);
    }

    // Visible for tests: lets the download be driven against a local server with test artifacts.
    void install(ProgressHandler handler, String baseUrl, List<Artifact> artifacts)
            throws IOException, InterruptedException {
        Files.createDirectories(pluginsDir);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        long total = artifacts.stream().mapToLong(Artifact::size).sum();
        long done = 0;
        for (Artifact artifact : artifacts) {
            done = downloadAndVerify(client, artifact, baseUrl, total, done, handler);
        }
    }

    private long downloadAndVerify(HttpClient client, Artifact artifact, String baseUrl, long total, long doneBefore,
            ProgressHandler handler) throws IOException, InterruptedException {
        Path target = pluginsDir.resolve(artifact.fileName());
        Path part = pluginsDir.resolve(artifact.fileName() + ".part");
        MessageDigest digest = newSha256();

        URI uri = artifact.uri(baseUrl);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("Download failed for " + uri, e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status " + response.statusCode() + " for " + uri);
        }

        long done = doneBefore;
        try (InputStream in = response.body()) {
            byte[] buffer = new byte[64 * 1024];
            try (var out = Files.newOutputStream(part, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (handler.isCancelled()) {
                        throw new InterruptedException("Download cancelled");
                    }
                    out.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    done += read;
                    handler.onProgress(Math.min(done, total), total);
                }
            }

            String actual = toHex(digest.digest());
            if (!artifact.sha256().equalsIgnoreCase(actual)) {
                throw new IOException("SHA-256 mismatch for " + artifact.fileName()
                        + " (expected " + artifact.sha256() + ", got " + actual + ")");
            }
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.fine("Installed codec jar " + target);
            return done;
        } catch (IOException | InterruptedException | RuntimeException e) {
            try {
                Files.deleteIfExists(part);
            } catch (IOException ignored) {
                // best effort cleanup
            }
            throw e;
        }
    }

    private static MessageDigest newSha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
