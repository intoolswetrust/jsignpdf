/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.NEW_LINE;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.JTextArea;

/**
 * Logging Handlerwhich appends messages to the provided {@link JTextArea} instance.
 *
 * @author Josef Cacek
 */
public class JTextAreaHandler extends Handler {

    private JTextArea jTextArea;

    /**
     * Constructor.
     *
     * @param jTextArea
     */
    public JTextAreaHandler(final JTextArea jTextArea) {
        if (jTextArea == null) {
            throw new IllegalArgumentException("JTextArea has to be not-null.");
        }
        this.jTextArea = jTextArea;
    }

    @Override
    public void close() {
        jTextArea = null;
    }

    @Override
    public void publish(LogRecord record) {
        jTextArea.append(record.getLevel() + " " + record.getMessage() + NEW_LINE);
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            try (StringWriter stringWriter = new StringWriter()) {
                thrown.printStackTrace(new PrintWriter(stringWriter, true));
                jTextArea.append(stringWriter.toString());
            } catch (IOException e) {
                // should not happen :)
            }
            jTextArea.append(NEW_LINE);
        }
        // scroll TextArea
        jTextArea.setCaretPosition(jTextArea.getText().length());
    }

    @Override
    public void flush() {
    }

}
