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

	private String[] files;

	private boolean printHelp = true;
	private boolean printVersion;
	private boolean listKeyStores;
	private boolean listKeys;

	//parse command line using CLI here
	public void loadCmdLine(final String[] anArgs) throws ParseException {
		if (anArgs == null) return;
		setPrintWriter(new PrintWriter(System.out));
		setAdvanced(true);

		//TODO which parser to use here? Basic, Posix, GNU?
		// create the command line parser
		final CommandLineParser parser = new PosixParser();
		// parse the command line arguments
		final CommandLine line = parser.parse(OPTS, anArgs);

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
		setKsPasswd(line.getOptionValue(ARG_KS_TYPE));
		setKsType(line.getOptionValue(ARG_KS_TYPE));
		setKeyAlias(line.getOptionValue(ARG_KEY_ALIAS));
		setKeyPasswd(line.getOptionValue(ARG_KEY_PWD));
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
		OptionBuilder.create();
		//commands
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_HELP_LONG)
				.create(ARG_VERSION)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_VERSION_LONG)
				.create(ARG_VERSION)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_LIST_KS_TYPES_LONG)
				.create(ARG_LIST_KS_TYPES)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_LIST_KEYS_LONG)
				.create(ARG_LIST_KEYS)
		);

		//keystore and key configuration options
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KS_TYPE_LONG)
				.hasArg()
				.withArgName("type")
				.create(ARG_KS_TYPE)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KS_FILE_LONG)
				.hasArg()
				.withArgName("file")
				.create(ARG_KS_FILE)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KS_PWD_LONG)
				.hasArg()
				.withArgName("password")
				.create(ARG_KS_PWD)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KEY_ALIAS_LONG)
				.hasArg()
				.withArgName("alias")
				.create(ARG_KEY_ALIAS)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_KEY_PWD_LONG)
				.hasArg()
				.withArgName("password")
				.create(ARG_KEY_PWD)
		);

		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_OPREFIX_LONG)
				.hasArg()
				.withArgName("prefix")
				.create(ARG_OPREFIX)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_OSUFFIX_LONG)
				.hasArg()
				.withArgName("suffix")
				.create(ARG_OSUFFIX)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_REASON_LONG)
				.hasArg()
				.withArgName("reason")
				.create(ARG_REASON)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_LOCATION_LONG)
				.hasArg()
				.withArgName("location")
				.create(ARG_LOCATION)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_APPEND_LONG)
				.create(ARG_APPEND)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_CERT_LEVEL_LONG)
				.hasArg()
				.withArgName("level")
				.create(ARG_CERT_LEVEL)
		);

		//Encryption and rights
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_ENCRYPTED_LONG)
				.create(ARG_ENCRYPTED)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_PWD_OWNER_LONG)
				.hasArg()
				.withArgName("password")
				.create(ARG_PWD_OWNER)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_PWD_USER_LONG)
				.hasArg()
				.withArgName("password")
				.create(ARG_PWD_USER)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_RIGHT_PRINT_LONG)
				.hasArg()
				.withArgName("right")
				.create(ARG_RIGHT_PRINT)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_COPY_LONG)
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_ASSEMBLY_LONG)
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_FILL_LONG)
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_SCREEN_READERS_LONG)
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_MODIFY_ANNOT_LONG)
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_DISABLE_MODIFY_CONTENT_LONG)
				.create()
		);

		//visible signature options
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_VISIBLE_LONG)
				.create(ARG_VISIBLE)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_PAGE_LONG)
				.hasArg()
				.withType(Number.class)
				.withArgName("pageNumber")
				.create(ARG_PAGE)
		);
		OPTS.addOption(
				OptionBuilder
				.hasArg()
				.withType(Number.class)
				.withArgName("position")
				.create(ARG_POS_LLX)
		);
		OPTS.addOption(
				OptionBuilder
				.hasArg()
				.withType(Number.class)
				.withArgName("position")
				.create(ARG_POS_LLY)
		);
		OPTS.addOption(
				OptionBuilder
				.hasArg()
				.withType(Number.class)
				.withArgName("position")
				.create(ARG_POS_URX)
		);
		OPTS.addOption(
				OptionBuilder
				.hasArg()
				.withType(Number.class)
				.withArgName("position")
				.create(ARG_POS_URY)
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_IMG_PATH)
				.hasArg()
				.withArgName("file")
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_BG_PATH)
				.hasArg()
				.withArgName("file")
				.create()
		);
		OPTS.addOption(
				OptionBuilder
				.hasArg()
				.withType(Number.class)
				.withArgName("scale")
				.create(ARG_BG_SCALE)
		);

		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_RENDER_MODE)
				.hasArg()
				.withArgName("mode")
				.create()
		);

		OPTS.addOption(
				OptionBuilder
				.withLongOpt(ARG_L2_TEXT)
				.hasArg()
				.withArgName("text")
				.create()
		);
		OPTS.addOption(
				OptionBuilder
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

}
