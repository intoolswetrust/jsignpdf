package net.sf.jsignpdf.utils;

import java.awt.Component;
import java.awt.Dimension;

/**
 * Helper class for swing GUI (window, component) manipulation
 * 
 * @author Josef Cacek
 */
public class GuiUtils {

	/**
	 * Locates the given component on the screen's center.
	 * 
	 * @param component
	 *            the component to be centered
	 */
	public static void center(final Component component) {
		final Dimension paneSize = component.getSize();
		final Dimension screenSize = component.getToolkit().getScreenSize();
		component.setLocation((screenSize.width - paneSize.width) / 2,
				(int) ((screenSize.height - paneSize.height) * 0.45));
	}

	/**
	 * Sets component size to cca 80% of screen size and centers window.
	 * 
	 * @param component
	 */
	public static void resizeAndCenter(final Component component) {
		final Dimension screenSize = component.getToolkit().getScreenSize();
		component.setSize((int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8));
		center(component);
	}

}