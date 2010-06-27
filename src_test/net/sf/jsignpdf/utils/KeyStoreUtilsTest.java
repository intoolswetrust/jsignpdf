package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.TestConstants.*;
import static org.junit.Assert.*;

import java.util.SortedSet;

import org.junit.Test;

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
	 * Tests
	 * {@link KeyStoreUtils#getKeyAlias(net.sf.jsignpdf.BasicSignerOptions)}
	 */
	@Test
	public void testGetKeyAliases() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetKeyAlias() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCertAliasesKeyStore() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCertAliasesStringStringString() {
		fail("Not yet implemented");
	}

	@Test
	public void testLoadKeyStoreStringStringString() {
		fail("Not yet implemented");
	}

	@Test
	public void testLoadKeyStoreStringStringCharArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testLoadCacertsKeyStore() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetPkInfo() {
		fail("Not yet implemented");
	}

}
