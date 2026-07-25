package net.sf.jsignpdf.engine.dss;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Embedded mini-CA for the DSS engine tests, the counterpart to {@link EmbeddedTsaServer}. It generates a
 * self-signed root CA on the fly and runs an in-JVM HTTP server on a random loopback port that serves the
 * root's CRL (no external network). Leaf signing certificates issued by {@link #issueSigningKeyStore} carry a
 * CRL distribution point pointing at that endpoint, so DSS can fetch revocation data for them and produce the
 * LT / LTA baseline levels offline and deterministically.
 *
 * <p>
 * Pin the root via {@link #getCaCertificate()} as a DSS trust anchor: DSS skips revocation fetching for
 * untrusted chains (the failure reported in issue #432), so the happy-path LT/LTA tests must trust the issuer
 * before the CRL is even consulted.
 * </p>
 */
final class EmbeddedCa {

    private static final String SIG_ALG = "SHA256withRSA";

    private HttpServer httpServer;
    private KeyPair caKeyPair;
    private X500Name caName;
    private X509Certificate caCertificate;
    private String crlUrl;
    private final AtomicLong crlNumber = new AtomicLong(1);
    private final AtomicLong leafSerial = new AtomicLong(2);

    /**
     * Generates the self-signed root CA (with {@code keyCertSign + cRLSign} usage) and starts the loopback CRL
     * endpoint. Must be called before issuing certificates.
     */
    void start() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        caKeyPair = kpg.generateKeyPair();

        caName = new X500Name("CN=JSignPdf Test Root CA, O=JSignPdf Test");
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        X509v3CertificateBuilder caBuilder = new JcaX509v3CertificateBuilder(
                caName, BigInteger.ONE, notBefore, notAfter, caName, caKeyPair.getPublic());
        caBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        caBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        caBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(caKeyPair.getPublic()));

        ContentSigner caSigner = new JcaContentSignerBuilder(SIG_ALG).setProvider("BC").build(caKeyPair.getPrivate());
        X509CertificateHolder caHolder = caBuilder.build(caSigner);
        caCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(caHolder);

        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/crl", new CrlHandler());
        httpServer.start();
        crlUrl = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/crl";
    }

    /** Stops the CRL HTTP server. */
    void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    /** @return the self-signed root CA certificate, to be pinned as a DSS trust anchor. */
    X509Certificate getCaCertificate() {
        return caCertificate;
    }

    /** @return the loopback CRL endpoint issued certificates point their distribution point at. */
    String getCrlUrl() {
        return crlUrl;
    }

    /**
     * Issues a fresh RSA-2048 leaf signing certificate (with a CRL distribution point pointing at this CA's
     * loopback CRL endpoint) and packages the private key plus the {@code [leaf, CA]} chain into an in-memory
     * JKS keystore.
     *
     * @param alias  the key entry alias
     * @param keyPwd the password protecting the key entry (and, for the test, the store)
     * @return a JKS keystore holding the signing key and its chain
     */
    KeyStore issueSigningKeyStore(String alias, char[] keyPwd) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair leafKeyPair = kpg.generateKeyPair();

        X500Name leafName = new X500Name("CN=JSignPdf Test Signer, O=JSignPdf Test");
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);
        BigInteger serial = BigInteger.valueOf(leafSerial.getAndIncrement());

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        X509v3CertificateBuilder leafBuilder = new JcaX509v3CertificateBuilder(
                caName, serial, notBefore, notAfter, leafName, leafKeyPair.getPublic());
        leafBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        leafBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        leafBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(leafKeyPair.getPublic()));
        leafBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(caCertificate));
        leafBuilder.addExtension(Extension.cRLDistributionPoints, false, crlDistPoint());

        ContentSigner caSigner = new JcaContentSignerBuilder(SIG_ALG).setProvider("BC").build(caKeyPair.getPrivate());
        X509CertificateHolder leafHolder = leafBuilder.build(caSigner);
        X509Certificate leafCertificate =
                new JcaX509CertificateConverter().setProvider("BC").getCertificate(leafHolder);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry(alias, leafKeyPair.getPrivate(), keyPwd,
                new X509Certificate[] { leafCertificate, caCertificate });
        return ks;
    }

    /** Builds the CRL distribution point extension value pointing at this CA's loopback CRL endpoint. */
    private CRLDistPoint crlDistPoint() {
        DistributionPointName dpn = new DistributionPointName(
                new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, crlUrl)));
        return new CRLDistPoint(new DistributionPoint[] { new DistributionPoint(dpn, null, null) });
    }

    /** Generates a DER-encoded, CA-signed CRL with an empty revoked list (every issued leaf is "good"). */
    private byte[] generateCrl() throws Exception {
        Date thisUpdate = new Date(System.currentTimeMillis() - 60 * 60 * 1000L);
        Date nextUpdate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);

        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(caName, thisUpdate);
        crlBuilder.setNextUpdate(nextUpdate);
        crlBuilder.addExtension(Extension.cRLNumber, false,
                new CRLNumber(BigInteger.valueOf(crlNumber.getAndIncrement())));
        crlBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(caCertificate));

        ContentSigner caSigner = new JcaContentSignerBuilder(SIG_ALG).setProvider("BC").build(caKeyPair.getPrivate());
        X509CRLHolder crlHolder = crlBuilder.build(caSigner);
        return crlHolder.getEncoded();
    }

    /** HTTP handler that serves the CA's current CRL on GET. */
    private final class CrlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                byte[] crl = generateCrl();
                exchange.getResponseHeaders().set("Content-Type", "application/pkix-crl");
                exchange.sendResponseHeaders(200, crl.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(crl);
                }
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }
}
