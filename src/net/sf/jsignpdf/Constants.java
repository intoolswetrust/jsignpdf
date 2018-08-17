/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf;

import java.net.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.utils.ConfigProvider;
import net.sf.jsignpdf.utils.ResourceProvider;

/**
 * Constants used in PDF signer application.
 * 
 * @author Josef Cacek
 */
public class Constants {

	/**
	 * Version of JSignPdf
	 */
	public static final String VERSION = "@JSIGNPDF_VERSION@";

	/**
	 * Home directory of current user. It's not a real constant it only holds
	 * value of <code>System.getProperty("user.home")</code>
	 */
	public static final String USER_HOME = System.getProperty("user.home");

	/**
	 * Filename (in USER_HOME), where filled values from JSign application are
	 * stored.
	 * 
	 * @see #USER_HOME
	 */
	public static final String PROPERTIES_FILE = ".JSignPdf";

	public static final String CONF_FILE = "conf/conf.properties";

	/**
	 * Name of X.509 certificate type.
	 */
	public static final String CERT_TYPE_X509 = "X.509";

	/**
	 * Name (path) of resource bundle
	 */
	public static final String RESOURCE_BUNDLE_BASE = "net.sf.jsignpdf.translations.messages";

	public static final String L2TEXT_FONT_PATH = "/net/sf/jsignpdf/fonts/DejaVuSans.ttf";
	public static final String L2TEXT_FONT_NAME = "DejaVuSans.ttf";

	public static final String L2TEXT_PLACEHOLDER_TIMESTAMP = "timestamp";
	public static final String L2TEXT_PLACEHOLDER_SIGNER = "signer";
	public static final String L2TEXT_PLACEHOLDER_LOCATION = "location";
	public static final String L2TEXT_PLACEHOLDER_REASON = "reason";
	public static final String L2TEXT_PLACEHOLDER_CONTACT = "contact";

	public static final String DEFAULT_OUT_SUFFIX = "_signed";

	public static final String KEYSTORE_TYPE_WINDOWS_MY = "WINDOWS-MY";
	public static final String KEYSTORE_TYPE_CLOUDFOXY = "CloudFoxy";

	public static final String NEW_LINE = System.getProperty("line.separator");

