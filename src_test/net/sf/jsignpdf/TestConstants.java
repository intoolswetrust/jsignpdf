package net.sf.jsignpdf;

/**
 * Constants specific to JUnit tests.
 * 
 * @author Josef Cacek
 */
public class TestConstants {

	public static final String KEYSTORE_JKS = "JKS";
	public static final String KEYSTORE_PKCS12 = "PKCS12";
	public static final String KEYSTORE_BCPKCS12 = "BCPKCS12";

	public static final char[] KEYSTORE_TEST_PASSWD = "keystorepass".toCharArray();
	public static final String KEYSTORE_FILE_JKS = "testdata/test-keystore.jks";
	public static final String KEYSTORE_FILE_PKCS12 = "testdata/test-keystore.p12";

	public static final String KEY_PASSWD_SUFFIX = "pass";

	public static enum Keystore {
		JKS(KEYSTORE_FILE_JKS),
		PKCS12(KEYSTORE_FILE_PKCS12),
		BCPKCS12(KEYSTORE_FILE_PKCS12);

		private final String ksFile;

		private Keystore(final String aFilePath) {
			ksFile = aFilePath;
		}

		public String getKsFile() {
			return ksFile;
		}

		public String getKsType() {
			return name();
		}

		public char[] getPasswd() {
			return KEYSTORE_TEST_PASSWD;
		}
	}

	/**
	 * Test private keys present in test keystore file.
	 * 
	 * @author Josef Cacek
	 */
	public static enum TestPrivateKey {
		EXPIRED(true),
		RSA1024(false),
		RSA2048(false),
		RSA4096(false),
		DSA1024(false);

		private final boolean expired;

		private TestPrivateKey(boolean anExpired) {
			expired = anExpired;
		}

		public boolean isExpired() {
			return expired;
		}

		public String getAlias() {
			return name().toLowerCase();
		}

		public char[] getPasswd() {
			return (name() + KEY_PASSWD_SUFFIX).toCharArray();
		}

		public BasicSignerOptions toSignerOptions(final Keystore aKeystore) {
			final BasicSignerOptions options = new BasicSignerOptions();
			options.setAdvanced(true);
			options.setKsType(aKeystore.getKsType());
			options.setKsFile(aKeystore.getKsFile());
			options.setKsPasswd(aKeystore.getPasswd());
			options.setKeyAlias(getAlias());
			options.setKeyPasswd(getPasswd());
			return options;

		}
	}

}
