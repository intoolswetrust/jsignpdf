/*
 * $Id: PdfRendererController.java,v 1.1 2010/04/14 17:50:44 kwart Exp $
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

import java.util.Observable;
import java.util.Observer;

import javax.swing.JSplitPane;

import com.lowagie.rups.model.PageLoader;
import com.lowagie.rups.model.PdfFile;
import com.lowagie.rups.view.PageNavigationListener;
import com.lowagie.rups.view.RupsMenuBar;
import com.lowagie.rups.view.renderer.PagePanel;
import com.lowagie.rups.view.renderer.ToolBar;

/**
 * This is the part of Trapeze that uses SUN's PDF Renderer.
 */
public class PdfRendererController extends JSplitPane
	implements Observer {
	
	/** The page loader that provides access to the PDFPage objects. */
	protected PageLoader pageLoader = null;
	/** The ToolBar. */
	protected ToolBar toolbar;
	/** The PagePanel */
	protected PagePanel pagePanel;
	
	/** A Serial Version UID. */
	private static final long serialVersionUID = 3270054619281094248L;

	/**
	 * Constructs the rendering controller.
	 * @param	listener 	a page navigation listener
	 */
	public PdfRendererController(PageNavigationListener listener) {
		setOrientation(JSplitPane.VERTICAL_SPLIT);
		setDividerLocation(33);
		setDividerSize(0);
        pagePanel = new PagePanel();
		toolbar = new ToolBar(listener);
		add(toolbar, JSplitPane.TOP);
        add(pagePanel, JSplitPane.BOTTOM);
	}
	
	/**
	 * Starts loading pages.
	 * Shows page 1 of this file as soon as possible.
	 * @param	file	the PdfFile that needs to be rendered
	 */
	public void startPageLoader(PdfFile file) {
		this.pageLoader = new PageLoader(file.getPDFFile());
		gotoPage(1);
	}
	
	/**
	 * Shows a specific page in the page panel.
	 * @param	pageNumber a number of a specific page.
	 */
	protected int showPage(int pageNumber) {
		if (pageLoader == null) {
			return -1;
		}
		pagePanel.showPage(pageLoader.loadPage(pageNumber));
		pagePanel.requestFocus();
		return pageNumber;
	}

	// page navigation
	
	/**
	 * Gets the total number of pages in the document.
	 * @return	the total number of pages
	 */
	public int getTotalNumberOfPages() {
		if (pageLoader == null) return 0;
		return pageLoader.getNumberOfPages();
	}
	
	/**
	 * Getter for the current page number.
	 * @return	the page number of the page currently shown
	 */
	public int getCurrentPageNumber() {
		return pagePanel.getCurrentPageNumber();
	}
	
	/**
	 * Shows a specific page.
	 * @param	pageNumber	the number of a specific page.
	 */
	public int gotoPage(int pageNumber) {
		if (pageNumber == getCurrentPageNumber()) {
			return pageNumber;
		}
		if (pageNumber < 0) {
			toolbar.setPageNumber(-1);
			return -1;
		}
		if (pageNumber == 0) {
			pageNumber = 1;
		}
		else if (pageNumber > getTotalNumberOfPages()) {
			pageNumber = getTotalNumberOfPages();
		}
		pageNumber = showPage(pageNumber);
		toolbar.setPageNumber(pageNumber);
		return pageNumber;
	}
	
	// Observer interface
	
	/**
	 * Forwards updates from the RupsController to the Observers of this class.
	 * @param	observable	this should be the RupsController
	 * @param	obj	the object that has to be forwarded to the observers of PdfReaderController
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable observable, Object obj) {
		if (RupsMenuBar.CLOSE.equals(obj)) {
			pageLoader = null;
			pagePanel.showPage(null);
			toolbar.setPageNumber(-1);
		}
	}
}
