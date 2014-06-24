/*
 * $Id: StreamTextArea.java,v 1.1 2010/04/14 17:50:49 kwart Exp $
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

package com.lowagie.rups.view.itext;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.lowagie.rups.io.TextAreaOutputStream;
import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;

public class StreamTextArea extends JScrollPane implements Observer {
	
	/** The text area with the content stream. */
	protected JTextArea text;
	
	/**
	 * Constructs a StreamTextArea.
	 */
	public StreamTextArea() {
		super();
		text = new JTextArea();
		setViewportView(text);
	}
	
	/**
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable observable, Object obj) {
		text.setText(null);
	}
	
	/**
	 * Renders the content stream of a PdfObject or empties the text area.
	 * @param object	the object of which the content stream needs to be rendered
	 */
	public void render(PdfObject object) {
		if (object instanceof PRStream) {
			PRStream stream = (PRStream)object;
			try {
				TextAreaOutputStream taos = new TextAreaOutputStream(text);
				taos.write(PdfReader.getStreamBytes(stream));
				//text.addMouseListener(new StreamEditorAction(stream));
			}
			catch(IOException e) {
				text.setText("The stream could not be read: " + e.getMessage());
			}
		}
		else {
			update(null, null);
			return;
		}
		text.repaint();
		repaint();
	}

	/** a serial version id. */
	private static final long serialVersionUID = 1302283071087762494L;

}
