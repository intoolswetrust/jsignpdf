/*
 * $Id: Rups.java,v 1.1 2010/04/14 17:50:35 kwart Exp $
 *
 * Copyright 2007 Bruno Lowagie.
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

package com.lowagie.rups;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

import com.lowagie.rups.controller.RupsController;

/**
 * iText RUPS is a tool that combines SUN's PDF Renderer (to view PDF
 * documents), iText's PdfReader (to inspect the internal structure of a
 * PDF file), and iText's PdfStamper to manipulate a PDF file.
 */
public class Rups {

	/** The version String; this String may only be changed by Bruno Lowagie. */
	public static final String VERSION = "RUPS 0.0.1 (by lowagie.com)";
	
	// main method
	/**
	 * Main method of this PdfFrame class.
	 * @param	args	no arguments needed
	 */
	public static void main(String[] args) {
		startApplication();
	}
	
	// methods
	
    /**
     * Initializes the main components of the Rups application.
     */
    public static void startApplication() {
    	JFrame frame = new JFrame();
        // size and location
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize((int)(screen.getWidth() * .90), (int)(screen.getHeight() * .90));
        frame.setLocation((int)(screen.getWidth() * .05), (int)(screen.getHeight() * .05));
        frame.setResizable(true);
        
        // title bar
        frame.setTitle("iText RUPS");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // the content
        RupsController controller = new RupsController(frame.getSize());
        frame.setJMenuBar(controller.getMenuBar());
        frame.getContentPane().add(controller.getMasterComponent(), java.awt.BorderLayout.CENTER);
		frame.setVisible(true);
    }
	
	// other member variables
	
	/** Serial Version UID. */
	private static final long serialVersionUID = 4386633640535735848L;

}