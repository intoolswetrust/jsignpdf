package net.sf.jsignpdf.gui;

import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import net.sf.jsignpdf.CertificationLevel;
import net.sf.jsignpdf.KeyStoreUtils;
import net.sf.jsignpdf.PrintRight;
import net.sf.jsignpdf.SignerFileChooser;
import net.sf.jsignpdf.SignerOptions;
import net.sf.jsignpdf.gui.action.ChooseFileAction;
import net.sf.jsignpdf.gui.action.RefreshKeysAction;

import com.jeta.forms.components.panel.FormPanel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.adapter.ComboBoxAdapter;
import com.jgoodies.binding.adapter.SpinnerAdapterFactory;
import com.jgoodies.binding.beans.PropertyConnector;

public class SignerView {

	private SignerOptionsModel presentationModel;

	private FormPanel panel;

	public SignerView(SignerOptionsModel signerOptionsModel) {
		presentationModel = signerOptionsModel;
	}

	// Initialization *********************************************************

	/**
	 * intializes mapping of the UI components.
	 */
	private void initComponents() {
		final SignerOptions tmpOpts = presentationModel.getBean();

		// Input
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.INPUT_PDFPATH_TEXTFIELD),
				presentationModel.getModel(SignerOptions.PROPERTY_IN_FILE));

		ChooseFileAction tmpChooseFileAction = new ChooseFileAction(SignerFileChooser.FILEFILTER_PDF, true);
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.INPUT_PDFPATH_TEXTFIELD),
				tmpChooseFileAction.getPathValueHolder());
		AbstractButton tmpButton = panel.getButton(GuiConstants.Components.INPUT_CHOOSEFILE_BUTTON);
		tmpButton.setAction(tmpChooseFileAction);
		tmpButton.setText(null);

		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.INPUT_OWNERPWD_PASSWORD),
				presentationModel.getModel(SignerOptions.PROPERTY_IN_FILE_PWD));

		// Output
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.OUTPUT_PDFPATH_TEXTFIELD),
				presentationModel.getModel(SignerOptions.PROPERTY_OUT_FILE));

		tmpChooseFileAction = new ChooseFileAction(SignerFileChooser.FILEFILTER_PDF, false);
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.OUTPUT_PDFPATH_TEXTFIELD),
				tmpChooseFileAction.getPathValueHolder());
		tmpButton = panel.getButton(GuiConstants.Components.OUTPUT_CHOOSEFILE_BUTTON);
		tmpButton.setAction(tmpChooseFileAction);
		tmpButton.setText(null);

		// Keystore
		final ComboBoxAdapter<String> ksTypeComboBoxAdapter = new ComboBoxAdapter<String>(KeyStoreUtils.getKeyStores(),
				presentationModel.getModel(SignerOptions.PROPERTY_KEYSTORE_TYPE));
		panel.getComboBox(GuiConstants.Components.KEYSTORE_TYPE_COMBOBOX).setModel(ksTypeComboBoxAdapter);

		tmpChooseFileAction = new ChooseFileAction(null, true);
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.KEYSTORE_PATH_TEXTFIELD),
				tmpChooseFileAction.getPathValueHolder());
		tmpButton = panel.getButton(GuiConstants.Components.KEYSTORE_CHOOSEFILE_BUTTON);
		tmpButton.setAction(tmpChooseFileAction);
		tmpButton.setText(null);

		// Key
		final RefreshKeysAction tmpRefreshKeysAction = new RefreshKeysAction(presentationModel);
		tmpButton = panel.getButton(GuiConstants.Components.KEY_REFRESHALIASES_BUTTON);
		tmpButton.setAction(tmpRefreshKeysAction);
		tmpButton.setText(null);

		panel.getComboBox(GuiConstants.Components.KEY_ALIAS_COMBOBOX).setModel(
				tmpRefreshKeysAction.getComboboBoxModel());
		panel.getComboBox(GuiConstants.Components.KEY_ALIAS_COMBOBOX).setEditable(true);

		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.KEY_PWD_PASSWORD), presentationModel
				.getModel(SignerOptions.PROPERTY_KEY_PASSWD));

		Bindings.bind(panel.getCheckBox(GuiConstants.Components.KEY_OCSP_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_OCSP_ENABLED));

		// Signature properties
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.PROPERTIES_LOCATION_TEXTFIELD),
				presentationModel.getModel(SignerOptions.PROPERTY_LOCATION));
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.PROPERTIES_REASON_TEXTFIELD),
				presentationModel.getModel(SignerOptions.PROPERTY_REASON));
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.PROPERTIES_CONTACT_TEXTFIELD),
				presentationModel.getModel(SignerOptions.PROPERTY_CONTACT));
		DefaultComboBoxModel tmpModel = new DefaultComboBoxModel(CertificationLevel.values());
		panel.getComboBox(GuiConstants.Components.PROPERTIES_CERTIFICATE_COMBOBOX).setModel(
				new ComboBoxAdapter<CertificationLevel>(tmpModel, presentationModel
						.getModel(SignerOptions.PROPERTY_CERT_LEVEL)));
		Bindings.bind(panel.getCheckBox(GuiConstants.Components.PROPERTIES_APPEND_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_APPEND));

		// Timestamps
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.TIMESTAMP_TSAURL_TEXTFIELD),
				presentationModel.getModel(SignerOptions.PROPERTY_TSA_URL));
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.TIMESTAMP_USER_TEXTFIELD),
				presentationModel.getModel(SignerOptions.PROPERTY_TSA_USER));
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.TIMESTAMP_PWD_PASSWORD),
				presentationModel.getModel(SignerOptions.PROPERTY_TSA_PWD));

		// Rights
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.RIGHTS_USERPWD_PASSWORD),
				presentationModel.getModel(SignerOptions.PROPERTY_OUT_USER_PWD));
		Bindings.bind((JTextField) panel.getTextComponent(GuiConstants.Components.RIGHTS_OWNERPWD_PASSWORD),
				presentationModel.getModel(SignerOptions.PROPERTY_OUT_OWNER_PWD));
		Bindings.bind(panel.getCheckBox(GuiConstants.Components.RIGHTS_COPY_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_RIGHT_COPY));
		Bindings.bind(panel.getCheckBox(GuiConstants.Components.RIGHTS_ASSEMBLY_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_RIGHT_ASSEMBLY));
		Bindings.bind(panel.getCheckBox(GuiConstants.Components.RIGHTS_FILLIN_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_RIGHT_FILLIN));
		Bindings.bind(panel.getCheckBox(GuiConstants.Components.RIGHTS_SCREENREADERS_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_RIGHT_SCREENREADERS));
		Bindings.bind(panel.getCheckBox(GuiConstants.Components.RIGHTS_MODIFYANNOTATIONS_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_RIGHT_MODIFYANNOT));
		Bindings.bind(panel.getCheckBox(GuiConstants.Components.RIGHTS_MODIFYCONTENTS_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_RIGHT_MODIFYCONT));
		tmpModel = new DefaultComboBoxModel(PrintRight.values());
		panel.getComboBox(GuiConstants.Components.RIGHTS_PRINTING_COMBOBOX).setModel(
				new ComboBoxAdapter<CertificationLevel>(tmpModel, presentationModel
						.getModel(SignerOptions.PROPERTY_RIGHT_PRINTING)));

		// Visible signature
		Bindings.bind(panel.getCheckBox(GuiConstants.Components.VISIBLE_ENABLED_CHECKBOX), presentationModel
				.getModel(SignerOptions.PROPERTY_VISIBLE_ENABLED));
		panel.getSpinner(GuiConstants.Components.VISIBLE_PAGE_SPINNER).setModel(
				SpinnerAdapterFactory.createNumberAdapter(presentationModel.getModel(SignerOptions.PROPERTY_PAGE), 1, // defaultValue
						1, // minValue
						1, // maxValue
						1)); // step
		// FIXME - doesn't work - position properties are floats
		Bindings.bind(panel.getTextField(GuiConstants.Components.VISIBLE_LLX_TEXTFIELD), presentationModel
				.getModel(SignerOptions.PROPERTY_POSITION_LLX));
		Bindings.bind(panel.getTextField(GuiConstants.Components.VISIBLE_LLY_TEXTFIELD), presentationModel
				.getModel(SignerOptions.PROPERTY_POSITION_LLY));
		Bindings.bind(panel.getTextField(GuiConstants.Components.VISIBLE_URX_TEXTFIELD), presentationModel
				.getModel(SignerOptions.PROPERTY_POSITION_URX));
		Bindings.bind(panel.getTextField(GuiConstants.Components.VISIBLE_URY_TEXTFIELD), presentationModel
				.getModel(SignerOptions.PROPERTY_POSITION_URY));
	}

	/**
	 * initializes additional bindings necessary for GUI updates
	 */
	private void initEventHandling() {
		PropertyConnector
				.connect(presentationModel, SignerOptions.PROPERTY_VISIBLE_ENABLED,
						panel.getComponentByName(GuiConstants.Components.VISIBLE_PAGE_SPINNER),
						GuiConstants.Properties.ENABLED).updateProperty2();
		PropertyConnector.connect(presentationModel, SignerOptions.PROPERTY_VISIBLE_ENABLED,
				panel.getComponentByName(GuiConstants.Components.VISIBLE_LLX_TEXTFIELD),
				GuiConstants.Properties.ENABLED).updateProperty2();
		PropertyConnector.connect(presentationModel, SignerOptions.PROPERTY_VISIBLE_ENABLED,
				panel.getComponentByName(GuiConstants.Components.VISIBLE_LLY_TEXTFIELD),
				GuiConstants.Properties.ENABLED).updateProperty2();
		PropertyConnector.connect(presentationModel, SignerOptions.PROPERTY_VISIBLE_ENABLED,
				panel.getComponentByName(GuiConstants.Components.VISIBLE_URX_TEXTFIELD),
				GuiConstants.Properties.ENABLED).updateProperty2();
		PropertyConnector.connect(presentationModel, SignerOptions.PROPERTY_VISIBLE_ENABLED,
				panel.getComponentByName(GuiConstants.Components.VISIBLE_URY_TEXTFIELD),
				GuiConstants.Properties.ENABLED).updateProperty2();
	}

	// Building ***************************************************************

	/**
	 * Builds and returns the editor panel.
	 * 
	 * @return the built panel
	 */
	public JComponent buildPanel() {
		panel = new FormPanel(GuiConstants.DEFAULT_FORM_PATH);
		initComponents();
		initEventHandling();

		return panel;
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setSize(600, 400);
		frame.setLocation(100, 100);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		SignerView signerView = new SignerView(new SignerOptionsModel(new SignerOptions()));
		frame.getContentPane().add(signerView.buildPanel());
		frame.pack();
		frame.setVisible(true);
	}
}
