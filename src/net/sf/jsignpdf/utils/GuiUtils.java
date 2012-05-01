/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 * 
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 * 
 * Contributor(s): Josef Cacek.
 * 
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
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
	 * @param component
	 *            the component to be centered
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
