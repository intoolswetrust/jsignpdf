package net.sf.jsignpdf.pkcs11;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Pkcs11DetectorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void normalizesOsAndArch() {
        assertEquals("x86_64", Pkcs11Detector.normalizeArch("amd64"));
        assertEquals("x86_64", Pkcs11Detector.normalizeArch("x86_64"));
        assertEquals("aarch64", Pkcs11Detector.normalizeArch("arm64"));
        assertEquals("windows", Pkcs11Detector.normalizeOs("Windows 11"));
        assertEquals("macos", Pkcs11Detector.normalizeOs("Mac OS X"));
        assertEquals("linux", Pkcs11Detector.normalizeOs("Linux"));
    }

    @Test
    public void expandsVariablesAndDropsUndefinedOnes() {
        Map<String, String> env = Map.of("ProgramFiles(x86)", "C:\\PF86", "HOME", "/home/u");
        Pkcs11Detector d = new Pkcs11Detector("windows", "x86_64", env::get, "/home/u", false);
        assertEquals("C:\\PF86\\V\\p.dll", d.expand("%ProgramFiles(x86)%\\V\\p.dll"));
        assertEquals("/home/u/lib/p.so", d.expand("$HOME/lib/p.so"));
        assertEquals("/home/u/lib/p.so", d.expand("${HOME}/lib/p.so"));
        assertEquals("/home/u/.local/p.so", d.expand("~/.local/p.so"));
        assertNull(d.expand("%MISSING%\\p.dll"));
    }

    @Test
    public void findsGlobMatchesNewestFirstAndFiltersByPlatform() throws Exception {
        Path root = tmp.newFolder().toPath();
        Path older = root.resolve("usr/lib/v1/libtoken.so");
        Path newer = root.resolve("usr/lib/v2/libtoken.so");
        Files.createDirectories(older.getParent());
        Files.createDirectories(newer.getParent());
        Files.writeString(older, "");
        Files.writeString(newer, "");
        Files.setLastModifiedTime(older, FileTime.fromMillis(1_000_000L));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000_000L));
        String rootPath = root.toAbsolutePath().toString().replace('\\', '/');
        String json = "{\"schemaVersion\":1,\"entries\":["
                + "{\"id\":\"tok\",\"revision\":1,\"label\":\"Tok\",\"libraries\":["
                + "{\"os\":\"linux\",\"arch\":[\"x86_64\"],\"paths\":[\"$ROOT/usr/lib/*/libtoken.so\",\"$ROOT/none.so\"]}]},"
                + "{\"id\":\"arm\",\"revision\":1,\"label\":\"Arm\",\"libraries\":["
                + "{\"os\":\"linux\",\"arch\":[\"aarch64\"],\"paths\":[\"$ROOT/usr/lib/*/libtoken.so\"]}]},"
                + "{\"id\":\"p11-kit-proxy\",\"revision\":1,\"label\":\"Proxy\",\"libraries\":["
                + "{\"os\":\"linux\",\"arch\":[\"x86_64\"],\"paths\":[\"$ROOT/usr/lib/v1/libtoken.so\"]}]}]}";
        Pkcs11Catalog catalog = Pkcs11Catalog.parse(json);
        Map<String, String> env = Map.of("ROOT", rootPath);

        Pkcs11Detector d = new Pkcs11Detector("linux", "x86_64", env::get, null, false);
        List<Pkcs11Detector.Candidate> found = d.detect(catalog);
        assertEquals(List.of("tok", "tok", "p11-kit-proxy"),
                found.stream().map(c -> c.entry().id()).collect(Collectors.toList()));
        assertEquals(newer, found.get(0).library());
        assertEquals(older, found.get(1).library());

        Pkcs11Detector flatpak = new Pkcs11Detector("linux", "x86_64", env::get, null, true);
        assertTrue(flatpak.detect(catalog).isEmpty());
        assertTrue(flatpak.applicableEntries(catalog).isEmpty());
    }
}
