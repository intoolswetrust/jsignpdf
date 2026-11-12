package net.sf.jsignpdf.fx.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProviderException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;

/**
 * Tests the keystore-type entries built from the PKCS#11 profiles ({@link CertificateSettingsController#buildChoices}).
 */
public class KeystoreChoicesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path cfgDir;

    @Before
    public void setUp() throws Exception {
        cfgDir = tmp.newFolder("cfg").toPath();
        Files.createDirectories(cfgDir.resolve("pkcs11"));
    }

    @After
    public void tearDown() {
        Pkcs11Profiles.setInstance(null);
    }

    private void write(String relative, String body) throws Exception {
        Files.writeString(cfgDir.resolve(relative), body);
    }

    private Pkcs11Profiles load() {
        Pkcs11Profiles profiles = new Pkcs11Profiles(cfgDir, (backend, file) -> {
            throw new ProviderException("driver unavailable");
        });
        Pkcs11Profiles.setInstance(profiles);
        return profiles;
    }

    private static List<String> pkcs11(List<KeystoreChoice> choices) {
        return choices.stream().filter(c -> c.profileId() != null).map(KeystoreChoice::display)
                .collect(Collectors.toList());
    }

    @Test
    public void oneEntryPerProfileAndType() throws Exception {
        write("pkcs11/a.cfg", "#jsignpdf:label=Card A\nname=a\n");
        write("pkcs11/b.cfg", "#jsignpdf:provider=both\nname=b\n");
        load();
        List<KeystoreChoice> choices = CertificateSettingsController.buildChoices(null, null);
        assertEquals(List.of("JSIGNPKCS11 — b", "PKCS11 — Card A", "PKCS11 — b"), pkcs11(choices));
        assertTrue(choices.stream().anyMatch(c -> c.profileId() == null && "PKCS12".equals(c.type())));
    }

    @Test
    public void failedProfileStaysSelectableWithTheReason() throws Exception {
        write("pkcs11/a.cfg", "name=a\n");
        Pkcs11Profiles profiles = load();
        profiles.register("a");
        KeystoreChoice a = CertificateSettingsController.buildChoices("PKCS11", "a").stream()
                .filter(c -> "a".equals(c.profileId())).findFirst().orElseThrow();
        assertTrue(a.isEnabled());
        assertTrue(a.note(), a.note().contains("driver unavailable"));
    }

    @Test
    public void invalidProfileIsListedDisabledEvenWithoutAValidOne() throws Exception {
        write("pkcs11/bad name.cfg", "name=x\n");
        load();
        List<KeystoreChoice> choices = CertificateSettingsController.buildChoices(null, null);
        KeystoreChoice bad = choices.stream().filter(c -> c.profileId() != null).findFirst().orElseThrow();
        assertFalse(bad.isEnabled());
        assertEquals("PKCS11", bad.type());
    }

    @Test
    public void legacyJsignSelectionIsListedOnlyWhenCurrent() throws Exception {
        write("pkcs11.cfg", "name=JSignPdf\n");
        load();
        assertEquals(List.of("PKCS11 — default"), pkcs11(CertificateSettingsController.buildChoices(null, null)));
        assertEquals(List.of("JSIGNPKCS11 — default", "PKCS11 — default"),
                pkcs11(CertificateSettingsController.buildChoices("JSIGNPKCS11", "default")));
    }

    @Test
    public void tsaTypesOfferPkcs11OnlyForASingleProfile() throws Exception {
        write("pkcs11/a.cfg", "name=a\n");
        Pkcs11Profiles profiles = load();
        assertTrue(TsaSettingsController.tsaKeyStoreTypes().contains("PKCS11"));
        write("pkcs11/b.cfg", "name=b\n");
        profiles.reload();
        assertFalse(TsaSettingsController.tsaKeyStoreTypes().contains("PKCS11"));
        assertTrue(TsaSettingsController.tsaKeyStoreTypes().contains("PKCS12"));
    }
}
