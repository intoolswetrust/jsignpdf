package net.sf.jsignpdf.pkcs11;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class Pkcs11CatalogTest {

    private static String doc(String... entries) {
        return "{\"schemaVersion\":1,\"updated\":\"2026-09-14\",\"entries\":[" + String.join(",", entries) + "]}";
    }

    private static String entry(String id, String extra) {
        return "{\"id\":\"" + id + "\",\"revision\":2,\"label\":\"Token " + id + "\","
                + "\"libraries\":[{\"os\":\"linux\",\"arch\":[\"x86_64\"],\"paths\":[\"/usr/lib/" + id + ".so\"]}]"
                + (extra.isEmpty() ? "" : "," + extra) + "}";
    }

    private static List<String> ids(Pkcs11Catalog c) {
        return c.entries().stream().map(Pkcs11Catalog.Entry::id).collect(Collectors.toList());
    }

    @Test
    public void bundledCatalog_parsesWithoutRejections() {
        Pkcs11Catalog c = Pkcs11Catalog.loadBundled();
        assertNotNull(c.updated());
        assertEquals(List.of("opensc", "p11-kit-proxy", "yubikey-piv", "be-eid", "safenet-etoken"), ids(c));
    }

    @Test
    public void allowlist_rejectsUnknownKeysAndBadValues() {
        Pkcs11Catalog c = Pkcs11Catalog.parse(doc(
                entry("ok", "\"config\":{\"slotListIndex\":1,\"description\":\"Card reader 1\"}"),
                entry("attrs", "\"config\":{\"attributes\":\"compatibility\"}"),
                entry("lib", "\"config\":{\"library\":\"/tmp/evil.so\"}"),
                entry("newline", "\"config\":{\"description\":\"x\\nlibrary=/tmp/evil.so\"}"),
                entry("equals", "\"config\":{\"description\":\"a=b\"}"),
                entry("braces", "\"config\":{\"description\":\"${user.home}\"}"),
                entry("string", "\"config\":{\"slotListIndex\":\"0\\nlibrary=/tmp/evil.so\"}"),
                entry("negative", "\"config\":{\"slot\":-1}"),
                entry("both", "\"config\":{\"slot\":1,\"slotListIndex\":0}"),
                entry("provider", "\"provider\":\"other\""),
                entry("url", "\"infoUrl\":\"http://example.com\""),
                entry("bad id", "")));
        assertEquals(List.of("ok"), ids(c));
    }

    @Test
    public void minSchemaVersion_skipsNewerEntries() {
        Pkcs11Catalog c = Pkcs11Catalog.parse(doc(entry("now", "\"minSchemaVersion\":1"),
                entry("later", "\"minSchemaVersion\":2")));
        assertEquals(List.of("now"), ids(c));
    }

    @Test
    public void rejectsPathWithQuoteOrNewline() {
        String bad = "{\"id\":\"q\",\"revision\":1,\"label\":\"Q\",\"libraries\":[{\"os\":\"linux\","
                + "\"arch\":[\"x86_64\"],\"paths\":[\"/usr/lib/a\\\"b.so\"]}]}";
        assertTrue(Pkcs11Catalog.parse(doc(bad)).entries().isEmpty());
    }

    @Test
    public void toProfileBody_producesAParsableProfile() {
        Pkcs11Catalog c = Pkcs11Catalog.parse(doc(entry("vendor", "\"config\":{\"slotListIndex\":1,"
                + "\"description\":\"Card reader 1\"},"
                + "\"provider\":\"both\",\"tested\":[{\"os\":\"linux\",\"osVersion\":\"Fedora 42\",\"date\":\"2026-04-11\"},"
                + "{\"os\":\"windows\",\"date\":\"2025-01-01\"}]")));
        Pkcs11Catalog.Entry e = c.entries().get(0);
        String body = Pkcs11Catalog.toProfileBody(e, "vendor-2", "C:\\Program Files (x86)\\V\\p11.dll");
        Pkcs11Profile p = new Pkcs11Profile("vendor-2", Path.of("vendor-2.cfg"), false, body);
        assertEquals("Token vendor", p.label());
        assertEquals(ProviderMode.BOTH, p.mode());
        assertEquals("vendor@2", p.catalogRef());
        assertEquals("vendor-2", p.cfgName());
        assertEquals("C:/Program Files (x86)/V/p11.dll", p.library());
        assertTrue(body.contains("library=\"C:/Program Files (x86)/V/p11.dll\"\n"));
        assertTrue(body.contains("slotListIndex=1\n"));
        assertTrue(body.contains("description=\"Card reader 1\"\n"));
        assertFalse(body.contains("\\"));
        assertEquals("Fedora 42", Pkcs11Catalog.latestAttestation(e).osVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedDocumentThrows() {
        Pkcs11Catalog.parse("{\"schemaVersion\":1,\"entries\":[}");
    }
}
