/*
 * $Id: FormList.java,v 1.1 2010/04/14 17:50:43 kwart Exp $
 *
 * This code is part of the 'iText Tutorial'.
 * You can find the complete tutorial at the following address:
 * http://itextdocs.lowagie.com/tutorial/
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * itext-questions@lists.sourceforge.net
 */

package com.lowagie.examples.forms;


import java.io.FileOutputStream;
import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfAppearance;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Generates an Acroform with a List
 * @author blowagie
 */
public class FormList {
    /**
     * Generates an Acroform with a list
     * @param args no arguments needed here
     */
    public static void main(String[] args) {
        
        System.out.println("List");
        
        // step 1: creation of a document-object
        Document document = new Document(PageSize.A4);
        
        try {
            
            // step 2:
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("list.pdf"));
            
            // step 3: we open the document
            document.open();
            
            // step 4:
            PdfContentByte cb = writer.getDirectContent();
            cb.moveTo(0, 0);
            String options[] = {"Red", "Green", "Blue"};
            PdfFormField field = PdfFormField.createList(writer, options, 0);
            PdfAppearance app = cb.createAppearance(80, 60);
            app.rectangle(1, 1, 78, 58);
            app.setGrayFill(0.8f);
            app.fill();
            app.resetGrayFill();
            field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, app);
            field.setWidget(new Rectangle(100, 700, 180, 760), PdfAnnotation.HIGHLIGHT_OUTLINE);
            field.setFieldName("AList");
            field.setValueAsString("Red");
            writer.addAnnotation(field);
            
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        
        // step 5: we close the document
        document.close();
    }
}