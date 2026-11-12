package net.sf.jsignpdf.signing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;
import net.sf.jsignpdf.utils.KeyStoreUtils;

/**
 * Two SoftHSM2 tokens as two PKCS#11 profiles: key listing and signing must hit the selected one. A third token, on a
 * profile with both providers, signs through {@code JSIGNPKCS11}, which needs the launchers' {@code --add-exports}
 * (mirrored in the surefire {@code argLine}). Skipped unless
 * SoftHSM2 is installed and {@code SOFTHSM2_CONF} (set by the surefire configuration) is present. The library and the
 * {@code softhsm2-util} binary can be overridden with the {@code softhsm2.library} / {@code softhsm2.util} system
 * properties.
 */
public class Pkcs11SoftHsmSigningTest extends SigningTestBase {

    private static final String PIN = "1234";
    private static final List<String> LIBRARY_CANDIDATES = List.of("/usr/lib/softhsm/libsofthsm2.so",
            "/usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so", "/usr/lib64/pkcs11/libsofthsm2.so",
            "/usr/local/lib/softhsm/libsofthsm2.so", "/opt/homebrew/lib/softhsm/libsofthsm2.so");

    /**
     * Hides the hash-and-sign mechanisms, as many smart cards lack them, so the provider hashes in Java and signs raw.
     */
    private static final String SMART_CARD_MECHANISMS = "disabledMechanisms = {\n"
            + "  CKM_SHA1_RSA_PKCS CKM_SHA224_RSA_PKCS CKM_SHA256_RSA_PKCS CKM_SHA384_RSA_PKCS CKM_SHA512_RSA_PKCS\n"
            + "  CKM_ECDSA_SHA1 CKM_ECDSA_SHA224 CKM_ECDSA_SHA256 CKM_ECDSA_SHA384 CKM_ECDSA_SHA512\n"
            + "}\n";

    private static Pkcs11Profiles profiles;

    @BeforeClass
    public static void setUpTokens() throws Exception {
        String conf = System.getenv("SOFTHSM2_CONF");
        String library = System.getProperty("softhsm2.library");
        if (library == null) {
            library = LIBRARY_CANDIDATES.stream().filter(p -> Files.isRegularFile(Path.of(p))).findFirst().orElse(null);
        }
        String util = System.getProperty("softhsm2.util", "softhsm2-util");
        assumeTrue("SOFTHSM2_CONF not set", conf != null);
        assumeTrue("SoftHSM2 library not found", library != null && Files.isRegularFile(Path.of(library)));

        Path confFile = Path.of(conf).toAbsolutePath().normalize();
        Path base = confFile.getParent();
        assumeTrue("SOFTHSM2_CONF is not the build's target/softhsm/softhsm2.conf, not touching it: " + confFile,
                isBuildScratchConf(confFile));
        Path tokens = base.resolve("tokens");
        deleteRecursively(tokens);
        deleteRecursively(base.resolve("cfg"));
        Files.createDirectories(tokens);
        Files.writeString(confFile, "directories.tokendir = " + tokens.toAbsolutePath() + "\nobjectstore.backend = file\n");

        long slotA = initToken(util, library, confFile, "token-a");
        long slotB = initToken(util, library, confFile, "token-b");
        long slotC = initToken(util, library, confFile, "token-c");

        Path cfgDir = base.resolve("cfg");
        Files.createDirectories(cfgDir.resolve("pkcs11"));
        Files.writeString(cfgDir.resolve("pkcs11/a.cfg"), "#jsignpdf:label=Token A\nname=a\nlibrary=" + library
                + "\nslot=" + slotA + "\n");
        Files.writeString(cfgDir.resolve("pkcs11/b.cfg"), "#jsignpdf:label=Token B\nname=b\nlibrary=" + library
                + "\nslot=" + slotB + "\n");
        Files.writeString(cfgDir.resolve("pkcs11/c.cfg"), "#jsignpdf:label=Token C\n#jsignpdf:provider=both\nname=c\n"
                + "library=" + library + "\nslot=" + slotC + "\n" + SMART_CARD_MECHANISMS);
        profiles = new Pkcs11Profiles(cfgDir);
        Pkcs11Profiles.setInstance(profiles);

        storeKey(profiles.provider("a", "PKCS11"), "key-a", "CN=Token A", "RSA");
        storeKey(profiles.provider("b", "PKCS11"), "key-b", "CN=Token B", "RSA");
        storeKey(profiles.provider("c", "PKCS11"), "key-rsa", "CN=Token C RSA", "RSA");
        storeKey(profiles.provider("c", "PKCS11"), "key-ec", "CN=Token C EC", "EC");
    }

    @AfterClass
    public static void tearDownTokens() {
        if (profiles != null) {
            profiles.unregisterAll();
        }
        Pkcs11Profiles.setInstance(null);
    }

