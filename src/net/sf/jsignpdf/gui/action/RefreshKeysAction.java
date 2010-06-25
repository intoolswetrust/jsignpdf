package net.sf.jsignpdf.gui.action;

import java.awt.event.ActionEvent;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;

import net.sf.jsignpdf.KeyStoreUtils;
import net.sf.jsignpdf.SignerOptions;
import net.sf.jsignpdf.gui.GuiConstants;
import net.sf.jsignpdf.gui.SignerOptionsModel;

import com.jgoodies.binding.adapter.ComboBoxAdapter;

/**
 * Action which (Re)Loads key aliases and maps combobox to keyAlias property.
 * 
 * @author Josef Cacek
 */
public class RefreshKeysAction extends I18nAbstractAction {

	private static final long serialVersionUID = 1L;

	private DefaultListModel keys = new DefaultListModel();
	private ComboBoxAdapter<String> comboBoxAdapter;
	private SignerOptionsModel optionsModel;

	/**
	 * Ctor - parameters has to be not-null.
	 * 
	 * @param anOptions
	 *            signer options
	 */
	public RefreshKeysAction(SignerOptionsModel anOptions) {
		super(GuiConstants.Actions.REFRESHKEY_NAME, GuiConstants.Actions.REFRESHKEY_TOOLTIP,
				GuiConstants.Actions.REFRESHKEY_ICON);
		optionsModel = anOptions;
		comboBoxAdapter = new ComboBoxAdapter<String>(keys, optionsModel.getModel(SignerOptions.PROPERTY_KEY_ALIAS));
	}

	/**
	 * Loads aliases to combobox model.
	 */
	public void actionPerformed(ActionEvent e) {
		keys.removeAllElements();
		for (String alias : KeyStoreUtils.getKeyAliases(optionsModel.getBean())) {
			keys.addElement(alias);
		}
	}

	public ComboBoxModel getComboboBoxModel() {
		return comboBoxAdapter;
	}
}
