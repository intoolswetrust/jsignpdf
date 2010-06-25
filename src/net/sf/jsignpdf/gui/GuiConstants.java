package net.sf.jsignpdf.gui;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants used in GUI part of application
 * 
 * @author Josef Cacek
 */
public class GuiConstants {

	/**
	 * Path to default form design
	 */
	public static final String DEFAULT_FORM_PATH = "net/sf/jsignpdf/forms/2010a.jfrm";

	/**
	 * BaseName of GUI resource bundle
	 */
	public static final String RESOURCE_BUNDLE_GUI = "net.sf.jsignpdf.translations.gui";

	public static class Actions {

		public static final String CHOOSEFILE_NAME = "action.choosefile.name";
		public static final String CHOOSEFILE_TOOLTIP = "action.refreshkey.tooltip";
		public static final String CHOOSEFILE_ICON = "/net/sf/jsignpdf/fileopen16.png";

		public static final String REFRESHKEY_NAME = "action.refreshkey.name";
		public static final String REFRESHKEY_TOOLTIP = "action.refreshkey.tooltip";
		public static final String REFRESHKEY_ICON = "/net/sf/jsignpdf/refresh16.png";

		private Actions() {
			// don't want to initialize this class
		}
	}

	/**
	 * Names of GUI properties
	 */
	public static class Properties {

		public static final String ENABLED = "enabled";

		private Properties() {
			// don't want to initialize this class
		}
	}
	
	/**
	 * Contains constants with component names on the JSignPdf form
	 * 
	 * @author Josef Cacek
	 */
	public static class Components {

		public static final String SIGNPDF_BUTTON = "signpdf.button";

		public static final String INPUT_HEADER = "input.header";
		public static final String INPUT_PDFPATH_LABEL = "input.pdfpath.label";
		public static final String INPUT_PDFPATH_TEXTFIELD = "input.pdfpath.textfield";
		public static final String INPUT_CHOOSEFILE_BUTTON = "input.choosefile.button";
		public static final String INPUT_ENCRYPTED_CHECKBOX = "input.encrypted.checkbox";
		public static final String INPUT_OWNERPWD_LABEL = "input.ownerpwd.label";
		public static final String INPUT_OWNERPWD_PASSWORD = "input.ownerpwd.password";

		public static final String OUTPUT_HEADER = "output.header";
		public static final String OUTPUT_PDFPATH_LABEL = "output.pdfpath.label";
		public static final String OUTPUT_PDFPATH_TEXTFIELD = "output.pdfpath.textfield";
		public static final String OUTPUT_CHOOSEFILE_BUTTON = "output.choosefile.button";

		public static final String KEYSTORE_HEADER = "keystore.header";
		public static final String KEYSTORE_TYPE_LABEL = "keystore.type.label";
		public static final String KEYSTORE_TYPE_COMBOBOX = "keystore.type.combobox";
		public static final String KEYSTORE_PWD_LABEL = "keystore.pwd.label";
		public static final String KEYSTORE_PWD_PASSWORD = "keystore.pwd.password";
		public static final String KEYSTORE_PATH_LABEL = "keystore.path.label";
		public static final String KEYSTORE_PATH_TEXTFIELD = "keystore.path.textfield";
		public static final String KEYSTORE_CHOOSEFILE_BUTTON = "keystore.choosefile.button";

		public static final String KEY_HEADER = "key.header";
		public static final String KEY_ALIAS_LABEL = "key.alias.label";
		public static final String KEY_ALIAS_COMBOBOX = "key.alias.combobox";
		public static final String KEY_REFRESHALIASES_BUTTON = "key.refreshaliases.button";
		public static final String KEY_PWD_LABEL = "key.pwd.label";
		public static final String KEY_PWD_PASSWORD = "key.pwd.password";
		public static final String KEY_OCSP_CHECKBOX = "key.ocsp.checkbox";

		public static final String PROPERTIES_HEADER = "properties.header";
		public static final String PROPERTIES_LOCATION_LABEL = "properties.location.label";
		public static final String PROPERTIES_LOCATION_TEXTFIELD = "properties.location.textfield";
		public static final String PROPERTIES_REASON_LABEL = "properties.reason.label";
		public static final String PROPERTIES_REASON_TEXTFIELD = "properties.reason.textfield";
		public static final String PROPERTIES_CONTACT_LABEL = "properties.contact.label";
		public static final String PROPERTIES_CONTACT_TEXTFIELD = "properties.contact.textfield";
		public static final String PROPERTIES_CERTIFICATE_LABEL = "properties.certificate.label";
		public static final String PROPERTIES_CERTIFICATE_COMBOBOX = "properties.certificate.combobox";
		public static final String PROPERTIES_APPEND_CHECKBOX = "properties.append.checkbox";

		public static final String TIMESTAMP_HEADER = "timestamp.header";
		public static final String TIMESTAMP_ENABLED_CHECKBOX = "timestamp.enabled.checkbox";
		public static final String TIMESTAMP_TSAURL_LABEL = "timestamp.tsaurl.label";
		public static final String TIMESTAMP_TSAURL_TEXTFIELD = "timestamp.tsaurl.textfield";
		public static final String TIMESTAMP_USER_LABEL = "timestamp.user.label";
		public static final String TIMESTAMP_USER_TEXTFIELD = "timestamp.user.textfield";
		public static final String TIMESTAMP_PWD_LABEL = "timestamp.pwd.label";
		public static final String TIMESTAMP_PWD_PASSWORD = "timestamp.pwd.password";

