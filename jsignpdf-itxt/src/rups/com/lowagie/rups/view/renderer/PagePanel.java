/*
 * $Id: PagePanel.java,v 1.1 2010/04/14 17:50:48 kwart Exp $
 *
 * Copyright 2007 Bruno Lowagie.
 * Inspired by a demo shipped with SUN's PDF Renderer (released under the LGPL)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.lowagie.rups.view.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;

import javax.swing.JPanel;

import com.sun.pdfview.PDFPage;

/**
 * The panel that will show a rendered PDF page.
 */
public class PagePanel extends JPanel
	implements ImageObserver, MouseListener, MouseMotionListener {

	/** A Serial Version UID. */
	private static final long serialVersionUID = -130815955294007634L;

    /** The current PDFPage that was rendered into currentImage */
	protected PDFPage currentPage;
    
    /** The image of the rendered PDF page being displayed */
    protected Image currentImage;
    
    /** The horizontal offset of the image from the left edge of the panel */
    protected int offx;

    /** The vertical offset of the image from the top of the panel */
    protected int offy;

    /** the size of the image */
    Dimension prevSize;
    
    /** the current clip, in device space */
    Rectangle2D clip;
   
    /** the clipping region used for the image */
    Rectangle2D prevClip;
    
    /** the zooming marquee */
    protected Rectangle zoomRect;
    
    /** the current transform from device space to page space */
    AffineTransform deviceToPageSpaceTransformation;
    
    /**
     * Creates a new PagePanel
     */
    public PagePanel() {
    	super();
    	setFocusable(true);
    	addMouseListener(this);
    	addMouseMotionListener(this);
    }

	/**
	 * Gets the current page number.
	 * @return	the number of the currently shown page.
	 */
	public int getCurrentPageNumber() {
		if (currentPage == null) return 0;
		return currentPage.getPageNumber();
	}
    
    /**
     * Draw the image.
     */
    public void paint(Graphics g) {
    	Dimension sz= getSize();
    	g.setColor(Color.DARK_GRAY);
    	g.fillRect(0, 0, getWidth(), getHeight());
    	if (currentImage == null) {
    		// No image -- draw an empty box
    		g.setColor(Color.black);
    		g.drawString("No page selected", getWidth()/2-30, getHeight()/2);
    	} else {
    		// draw the image
    		int imwid= currentImage.getWidth(null);
    		int imhgt= currentImage.getHeight(null);

    		// draw it centered within the panel
    		offx = (sz.width-imwid)/2;
    		offy = (sz.height-imhgt)/2;

    		if ((imwid == sz.width && imhgt <= sz.height) ||
    				(imhgt == sz.height && imwid <= sz.width)) {
    			g.drawImage(currentImage, offx, offy, this);
    		}
    		else {
    			if (currentPage!=null) {
    				showPage(currentPage);
    			}
    		}
    	}
    	// draw the zoomrect if there is one.
    	if (zoomRect!=null) {
    		g.setColor(Color.red);
    		g.drawRect(zoomRect.x, zoomRect.y,
		       zoomRect.width, zoomRect.height);
    	}
    }

    /**
     * Stop the generation of any previous page, and draw the new one.
     * @param page the PDFPage to draw.
     */
    public synchronized void showPage(PDFPage page) {
    	// stop drawing the previous page
        if (currentPage != null && prevSize != null) {
            currentPage.stop(prevSize.width, prevSize.height,  prevClip);
        }

        // set up the new page
        currentPage= page;
        
        if (page==null) {
        	// no page
        	currentImage= null;
        	clip= null;
            deviceToPageSpaceTransformation = null;
            repaint();
        } else {
        	Dimension sz= getSize();
        	if (sz.width + sz.height == 0) {
        		// no image to draw.
        		return;
        	}
        	// calculate the clipping rectangle in page space from the
        	// desired clip in screen space.
            Rectangle2D useClip = clip;
            if (clip != null && deviceToPageSpaceTransformation != null) {
                useClip = deviceToPageSpaceTransformation.createTransformedShape(clip).getBounds2D();
            }
                               
            Dimension pageSize = page.getUnstretchedSize(sz.width, sz.height, useClip);

            // get the new image
            currentImage= page.getImage(pageSize.width, pageSize.height, 
                                        useClip, this);
            
            // calculate the transform from screen to page space
            deviceToPageSpaceTransformation = page.getInitialTransform(pageSize.width, 
                                                    pageSize.height,
                                                    useClip);
            try {
                deviceToPageSpaceTransformation = deviceToPageSpaceTransformation.createInverse();
            } catch (NoninvertibleTransformException nte) {
                System.out.println("Error inverting page transform!");
                nte.printStackTrace();
            }
            
            prevClip = useClip;
            prevSize = pageSize;
            
            repaint();
        }
    }

    /**
     * Handles notification of the fact that some part of the image
     * changed.  Repaints that portion.
     * @return true if more updates are desired.
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y,
			       int width, int height) {
        if ((infoflags & (SOMEBITS|ALLBITS))!=0) {
        	repaint(x + offx, y + offy, width, height);
        }
        return true;
    }

	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
