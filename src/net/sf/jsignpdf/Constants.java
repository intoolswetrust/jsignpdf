package net.sf.jsignpdf;

/**
 * Constants used in PDF signer application.
 * @author Josef Cacek
 */
public class Constants {

	/**
	 * Version of JSignPdf
	 */
	public static final String VERSION = "0.5";

	/**
	 * Home directory of current user. It's not a real constant
	 * it only holds value of <code>System.getProperty("user.home")</code>
	 */
	public static final String USER_HOME = System.getProperty("user.home");

	/**
	 * Filename (in USER_HOME), where filled values from JSign application
	 * are stored.
	 * @see #USER_HOME
	 */
	public static final String PROPERTIES_FILE = ".JSignPdf";

	/**
	 * Name (path) of resource bundle
	 */
	public static final String RESOURCE_BUNDLE_BASE = "translations.messages";

    /**
     * Property name.
     */
    public static final String EPROPERTY_USERHOME = "enc.home";
    public static final String EPROPERTY_KS_PWD = "enc.keystorePwd";
    public static final String EPROPERTY_KEY_PWD = "enc.keyPwd";
    public static final String EPROPERTY_OWNER_PWD = "enc.pdfOwnerPwd";
    public static final String EPROPERTY_USER_PWD = "enc.pdfUserPwd";

    public static final String PROPERTY_KSTYPE = "keystore.type";
    public static final String PROPERTY_ADVANCED = "view.advanced";
    public static final String PROPERTY_ALIAS = "keystore.alias";
    public static final String PROPERTY_STOREPWD = "store.passwords";

    public static final String PROPERTY_APPEND = "signature.append";
    public static final String PROPERTY_ENCRYPTED_PDF = "inpdf.encrypted";

    public static final String PROPERTY_CERT_LEVEL = "certification.level";

    public static final String PROPERTY_RIGHT_PRINT = "right.printing";
    public static final String PROPERTY_RIGHT_COPY = "right.copy";
    public static final String PROPERTY_RIGHT_ASSEMBLY = "right.assembly";
    public static final String PROPERTY_RIGHT_FILL_IN = "right.fillIn";
    public static final String PROPERTY_RIGHT_SCR_READ = "right.screenReaders";
    public static final String PROPERTY_RIGHT_MOD_ANNOT = "right.modify.annotations";
    public static final String PROPERTY_RIGHT_MOD_CONT = "right.modify.contents";


    /**
     * Property name.
     */
    public static final String PROPERTY_KEYSTORE = "keystore.file";
    /**
     * Property name.
     */
    public static final String PROPERTY_OUTPDF = "outpdf.file";
    /**
     * Property name.
     */
    public static final String PROPERTY_INPDF = "inpdf.file";
    /**
     * Property name.
     */
    public static final String PROPERTY_REASON = "signature.reason";
    /**
     * Property name.
     */
    public static final String PROPERTY_LOCATION = "signature.location";

}