	public static final ResourceProvider RES = new ResourceProvider(ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE));

	public static final boolean RELAX_SSL_SECURITY = ConfigProvider.getInstance().getAsBool("relax.ssl.security");

	public static final String PDF2IMAGE_JPEDAL = "jpedal";
	public static final String PDF2IMAGE_PDFBOX = "pdfbox";
	public static final String PDF2IMAGE_PDFRENDERER = "pdfrenderer";
	public static final String PDF2IMAGE_LIBRARIES_DEFAULT = PDF2IMAGE_JPEDAL + "," + PDF2IMAGE_PDFBOX + ","
			+ PDF2IMAGE_PDFRENDERER;
	public static final String PDF2IMAGE_LIBRARIES = ConfigProvider.getInstance().getNotEmptyProperty(
			"pdf2image.libraries", PDF2IMAGE_LIBRARIES_DEFAULT);

	public static final String DEFVAL_TSA_HASH_ALG = ConfigProvider.getInstance().getNotEmptyProperty(
			"tsa.hashAlgorithm", "SHA-1");

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
	public static final String PROPERTY_KEY_INDEX = "keystore.keyIndex";
	public static final String PROPERTY_STOREPWD = "store.passwords";

	public static final String PROPERTY_APPEND = "signature.append";
	@Deprecated
	public static final String PROPERTY_ENCRYPTED_PDF = "inpdf.encrypted";
	public static final String PROPERTY_PDF_ENCRYPTION = "pdf.encryption";
	public static final String PROPERTY_PDF_ENCRYPTION_CERT_FILE = "pdf.encryption.publicKeyFile";

	public static final String PROPERTY_CERT_LEVEL = "certification.level";
	public static final String PROPERTY_HASH_ALGORITHM = "hash.algorithm";

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
	public static final String PROPERTY_VISIBLE_L2TEXT_FONT_SIZE = "visibleSignature.l2textFontSize";
	public static final String PROPERTY_VISIBLE_L4TEXT = "visibleSignature.l4text";
	public static final String PROPERTY_VISIBLE_IMG = "visibleSignature.img";
	public static final String PROPERTY_VISIBLE_BGIMG = "visibleSignature.bgImg";
	public static final String PROPERTY_VISIBLE_ACRO6LAYERS = "visibleSignature.acro6layers";

	public static final String PROPERTY_TSA_ENABLED = "tsa.enabled";
	public static final String PROPERTY_TSA_URL = "tsa.url";
	public static final String PROPERTY_TSA_USER = "tsa.user";
	public static final String PROPERTY_TSA_SERVER_AUTHN = "tsa.serverAuthn";
	public static final String PROPERTY_TSA_CERT_FILE_TYPE = "tsa.cert.file.type";
	public static final String PROPERTY_TSA_CERT_FILE = "tsa.cert.file";
	public static final String EPROPERTY_TSA_CERT_PWD = "enc.tsa.cert.file";
	public static final String EPROPERTY_TSA_PWD = "enc.tsa.passwd";
	public static final String PROPERTY_TSA_POLICY = "tsa.policy";
	public static final String PROPERTY_TSA_HASH_ALG = "tsa.hash.algorithm";

	public static final String PROPERTY_OCSP_ENABLED = "ocsp.enabled";
	public static final String PROPERTY_OCSP_SERVER_URL = "ocsp.serverUrl";
	public static final String PROPERTY_CRL_ENABLED = "crl.enabled";

	public static final String PROPERTY_PROXY_TYPE = "proxy.type";
	public static final String PROPERTY_PROXY_HOST = "proxy.host";
	public static final String PROPERTY_PROXY_PORT = "proxy.port";

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

	public static final String PROPERTY_CONTACT = "signature.contact";

	public static final long DEFVAL_SIG_SIZE = 15000L;
	public static final String DEFVAL_CACERTS_PASSWD = "changeit";

	public static final HashAlgorithm DEFVAL_HASH_ALGORITHM = HashAlgorithm.SHA1;

	public static final boolean DEFVAL_APPEND = toBoolean(true);

	public static final int DEFVAL_KEY_INDEX = 0;
	public static final int DEFVAL_PAGE = 1;
	public static final float DEFVAL_LLX = 0f;
	public static final float DEFVAL_LLY = 0f;
	public static final float DEFVAL_URX = 100f;
	public static final float DEFVAL_URY = 100f;
	public static final float DEFVAL_L2_FONT_SIZE = 10f;
	public static final float DEFVAL_BG_SCALE = -1f;
	public static final boolean DEFVAL_ACRO6LAYERS = true;

	public static final Proxy.Type DEFVAL_PROXY_TYPE = Proxy.Type.DIRECT;
	public static final int DEFVAL_PROXY_PORT = 80;

	public static final int EXIT_CODE_PARSE_ERR = 1;
	public static final int EXIT_CODE_NO_COMMAND = 2;
	public static final int EXIT_CODE_SOME_SIG_FAILED = 3;
	public static final int EXIT_CODE_ALL_SIG_FAILED = 4;

	// new in SignatureCounter
	public static final int EXIT_CODE_CANT_READ_FILE = 5;
	public static final int EXIT_CODE_COMMON_ERROR = 6;

	public static final String ARG_HELP_LONG = "help";
	public static final String ARG_HELP = "h";

	public static final String ARG_VERSION_LONG = "version";
	public static final String ARG_VERSION = "v";

	public static final String ARG_LOADPROPS_LONG = "load-properties";
	public static final String ARG_LOADPROPS = "lp";

	public static final String ARG_LOADPROPS_FILE_LONG = "load-properties-file";
	public static final String ARG_LOADPROPS_FILE = "lpf";

	public static final String ARG_LIST_KS_TYPES = "lkt";
	public static final String ARG_LIST_KS_TYPES_LONG = "list-keystore-types";

	public static final String ARG_LIST_KEYS = "lk";
	public static final String ARG_LIST_KEYS_LONG = "list-keys";

	public static final String ARG_KS_TYPE_LONG = "keystore-type";
	public static final String ARG_KS_TYPE = "kst";

	public static final String ARG_KS_FILE_LONG = "keystore-file";
	public static final String ARG_KS_FILE = "ksf";

	public static final String ARG_KS_PWD_LONG = "keystore-password";
	public static final String ARG_KS_PWD = "ksp";

	public static final String ARG_KEY_PWD_LONG = "key-password";
	public static final String ARG_KEY_PWD = "kp";

	public static final String ARG_KEY_ALIAS_LONG = "key-alias";
	public static final String ARG_KEY_ALIAS = "ka";

	public static final String ARG_KEY_INDEX_LONG = "key-index";
	public static final String ARG_KEY_INDEX = "ki";

	public static final String ARG_OUTPATH = "d";
	public static final String ARG_OUTPATH_LONG = "out-directory";

	public static final String ARG_OPREFIX = "op";
	public static final String ARG_OPREFIX_LONG = "out-prefix";

	public static final String ARG_OSUFFIX = "os";
	public static final String ARG_OSUFFIX_LONG = "out-suffix";

	public static final String ARG_REASON = "r";
	public static final String ARG_REASON_LONG = "reason";

	public static final String ARG_LOCATION = "l";
	public static final String ARG_LOCATION_LONG = "location";

	public static final String ARG_CONTACT = "c";
	public static final String ARG_CONTACT_LONG = "contact";

	public static final String ARG_APPEND = "a";
	public static final String ARG_APPEND_LONG = "append";

	public static final String ARG_QUIET_LONG = "quiet";
	public static final String ARG_QUIET = "q";

	public static final String ARG_CERT_LEVEL = "cl";
	public static final String ARG_CERT_LEVEL_LONG = "certification-level";

	public static final String ARG_HASH_ALGORITHM = "ha";
	public static final String ARG_HASH_ALGORITHM_LONG = "hash-algorithm";

	public static final String ARG_ENCRYPTED = "e";
	public static final String ARG_ENCRYPTED_LONG = "encrypted";

	public static final String ARG_ENCRYPTION = "pe";
	public static final String ARG_ENCRYPTION_LONG = "encryption";

	public static final String ARG_PWD_OWNER = "opwd";
	public static final String ARG_PWD_OWNER_LONG = "owner-password";

	public static final String ARG_PWD_USER = "upwd";
	public static final String ARG_PWD_USER_LONG = "user-password";

	public static final String ARG_ENC_CERT = "ec";
	public static final String ARG_ENC_CERT_LONG = "encryption-certificate";

	public static final String ARG_RIGHT_PRINT = "pr";
	public static final String ARG_RIGHT_PRINT_LONG = "print-right";

	public static final String ARG_DISABLE_COPY_LONG = "disable-copy";
	public static final String ARG_DISABLE_ASSEMBLY_LONG = "disable-assembly";
	public static final String ARG_DISABLE_FILL_LONG = "disable-fill";
	public static final String ARG_DISABLE_SCREEN_READERS_LONG = "disable-screen-readers";
	public static final String ARG_DISABLE_MODIFY_ANNOT_LONG = "disable-modify-annotations";
	public static final String ARG_DISABLE_MODIFY_CONTENT_LONG = "disable-modify-content";

	public static final String ARG_VISIBLE = "V";
	public static final String ARG_VISIBLE_LONG = "visible-signature";
	public static final String ARG_PAGE = "pg";
	public static final String ARG_PAGE_LONG = "page";

	public static final String ARG_POS_LLX = "llx";
	public static final String ARG_POS_LLY = "lly";
	public static final String ARG_POS_URX = "urx";
	public static final String ARG_POS_URY = "ury";
	public static final String ARG_BG_SCALE = "bg-scale";
	public static final String ARG_RENDER_MODE = "render-mode";
	public static final String ARG_L2_TEXT_LONG = "l2-text";

	public static final String ARG_L2TEXT_FONT_SIZE = "fs";
	public static final String ARG_L2TEXT_FONT_SIZE_LONG = "font-size";

	public static final String ARG_L4_TEXT_LONG = "l4-text";
	public static final String ARG_IMG_PATH = "img-path";
	public static final String ARG_BG_PATH = "bg-path";

	public static final String ARG_DISABLE_ACRO6LAYERS = "disable-acrobat6-layer-mode";

	public static final String ARG_TSA_URL = "ts";
	public static final String ARG_TSA_URL_LONG = "tsa-server-url";

	public static final String ARG_TSA_AUTHN = "ta";
	public static final String ARG_TSA_AUTHN_LONG = "tsa-authentication";

	public static final String ARG_TSA_CERT_FILE_TYPE = "tsct";
	public static final String ARG_TSA_CERT_FILE_TYPE_LONG = "tsa-cert-file-type";

	public static final String ARG_TSA_CERT_FILE = "tscf";
	public static final String ARG_TSA_CERT_FILE_LONG = "tsa-cert-file";

	public static final String ARG_TSA_CERT_PWD = "tscp";
	public static final String ARG_TSA_CERT_PWD_LONG = "tsa-cert-password";

	public static final String ARG_TSA_USER = "tsu";
	public static final String ARG_TSA_USER_LONG = "tsa-user";

	public static final String ARG_TSA_PWD = "tsp";
	public static final String ARG_TSA_PWD_LONG = "tsa-password";

	public static final String ARG_TSA_POLICY_LONG = "tsa-policy-oid";

	public static final String ARG_TSA_HASH_ALG = "tsh";
	public static final String ARG_TSA_HASH_ALG_LONG = "tsa-hash-algorithm";

	public static final String ARG_OCSP_LONG = "ocsp";
	public static final String ARG_OCSP_SERVER_LONG = "ocsp-server-url";
	public static final String ARG_CRL_LONG = "crl";

	public static final String ARG_PROXY_TYPE_LONG = "proxy-type";
	public static final String ARG_PROXY_HOST_LONG = "proxy-host";
	public static final String ARG_PROXY_PORT_LONG = "proxy-port";

	public static final Set<String> SUPPORTED_CRITICAL_EXTENSION_OIDS;

	static {
		final Set<String> oidSet = new HashSet<String>();
		oidSet.add("2.5.29.15"); // KeyUsage
		oidSet.add("2.5.29.17"); // Subject Alternative Name
		oidSet.add("2.5.29.19"); // Basic Constraints
		oidSet.add("2.5.29.29"); // Certificate Issuer
		oidSet.add("2.5.29.37"); // Extended Key Usage
		SUPPORTED_CRITICAL_EXTENSION_OIDS = Collections.unmodifiableSet(oidSet);
	}

	private static boolean toBoolean(final boolean b) {
		return b;
	}

}
