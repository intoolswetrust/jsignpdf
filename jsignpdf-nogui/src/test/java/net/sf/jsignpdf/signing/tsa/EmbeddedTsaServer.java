package net.sf.jsignpdf.signing.tsa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampResponseGenerator;
import org.bouncycastle.tsp.TimeStampTokenGenerator;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Embedded RFC 3161 TSA server for integration tests. Uses a self-signed certificate
 * generated on the fly and BouncyCastle TSP to produce timestamp responses.
 * <p>
 * Usage:
 * <pre>
 * EmbeddedTsaServer tsa = new EmbeddedTsaServer();
 * tsa.start();
 * String tsaUrl = tsa.getUrl();
 * // ... use tsaUrl in signing options ...
 * tsa.stop();
 * </pre>
 */
public class EmbeddedTsaServer {

    private static final String TSA_POLICY_OID = "1.2.3.4.5";

    private HttpServer httpServer;
    private PrivateKey tsaPrivateKey;
    private X509Certificate tsaCertificate;
    private final AtomicLong serialCounter = new AtomicLong(1);
    private String requiredUsername;
    private String requiredPassword;

    /**
     * Configures the server to require HTTP Basic authentication. Must be called before {@link #start()}.
     */
    public void requireBasicAuth(String username, String password) {
        this.requiredUsername = username;
        this.requiredPassword = password;
    }

    /**
     * Generates a self-signed RSA key pair and certificate suitable for timestamping,
     * and starts an HTTP server on a random available port.
     */
    public void start() throws Exception {
        // Generate RSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        tsaPrivateKey = keyPair.getPrivate();

        // Create self-signed certificate with id-kp-timeStamping extended key usage
        X500Name subject = new X500Name("CN=Test TSA, O=JSignPdf Test");
        BigInteger serial = BigInteger.valueOf(1);
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.extendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));

        X509CertificateHolder certHolder = certBuilder.build(
                new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(tsaPrivateKey));
        tsaCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

        // Start HTTP server on a random port
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpContext context = httpServer.createContext("/tsa", new TsaHandler());
        if (requiredUsername != null) {
            context.setAuthenticator(new BasicAuthenticator("tsa") {
                @Override
                public boolean checkCredentials(String username, String password) {
                    return requiredUsername.equals(username) && requiredPassword.equals(password);
                }
            });
        }
        httpServer.start();
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    /**
     * Returns the URL of the running TSA server.
     */
    public String getUrl() {
        int port = httpServer.getAddress().getPort();
        return "http://127.0.0.1:" + port + "/tsa";
    }

    /**
     * HTTP handler that processes RFC 3161 timestamp requests.
     */
    private class TsaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                byte[] requestBytes;
                try (InputStream is = exchange.getRequestBody()) {
                    requestBytes = readAllBytes(is);
                }

                TimeStampRequest tsRequest = new TimeStampRequest(requestBytes);

                // Create a SHA-1 digest calculator (used internally by the token generator for serial number hashing)
                DigestCalculator digestCalculator = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                        .get(new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.3.14.3.2.26"))); // SHA-1

                // Build timestamp token generator
                TimeStampTokenGenerator tokenGenerator = new TimeStampTokenGenerator(
                        new JcaSimpleSignerInfoGeneratorBuilder()
                                .setProvider("BC")
                                .build("SHA256withRSA", tsaPrivateKey, tsaCertificate),
                        digestCalculator,
                        new ASN1ObjectIdentifier(TSA_POLICY_OID));

                tokenGenerator.addCertificates(new JcaCertStore(Collections.singletonList(tsaCertificate)));

                // Build response
                TimeStampResponseGenerator responseGenerator = new TimeStampResponseGenerator(
                        tokenGenerator, TSPAlgorithms.ALLOWED);

                BigInteger serialNumber = BigInteger.valueOf(serialCounter.getAndIncrement());
                TimeStampResponse tsResponse = responseGenerator.generate(tsRequest, serialNumber, new Date());

                byte[] responseBytes = tsResponse.getEncoded();
                exchange.getResponseHeaders().set("Content-Type", "application/timestamp-reply");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        }

        private byte[] readAllBytes(InputStream is) throws IOException {
            byte[] buf = new byte[4096];
            int total = 0;
            int n;
            while ((n = is.read(buf, total, buf.length - total)) != -1) {
                total += n;
                if (total == buf.length) {
                    byte[] newBuf = new byte[buf.length * 2];
                    System.arraycopy(buf, 0, newBuf, 0, total);
                    buf = newBuf;
                }
            }
            byte[] result = new byte[total];
            System.arraycopy(buf, 0, result, 0, total);
            return result;
        }
    }
}
