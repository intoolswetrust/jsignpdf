package net.sf.jsignpdf.gui.action;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import net.sf.jsignpdf.ResourceBundleBean;
import net.sf.jsignpdf.ResourceProvider;
import net.sf.jsignpdf.StringUtils;
import net.sf.jsignpdf.gui.GuiConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Action implementation with translation support.
 * 
 * @author Josef Cacek
 */
public abstract class I18nAbstractAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	protected ResourceBundleBean res = ResourceProvider.getBundleBean(GuiConstants.RESOURCE_BUNDLE_GUI);

	private final Logger logger = LoggerFactory.getLogger(I18nAbstractAction.class);

	public I18nAbstractAction() {
		res.addPropertyChangeListener(new ResourcesChangedHandler());
	}

	public I18nAbstractAction(String name, String shortDescripton, String iconPath) {
		this();
		if (StringUtils.hasLength(name)) {
			putValue(Action.NAME, name);
		}
		if (StringUtils.hasLength(shortDescripton)) {
			putValue(Action.SHORT_DESCRIPTION, shortDescripton);
		}
		if (StringUtils.hasLength(shortDescripton)) {
			String path = translate(iconPath);
			ImageIcon icon = new ImageIcon(getClass().getResource(path));
			putValue(Action.SMALL_ICON, icon);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.AbstractAction#getValue(java.lang.String)
	 */
	@Override
	public Object getValue(String key) {
		Object tmpVal = super.getValue(key);
		if (tmpVal instanceof String) {
			tmpVal = translate((String) tmpVal);
		}
		return tmpVal;
	}

	protected String translate(String aKey) {
		String tmpResult = aKey;
		try {
			tmpResult = res.get(aKey);
		} catch (Exception e) {
			logger.debug("Can't translate key {}", aKey);
		}
		return tmpResult;
	}

	/**
	 * This class observes language change in ResourceBundleBean and propagate
	 * the changes to components, which uses the action.
	 * 
	 * @author Josef Cacek
	 */
	private final class ResourcesChangedHandler implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			firePropertyChange(null, null, null);
		}
	}

}
