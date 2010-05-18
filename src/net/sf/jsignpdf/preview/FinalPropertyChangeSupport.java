package net.sf.jsignpdf.preview;

import java.beans.PropertyChangeSupport;

/**
 * Final subclass of {@link PropertyChangeSupport}. It should bring better
 * performance.
 * 
 * @author Josef Cacek
 */
public final class FinalPropertyChangeSupport extends PropertyChangeSupport {

	// Serialization version ID
	static final long serialVersionUID = 1L;

	/**
	 * Constructs a FinalPropertyChangeSupport object.
	 * 
	 * @param sourceBean
	 *            The bean to be given as the source for any events.
	 */
	public FinalPropertyChangeSupport(final Object sourceBean) {
		super(sourceBean);
	}

}