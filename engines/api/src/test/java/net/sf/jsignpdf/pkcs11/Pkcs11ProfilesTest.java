package net.sf.jsignpdf.pkcs11;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.utils.KeyStoreUtils;
import net.sf.jsignpdf.utils.PKCS11Utils;

public class Pkcs11ProfilesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path cfgDir;
    private TestProviders factory;
    private Path lib;
    private Pkcs11Profiles profiles;

    @Before
    public void setUp() throws Exception {
        cfgDir = tmp.newFolder("cfg").toPath();
        Files.createDirectories(cfgDir.resolve("pkcs11"));
        factory = new TestProviders();
        lib = cfgDir.resolve("lib.so");
        Files.writeString(lib, "");
    }

    @After
    public void tearDown() {
        if (profiles != null) {
            profiles.unregisterAll();
        }
        Pkcs11Profiles.setInstance(null);
    }

    private void write(String relative, String body) throws Exception {
        Path p = cfgDir.resolve(relative);
        Files.createDirectories(p.getParent());
        Files.writeString(p, body, StandardCharsets.UTF_8);
    }

    private String cfg(String name) {
        return "name=" + name + "\nlibrary=" + lib.toAbsolutePath() + "\n";
    }

    private Pkcs11Profiles load() {
        profiles = new Pkcs11Profiles(cfgDir, factory);
        profiles.setUnregisterDelayMillis(0);
        return profiles;
    }

    private static List<String> ids(List<Pkcs11Profile> list) {
        return list.stream().map(Pkcs11Profile::id).collect(Collectors.toList());
    }

    @Test
    public void discovery_legacyFirstThenSortedById() throws Exception {
        write("pkcs11/zeta.cfg", cfg("zeta"));
        write("pkcs11/alpha.cfg", cfg("alpha"));
        write("pkcs11.cfg", cfg("JSignPdf"));
        write("pkcs11/readme.txt", "not a profile");
        List<Pkcs11Profile> list = load().list();
        assertEquals(List.of("default", "alpha", "zeta"), ids(list));
        assertTrue(list.get(0).isLegacy());
    }

    @Test
    public void discovery_invalidFilesAreReportedNotListed() throws Exception {
        write("pkcs11/bad name.cfg", cfg("bad"));
        write("pkcs11/default.cfg", cfg("dflt"));
        write("pkcs11/Token.cfg", cfg("Token"));
        write("pkcs11/token.cfg", cfg("token2"));
        write("pkcs11/one.cfg", cfg("same"));
        write("pkcs11/two.cfg", cfg("same"));
        load();
        assertEquals(List.of("one", "Token"), ids(profiles.list()));
        List<ProfileStatus> invalid = profiles.statuses().stream()
                .filter(s -> s.state() == ProfileStatus.State.INVALID).collect(Collectors.toList());
        assertEquals(4, invalid.size());
        assertEquals(ProfileStatus.State.INVALID, profiles.status("two").state());
        assertTrue(profiles.status("two").message().contains("one"));
    }

    @Test
    public void discovery_readsCommentMetadata() throws Exception {
        write("pkcs11/cert.cfg", "#jsignpdf:label=Certilia (Croatian eID)\n#jsignpdf:provider=both\n"
                + "#jsignpdf:catalog=hr-certilia@3\n" + cfg("cert"));
        Pkcs11Profile p = load().find("CERT").orElseThrow();
        assertEquals("Certilia (Croatian eID)", p.displayLabel());
        assertEquals(ProviderMode.BOTH, p.mode());
        assertEquals("hr-certilia@3", p.catalogRef());
        assertEquals(List.of("PKCS11", "JSIGNPKCS11"), List.copyOf(p.offeredTypes()));
    }

    @Test
    public void discovery_loadsNoLibrary() throws Exception {
        write("pkcs11.cfg", cfg("JSignPdf"));
        write("pkcs11/a.cfg", cfg("a"));
        load();
        profiles.list();
        profiles.statuses();
        profiles.offeredTypes();
        assertEquals(0, factory.created.get());
        assertEquals(ProfileStatus.State.NOT_LOADED, profiles.status("a").state());
    }

    @Test
    public void resolve_rules() throws Exception {
        write("pkcs11/a.cfg", cfg("a"));
        load();
        assertNull(profiles.resolve(null, "PKCS12"));
        assertEquals("a", profiles.resolve(null, "PKCS11").id());
        assertEquals("a", profiles.resolve("A", "pkcs11").id());

        write("pkcs11/b.cfg", cfg("b"));
        profiles.reload();
        expectFailure(() -> profiles.resolve(null, "PKCS11"), "a, b");
        expectFailure(() -> profiles.resolve("missing", "PKCS11"), "missing");
        expectFailure(() -> profiles.resolve("a", "JSIGNPKCS11"), "JSIGNPKCS11");
    }

    @Test
    public void resolve_namedInvalidProfileReportsReason() throws Exception {
        write("pkcs11/one.cfg", cfg("same"));
        write("pkcs11/two.cfg", cfg("same"));
        load();
        expectFailure(() -> profiles.resolve("two", "PKCS11"), "one");
    }

    @Test
    public void legacyWithoutProviderMetadataAcceptsJsignType() throws Exception {
        write("pkcs11.cfg", cfg("JSignPdf"));
        load();
        Pkcs11Profile legacy = profiles.find("default").orElseThrow();
        assertEquals(List.of("PKCS11"), List.copyOf(legacy.offeredTypes()));
        assertTrue(legacy.accepts("JSIGNPKCS11"));
        assertEquals("default", profiles.resolve(null, "JSIGNPKCS11").id());

        write("pkcs11.cfg", "#jsignpdf:provider=sun\n" + cfg("JSignPdf"));
        profiles.reload();
        assertFalse(profiles.find("default").orElseThrow().accepts("JSIGNPKCS11"));
    }

    @Test
    public void provider_registersOnlyTheSelectedProfile() throws Exception {
        write("pkcs11/a.cfg", cfg("a"));
        write("pkcs11/b.cfg", cfg("b"));
        load();
        Provider pb = profiles.provider("b", "PKCS11");
        assertEquals("SunPKCS11-b", pb.getName());
        assertSame(pb, Security.getProvider("SunPKCS11-b"));
        assertEquals(1, factory.created.get());
        assertEquals(ProfileStatus.State.OK, profiles.status("b").state());
        assertEquals(ProfileStatus.State.NOT_LOADED, profiles.status("a").state());
        assertSame(pb, profiles.provider("b", "PKCS11"));
        assertEquals(1, factory.created.get());
    }

    @Test
    public void keyStoreLoading_usesTheNamedProviderWhenTwoOfferTheSameType() throws Exception {
        write("pkcs11/a.cfg", cfg("a"));
        write("pkcs11/b.cfg", cfg("b"));
        Pkcs11Profiles.setInstance(load());
        profiles.provider("a", "PKCS11");

        BasicSignerOptions options = new BasicSignerOptions();
        options.setKsType("PKCS11");
        options.setKsProvider("b");
        options.setKsPasswd("1234".toCharArray());
        KeyStore ks = KeyStoreUtils.loadKeyStore(options);
        assertEquals("SunPKCS11-b", ks.getProvider().getName());
        assertEquals(List.of("key-b"), Collections.list(ks.aliases()));
        assertEquals("SunPKCS11-b", PKCS11Utils.getProviderName(options));

        options.setKsProvider("a");
        assertEquals(List.of("key-a"), Collections.list(KeyStoreUtils.loadKeyStore(options).aliases()));
    }

    @Test
    public void keyStoreLoading_reportsTokenErrors() throws Exception {
        write("pkcs11/a.cfg", cfg("a"));
        Pkcs11Profiles.setInstance(load());
        BasicSignerOptions options = new BasicSignerOptions();
        options.setKsType("PKCS11");
        options.setKsPasswd("wrong".toCharArray());
        try {
            KeyStoreUtils.loadKeyStore(options);
            fail("Expected Pkcs11Exception");
        } catch (Pkcs11Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("\"a\""));
            assertTrue(e.getMessage(), e.getMessage().contains("incorrect PIN"));
        }
    }

    @Test
    public void registrationFailure_isCapturedWithReason() throws Exception {
        write("pkcs11/arch.cfg", cfg("arch"));
        write("pkcs11/nolib.cfg", "name=nolib\nlibrary=" + cfgDir.resolve("missing.so").toAbsolutePath() + "\n");
        profiles = new Pkcs11Profiles(cfgDir, (b, f) -> {
            throw new java.security.ProviderException("Initialization failed",
                    new java.io.IOException("/x/lib.so: wrong ELF class: ELFCLASS32"));
        });
        profiles.setUnregisterDelayMillis(0);
        expectFailure(() -> profiles.provider("arch", "PKCS11"), System.getProperty("os.arch"));
        ProfileStatus st = profiles.status("arch");
        assertEquals(ProfileStatus.State.FAILED, st.state());
        assertTrue(st.message(), st.message().contains("architecture"));

        expectFailure(() -> profiles.provider("nolib", "PKCS11"), "missing.so");
        assertEquals(ProfileStatus.State.FAILED, profiles.register("nolib").state());

        profiles.clearFailures();
        assertEquals(ProfileStatus.State.NOT_LOADED, profiles.status("arch").state());
    }

    @Test
    public void registration_rejectsAnExistingProviderName() throws Exception {
        write("pkcs11/dup.cfg", cfg("dup"));
        Provider existing = new TestProviders.FakeProvider("SunPKCS11-dup", "PKCS11", "x");
        Security.addProvider(existing);
        try {
            load();
            assertEquals(ProfileStatus.State.FAILED, profiles.register("dup").state());
            assertTrue(profiles.status("dup").message().contains("SunPKCS11-dup"));
        } finally {
            Security.removeProvider(existing.getName());
        }
    }

    @Test
    public void reload_unregistersChangedAndRemovedProfiles() throws Exception {
        write("pkcs11/a.cfg", cfg("a"));
        write("pkcs11/b.cfg", cfg("b"));
        write("pkcs11/c.cfg", cfg("c"));
        load();
        profiles.register("a");
        profiles.register("b");
        profiles.register("c");
        write("pkcs11/a.cfg", cfg("a") + "slotListIndex=1\n");
        Files.delete(cfgDir.resolve("pkcs11/b.cfg"));
        profiles.reload();
        assertNull(Security.getProvider("SunPKCS11-a"));
        assertNull(Security.getProvider("SunPKCS11-b"));
        assertNotNull(Security.getProvider("SunPKCS11-c"));
        profiles.unregisterAll();
        assertNull(Security.getProvider("SunPKCS11-c"));
    }

    @Test
    public void register_bothModeRegistersTwoProviders() throws Exception {
        write("pkcs11/two.cfg", "#jsignpdf:provider=both\n" + cfg("two"));
        load();
        assertEquals(ProfileStatus.State.OK, profiles.register("two").state());
        assertNotNull(Security.getProvider("SunPKCS11-two"));
        assertNotNull(Security.getProvider("JSignPKCS11-two"));
        assertEquals("JSignPKCS11-two", profiles.provider("two", "JSIGNPKCS11").getName());
    }

    @Test
    public void applyEdits_addRenameRemove() throws Exception {
        write("pkcs11.cfg", cfg("JSignPdf"));
        write("pkcs11/old.cfg", cfg("old"));
        write("pkcs11/gone.cfg", cfg("gone"));
        load();
        List<Pkcs11Profiles.ProfileEdit> edits = List.of(
                new Pkcs11Profiles.ProfileEdit("default", "default", ""),
                new Pkcs11Profiles.ProfileEdit("old", "renamed", cfg("old")),
                new Pkcs11Profiles.ProfileEdit(null, "fresh", "library=/x.so\n"));
        assertNull(profiles.validateEdits(edits));
        profiles.applyEdits(edits);

        assertFalse(Files.exists(cfgDir.resolve("pkcs11.cfg")));
        assertFalse(Files.exists(cfgDir.resolve("pkcs11/old.cfg")));
        assertFalse(Files.exists(cfgDir.resolve("pkcs11/gone.cfg")));
        assertEquals("name=renamed\nlibrary=" + lib.toAbsolutePath() + "\n",
                Files.readString(cfgDir.resolve("pkcs11/renamed.cfg")));
        assertEquals("name=fresh\nlibrary=/x.so\n", Files.readString(cfgDir.resolve("pkcs11/fresh.cfg")));
        assertEquals(List.of("fresh", "renamed"), ids(profiles.list()));
        assertEquals("renamed", profiles.renamedTo("OLD"));
    }

    @Test
    public void validateEdits_rejectsBadIds() throws Exception {
        write("pkcs11/taken.cfg", cfg("taken"));
        write("pkcs11/bad name.cfg", cfg("bad"));
        load();
        assertNotNull(profiles.validateEdits(List.of(new Pkcs11Profiles.ProfileEdit(null, "bad id", ""))));
        assertNotNull(profiles.validateEdits(List.of(new Pkcs11Profiles.ProfileEdit(null, "default", ""))));
        assertNotNull(profiles.validateEdits(List.of(new Pkcs11Profiles.ProfileEdit(null, "x", ""),
                new Pkcs11Profiles.ProfileEdit(null, "X", ""))));
        assertNotNull(profiles.validateEdits(List.of(new Pkcs11Profiles.ProfileEdit(null, "taken", ""))));
        assertNull(profiles.validateEdits(List.of(new Pkcs11Profiles.ProfileEdit("taken", "taken", ""))));
    }

    private static void expectFailure(Runnable r, String messagePart) {
        try {
            r.run();
            fail("Expected Pkcs11Exception");
        } catch (Pkcs11Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains(messagePart));
        }
    }
}
