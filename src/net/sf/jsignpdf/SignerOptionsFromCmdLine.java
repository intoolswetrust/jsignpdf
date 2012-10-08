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

import static net.sf.jsignpdf.Constants.*;

import java.net.Proxy;

import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PDFEncryption;
import net.sf.jsignpdf.types.PrintRight;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.types.ServerAuthentication;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * This class parses and holds options from command line
 * 
 * @author Josef Cacek
 */
@SuppressWarnings("static-access")
public class SignerOptionsFromCmdLine extends BasicSignerOptions {

//  private final static Logger LOGGER = Logger.getLogger(SignerOptionsFromCmdLine.class);

	static final Options OPTS = new Options();

	private String outPrefix;
	private String outSuffix = Constants.DEFAULT_OUT_SUFFIX;
	private String outPath;

	private String[] files;

	private boolean printHelp = true;
	private boolean printVersion;
	private boolean listKeyStores;
	private boolean listKeys;

	/**
	 * Parses options provided as command line arguments.
	 * 
	 * @param anArgs
	 * @throws ParseException
	 */
	public void loadCmdLine(final String[] anArgs) throws ParseException {
		if (anArgs == null)
			return;

		// create the command line parser
		final CommandLineParser parser = new PosixParser();
		// parse the command line arguments
		final CommandLine line = parser.parse(OPTS, anArgs);

		setAdvanced(true);

		if (line.hasOption(ARG_LOADPROPS_FILE)) {
			if (!line.hasOption(ARG_LOADPROPS)) {
				props.clear();
			}
			props.loadProperties(line.getOptionValue(ARG_LOADPROPS_FILE));
		}

		if (line.hasOption(ARG_LOADPROPS) || line.hasOption(ARG_LOADPROPS_FILE)) {
			loadOptions();
		}

		if (line.hasOption(ARG_QUIET)) {
			// disable logging
			Logger.getRootLogger().removeAllAppenders();
		}

		// the arguments, which are not options or option-values should be the
		// files
		setFiles(line.getArgs());

		// commands
		setPrintHelp(line.hasOption(ARG_HELP));
		setPrintVersion(line.hasOption(ARG_VERSION));
		setListKeyStores(line.hasOption(ARG_LIST_KS_TYPES));
		setListKeys(line.hasOption(ARG_LIST_KEYS));

		// basic options
		if (line.hasOption(ARG_KS_TYPE))
			setKsType(line.getOptionValue(ARG_KS_TYPE));
		if (line.hasOption(ARG_KS_FILE))
			setKsFile(line.getOptionValue(ARG_KS_FILE));
		if (line.hasOption(ARG_KS_PWD))
			setKsPasswd(line.getOptionValue(ARG_KS_PWD));
		if (line.hasOption(ARG_KEY_ALIAS))
			setKeyAlias(line.getOptionValue(ARG_KEY_ALIAS));
		if (line.hasOption(ARG_KEY_INDEX))
			setKeyIndex(getInt(line.getParsedOptionValue(ARG_KEY_INDEX), getKeyIndex()));
		if (line.hasOption(ARG_KEY_PWD))
			setKeyPasswd(line.getOptionValue(ARG_KEY_PWD));
		if (line.hasOption(ARG_OUTPATH))
			setOutPath(line.getOptionValue(ARG_OUTPATH));
		if (line.hasOption(ARG_OPREFIX))
			setOutPrefix(line.getOptionValue(ARG_OPREFIX));
		if (line.hasOption(ARG_OSUFFIX))
			setOutSuffix(line.getOptionValue(ARG_OSUFFIX));
		if (line.hasOption(ARG_REASON))
			setReason(line.getOptionValue(ARG_REASON));
		if (line.hasOption(ARG_LOCATION))
			setLocation(line.getOptionValue(ARG_LOCATION));
		if (line.hasOption(ARG_CONTACT))
			setContact(line.getOptionValue(ARG_CONTACT));
		setAppend(line.hasOption(ARG_APPEND));
		if (line.hasOption(ARG_CERT_LEVEL))
			setCertLevel(line.getOptionValue(ARG_CERT_LEVEL));
		if (line.hasOption(ARG_HASH_ALGORITHM))
			setHashAlgorithm(line.getOptionValue(ARG_HASH_ALGORITHM));

		// encryption
		if (line.hasOption(ARG_ENCRYPTED))
			setPdfEncryption(PDFEncryption.PASSWORD);
		if (line.hasOption(ARG_ENCRYPTION))
			setPdfEncryption(line.getOptionValue(ARG_ENCRYPTION));
		if (line.hasOption(ARG_PWD_OWNER))
			setPdfOwnerPwd(line.getOptionValue(ARG_PWD_OWNER));
		if (line.hasOption(ARG_PWD_USER))
			setPdfUserPwd(line.getOptionValue(ARG_PWD_USER));
		if (line.hasOption(ARG_ENC_CERT))
			setPdfEncryptionCertFile(line.getOptionValue(ARG_ENC_CERT));
		if (line.hasOption(ARG_RIGHT_PRINT))
			setRightPrinting(line.getOptionValue(ARG_RIGHT_PRINT));
		if (line.hasOption(ARG_DISABLE_COPY_LONG))
			setRightCopy(false);
		if (line.hasOption(ARG_DISABLE_ASSEMBLY_LONG))
			setRightAssembly(false);
		if (line.hasOption(ARG_DISABLE_FILL_LONG))
			setRightFillIn(false);
		if (line.hasOption(ARG_DISABLE_SCREEN_READERS_LONG))
			setRightScreanReaders(false);
		if (line.hasOption(ARG_DISABLE_MODIFY_ANNOT_LONG))
			setRightModifyAnnotations(false);
		if (line.hasOption(ARG_DISABLE_MODIFY_CONTENT_LONG))
			setRightModifyContents(false);

		// visible signature
		if (line.hasOption(ARG_VISIBLE))
			setVisible(true);
		if (line.hasOption(ARG_PAGE))
			setPage(getInt(line.getParsedOptionValue(ARG_PAGE), getPage()));
		if (line.hasOption(ARG_POS_LLX))
			setPositionLLX(getFloat(line.getParsedOptionValue(ARG_POS_LLX), getPositionLLX()));
		if (line.hasOption(ARG_POS_LLY))
			setPositionLLY(getFloat(line.getParsedOptionValue(ARG_POS_LLY), getPositionLLY()));
		if (line.hasOption(ARG_POS_URX))
			setPositionURX(getFloat(line.getParsedOptionValue(ARG_POS_URX), getPositionURX()));
		if (line.hasOption(ARG_POS_URY))
			setPositionURY(getFloat(line.getParsedOptionValue(ARG_POS_URY), getPositionURY()));
		if (line.hasOption(ARG_BG_SCALE))
			setBgImgScale(getFloat(line.getParsedOptionValue(ARG_BG_SCALE), getBgImgScale()));
		if (line.hasOption(ARG_RENDER_MODE))
			setRenderMode(line.getOptionValue(ARG_RENDER_MODE));
		if (line.hasOption(ARG_L2_TEXT_LONG))
			setL2Text(line.getOptionValue(ARG_L2_TEXT_LONG));
		if (line.hasOption(ARG_L2TEXT_FONT_SIZE))
			setL2TextFontSize(getFloat(line.getParsedOptionValue(ARG_L2TEXT_FONT_SIZE), getL2TextFontSize()));
		if (line.hasOption(ARG_L4_TEXT_LONG))
			setL4Text(line.getOptionValue(ARG_L4_TEXT_LONG));
		if (line.hasOption(ARG_IMG_PATH))
			setImgPath(line.getOptionValue(ARG_IMG_PATH));
		if (line.hasOption(ARG_BG_PATH))
			setBgImgPath(line.getOptionValue(ARG_BG_PATH));
		setAcro6Layers(!line.hasOption(ARG_DISABLE_ACRO6LAYERS));

		// TSA & OCSP
		if (line.hasOption(ARG_TSA_URL)) {
			setTimestamp(true);
			setTsaUrl(line.getOptionValue(ARG_TSA_URL));
		}
		if (line.hasOption(ARG_TSA_AUTHN))
			setTsaServerAuthn(line.getOptionValue(ARG_TSA_AUTHN));
		if (line.hasOption(ARG_TSA_CERT_FILE_TYPE))
			setTsaCertFileType(line.getOptionValue(ARG_TSA_CERT_FILE_TYPE));
		if (line.hasOption(ARG_TSA_CERT_FILE))
			setTsaCertFile(line.getOptionValue(ARG_TSA_CERT_FILE));
		if (line.hasOption(ARG_TSA_CERT_PWD))
			setTsaCertFilePwd(line.getOptionValue(ARG_TSA_CERT_PWD));

		if (line.hasOption(ARG_TSA_USER))
			setTsaUser(line.getOptionValue(ARG_TSA_USER));
		if (line.hasOption(ARG_TSA_PWD))
			setTsaPasswd(line.getOptionValue(ARG_TSA_PWD));
		if (line.hasOption(ARG_TSA_POLICY_LONG))
			setTsaPolicy(line.getOptionValue(ARG_TSA_POLICY_LONG));
		if (line.hasOption(ARG_OCSP_LONG))
			setOcspEnabled(true);
		if (line.hasOption(ARG_OCSP_SERVER_LONG))
			setOcspServerUrl(line.getOptionValue(ARG_OCSP_SERVER_LONG));

		if (line.hasOption(ARG_PROXY_TYPE_LONG))
			setProxyType(line.getOptionValue(ARG_PROXY_TYPE_LONG));
		if (line.hasOption(ARG_PROXY_HOST_LONG))
			setProxyHost(line.getOptionValue(ARG_PROXY_HOST_LONG));
		if (line.hasOption(ARG_PROXY_PORT_LONG))
			setProxyPort(getInt(line.getParsedOptionValue(ARG_PROXY_PORT_LONG), getProxyPort()));

	}

