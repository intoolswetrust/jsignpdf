package net.sf.jsignpdf.gui;

/**
 * Simple implementation of coordinates (X,Y) with floating-point (float) values
 * (e.g. relative values)
 * 
 * @author Josef Cacek
 */
public class FloatPoint {

	protected float x;
	protected float y;

	/**
	 * Default constructor
	 */
	public FloatPoint() {
		// default constructor
	}

	/**
	 * All parameters constructor
	 * 
	 * @param x
	 *            position on axis X
	 * @param y
	 *            position on axis Y
	 */
	public FloatPoint(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Returns position on axis X
	 */
	public float getX() {
		return x;
	}

	/**
	 * Sets position on axis X
	 */
	public void setX(float x) {
		this.x = x;
	}

	/**
	 * Returns position on axis Y
	 */
	public float getY() {
		return y;
	}

	/**
	 * Sets position on axis Y
	 */
	public void setY(float y) {
		this.y = y;
	}

}
