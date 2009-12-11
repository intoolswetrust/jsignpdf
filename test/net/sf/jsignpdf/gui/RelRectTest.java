package net.sf.jsignpdf.gui;

import java.awt.Point;

import junit.framework.TestCase;

/**
 * Test for {@link RelRect} implementation of resizeable rectangle
 * 
 * @author Josef Cacek
 */
public class RelRectTest extends TestCase {

	private static final int WIDTH = 600;
	private static final int HEIGHT = 400;

	private RelRect relRect = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		relRect = new RelRect();
		relRect.scale(WIDTH, HEIGHT);
	}

	/**
	 * test {@link RelRect#isValid()} method
	 */
	public void testIsValid() {
		assertFalse(relRect.isValid());
		relRect.setStartPoint(new Point(100, 100));
		assertFalse(relRect.isValid());
		relRect.setEndPoint(new Point(200, 300));
		assertTrue(relRect.isValid());
		relRect.setStartPoint(new Point(200, 300));
		assertTrue(relRect.isValid());

		// rectangle should stay valid after resize
		relRect.scale(WIDTH + 100, HEIGHT + 100);
		assertTrue(relRect.isValid());

		relRect.setStartPoint(null);
		assertFalse(relRect.isValid());
	}

	/**
	 * Test integer getters of rectangle
	 */
	public void testIntGetters() {
		relRect.setStartPoint(new Point(50, 100));
		relRect.setEndPoint(new Point(200, 300));

		assertEquals(relRect.getTop(), 100);
		assertEquals(relRect.getBottom(), 300);
		assertEquals(relRect.getLeft(), 50);
		assertEquals(relRect.getRight(), 200);
		assertEquals(relRect.getHeight(), 200);
		assertEquals(relRect.getWidth(), 150);

		relRect.setStartPoint(new Point(200, 300));
		relRect.setEndPoint(new Point(50, 100));

		assertEquals(relRect.getTop(), 100);
		assertEquals(relRect.getBottom(), 300);
		assertEquals(relRect.getLeft(), 50);
		assertEquals(relRect.getRight(), 200);
		assertEquals(relRect.getHeight(), 200);
		assertEquals(relRect.getWidth(), 150);
	}

	/**
	 * Tests stored relative points
	 */
	public void testFloatGetters() {
		relRect.setStartPoint(new Point(50, 100));
		relRect.setEndPoint(new Point(200, 300));

		assertTrue(areEqual(relRect.getRelTop(), 100f / HEIGHT));
		assertTrue(areEqual(relRect.getRelBottom(), 300f / HEIGHT));
		assertTrue(areEqual(relRect.getRelLeft(), 50f / WIDTH));
		assertTrue(areEqual(relRect.getRelRight(), 200f / WIDTH));

		relRect.setStartPoint(new Point(200, 300));
		relRect.setEndPoint(new Point(50, 100));

		assertTrue(areEqual(relRect.getRelTop(), 100f / HEIGHT));
		assertTrue(areEqual(relRect.getRelBottom(), 300f / HEIGHT));
		assertTrue(areEqual(relRect.getRelLeft(), 50f / WIDTH));
		assertTrue(areEqual(relRect.getRelRight(), 200f / WIDTH));
	}

	/**
	 * Tests scaling
	 */
	public void testScale() {
		relRect.setStartPoint(new Point(50, 100));
		relRect.setEndPoint(new Point(200, 300));

		relRect.scale(WIDTH / 2, HEIGHT);
		assertEquals(relRect.getTop(), 100);
		assertEquals(relRect.getBottom(), 300);
		assertEquals(relRect.getLeft(), 25);
		assertEquals(relRect.getRight(), 100);
		assertEquals(relRect.getHeight(), 200);
		assertEquals(relRect.getWidth(), 75);
		assertTrue(areEqual(relRect.getRelTop(), 100f / HEIGHT));
		assertTrue(areEqual(relRect.getRelBottom(), 300f / HEIGHT));
		assertTrue(areEqual(relRect.getRelLeft(), 50f / WIDTH));
		assertTrue(areEqual(relRect.getRelRight(), 200f / WIDTH));

		relRect.scale(WIDTH, HEIGHT / 2);
		assertEquals(relRect.getTop(), 50);
		assertEquals(relRect.getBottom(), 150);
		assertEquals(relRect.getLeft(), 50);
		assertEquals(relRect.getRight(), 200);
		assertEquals(relRect.getHeight(), 100);
		assertEquals(relRect.getWidth(), 150);
		assertTrue(areEqual(relRect.getRelTop(), 100f / HEIGHT));
		assertTrue(areEqual(relRect.getRelBottom(), 300f / HEIGHT));
		assertTrue(areEqual(relRect.getRelLeft(), 50f / WIDTH));
		assertTrue(areEqual(relRect.getRelRight(), 200f / WIDTH));

	}

	/**
	 * Tests two floats if they are "equals" (a small delta is allowed)
	 * 
	 * @param aNum1
	 * @param aNum2
	 * @return
	 */
	private boolean areEqual(float aNum1, float aNum2) {
		return Math.abs(1 - (aNum1 / aNum2)) < 0.00001f;
	}

}