    @Test
    public void keyListingHitsTheSelectedProfile() {
        BasicSignerOptions options = new BasicSignerOptions();
        options.setKsType("PKCS11");
        options.setKsPasswd(PIN.toCharArray());
        options.setKsProvider("a");
        assertArrayEquals(new String[] { "key-a" }, KeyStoreUtils.getKeyAliases(options));
        options.setKsProvider("b");
        assertArrayEquals(new String[] { "key-b" }, KeyStoreUtils.getKeyAliases(options));
    }

    @Test
    public void openPdfSigningUsesTheSelectedProfile() throws Exception {
        assertSignedWithTokenB("openpdf");
    }

    @Test
    public void dssSigningUsesTheSelectedProfile() throws Exception {
        assertSignedWithTokenB("dss");
    }

    @Test
    public void jsignProviderSignsWithRsaKeyInOpenPdf() throws Exception {
        assertSignedWithJsignProvider("openpdf", "key-rsa", "Token C RSA");
    }

    @Test
    public void jsignProviderSignsWithEcKeyInOpenPdf() throws Exception {
        assertSignedWithJsignProvider("openpdf", "key-ec", "Token C EC");
    }

    @Test
    public void jsignProviderSignsWithRsaKeyInDss() throws Exception {
        assertSignedWithJsignProvider("dss", "key-rsa", "Token C RSA");
    }

    @Test
    public void jsignProviderSignsWithEcKeyInDss() throws Exception {
        assertSignedWithJsignProvider("dss", "key-ec", "Token C EC");
    }

    @Test
    public void scratchConfGuardRejectsForeignPaths() {
        assertTrue(isBuildScratchConf(Path.of("/work/jsignpdf/target/softhsm/softhsm2.conf")));
        assertFalse(isBuildScratchConf(Path.of("/home/dev/softhsm2.conf")));
        assertFalse(isBuildScratchConf(Path.of("/etc/softhsm/softhsm2.conf")));
        assertFalse(isBuildScratchConf(Path.of("softhsm2.conf")));
    }

    private void assertSignedWithTokenB(String engine) throws Exception {
        assertSigned(engine, "PKCS11", "b", null, "Token B");
    }

    private void assertSignedWithJsignProvider(String engine, String alias, String subject) throws Exception {
        assertSigned(engine, "JSIGNPKCS11", "c", alias, subject);
    }

    private void assertSigned(String engine, String ksType, String profile, String alias, String subject)
            throws Exception {
        File inFile = new File(tempFolder.getRoot(), "input.pdf");
        Files.copy(getUnsignedPdf().toPath(), inFile.toPath());
        BasicSignerOptions options = new BasicSignerOptions();
        options.setKsType(ksType);
        options.setKsProvider(profile);
        if (alias != null) {
            options.setAdvanced(true);
            options.setKeyAlias(alias);
        }
        options.setEngine(engine);
        options.setKsPasswd(PIN.toCharArray());
        options.setInFile(inFile.getAbsolutePath());
        options.setOutFile(new File(tempFolder.getRoot(), "output.pdf").getAbsolutePath());
        ValidationResult result = signAndValidate(options);
        assertTrue(result.signatureValid);
        assertTrue(result.signerCertificateSubject, result.signerCertificateSubject.contains(subject));
    }

    /** Only a conf file the surefire configuration points into {@code target/softhsm/} is written or cleaned. */
    static boolean isBuildScratchConf(Path confFile) {
        Path dir = confFile.getParent();
        return dir != null && dir.getParent() != null && "softhsm2.conf".equals(confFile.getFileName().toString())
                && "softhsm".equals(dir.getFileName().toString())
                && "target".equals(dir.getParent().getFileName().toString());
    }

    private static long initToken(String util, String library, Path conf, String label) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(util, "--init-token", "--free", "--label", label, "--pin", PIN,
                "--so-pin", "5678", "--module", library);
        pb.environment().put("SOFTHSM2_CONF", conf.toString());
        pb.redirectErrorStream(true);
        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            assumeTrue("softhsm2-util not available: " + e.getMessage(), false);
            return -1;
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(out, 0, p.waitFor());
        Matcher m = Pattern.compile("reassigned to slot (\\d+)").matcher(out);
        assertTrue(out, m.find());
        return Long.parseLong(m.group(1));
    }

    private static void storeKey(Provider provider, String alias, String dn, String algorithm) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS11", provider);
        ks.load(null, PIN.toCharArray());
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm, provider);
        if ("EC".equals(algorithm)) {
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
        } else {
            kpg.initialize(2048);
        }
        KeyPair kp = kpg.generateKeyPair();
        X500Name name = new X500Name(dn);
        Date now = new Date();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE,
                new Date(now.getTime() - 60_000L), new Date(now.getTime() + 86_400_000L), name, kp.getPublic());
        X509Certificate cert = new JcaX509CertificateConverter()
                .getCertificate(builder.build(new JcaContentSignerBuilder("EC".equals(algorithm) ? "SHA256withECDSA"
                        : "SHA256withRSA").setProvider(provider)
                        .build(kp.getPrivate())));
        ks.setKeyEntry(alias, kp.getPrivate(), null, new Certificate[] { cert });
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
        }
    }
}
