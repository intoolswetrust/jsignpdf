package net.sf.jsignpdf;

import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import static net.sf.jsignpdf.Constants.*;

/**
 * This class parses and holds options from command line
 * @author Josef Cacek
 */
@SuppressWarnings("static-access")
public class SignerOptionsFromCmdLine extends BasicSignerOptions {

	static final Options OPTS = new Options();

	private String outPrefix;
	private String outSuffix;
	private String outPath;

	private String[] files;

	private boolean printHelp = true;
	private boolean printVersion;
	private boolean listKeyStores;
	private boolean listKeys;

	//parse command line using CLI here
	public void loadCmdLine(final String[] anArgs) throws ParseException {
		if (anArgs == null) return;
		setAdvanced(true);

		// create the command line parser
		final CommandLineParser parser = new PosixParser();
		// parse the command line arguments
		final CommandLine line = parser.parse(OPTS, anArgs);

		//enable logging if not quiet run
		if (! line.hasOption(ARG_QUIET)) {
			setPrintWriter(new PrintWriter(System.out));
		}

		//the arguments, which are not options or option-values should be the files
		setFiles(line.getArgs());

		//commands
		setPrintHelp(line.hasOption(ARG_HELP));
		setPrintVersion(line.hasOption(ARG_VERSION));
		setListKeyStores(line.hasOption(ARG_LIST_KS_TYPES));
		setListKeys(line.hasOption(ARG_LIST_KEYS));

		//basic options
		setKsType(line.getOptionValue(ARG_KS_TYPE));
		setKsFile(line.getOptionValue(ARG_KS_FILE));
		setKsPasswd(line.getOptionValue(ARG_KS_PWD));
		setKeyAlias(line.getOptionValue(ARG_KEY_ALIAS));
		setKeyPasswd(line.getOptionValue(ARG_KEY_PWD));
		setOutPath(line.getOptionValue(ARG_OUTPATH));
		setOutPrefix(line.getOptionValue(ARG_OPREFIX));
		setOutSuffix(line.getOptionValue(ARG_OSUFFIX));
		setReason(line.getOptionValue(ARG_REASON));
		setLocation(line.getOptionValue(ARG_LOCATION));
		setAppend(line.hasOption(ARG_APPEND));
		setCertLevel(line.getOptionValue(ARG_CERT_LEVEL));

		//encryption
		setEncrypted(line.hasOption(ARG_ENCRYPTED));
		setPdfOwnerPwd(line.getOptionValue(ARG_PWD_OWNER));
		setPdfUserPwd(line.getOptionValue(ARG_PWD_USER));
		setRightPrinting(line.getOptionValue(ARG_RIGHT_PRINT));
		setRightCopy(! line.hasOption(ARG_DISABLE_COPY_LONG));
		setRightAssembly(! line.hasOption(ARG_DISABLE_ASSEMBLY_LONG));
		setRightFillIn(! line.hasOption(ARG_DISABLE_FILL_LONG));
		setRightScreanReaders(! line.hasOption(ARG_DISABLE_SCREEN_READERS_LONG));
		setRightModifyAnnotations(! line.hasOption(ARG_DISABLE_MODIFY_ANNOT_LONG));
		setRightModifyContents(! line.hasOption(ARG_DISABLE_MODIFY_CONTENT_LONG));

		//visible signature
		setVisible(line.hasOption(ARG_VISIBLE));
		setPage(getInt(line.getParsedOptionValue(ARG_PAGE), getPage()));
		setPositionLLX(getFloat(line.getParsedOptionValue(ARG_POS_LLX), getPositionLLX()));
		setPositionLLY(getFloat(line.getParsedOptionValue(ARG_POS_LLY), getPositionLLY()));
		setPositionURX(getFloat(line.getParsedOptionValue(ARG_POS_URX), getPositionURX()));
		setPositionURY(getFloat(line.getParsedOptionValue(ARG_POS_URY), getPositionURY()));
		setBgImgScale(getFloat(line.getParsedOptionValue(ARG_BG_SCALE), getBgImgScale()));
		setRenderMode(line.getOptionValue(ARG_RENDER_MODE));
		setL2Text(line.getOptionValue(ARG_L2_TEXT));
		setL4Text(line.getOptionValue(ARG_L4_TEXT));
		setImgPath(line.getOptionValue(ARG_IMG_PATH));
		setBgImgPath(line.getOptionValue(ARG_BG_PATH));

		if (StringUtils.isEmpty(outPrefix) && StringUtils.isEmpty(outSuffix)) {
			outSuffix = "_signed";
		}
	}

