package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.TestConstants.KEYSTORE_BCPKCS12;
import static net.sf.jsignpdf.TestConstants.KEYSTORE_JKS;
import static net.sf.jsignpdf.TestConstants.KEYSTORE_PKCS12;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.TestConstants;

/**
 * JUnit tests for {@link KeyStoreUtils} class.
 *
 * @author Josef Cacek
 */
public class KeyStoreUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Tests {@link KeyStoreUtils#getKeyStores()}.
     */
    @Test
    public void testGetKeyStores() {
        final SortedSet<String> keyStores = KeyStoreUtils.getKeyStores();
        assertNotNull(keyStores);
        // basic types
        assertTrue(keyStores.contains(KEYSTORE_JKS));
        assertTrue(keyStores.contains(KEYSTORE_PKCS12));
        // bouncy castle keystore(s)
        assertTrue(keyStores.contains(KEYSTORE_BCPKCS12));
    }

    /**
     * Tests {@link KeyStoreUtils#getKeyAlias(net.sf.jsignpdf.BasicSignerOptions)}
     */
    @Test
    public void testGetKeyAliases() {
        for (TestConstants.Keystore keystore : TestConstants.Keystore.values()) {
            for (TestConstants.TestPrivateKey privateKey : TestConstants.TestPrivateKey.values()) {
                System.out.println("Testing " + keystore + ":" + privateKey);
                String[] keyAliases = KeyStoreUtils.getKeyAliases(privateKey.toSignerOptions(keystore));
                assertNotNull(keyAliases);
                assertTrue(keyAliases.length > 0);
                List<String> keyList = Arrays.asList(keyAliases);
                assertTrue(keyList.contains(privateKey.getAlias()) != privateKey.isExpired());
            }
        }
    }

    /**
     * Tests that {@link KeyStoreUtils#getPkInfo} returns null (instead of throwing NPE)
     * when the keystore is empty and no alias can be resolved.
     */
    @Test
    public void testGetPkInfoReturnsNullForEmptyKeystore() throws Exception {
        // Create an empty PKCS12 keystore
        File emptyKs = tempFolder.newFile("empty.p12");
        char[] password = "testpass".toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, password);
        try (FileOutputStream fos = new FileOutputStream(emptyKs)) {
            ks.store(fos, password);
        }

        BasicSignerOptions opts = new BasicSignerOptions();
        opts.setKsType(KEYSTORE_PKCS12);
        opts.setKsFile(emptyKs.getAbsolutePath());
        opts.setKsPasswd(password);
        // No alias set, empty keystore -> getKeyAliasInternal returns null
        assertNull(KeyStoreUtils.getPkInfo(opts));
    }
}
