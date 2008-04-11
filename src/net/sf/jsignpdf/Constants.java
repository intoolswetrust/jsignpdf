package net.sf.jsignpdf;

/**
 * Constants used in PDF signer application.
 * @author Josef Cacek
 */
public class Constants {

	/**
	 * Version of JSignPdf
	 */
	public static final String VERSION = "0.6";

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

    public static final String PROPERTY_VISIBLE_ENABLED = "visibleSignature.enabled";
    public static final String PROPERTY_VISIBLE_PAGE = "visibleSignature.page";
    public static final String PROPERTY_VISIBLE_POS_LLX = "visibleSignature.llx";
    public static final String PROPERTY_VISIBLE_POS_LLY = "visibleSignature.lly";
    public static final String PROPERTY_VISIBLE_POS_URX = "visibleSignature.urx";
    public static final String PROPERTY_VISIBLE_POS_URY = "visibleSignature.ury";
    public static final String PROPERTY_VISIBLE_BGSCALE = "visibleSignature.bgScale";
    public static final String PROPERTY_VISIBLE_RENDER = "visibleSignature.render";
    public static final String PROPERTY_VISIBLE_L2TEXT = "visibleSignature.l2text";
    public static final String PROPERTY_VISIBLE_L4TEXT = "visibleSignature.l4text";
    public static final String PROPERTY_VISIBLE_IMG = "visibleSignature.img";
    public static final String PROPERTY_VISIBLE_BGIMG = "visibleSignature.bgImg";

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


    public static final int DEFVAL_PAGE = 1;
    public static final float DEFVAL_LLX = 0f;
    public static final float DEFVAL_LLY = 0f;
    public static final float DEFVAL_URX = 100f;
    public static final float DEFVAL_URY = 100f;
    public static final float DEFVAL_BG_SCALE = -1f;


}
