package net.sf.jsignpdf;

/**
 * Constants used in PDF signer application.
 * @author Josef Cacek
 */
public class Constants {

	/**
	 * Version of JSignPdf
	 */
	public static final String VERSION = "0.2";

	/**
	 * Name (path) of resource bundle
	 */
	public static final String RESOURCE_BUNDLE_BASE = "translations.messages";

    /**
     * Property name.
     */
    public static final String PROPERTY_KSTYPE = "keystore.type";
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

    /**
     * Keystore type name for PKCS#12
     */
    public static final String KS_TYPE_PKCS12 = "pkcs12";
    /**
     * Keystore type name for Java Key Store
     */
    public static final String KS_TYPE_JKS = "jks";

}