	/**
	 * Returns int value from parsed option object
	 * 
	 * @param aVal
	 *            value returned by parser
	 * @param aDefVal
	 *            default value
	 * @return
	 */
	private int getInt(Object aVal, int aDefVal) {
		if (aVal instanceof Number) {
			return ((Number) aVal).intValue();
		}
		return aDefVal;
	}

	/**
	 * Returns float value from parsed option object
	 * 
	 * @param aVal
	 *            value returned by parser
	 * @param aDefVal
	 *            default value
	 * @return
	 */
	private float getFloat(Object aVal, float aDefVal) {
		if (aVal instanceof Number) {
			return ((Number) aVal).floatValue();
		}
		return aDefVal;
	}

	static {
		// reset option builder
		OptionBuilder.withLongOpt(ARG_HELP_LONG).create();
		// commands
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_HELP_LONG).withDescription(RES.get("hlp.help")).create(ARG_HELP));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_VERSION_LONG).withDescription(RES.get("hlp.version"))
				.create(ARG_VERSION));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_LOADPROPS_LONG).withDescription(RES.get("hlp.loadProperties"))
				.create(ARG_LOADPROPS));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_LOADPROPS_FILE_LONG)
				.withDescription(RES.get("hlp.loadPropertiesFile")).hasArg().withArgName("file")
				.create(ARG_LOADPROPS_FILE));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_LIST_KS_TYPES_LONG).withDescription(RES.get("hlp.listKsTypes"))
				.create(ARG_LIST_KS_TYPES));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_LIST_KEYS_LONG).withDescription(RES.get("hlp.listKeys"))
				.create(ARG_LIST_KEYS));

		// keystore and key configuration options
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_KS_TYPE_LONG).withDescription(RES.get("hlp.ksType")).hasArg()
				.withArgName("type").create(ARG_KS_TYPE));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_KS_FILE_LONG).withDescription(RES.get("hlp.ksFile")).hasArg()
				.withArgName("file").create(ARG_KS_FILE));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_KS_PWD_LONG).withDescription(RES.get("hlp.ksPwd")).hasArg()
				.withArgName("password").create(ARG_KS_PWD));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_KEY_ALIAS_LONG).withDescription(RES.get("hlp.keyAlias")).hasArg()
				.withArgName("alias").create(ARG_KEY_ALIAS));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_KEY_INDEX_LONG).withDescription(RES.get("hlp.keyIndex")).hasArg()
				.withType(Number.class).withArgName("index").create(ARG_KEY_INDEX));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_KEY_PWD_LONG).withDescription(RES.get("hlp.keyPwd")).hasArg()
				.withArgName("password").create(ARG_KEY_PWD));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_OUTPATH_LONG).withDescription(RES.get("hlp.outPath")).hasArg()
				.withArgName("path").create(ARG_OUTPATH));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_OPREFIX_LONG).withDescription(RES.get("hlp.outPrefix")).hasArg()
				.withArgName("prefix").create(ARG_OPREFIX));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_OSUFFIX_LONG).withDescription(RES.get("hlp.outSuffix")).hasArg()
				.withArgName("suffix").create(ARG_OSUFFIX));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_REASON_LONG).withDescription(RES.get("hlp.reason")).hasArg()
				.withArgName("reason").create(ARG_REASON));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_LOCATION_LONG).withDescription(RES.get("hlp.location")).hasArg()
				.withArgName("location").create(ARG_LOCATION));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_CONTACT_LONG).withDescription(RES.get("hlp.contact")).hasArg()
				.withArgName("contact").create(ARG_CONTACT));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_APPEND_LONG).withDescription(RES.get("hlp.append"))
				.create(ARG_APPEND));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_CERT_LEVEL_LONG)
				.withDescription(RES.get("hlp.certLevel", getEnumValues(CertificationLevel.values()))).hasArg()
				.withArgName("level").create(ARG_CERT_LEVEL));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_HASH_ALGORITHM_LONG)
				.withDescription(RES.get("hlp.hashAlgorithm", getEnumValues(HashAlgorithm.values()))).hasArg()
				.withArgName("algorithm").create(ARG_HASH_ALGORITHM));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_QUIET_LONG).withDescription(RES.get("hlp.quiet"))
				.create(ARG_QUIET));

		// Encryption and rights
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_ENCRYPTED_LONG).withDescription(RES.get("hlp.encrypted"))
				.create(ARG_ENCRYPTED));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_ENCRYPTION_LONG)
				.withDescription(RES.get("hlp.encryption", getEnumValues(PDFEncryption.values()))).hasArg()
				.withArgName("mode").create(ARG_ENCRYPTION));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_PWD_OWNER_LONG).withDescription(RES.get("hlp.ownerpwd")).hasArg()
				.withArgName("password").create(ARG_PWD_OWNER));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_PWD_USER_LONG).withDescription(RES.get("hlp.userpwd")).hasArg()
				.withArgName("password").create(ARG_PWD_USER));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_ENC_CERT_LONG).withDescription(RES.get("hlp.encCert")).hasArg()
				.withArgName("file").create(ARG_ENC_CERT));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_RIGHT_PRINT_LONG)
				.withDescription(RES.get("hlp.printRight", getEnumValues(PrintRight.values()))).hasArg()
				.withArgName("right").create(ARG_RIGHT_PRINT));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_DISABLE_COPY_LONG).withDescription(RES.get("hlp.disableCopy"))
				.create());
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_DISABLE_ASSEMBLY_LONG)
				.withDescription(RES.get("hlp.disableAssembly")).create());
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_DISABLE_FILL_LONG).withDescription(RES.get("hlp.disableFill"))
				.create());
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_DISABLE_SCREEN_READERS_LONG)
				.withDescription(RES.get("hlp.disableScrRead")).create());
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_DISABLE_MODIFY_ANNOT_LONG)
				.withDescription(RES.get("hlp.disableAnnot")).create());
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_DISABLE_MODIFY_CONTENT_LONG)
				.withDescription(RES.get("hlp.disableContent")).create());

		// visible signature options
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_VISIBLE_LONG).withDescription(RES.get("hlp.visible"))
				.create(ARG_VISIBLE));
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_PAGE_LONG).withDescription(RES.get("hlp.page")).hasArg()
				.withType(Number.class).withArgName("pageNumber").create(ARG_PAGE));
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.posLLX")).hasArg().withType(Number.class)
				.withArgName("position").create(ARG_POS_LLX));
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.posLLY")).hasArg().withType(Number.class)
				.withArgName("position").create(ARG_POS_LLY));
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.posURX")).hasArg().withType(Number.class)
				.withArgName("position").create(ARG_POS_URX));
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.posURY")).hasArg().withType(Number.class)
				.withArgName("position").create(ARG_POS_URY));
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.imgPath")).withLongOpt(ARG_IMG_PATH).hasArg()
				.withArgName("file").create());
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.bgPath")).withLongOpt(ARG_BG_PATH).hasArg()
				.withArgName("file").create());
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.bgScale")).withLongOpt(ARG_BG_SCALE).hasArg()
				.withType(Number.class).withArgName("scale").create());
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.disableAcro6Layers"))
				.withLongOpt(ARG_DISABLE_ACRO6LAYERS).create());

		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.renderMode", getEnumValues(RenderMode.values())))
				.withLongOpt(ARG_RENDER_MODE).hasArg().withArgName("mode").create());
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.l2Text")).withLongOpt(ARG_L2_TEXT_LONG).hasArg()
				.withArgName("text").create());
		OPTS.addOption(OptionBuilder
				.withDescription(RES.get("hlp.l2TextFontSize", String.valueOf(Constants.DEFVAL_L2_FONT_SIZE)))
				.withLongOpt(ARG_L2TEXT_FONT_SIZE_LONG).hasArg().withType(Number.class).withArgName("size")
				.create(ARG_L2TEXT_FONT_SIZE));
		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.l4Text")).withLongOpt(ARG_L4_TEXT_LONG).hasArg()
				.withArgName("text").create());

		// TSA & OCSP
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_TSA_URL_LONG).withDescription(RES.get("hlp.tsaUrl")).hasArg()
				.withArgName("URL").create(ARG_TSA_URL));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_TSA_AUTHN_LONG)
				.withDescription(RES.get("hlp.tsaAuthn", getEnumValues(ServerAuthentication.values()))).hasArg()
				.withArgName("method").create(ARG_TSA_AUTHN));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_TSA_CERT_FILE_TYPE_LONG)
				.withDescription(RES.get("hlp.tsaCertFileType")).hasArg().withArgName("ks-type")
				.create(ARG_TSA_CERT_FILE_TYPE));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_TSA_CERT_FILE_LONG).withDescription(RES.get("hlp.tsaCertFile"))
				.hasArg().withArgName("file").create(ARG_TSA_CERT_FILE));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_TSA_CERT_PWD_LONG).withDescription(RES.get("hlp.tsaCertPasswd"))
				.hasArg().withArgName("password").create(ARG_TSA_CERT_PWD));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_TSA_USER_LONG).withDescription(RES.get("hlp.tsaUser")).hasArg()
				.withArgName("username").create(ARG_TSA_USER));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_TSA_PWD_LONG).withDescription(RES.get("hlp.tsaPwd")).hasArg()
				.withArgName("password").create(ARG_TSA_PWD));

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_TSA_POLICY_LONG).withDescription(RES.get("hlp.tsaPolicy"))
				.hasArg().withArgName("policyOID").create());

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_OCSP_LONG).withDescription(RES.get("hlp.ocsp")).create());
		OPTS.addOption(OptionBuilder.withLongOpt(ARG_OCSP_SERVER_LONG).withDescription(RES.get("hlp.ocspServerUrl"))
				.create());

		OPTS.addOption(OptionBuilder.withLongOpt(ARG_CRL_LONG).withDescription(RES.get("hlp.crl")).create());

		OPTS.addOption(OptionBuilder
				.withDescription(RES.get("hlp.proxyType", DEFVAL_PROXY_TYPE.name(), getEnumValues(Proxy.Type.values())))
				.withLongOpt(ARG_PROXY_TYPE_LONG).hasArg().withArgName("type").create());

		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.proxyHost")).withLongOpt(ARG_PROXY_HOST_LONG)
				.hasArg().withArgName("hostname").create());

		OPTS.addOption(OptionBuilder.withDescription(RES.get("hlp.proxyPort", String.valueOf(DEFVAL_PROXY_PORT)))
				.withLongOpt(ARG_PROXY_PORT_LONG).hasArg().withType(Number.class).withArgName("port").create());

	}

	/**
	 * @return the outPrefix
	 */
	public String getOutPrefix() {
		if (outPrefix == null)
			outPrefix = "";
		return outPrefix;
	}

	/**
	 * Return comma separated names from enum values array.
	 * 
	 * @param aEnumVals
	 * @return
	 */
	private static String getEnumValues(Enum<?>[] aEnumVals) {
		final StringBuilder tmpResult = new StringBuilder();
		boolean tmpFirst = true;
		for (Enum<?> tmpEnu : aEnumVals) {
			if (tmpFirst) {
				tmpFirst = false;
			} else {
				tmpResult.append(", ");
			}
			tmpResult.append(tmpEnu.name());
		}
		return tmpResult.toString();
	}

	/**
	 * @param outPrefix
	 *            the outPrefix to set
	 */
	public void setOutPrefix(String outPrefix) {
		this.outPrefix = outPrefix;
	}

	/**
	 * @return the outSuffix
	 */
	public String getOutSuffix() {
		if (outSuffix == null)
			outSuffix = "";
		return outSuffix;
	}

	/**
	 * @param outSuffix
	 *            the outSuffix to set
	 */
	public void setOutSuffix(String outSuffix) {
		this.outSuffix = outSuffix;
	}

	/**
	 * @return the files
	 */
	public String[] getFiles() {
		return files;
	}

	/**
	 * @param files
	 *            the files to set
	 */
	public void setFiles(String[] files) {
		this.files = files;
	}

	/**
	 * @return the printHelp
	 */
	public boolean isPrintHelp() {
		return printHelp;
	}

	/**
	 * @param printHelp
	 *            the printHelp to set
	 */
	public void setPrintHelp(boolean printHelp) {
		this.printHelp = printHelp;
	}

	/**
	 * @return the printVersion
	 */
	public boolean isPrintVersion() {
		return printVersion;
	}

	/**
	 * @param printVersion
	 *            the printVersion to set
	 */
	public void setPrintVersion(boolean printVersion) {
		this.printVersion = printVersion;
	}

	/**
	 * @return the listKeyStores
	 */
	public boolean isListKeyStores() {
		return listKeyStores;
	}

	/**
	 * @param listKeyStores
	 *            the listKeyStores to set
	 */
	public void setListKeyStores(boolean listKeyStores) {
		this.listKeyStores = listKeyStores;
	}

	/**
	 * @return the listKeys
	 */
	public boolean isListKeys() {
		return listKeys;
	}

	/**
	 * @param listKeys
	 *            the listKeys to set
	 */
	public void setListKeys(boolean listKeys) {
		this.listKeys = listKeys;
	}

	/**
	 * Returns output path including tailing slash character
	 * 
	 * @return the outPath
	 */
	public String getOutPath() {
		String tmpResult;
		if (StringUtils.isEmpty(outPath)) {
			tmpResult = "./";
		} else {
			tmpResult = outPath.replaceAll("\\\\", "/");
			if (!tmpResult.endsWith("/")) {
				tmpResult = tmpResult + "/";
			}
		}
		return tmpResult;
	}

	/**
	 * @param outPath
	 *            the outPath to set
	 */
	public void setOutPath(String outPath) {
		this.outPath = outPath;
	}

}
