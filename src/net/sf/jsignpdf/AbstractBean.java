package net.sf.jsignpdf;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Base class for for Domain objects add JavaBeans PropertyChangeSupport
 * 
 * @author Josef Cacek
 */
public abstract class AbstractBean {

	private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(final PropertyChangeListener aListener) {
		changeSupport.addPropertyChangeListener(aListener);
	}

	public void removePropertyChangeListener(final PropertyChangeListener aListener) {
		changeSupport.removePropertyChangeListener(aListener);
	}

	protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
		changeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}

	protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
		changeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}

	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		changeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}
}
