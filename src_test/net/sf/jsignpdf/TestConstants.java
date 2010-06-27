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

	// keytool -genkeypair -keyalg RSA -keysize 2048 -dname
	// "cn=Expired Key, ou=PDF support, o=JSignPdf s.r.o., c=CZ" -alias expired
	// -keypass expiredpass -keystore test-keystore.jks -storepass keystorepass
	// -validity 90
	public static final BasicSignerOptions OPTIONS_EXPIREDKEY = new BasicSignerOptions();
	static {
		OPTIONS_EXPIREDKEY.setAdvanced(true);
		OPTIONS_EXPIREDKEY.setKeyAlias("expired");
		OPTIONS_EXPIREDKEY.setKeyPasswd("expiredpass".toCharArray());
		OPTIONS_EXPIREDKEY.setKsPasswd(KEYSTORE_TEST_PASSWD);
	}

	// TODO trida pro klice

	public static final BasicSignerOptions OPTIONS_VALIDKEY = new BasicSignerOptions();
	static {
		OPTIONS_VALIDKEY.setAdvanced(true);
		OPTIONS_VALIDKEY.setKeyAlias("valid");
		OPTIONS_VALIDKEY.setKeyPasswd("validpass".toCharArray());
		OPTIONS_VALIDKEY.setKsPasswd(KEYSTORE_TEST_PASSWD);
	}

}
