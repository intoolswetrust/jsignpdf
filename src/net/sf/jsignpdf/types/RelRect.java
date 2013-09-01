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

	/**
	 * Value returned by getters if {@link #isValid()} method returns false
	 */
	public int ERR = -1;

	private final float[] coords = new float[] { 0f, 0f, 1f, 1f };

	private float[] origSize = new float[2];

	private int rotation = 0;

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

	private int getImgWidthRotated() {
		return rotation % 2 == 0 ? imageSize.width : imageSize.height;
	}

	private int getImgHeightRotated() {
		return rotation % 2 == 1 ? imageSize.width : imageSize.height;
	}

	public int[] getP1() {
		return makeImgPoint(0);
	}

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

	public float[] getCoords() {
		return coords;
	}

	// Page (without rotation) 600x400: [50,100; 150,130]
	// rot 0: [50, 400-100, 150, 400-130]
	// rot 1 (90deg): [ 100, 50, 130, 150]
	// rot 2 (180deg): [ 600 - 50, 100, 600-150, 130]
	// rot 3 (270deg): [ 400 - 100, 600 - 50, 400 - 130, 600 - 150 ]

	private int[] makeImgPoint(int coordsOffset) {
		int x = Math.round(coords[coordsOffset] * getImgWidthRotated());
		int y = Math.round(coords[coordsOffset + 1] * getImgHeightRotated());
		if (rotation == 0 || rotation == 3) {
			y = getImgHeightRotated() - y;
		}
		if (rotation == 2 || rotation == 3) {
			x = getImgWidthRotated() - x;
		}
		if (rotation % 2 == 1) {
			int tmp = x;
			x = y;
			y = tmp;
		}
		return new int[] { x, y };
	}

	private void setRelPoint(Point point, int offset) {
		final float[] oldVal = Arrays.copyOf(coords, coords.length);
		if (point == null) {
			coords[offset] = 0f;
			coords[offset + 1] = 0f;
		} else {
			float x = (float) point.x / imageSize.width;
			float y = (float) point.y / imageSize.height;
			if (rotation % 2 == 1) {
				float tmp = x;
				x = y;
				y = tmp;
			}
			if (rotation == 0 || rotation == 3) {
				y = 1f - y;
			}
			if (rotation == 2 || rotation == 3) {
				x = 1f - x;
			}
			coords[offset] = x;
			coords[offset + 1] = y;
		}
		pcs.firePropertyChange(PROPERTY_COORDS, oldVal, coords);
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

	public void setRotation(int degrees) {
		rotation = Math.round(degrees / 90) % 4;
	}

	public void setOrigSize(float x, float y) {
		origSize[0] = x;
		origSize[1] = y;
	}

	@Override
	public String toString() {
		return "RelRect [dimension=" + imageSize + ", rotation=" + rotation + ", coords=" + Arrays.toString(coords)
				+ "]";
	}
}
