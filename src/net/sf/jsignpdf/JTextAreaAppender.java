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

import javax.swing.JTextArea;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Log4J Appender which appends messages to the provided {@link JTextArea}
 * instance.
 * 
 * @author Josef Cacek
 */
public class JTextAreaAppender extends AppenderSkeleton {

  private JTextArea jTextArea;

  /**
   * Constructor.
   * 
   * @param jTextArea
   */
  public JTextAreaAppender(final JTextArea jTextArea) {
    if (jTextArea == null) {
      throw new IllegalArgumentException("JTextArea has to be not-null.");
    }
    this.jTextArea = jTextArea;
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.Appender#close()
   */
  public void close() {
    jTextArea = null;
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.Appender#requiresLayout()
   */
  public boolean requiresLayout() {
    return true;
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
   */
  @Override
  protected void append(LoggingEvent event) {
    if (layout == null) {
      errorHandler.error("No layout for appender " + name, null, ErrorCode.MISSING_LAYOUT);
      return;
    }
    final String message = layout.format(event);
    jTextArea.append(message);
    //TODO do we need this code
    if (layout.ignoresThrowable()) {
      for (String throwableRow : ArrayUtils.nullToEmpty(event.getThrowableStrRep())) {
        jTextArea.append(throwableRow + NEW_LINE);
      }
    }
    // scroll TextArea
    jTextArea.setCaretPosition(jTextArea.getText().length());
  }

}
