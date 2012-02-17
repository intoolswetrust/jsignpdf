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

import net.sf.jsignpdf.preview.FinalPropertyChangeSupport;

/**
 * Rectangle implementation based on relative positions.
 * 
 * @author Josef Cacek
 */
public class RelRect {

	public static final String PROPERTY_START_POINT = "startPoint";
	public static final String PROPERTY_END_POINT = "endPoint";

	/**
	 * Value returned by getters if {@link #isValid()} method returns false
	 */
	public int ERR = -1;

	private FloatPoint startPoint;
	private FloatPoint endPoint;

	private Dimension dimension = new Dimension(1, 1);

	private FinalPropertyChangeSupport pcs = new FinalPropertyChangeSupport(this);

	/**
	 * Return if the rectangle is valid (i.e. both startPoint and endPoint are
	 * set correctly)
	 * 
	 * @return true if valid, false otherwise
	 */
	public boolean isValid() {
		return endPoint != null && startPoint != null;
	}

	/**
	 * Returns absolute position of rectangle's top line in given scale
	 */
	public int getTop() {
		if (!isValid())
			return ERR;
		return Math.round(getRelTop() * dimension.height);
	}

	/**
	 * Returns absolute position of rectangle's bottom line in given scale
	 */
	public int getBottom() {
		if (!isValid())
			return ERR;
		return Math.round(getRelBottom() * dimension.height);
	}

	/**
	 * Returns absolute position of rectangle's left line in given scale
	 */
	public int getLeft() {
		if (!isValid())
			return ERR;
		return Math.round(getRelLeft() * dimension.width);
	}

	/**
	 * Returns absolute position of rectangle's right line in given scale
	 */
	public int getRight() {
		if (!isValid())
			return ERR;
		return Math.round(getRelRight() * dimension.width);
	}

	/**
	 * Returns rectangle width
	 */
	public int getWidth() {
		if (!isValid())
			return ERR;
		return Math.round(Math.abs(startPoint.x - endPoint.x) * dimension.width);
	}

	/**
	 * Returns rectangle height
	 */
	public int getHeight() {
		if (!isValid())
			return ERR;
		return Math.round(Math.abs(startPoint.y - endPoint.y) * dimension.height);
	}

	/**
	 * Returns relative position of top line
	 */
	public float getRelTop() {
		if (!isValid())
			return ERR;
		return Math.min(startPoint.y, endPoint.y);
	}

	/**
	 * Returns relative position of bottom line
	 */
	public float getRelBottom() {
		if (!isValid())
			return ERR;
		return Math.max(startPoint.y, endPoint.y);
	}

	/**
	 * Returns relative position of left line
	 */
	public float getRelLeft() {
		if (!isValid())
			return ERR;
		return Math.min(startPoint.x, endPoint.x);
	}

	/**
	 * Returns relative position of right line
	 */
	public float getRelRight() {
		if (!isValid())
			return ERR;
		return Math.max(startPoint.x, endPoint.x);
	}

	/**
	 * Sets the start Point
	 * 
	 * @param aPoint
	 *            the startPoint to set
	 */
	public void setStartPoint(Point aPoint) {
		setStartPoint(getRelPoint(aPoint));
	}

	/**
	 * Sets the start Point
	 * 
	 * @param aPoint
	 *            the startPoint to set
	 */
	public void setStartPoint(FloatPoint aPoint) {
		final FloatPoint oldPoint = startPoint;
		startPoint = aPoint;
		pcs.firePropertyChange(PROPERTY_START_POINT, oldPoint, startPoint);
	}

	/**
	 * Sets the end Point
	 * 
	 * @param aPoint
	 *            the endPoint to set
	 */
	public void setEndPoint(Point aPoint) {
		setEndPoint(getRelPoint(aPoint));
	}

	/**
	 * Sets the end Point
	 * 
	 * @param aPoint
	 *            the endPoint to set
	 */
	public void setEndPoint(FloatPoint aPoint) {
		final FloatPoint oldPoint = endPoint;
		endPoint = aPoint;
		pcs.firePropertyChange(PROPERTY_END_POINT, oldPoint, endPoint);
	}

	/**
	 * Sets new dimension. Minimal values for both sizes (width, height) are
	 * [1,1]
	 * 
	 * @param newWidth
	 * @param newHeight
	 */
	public void scale(int newWidth, int newHeight) {
		if (newWidth < 1)
			newWidth = 1;
		if (newHeight < 1)
			newHeight = 1;

		dimension.setSize(newWidth, newHeight);
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
	 * Converts given point to relative FloatPoint.
	 * 
	 * @param aPoint
	 * @return FloatPoint or null if aPoint is null.
	 */
	private FloatPoint getRelPoint(Point aPoint) {
		if (aPoint == null) {
			return null;
		}

		return new FloatPoint(((float) aPoint.x) / dimension.width, ((float) aPoint.y) / dimension.height);
	}

	@Override
	public String toString() {
		return "RelRect [dimension=" + dimension + ", endPoint=" + endPoint + ", startPoint=" + startPoint + "]";
	}
}