	/**
	 * Returns int value from parsed option object
	 * @param aVal value returned by parser
	 * @param aDefVal default value
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
	 * @param aVal value returned by parser
	 * @param aDefVal default value
	 * @return
	 */
	private float getFloat(Object aVal, float aDefVal) {
		if (aVal instanceof Number) {
			return ((Number) aVal).floatValue();
		}
		return aDefVal;
	}


	static {
		//reset option builder
		OptionBuilder.withLongOpt(ARG_HELP_LONG).create();
		//commands
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_HELP_LONG)
				.withDescription(res.get("hlp.help"))
				.create(ARG_HELP)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_VERSION_LONG)
				.withDescription(res.get("hlp.version"))
				.create(ARG_VERSION)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_LIST_KS_TYPES_LONG)
				.withDescription(res.get("hlp.listKsTypes"))
				.create(ARG_LIST_KS_TYPES)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_LIST_KEYS_LONG)
				.withDescription(res.get("hlp.listKeys"))
				.create(ARG_LIST_KEYS)
		);

		//keystore and key configuration options
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KS_TYPE_LONG)
				.withDescription(res.get("hlp.ksType"))
				.hasArg()
				.withArgName("type")
				.create(ARG_KS_TYPE)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KS_FILE_LONG)
				.withDescription(res.get("hlp.ksFile"))
				.hasArg()
				.withArgName("file")
				.create(ARG_KS_FILE)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KS_PWD_LONG)
				.withDescription(res.get("hlp.ksPwd"))
				.hasArg()
				.withArgName("password")
				.create(ARG_KS_PWD)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KEY_ALIAS_LONG)
				.withDescription(res.get("hlp.keyAlias"))
				.hasArg()
				.withArgName("alias")
				.create(ARG_KEY_ALIAS)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KEY_PWD_LONG)
				.withDescription(res.get("hlp.keyPwd"))
				.hasArg()
				.withArgName("password")
				.create(ARG_KEY_PWD)
		);

		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_OUTPATH_LONG)
				.withDescription(res.get("hlp.outPath"))
				.hasArg()
				.withArgName("path")
				.create(ARG_OUTPATH)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_OPREFIX_LONG)
				.withDescription(res.get("hlp.outPrefix"))
				.hasArg()
				.withArgName("prefix")
				.create(ARG_OPREFIX)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_OSUFFIX_LONG)
				.withDescription(res.get("hlp.outSuffix"))
				.hasArg()
				.withArgName("suffix")
				.create(ARG_OSUFFIX)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_REASON_LONG)
				.withDescription(res.get("hlp.reason"))
				.hasArg()
				.withArgName("reason")
				.create(ARG_REASON)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_LOCATION_LONG)
				.withDescription(res.get("hlp.location"))
				.hasArg()
				.withArgName("location")
				.create(ARG_LOCATION)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_APPEND_LONG)
				.withDescription(res.get("hlp.append"))
				.create(ARG_APPEND)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_CERT_LEVEL_LONG)
				.withDescription(res.get("hlp.certLevel", getEnumValues(CertificationLevel.values())))
				.hasArg()
				.withArgName("level")
				.create(ARG_CERT_LEVEL)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_QUIET_LONG)
				.withDescription(res.get("hlp.quiet"))
				.create(ARG_QUIET)
		);

		//Encryption and rights
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_ENCRYPTED_LONG)
				.withDescription(res.get("hlp.encrypted"))
				.create(ARG_ENCRYPTED)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_PWD_OWNER_LONG)
				.withDescription(res.get("hlp.ownerpwd"))
				.hasArg()
				.withArgName("password")
				.create(ARG_PWD_OWNER)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_PWD_USER_LONG)
				.withDescription(res.get("hlp.userpwd"))
				.hasArg()
				.withArgName("password")
				.create(ARG_PWD_USER)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_RIGHT_PRINT_LONG)
				.withDescription(res.get("hlp.printRight", getEnumValues(PrintRight.values())))
				.hasArg()
				.withArgName("right")
				.create(ARG_RIGHT_PRINT)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_COPY_LONG)
				.withDescription(res.get("hlp.disableCopy"))
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_ASSEMBLY_LONG)
				.withDescription(res.get("hlp.disableAssembly"))
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_FILL_LONG)
				.withDescription(res.get("hlp.disableFill"))
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_SCREEN_READERS_LONG)
				.withDescription(res.get("hlp.disableScrRead"))
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_MODIFY_ANNOT_LONG)
				.withDescription(res.get("hlp.disableAnnot"))
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_MODIFY_CONTENT_LONG)
				.withDescription(res.get("hlp.disableContent"))
				.create()
		);

		//visible signature options
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_VISIBLE_LONG)
				.withDescription(res.get("hlp.visible"))
				.create(ARG_VISIBLE)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_PAGE_LONG)
				.withDescription(res.get("hlp.page"))
				.hasArg()
				.withType(Number.class)
				.withArgName("pageNumber")
				.create(ARG_PAGE)
		);
		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.posLLX"))
				.hasArg()
				.withType(Number.class)
				.withArgName("position")
				.create(ARG_POS_LLX)
		);
		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.posLLY"))
				.hasArg()
				.withType(Number.class)
				.withArgName("position")
				.create(ARG_POS_LLY)
		);
		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.posURX"))
				.hasArg()
				.withType(Number.class)
				.withArgName("position")
				.create(ARG_POS_URX)
		);
		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.posURY"))
				.hasArg()
				.withType(Number.class)
				.withArgName("position")
				.create(ARG_POS_URY)
		);
		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.imgPath"))
				.withLongOpt(ARG_IMG_PATH)
				.hasArg()
				.withArgName("file")
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.bgPath"))
				.withLongOpt(ARG_BG_PATH)
				.hasArg()
				.withArgName("file")
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.bgScale"))
				.withLongOpt(ARG_BG_SCALE)
				.hasArg()
				.withType(Number.class)
				.withArgName("scale")
				.create()
		);

		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.renderMode", getEnumValues(RenderMode.values())))
				.withLongOpt(ARG_RENDER_MODE)
				.hasArg()
				.withArgName("mode")
				.create()
		);

		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.l2Text"))
				.withLongOpt(ARG_L2_TEXT)
				.hasArg()
				.withArgName("text")
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withDescription(res.get("hlp.l4Text"))
				.withLongOpt(ARG_L4_TEXT)
				.hasArg()
				.withArgName("text")
				.create()
		);

	}

	/**
	 * @return the outPrefix
	 */
	public String getOutPrefix() {
		if (outPrefix==null) outPrefix="";
		return outPrefix;
	}

	/**
	 * Return comma separated names from enum values array.
	 * @param aEnumVals
	 * @return
	 */
	private static String[] getEnumValues(Enum<?>[] aEnumVals) {
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
		return new String[] { tmpResult.toString() };
	}

	/**
	 * @param outPrefix the outPrefix to set
	 */
	public void setOutPrefix(String outPrefix) {
		this.outPrefix = outPrefix;
	}

	/**
	 * @return the outSuffix
	 */
	public String getOutSuffix() {
		if (outSuffix==null) outSuffix="";
		return outSuffix;
	}

	/**
	 * @param outSuffix the outSuffix to set
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
	 * @param files the files to set
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
	 * @param printHelp the printHelp to set
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
	 * @param printVersion the printVersion to set
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
	 * @param listKeyStores the listKeyStores to set
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
	 * @param listKeys the listKeys to set
	 */
	public void setListKeys(boolean listKeys) {
		this.listKeys = listKeys;
	}

	/**
	 * Returns output path including tailing slash character
	 * @return the outPath
	 */
	public String getOutPath() {
		String tmpResult;
		if (StringUtils.isEmpty(outPath)) {
			tmpResult = "./";
		} else {
			tmpResult = outPath.replaceAll("\\", "/");
			if (tmpResult.endsWith("/")) {
				tmpResult = tmpResult + "/";
			}
		}
		return tmpResult;
	}

	/**
	 * @param outPath the outPath to set
	 */
	public void setOutPath(String outPath) {
		this.outPath = outPath;
	}

}
