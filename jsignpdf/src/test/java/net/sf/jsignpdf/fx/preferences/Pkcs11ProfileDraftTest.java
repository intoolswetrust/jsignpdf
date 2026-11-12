package net.sf.jsignpdf.fx.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;
import net.sf.jsignpdf.pkcs11.ProfileStatus;

public class Pkcs11ProfileDraftTest {

    @Test
    public void idFromLabel() {
        assertEquals("my-safenet-etoken", Pkcs11ProfileDraft.idFromLabel("My SafeNet eToken"));
        assertEquals("prilis-zlutoucky-kun", Pkcs11ProfileDraft.idFromLabel("Příliš žluťoučký kůň"));
        assertEquals("a-b", Pkcs11ProfileDraft.idFromLabel("  a - b  "));
        assertEquals("yubikey-5.4", Pkcs11ProfileDraft.idFromLabel("YubiKey 5.4"));
        assertEquals("token", Pkcs11ProfileDraft.idFromLabel("中文"));
        assertEquals("token", Pkcs11ProfileDraft.idFromLabel(null));
        String longId = Pkcs11ProfileDraft.idFromLabel("a very long label that will not fit into thirty two");
        assertEquals("a-very-long-label-that-will-not", longId);
        assertTrue(Pkcs11Profiles.isValidId(longId));
    }

    @Test
    public void changeIdUpdatesNameOnlyWhenItMatchedTheOldId() {
        Pkcs11ProfileDraft same = new Pkcs11ProfileDraft(null, "a", "name=a\nlibrary=/x.so\n", null);
        same.changeId("b");
        assertEquals("name=b\nlibrary=/x.so\n", same.body());

        Pkcs11ProfileDraft custom = new Pkcs11ProfileDraft(null, "a", "name=Custom\nlibrary=/x.so\n", null);
        custom.changeId("b");
        assertEquals("name=Custom\nlibrary=/x.so\n", custom.body());
    }

    @Test
    public void display() {
        assertEquals(Pkcs11ProfileDraft.Display.NEW, new Pkcs11ProfileDraft(null, "a", "", null).display());
        Pkcs11ProfileDraft loaded = new Pkcs11ProfileDraft("a", "a", "name=a\n",
                new ProfileStatus("a", ProfileStatus.State.FAILED, "boom"));
        assertEquals(Pkcs11ProfileDraft.Display.FAILED, loaded.display());
        loaded.setLabel("x");
        assertEquals(Pkcs11ProfileDraft.Display.EDITED, loaded.display());
        assertEquals(Pkcs11ProfileDraft.Display.NOT_LOADED,
                new Pkcs11ProfileDraft("b", "b", "", null).display());
    }

    @Test
    public void libraryHelpers() {
        assertEquals("opensc-pkcs11.so", Pkcs11ProfileDraft.libraryFileName("/usr/lib/opensc-pkcs11.so"));
        assertEquals("eTPKCS11.dll", Pkcs11ProfileDraft.libraryFileName("C:\\Windows\\System32\\eTPKCS11.dll"));
        assertEquals("p11.so", Pkcs11ProfileDraft.libraryFileName("p11.so"));
        assertFalse(Pkcs11ProfileDraft.isLibraryMissing("p11.so"));
        assertFalse(Pkcs11ProfileDraft.isLibraryMissing(""));
        assertTrue(Pkcs11ProfileDraft.isLibraryMissing("/nonexistent/dir/p11.so"));
    }
}
