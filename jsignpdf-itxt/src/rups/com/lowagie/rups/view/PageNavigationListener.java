/*
 * $Id: PageNavigationListener.java,v 1.1 2010/04/14 17:50:52 kwart Exp $
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

/**
 * Interface that has to be implemented by every class that listens
 * to page navigation features; keys that are pressed, buttons that
 * are pushed,...
 */
public interface PageNavigationListener {

	/**
	 * Returns the total number of pages in a document.
	 * @return	the total number of pages in a document.
	 */
	public int getTotalNumberOfPages();
	/**
	 * Returns the current page number of a document.
	 * @return	the current page number.
	 */
	public int getCurrentPageNumber();
	/**
	 * Goes to the first page in a document.
	 * @return	the resulting page number.
	 * 		Can be different from 1 if the document is null
	 */
	public int gotoFirstPage();
	/**
	 * Goes to the previous page in a document.
	 * @return	the resulting page number.
	 * 		Can be different from (current page - 1) if the document is null
	 * 		or the current page = 1.
	 */
	public int gotoPreviousPage();
	/**
	 * Goes to a specific page number in a document.
	 * @param	pageNumber
	 * @return	the resulting page number.
	 * 		Can be different from pageNumber if pageNumber doesn't exist.
	 */
	public int gotoPage(int pageNumber);
	/**
	 * Goes to the previous page in a document.
	 * @return	the resulting page number.
	 * 		Can be different from (current page + 1) if the document is null
	 * 		or the current page equals the total number of pages.
	 */
	public int gotoNextPage();
	/**
	 * Goes to the last page in a document.
	 * @return	the resulting page number.
	 * 		Can be different from the total number of pages if the document is null
	 */
	public int gotoLastPage();
}
