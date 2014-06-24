/*
 * $Id: IconActionListener.java,v 1.1 2010/04/14 17:50:30 kwart Exp $
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

package com.lowagie.rups.view.icons;

import java.awt.event.ActionListener;

import javax.swing.Icon;

/**
 * Special ActionListener to be implemented by Buttons that need an Icon.
 */
public interface IconActionListener extends ActionListener {
	/** Returns an Icon. */
	public Icon getIcon();
}
