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
package net.sf.jsignpdf.types;

import java.awt.Dimension;
import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import net.sf.jsignpdf.preview.FinalPropertyChangeSupport;

/**
 * Rectangle implementation based on relative positions.
 * 
 * @author Josef Cacek
 */
public class RelRect {

	public static final String PROPERTY_COORDS = "coords";

	private final Float[] coords = new Float[] { 0f, 0f, 1f, 1f };
	private Dimension imageSize = new Dimension(1, 1);

	private FinalPropertyChangeSupport pcs = new FinalPropertyChangeSupport(this);

	/**
	 * Return if the rectangle is valid (i.e. both startPoint and endPoint are
	 * set correctly)
	 * 
	 * @return true if valid, false otherwise
	 */
	public boolean isValid() {
		for (Float tmpCoord : coords) {
			if (tmpCoord == null)
				return false;
		}
		return true;
	}

	/**
	 * Returns startPoint coordinates in the image.
	 * 
	 * @return
	 */
	public int[] getP1() {
		return makeImgPoint(0);
	}

	/**
	 * Returns endPoint coordinates in the image.
	 * 
	 * @return
	 */
	public int[] getP2() {
		return makeImgPoint(2);
	}

	/**
	 * Sets the start Point
	 * 
	 * @param aPoint
	 *            the startPoint to set
	 */
	public void setStartPoint(Point aPoint) {
		setRelPoint(aPoint, 0);
	}

	/**
	 * Returns relative coordinates of the startPoint [x1,y1] and endPoint
	 * [x2,y2] as Float array [x1,y1,x2,y2]
	 * 
	 * @return
	 */
	public Float[] getCoords() {
		return coords;
	}

	/**
	 * Sets the end Point
	 * 
	 * @param aPoint
	 *            the endPoint to set
	 */
	public void setEndPoint(Point aPoint) {
		setRelPoint(aPoint, 2);
	}

	/**
	 * Sets new dimension. Minimal values for both sizes (width, height) are
	 * [1,1]
	 * 
	 * @param newWidth
	 * @param newHeight
	 */
	public void setImgSize(int newWidth, int newHeight) {
		if (newWidth < 1)
			newWidth = 1;
		if (newHeight < 1)
			newHeight = 1;

		imageSize.setSize(newWidth, newHeight);
	}

	/**
	 * Adds propertyChangeListener for this bean
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Removes propertyChangeListener from this bean
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}

	/**
	 * Calculates point in the image.
	 * 
	 * @param coordsOffset
	 *            use 0 for startPoint and 2 for endPoint.
	 * @return coordinates [x,y] of a point in the image
	 */
	private int[] makeImgPoint(int coordsOffset) {
		int x = Math.round(coords[coordsOffset] * imageSize.width);
		int y = imageSize.height - Math.round(coords[coordsOffset + 1] * imageSize.height);
		return new int[] { x, y };
	}

	/**
	 * Sets coordinates of a relative point (startPoint or endPoint) based on
	 * given Point in the image.
	 * 
	 * @param point
	 *            point with coordinates in the image
	 * @param offset
	 *            use 0 for startPoint and 2 for endPoint.
	 */
	private void setRelPoint(Point point, int offset) {
		final Float[] oldVal = Arrays.copyOf(coords, coords.length);
		if (point == null) {
			coords[offset] = null;
			coords[offset + 1] = null;
		} else {
			coords[offset] = (float) point.x / imageSize.width;
			coords[offset + 1] = 1f - (float) point.y / imageSize.height;
		}
		pcs.firePropertyChange(PROPERTY_COORDS, oldVal, coords);
	}

}
