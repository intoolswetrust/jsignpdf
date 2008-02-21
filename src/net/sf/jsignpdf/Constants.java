package net.sf.jsignpdf;

/**
 * Constants used in PDF signer application.
 * @author Josef Cacek
 */
public class Constants {

	/**
	 * Version of JSignPdf
	 */
	public static final String VERSION = "0.3";

	public static final String USER_HOME = System.getProperty("user.home");

	/**
	 * Name (path) of resource bundle
	 */
	public static final String RESOURCE_BUNDLE_BASE = "translations.messages";

    /**
     * Property name.
     */
    public static final String PROPERTY_USERHOME = "enc.home";

    public static final String PROPERTY_KSTYPE = "keystore.type";

    public static final String PROPERTY_ADVANCED = "view.advanced";
    public static final String PROPERTY_ALIAS = "keystore.alias";
    public static final String PROPERTY_STOREPWD = "store.passwords";

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
