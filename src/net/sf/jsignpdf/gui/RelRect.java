package net.sf.jsignpdf.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.beans.PropertyChangeListener;

import javax.swing.event.SwingPropertyChangeSupport;

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

	private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

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
		final FloatPoint oldPoint = startPoint;
		startPoint = getRelPoint(aPoint);
		pcs.firePropertyChange(PROPERTY_START_POINT, oldPoint, startPoint);
	}

	/**
	 * Sets the end Point
	 * 
	 * @param aPoint
	 *            the endPoint to set
	 */
	public void setEndPoint(Point aPoint) {
		final FloatPoint oldPoint = endPoint;
		endPoint = getRelPoint(aPoint);
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
}