		public static final String RIGHTS_HEADER = "rights.header";
		public static final String RIGHTS_ENABLED_CHECKBOX = "rights.enabled.checkbox";
		public static final String RIGHTS_OWNERPWD_LABEL = "rights.ownerpwd.label";
		public static final String RIGHTS_OWNERPWD_PASSWORD = "rights.ownerpwd.password";
		public static final String RIGHTS_USERPWD_LABEL = "rights.userpwd.label";
		public static final String RIGHTS_USERPWD_PASSWORD = "rights.userpwd.password";
		public static final String RIGHTS_RIGHTS_LABEL = "rights.rights.label";
		public static final String RIGHTS_COPY_CHECKBOX = "rights.copy.checkbox";
		public static final String RIGHTS_ASSEMBLY_CHECKBOX = "rights.assembly.checkbox";
		public static final String RIGHTS_FILLIN_CHECKBOX = "rights.fillin.checkbox";
		public static final String RIGHTS_SCREENREADERS_CHECKBOX = "rights.screenreaders.checkbox";
		public static final String RIGHTS_MODIFYANNOTATIONS_CHECKBOX = "rights.modifyannotations.checkbox";
		public static final String RIGHTS_MODIFYCONTENTS_CHECKBOX = "rights.modifycontents.checkbox";
		public static final String RIGHTS_PRINTING_LABEL = "rights.printing.label";
		public static final String RIGHTS_PRINTING_COMBOBOX = "rights.printing.combobox";

		public static final String VISIBLE_HEADER = "visible.header";
		public static final String VISIBLE_ENABLED_CHECKBOX = "visible.enabled.checkbox";
		public static final String VISIBLE_PAGE_LABEL = "visible.page.label";
		public static final String VISIBLE_PAGE_SPINNER = "visible.page.spinner";
		public static final String VISIBLE_LLX_LABEL = "visible.llx.label";
		public static final String VISIBLE_LLX_TEXTFIELD = "visible.llx.textfield";
		public static final String VISIBLE_LLY_LABEL = "visible.lly.label";
		public static final String VISIBLE_LLY_TEXTFIELD = "visible.lly.textfield";
		public static final String VISIBLE_URX_LABEL = "visible.urx.label";
		public static final String VISIBLE_URX_TEXTFIELD = "visible.urx.textfield";
		public static final String VISIBLE_URY_LABEL = "visible.ury.label";
		public static final String VISIBLE_URY_TEXTFIELD = "visible.ury.textfield";
		public static final String VISIBLE_DISPLAYTYPE_LABEL = "visible.displaytype.label";
		public static final String VISIBLE_DISPLAYTYPE_COMBOBOX = "visible.displaytype.combobox";
		public static final String VISIBLE_L2TEXT_LABEL = "visible.l2text.label";
		public static final String VISIBLE_L2TEXT_TEXTAREA = "visible.l2text.textarea";
		public static final String VISIBLE_L2TEXT_CHECKBOX = "visible.l2text.checkbox";
		public static final String VISIBLE_L4TEXT_LABEL = "visible.l4text.label";
		public static final String VISIBLE_L4TEXT_TEXTAREA = "visible.l4text.textarea";
		public static final String VISIBLE_L4TEXT_CHECKBOX = "visible.l4text.checkbox";
		public static final String VISIBLE_FONTSIZE_LABEL = "visible.fontsize.label";
		public static final String VISIBLE_FONTSIZE_SPINNER = "visible.fontsize.spinner";
		public static final String VISIBLE_BGIMGSCALE_LABEL = "visible.bgimgscale.label";
		public static final String VISIBLE_BGIMGSCALE_TEXTFIELD = "visible.bgimgscale.textfield";
		public static final String VISIBLE_IMGPATH_LABEL = "visible.imgpath.label";
		public static final String VISIBLE_IMGPATH_TEXTFIELD = "visible.imgpath.textfield";
		public static final String VISIBLE_CHOOSEIMGPATH_BUTTON = "visible.chooseimgpath.button";
		public static final String VISIBLE_BGIMGPATH_LABEL = "visible.bgimgpath.label";
		public static final String VISIBLE_BGIMGPATH_TEXTFIELD = "visible.bgimgpath.textfield";
		public static final String VISIBLE_CHOOSEBGIMGPATH_BUTTON = "visible.choosebgimgpath.button";

		public static final String LOG_HEADER = "log.header";
		public static final String LOG_TEXTAREA = "log.textarea";

		private Components() {
			// we don't want to initialize this class
		}

	}

	/**
	 * Set of component names
	 * 
	 * @see GuiConstants.Components
	 */
	public static final Set<String> COMPONENT_SET;

	private static final Logger logger = LoggerFactory.getLogger(GuiConstants.class);

	static {
		logger.debug("Initializing component names");
		COMPONENT_SET = new HashSet<String>();
		for (Field field : Components.class.getDeclaredFields()) {
			if (field.getType().equals(String.class)) {
				try {
					final String tmpName = (String) field.get(null);
					logger.debug("Adding component {}", tmpName);
					COMPONENT_SET.add(tmpName);
				} catch (IllegalArgumentException e) {
					logger.warn("Can't get value of a field", e);
				} catch (IllegalAccessException e) {
					logger.warn("Can't get value of a field", e);
				}
			}
		}
	}

	private GuiConstants() {
		// we don't want to initialize this class
	}

	// TODO JUnit test for component availbility
	// TODO collection of component names which can be translated?? what about
	// tooltips?

}
