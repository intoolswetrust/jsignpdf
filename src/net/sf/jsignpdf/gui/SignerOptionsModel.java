package net.sf.jsignpdf.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.sf.jsignpdf.SignerOptions;
import net.sf.jsignpdf.StringUtils;

import com.jgoodies.binding.PresentationModel;

public class SignerOptionsModel extends PresentationModel<SignerOptions> {

	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_OUT_ENCRYPTED = "outEncrypted";

	public SignerOptionsModel(SignerOptions bean) {
		super(bean);
		initEventHandling();
	}

	public boolean isOutEncrypted() {
		return StringUtils.hasLength(getBean().getPdfOwnerPwd());
	}

	// Initialization *********************************************************

	/**
	 * Listens to changes in all properties of the current Order and to Order
	 * changes.
	 */
	private void initEventHandling() {
		getModel(SignerOptions.PROPERTY_OUT_OWNER_PWD).addValueChangeListener(new OutOwnerPwdChangeHandler());
	}

	/**
	 * Handles changes in the <em>outOwnerPwd</em> property. Just notifies
	 * listeners about a change in the aggregated property
	 */
	private final class OutOwnerPwdChangeHandler implements PropertyChangeListener {

		public void propertyChange(PropertyChangeEvent evt) {
			firePropertyChange(PROPERTY_OUT_ENCRYPTED, null, Boolean.valueOf(isOutEncrypted()));
		}
	}
}
