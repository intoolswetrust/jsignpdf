package net.sf.jsignpdf.preview;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Resizeable image component with rectangle selection implementation. It
 * extends {@link JPanel} component and draws itself on the panel surface.
 * 
 * @author Josef Cacek
 */
public class SelectionImage extends JPanel {

	private static final long serialVersionUID = 1L;

	private BufferedImage image = null;
	private BufferedImage originalImage = null;

	private RelRect relRect = new RelRect();

	private Point currentPoint;

	/**
	 * Mouse adapter which stores current position of mouse and stores
	 * selection.
	 * 
	 * @author Josef Cacek
	 */
	class SelMouseAdapter extends MouseAdapter implements MouseMotionListener {

		private int btnCode;
		private boolean btnPressed = false;

		public SelMouseAdapter(int aBtnCode) {
			btnCode = aBtnCode;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() != btnCode
			// || image == null
			// || e.getPoint().x >= image.getWidth()
			// || e.getPoint().y > image.getHeight()
			)
				return;
			btnPressed = true;
			relRect.setStartPoint(e.getPoint());
			relRect.setEndPoint(null);
			repaint();
		}

		public void mouseDragged(MouseEvent e) {
			currentPoint = e.getPoint();
			if (!btnPressed)
				return;
			relRect.setEndPoint(e.getPoint());
			repaint();
		}

		public void mouseMoved(MouseEvent e) {
			currentPoint = e.getPoint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.getButton() != btnCode || !btnPressed)
				return;
			btnPressed = false;
			relRect.setEndPoint(e.getPoint());
			repaint();
		}

	}

	/**
	 * Default constructor.
	 */
	public SelectionImage() {
		SelMouseAdapter tmpMouseAdapter = new SelMouseAdapter(MouseEvent.BUTTON1);
		addMouseListener(tmpMouseAdapter);
		addMouseMotionListener(tmpMouseAdapter);
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				createResizedImage();
			}

		});
	}

	/**
	 * Returns last mouse position
	 * 
	 * @return the currentPoint
	 */
	public Point getCurrentPoint() {
		return currentPoint;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			g.drawImage(image, 0, 0, null);
			if (relRect.isValid()) {
				g.drawRect(relRect.getLeft(), relRect.getTop(), relRect.getWidth(), relRect.getHeight());
			}
		}
	}

	/**
	 * Sets original image
	 * 
	 * @param image
	 */
	public void setImage(BufferedImage image) {
		this.originalImage = image;
		createResizedImage();
	}

	/**
	 * Returns original image
	 * 
	 * @return
	 */
	public BufferedImage getImage() {
		return originalImage;
	}

	/**
	 * Returns true if image is set and selection is made
	 * 
	 * @return
	 */
	public boolean isValidPosition() {
		return image != null && relRect != null;
	}

	/**
	 * Creates resized image and scales selection rectangle
	 */
	private void createResizedImage() {
		if (originalImage == null) {
			image = null;
		} else {
			image = resize(originalImage, getWidth(), getHeight());
			relRect.scale(image.getWidth(), image.getHeight());
		}
		repaint();
	}

	/**
	 * Resizes given image to new dimension. It doesn't break original
	 * proportions.
	 * 
	 * @param aImg
	 *            image to resize
	 * @param aWidth
	 *            new image width
	 * @param aHeight
	 *            new image height
	 * @return resized image
	 */
	private static BufferedImage resize(BufferedImage aImg, int aWidth, int aHeight) {
		if (aWidth < 1)
			aWidth = 1;
		if (aHeight < 1)
			aHeight = 1;

		int w = aImg.getWidth();
		int h = aImg.getHeight();
		float rel = Math.min(((float) aWidth) / w, ((float) aHeight) / h);

		BufferedImage dimg = new BufferedImage(Math.round(w * rel), Math.round(h * rel), aImg.getType());
		Graphics2D g = dimg.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(aImg, 0, 0, dimg.getWidth(), dimg.getHeight(), 0, 0, w, h, null);
		g.dispose();
		return dimg;
	}

	public RelRect getRelRect() {
		return relRect;
	}

}
