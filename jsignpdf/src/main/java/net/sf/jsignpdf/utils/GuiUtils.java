package net.sf.jsignpdf.utils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 * Helper class for swing GUI (window, component) manipulation
 * 
 * @author Josef Cacek
 */
public class GuiUtils {

    /**
     * Locates the given component on the screen's center.
     * 
     * @param component the component to be centered
     */
    public static void center(final Component component) {
        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        final int screenWidth = gd.getDisplayMode().getWidth();
        final int screenHeight = gd.getDisplayMode().getHeight();
        final Dimension paneSize = component.getSize();
        component.setLocation((screenWidth - paneSize.width) / 2, (int) ((screenHeight - paneSize.height) * 0.45));
    }

    /**
     * Sets component size to cca 80% of screen size and centers window.
     * 
     * @param component
     */
    public static void resizeAndCenter(final Component component) {
        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        final int screenWidth = gd.getDisplayMode().getWidth();
        final int screenHeight = gd.getDisplayMode().getHeight();
        component.setSize((int) (screenWidth * 0.8), (int) (screenHeight * 0.8));
        center(component);
    }

}
