package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.TestConstants.KEYSTORE_BCPKCS12;
import static net.sf.jsignpdf.TestConstants.KEYSTORE_JKS;
import static net.sf.jsignpdf.TestConstants.KEYSTORE_PKCS12;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import org.junit.Test;

import net.sf.jsignpdf.TestConstants;

/**
 * JUnit tests for {@link KeyStoreUtils} class.
 *
 * @author Josef Cacek
 */
public class KeyStoreUtilsTest {

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
}
