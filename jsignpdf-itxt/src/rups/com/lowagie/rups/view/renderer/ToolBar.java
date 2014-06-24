/*
 * $Id: ToolBar.java,v 1.1 2010/04/14 17:50:48 kwart Exp $
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

package com.lowagie.rups.view.renderer;

import javax.swing.Box;
import javax.swing.JToolBar;

import com.lowagie.rups.view.PageNavigationAction;
import com.lowagie.rups.view.PageNavigationListener;
import com.lowagie.rups.view.icons.IconButton;

/**
 * The Tool Bar that will be used to navigate from page to page
 * and change the way a page is rendered.
 */
public class ToolBar extends JToolBar {

    /** the current page number text field */
    protected PageField pageField;
	
	/**
	 * Creates the tool bar.
	 * @param listener	an object that is listening to page navigation changes.
	 */
	public ToolBar(PageNavigationListener listener) {
		super();
		pageField = new PageField(listener);
		
		setFloatable(false);
		add(Box.createHorizontalGlue());
		addNavigationButton(listener, PageNavigationAction.FIRST_PAGE);
		addNavigationButton(listener, PageNavigationAction.PREVIOUS_PAGE);
		add(pageField);
		addNavigationButton(listener, PageNavigationAction.NEXT_PAGE);
		addNavigationButton(listener, PageNavigationAction.LAST_PAGE);
	}
	
	/**
	 * Changes the page number in the page field.
	 * @param pageNumber	the number of the currently shown page
	 */
	public void setPageNumber(int pageNumber) {
		if (pageNumber > 0) {
			pageField.setText(String.valueOf(pageNumber));
		}
		else {
			pageField.setText("");
		}
	}
	
	/**
	 * Convenience method to create navigation buttons.
	 * @param listener	a page navigation listener
	 * @param type	a type of page navigation
	 */
	protected void addNavigationButton(PageNavigationListener listener, int type) {
		add(new IconButton(new PageNavigationAction(listener, type, true)));
	}
	
	/** A Serial Version UID. */
	private static final long serialVersionUID = -2062747250230455558L;

}