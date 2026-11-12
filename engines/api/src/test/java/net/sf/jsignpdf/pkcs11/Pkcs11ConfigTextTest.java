package net.sf.jsignpdf.pkcs11;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

public class Pkcs11ConfigTextTest {

    @Test
    public void readsValuesAndMetadata() {
        String body = "#jsignpdf:label=My card\n# comment name=x\nname = card\nlibrary=\"/opt/a b/p.so\"\nslotListIndex=0\n";
        assertEquals(Map.of("label", "My card"), Pkcs11ConfigText.metadata(body));
        assertEquals("card", Pkcs11ConfigText.value(body, "name"));
        assertEquals("/opt/a b/p.so", Pkcs11ConfigText.value(body, "library"));
        assertEquals("0", Pkcs11ConfigText.value(body, "slotListIndex"));
        assertNull(Pkcs11ConfigText.value(body, "slot"));
    }

    @Test
    public void editsKeepOtherLines() {
        String body = "# keep\nname=old\nattributes(*,*,*) = {\n  CKA_TOKEN = true\n}\n";
        String named = Pkcs11ConfigText.withValue(body, "name", "new");
        assertEquals("# keep\nname=new\nattributes(*,*,*) = {\n  CKA_TOKEN = true\n}\n", named);
        String labelled = Pkcs11ConfigText.withMetadata(named, "label", "L");
        assertEquals("#jsignpdf:label=L\n" + named, labelled);
        assertEquals(named, Pkcs11ConfigText.withMetadata(labelled, "label", ""));
        assertEquals("#jsignpdf:label=L\nname=x\n", Pkcs11ConfigText.withValue("#jsignpdf:label=L\n", "name", "x"));
        assertEquals("name=x\r\nlibrary=/l.so\r\n", Pkcs11ConfigText.withValue("name=x\r\n", "library", "/l.so"));
    }

    @Test
    public void formatsLibraryValues() {
        assertEquals("/usr/lib/opensc-pkcs11.so", Pkcs11ConfigText.formatLibraryValue("/usr/lib/opensc-pkcs11.so"));
        assertEquals("\"C:/Program Files/OpenSC Project/p.dll\"",
                Pkcs11ConfigText.formatLibraryValue("C:\\Program Files\\OpenSC Project\\p.dll"));
        assertEquals("C:/Windows/System32/eTPKCS11.dll",
                Pkcs11ConfigText.formatLibraryValue("C:\\Windows\\System32\\eTPKCS11.dll"));
    }
}
