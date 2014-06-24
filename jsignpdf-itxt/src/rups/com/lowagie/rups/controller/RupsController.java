/*
 * $Id: RupsController.java,v 1.1 2010/04/14 17:50:44 kwart Exp $
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

package com.lowagie.rups.controller;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Observable;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.lowagie.rups.io.FileChooserAction;
import com.lowagie.rups.io.FileCloseAction;
import com.lowagie.rups.model.PdfFile;
import com.lowagie.rups.view.Console;
import com.lowagie.rups.view.PageNavigationListener;
import com.lowagie.rups.view.RupsMenuBar;
import com.lowagie.rups.view.itext.treenodes.PdfObjectTreeNode;
import com.lowagie.rups.view.itext.treenodes.PdfTrailerTreeNode;
import com.lowagie.text.DocumentException;

/**
 * This class controls all the GUI components that are shown in
 * the Trapeze application: the menu bar, the panels,...
 */
public class RupsController extends Observable
	implements TreeSelectionListener, PageNavigationListener {
	
	// member variables
	
	/* file */
	/** The Pdf file that is currently open in the application. */
	protected PdfFile pdfFile;

	/* main components */
	/** The JMenuBar for the Trapeze application. */
	protected RupsMenuBar menuBar;
	/** Contains all other components: the page panel, the outline tree, etc. */
	protected JSplitPane masterComponent;
	
	/* Other controllers */
	/** Object with the GUI components for SUN's PDF Renderer. */
	protected PdfRendererController renderer;
	/** Object with the GUI components for iText. */
	protected PdfReaderController reader;
	
	// constructor
	/**
	 * Constructs the GUI components of the Trapeze application.
	 */
	public RupsController(Dimension dimension) {
		// creating components and controllers
        menuBar = new RupsMenuBar(this);
        addObserver(menuBar);
		Console console = Console.getInstance();
		addObserver(console);
		renderer = new PdfRendererController(this);
		addObserver(renderer);
		reader = new PdfReaderController(this, this);
		addObserver(reader);

        // creating the master component
		masterComponent = new JSplitPane();
		masterComponent.setOrientation(JSplitPane.VERTICAL_SPLIT);
		masterComponent.setDividerLocation((int)(dimension.getHeight() * .70));
		masterComponent.setDividerSize(2);
		
		JSplitPane content = new JSplitPane();
		masterComponent.add(content, JSplitPane.TOP);
		JSplitPane info = new JSplitPane();
		masterComponent.add(info, JSplitPane.BOTTOM);
		
		content.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		content.setDividerLocation((int)(dimension.getWidth() * .75));
		content.setDividerSize(1);
		
		JSplitPane viewers = new JSplitPane();
        content.add(viewers, JSplitPane.LEFT);
		content.add(reader.getNavigationTabs(), JSplitPane.RIGHT);

		viewers.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		viewers.setDividerLocation((int)(dimension.getWidth() * .35));
		viewers.setDividerSize(1);
		viewers.add(renderer, JSplitPane.RIGHT);
		viewers.add(RupsController.getScrollPane(reader.getPdfTree()), JSplitPane.LEFT);
        
		info.setDividerLocation((int) (dimension.getWidth() * .3));
		info.setDividerSize(1);
		info.add(reader.getObjectPanel(), JSplitPane.LEFT);
		JTabbedPane editorPane = reader.getEditorTabs();
		JScrollPane cons = RupsController.getScrollPane(console.getTextArea());
		editorPane.addTab("Console", null, cons, "Console window (System.out/System.err)");
		editorPane.setSelectedComponent(cons);
		info.add(editorPane, JSplitPane.RIGHT);
		
	}

	/** Getter for the menubar. */
	public RupsMenuBar getMenuBar() {
		return menuBar;
	}
	
	/** Getter for the master component. */
	public Component getMasterComponent() {
		return masterComponent;
	}

	// Observable
	
	/**
	 * @see java.util.Observable#notifyObservers(java.lang.Object)
	 */
	@Override
	public void notifyObservers(Object obj) {
		if (obj instanceof FileChooserAction) {
			File file = ((FileChooserAction)obj).getFile();
			try {
				pdfFile = new PdfFile(file);
				setChanged();
				super.notifyObservers(RupsMenuBar.OPEN);
				renderer.startPageLoader(pdfFile);
				reader.startObjectLoader(pdfFile);
			}
			catch(IOException ioe) {
				JOptionPane.showMessageDialog(masterComponent, ioe.getMessage(), "Dialog", JOptionPane.ERROR_MESSAGE);
			}
			catch (DocumentException de) {
				JOptionPane.showMessageDialog(masterComponent, de.getMessage(), "Dialog", JOptionPane.ERROR_MESSAGE);
			}
			return;
		}
		if (obj instanceof FileCloseAction) {
			pdfFile = null;
			setChanged();
			super.notifyObservers(RupsMenuBar.CLOSE);
			return;
		}
	}

	// tree selection
	
	/**
	 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
	 */
	public void valueChanged(TreeSelectionEvent evt) {
		Object selectednode = reader.getPdfTree().getLastSelectedPathComponent();
		if (selectednode instanceof PdfTrailerTreeNode) {
			menuBar.update(this, RupsMenuBar.FILE_MENU);
			return;
		}
		if (selectednode instanceof PdfObjectTreeNode) {
			reader.update(this, selectednode);
		}
	}

	// page navigation
	
	/**
	 * @see com.lowagie.rups.view.PageNavigationListener#getCurrentPageNumber()
	 */
	public int getCurrentPageNumber() {
		return renderer.getCurrentPageNumber();
	}

	/**
	 * @see com.lowagie.rups.view.PageNavigationListener#getTotalNumberOfPages()
	 */
	public int getTotalNumberOfPages() {
		return renderer.getTotalNumberOfPages();
	}
	
	/**
	 * @see com.lowagie.rups.view.PageNavigationListener#gotoFirstPage()
	 */
	public int gotoFirstPage() {
		return gotoPage(1);
	}

	/**
	 * @see com.lowagie.rups.view.PageNavigationListener#gotoPreviousPage()
	 */
	public int gotoPreviousPage() {
		return gotoPage(getCurrentPageNumber() - 1);
	}

	/**
	 * @see com.lowagie.rups.view.PageNavigationListener#gotoPage(int)
	 */
	public int gotoPage(int pageNumber) {
		pageNumber = renderer.gotoPage(pageNumber);
		reader.gotoPage(pageNumber);
		return pageNumber;
	}
	
	/**
	 * @see com.lowagie.rups.view.PageNavigationListener#gotoNextPage()
	 */
	public int gotoNextPage() {
		return gotoPage(getCurrentPageNumber() + 1);
	}

	/**
	 * @see com.lowagie.rups.view.PageNavigationListener#gotoLastPage()
	 */
	public int gotoLastPage() {
		return gotoPage(getTotalNumberOfPages());
	}

	/**
	 * Adds a component to a ScrollPane.
	 * @param	component	the component that has to be scrollable
	 * @return	a JScrollPane
	 * @since 2.1.0
	 */
	public static JScrollPane getScrollPane(Component component) {
		JScrollPane scrollpane = new JScrollPane();
		scrollpane.setViewportView(component);
		return scrollpane;
	}
}
