package net.sf.jsignpdf.preview;

/**
 * Simple implementation of coordinates (X,Y) with floating-point (float) values
 * (e.g. relative values)
 * 
 * @author Josef Cacek
 */
public final class FloatPoint {

	protected final float x;
	protected final float y;

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
	 * Returns position on axis Y
	 */
	public float getY() {
		return y;
	}
}
