package net.sf.jsignpdf.gui;

import java.awt.Dimension;
import java.awt.Point;

/**
 * Rectangle implementation based on relative positions.
 * 
 * @author Josef Cacek
 */
public class RelRect {

	/**
	 * Value returned by getters if {@link #isValid()} method returns false
	 */
	public int ERR = -1;

	private FloatPoint startPoint;
	private FloatPoint endPoint;

	private Dimension dimension = new Dimension(1, 1);

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
		startPoint = getRelPoint(aPoint, startPoint);
	}

	/**
	 * Sets the end Point
	 * 
	 * @param aPoint
	 *            the endPoint to set
	 */
	public void setEndPoint(Point aPoint) {
		endPoint = getRelPoint(aPoint, endPoint);
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
	 * Converts given point to relative FloatPoint. If the second argument is
	 * not-null, then the object provided is only changed and returned (reuses
	 * the provided object).
	 * 
	 * @param aPoint
	 * @param aRelPoint
	 * @return FloatPoint or null if aPoint is null.
	 */
	private FloatPoint getRelPoint(Point aPoint, final FloatPoint aRelPoint) {
		if (aPoint == null) {
			return null;
		}

		final FloatPoint tmpResult;
		if (aRelPoint == null) {
			tmpResult = new FloatPoint();
		} else {
			tmpResult = aRelPoint;
		}
		tmpResult.x = ((float) aPoint.x) / dimension.width;
		tmpResult.y = ((float) aPoint.y) / dimension.height;
		return tmpResult;
	}
}
