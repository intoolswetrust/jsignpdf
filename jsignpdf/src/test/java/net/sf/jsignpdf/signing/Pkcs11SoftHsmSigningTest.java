package net.sf.jsignpdf.signing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
 * Two SoftHSM2 tokens as two PKCS#11 profiles: key listing and signing must hit the selected one. Skipped unless
 * SoftHSM2 is installed and {@code SOFTHSM2_CONF} (set by the surefire configuration) is present. The library and the
 * {@code softhsm2-util} binary can be overridden with the {@code softhsm2.library} / {@code softhsm2.util} system
 * properties.
 */
public class Pkcs11SoftHsmSigningTest extends SigningTestBase {

    private static final String PIN = "1234";
    private static final List<String> LIBRARY_CANDIDATES = List.of("/usr/lib/softhsm/libsofthsm2.so",
            "/usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so", "/usr/lib64/pkcs11/libsofthsm2.so",
            "/usr/local/lib/softhsm/libsofthsm2.so", "/opt/homebrew/lib/softhsm/libsofthsm2.so");

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

        Path confFile = Path.of(conf);
        Path base = confFile.getParent();
        Path tokens = base.resolve("tokens");
        deleteRecursively(base);
        Files.createDirectories(tokens);
        Files.writeString(confFile, "directories.tokendir = " + tokens.toAbsolutePath() + "\nobjectstore.backend = file\n");

        long slotA = initToken(util, library, confFile, "token-a");
        long slotB = initToken(util, library, confFile, "token-b");

        Path cfgDir = base.resolve("cfg");
        Files.createDirectories(cfgDir.resolve("pkcs11"));
        Files.writeString(cfgDir.resolve("pkcs11/a.cfg"), "#jsignpdf:label=Token A\nname=a\nlibrary=" + library
                + "\nslot=" + slotA + "\n");
        Files.writeString(cfgDir.resolve("pkcs11/b.cfg"), "#jsignpdf:label=Token B\nname=b\nlibrary=" + library
                + "\nslot=" + slotB + "\n");
        profiles = new Pkcs11Profiles(cfgDir);
        Pkcs11Profiles.setInstance(profiles);

        storeKey(profiles.provider("a", "PKCS11"), "key-a", "CN=Token A");
        storeKey(profiles.provider("b", "PKCS11"), "key-b", "CN=Token B");
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
    public void signingUsesTheSelectedProfile() throws Exception {
        File inFile = new File(tempFolder.getRoot(), "input.pdf");
        Files.copy(getUnsignedPdf().toPath(), inFile.toPath());
        BasicSignerOptions options = new BasicSignerOptions();
        options.setKsType("PKCS11");
        options.setKsProvider("b");
        options.setKsPasswd(PIN.toCharArray());
        options.setInFile(inFile.getAbsolutePath());
        options.setOutFile(new File(tempFolder.getRoot(), "output.pdf").getAbsolutePath());
        ValidationResult result = signAndValidate(options);
        assertTrue(result.signatureValid);
        assertTrue(result.signerCertificateSubject, result.signerCertificateSubject.contains("Token B"));
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

    private static void storeKey(Provider provider, String alias, String dn) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS11", provider);
        ks.load(null, PIN.toCharArray());
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", provider);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        X500Name name = new X500Name(dn);
        Date now = new Date();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE,
                new Date(now.getTime() - 60_000L), new Date(now.getTime() + 86_400_000L), name, kp.getPublic());
        X509Certificate cert = new JcaX509CertificateConverter()
                .getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider(provider)
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
