/*
 * $Id: PageNavigationAction.java,v 1.1 2010/04/14 17:50:52 kwart Exp $
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

package com.lowagie.rups.view;

import java.awt.event.ActionEvent;

import javax.swing.Icon;

import com.lowagie.rups.view.icons.IconActionListener;
import com.lowagie.rups.view.icons.IconFetcher;

/**
 * Class that can be used for some basic navigation actions.
 * Each action also has a corresponding icon.
 */
public class PageNavigationAction implements IconActionListener {
	/** Type to go to the first page. */
	public static final int FIRST_PAGE = 1;
	/** Type to go to the previous page. */
	public static final int PREVIOUS_PAGE = 2;
	/** Type to go to the next page. */
	public static final int NEXT_PAGE = 3;
	/** Type to go to the last page. */
	public static final int LAST_PAGE = 4;
	
	/** The type of this action. */
	protected int type;
	/** The object that is listening to the page navigation. */
	protected PageNavigationListener listener;
	/** The icon corresponding with the action. */
	protected Icon icon = null;
	
	/**
	 * Creates a page navigation action.
	 * @param listener	the object listening to the page navigation
	 * @param type		the type of action
	 * @param withIcon	if false, no icon will be attached to the action
	 */
	public PageNavigationAction(PageNavigationListener listener, int type, boolean withIcon) {
		super();
		this.listener = listener;
		this.type = type;
		if (withIcon) {
			switch(type) {
			case FIRST_PAGE:
				icon = IconFetcher.getIcon("navigation_first.png");
				break;
			case PREVIOUS_PAGE:
				icon = IconFetcher.getIcon("navigation_previous.png");
				break;
			case NEXT_PAGE:
				icon = IconFetcher.getIcon("navigation_next.png");
				break;
			case LAST_PAGE:
				icon = IconFetcher.getIcon("navigation_last.png");
				break;
			}
		}
	}

	/**
	 * @see com.lowagie.rups.view.icons.IconActionListener#getIcon()
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent evt) {
		switch(type) {
		case FIRST_PAGE:
			listener.gotoFirstPage();
			return;
		case PREVIOUS_PAGE:
			listener.gotoPreviousPage();
			return;
		case NEXT_PAGE:
			listener.gotoNextPage();
			return;
		case LAST_PAGE:
			listener.gotoLastPage();
			return;
		}	
	}
}
