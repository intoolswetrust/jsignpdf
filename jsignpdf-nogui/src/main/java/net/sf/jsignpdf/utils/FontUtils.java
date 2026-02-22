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
package net.sf.jsignpdf.utils;

import java.awt.Font;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sf.jsignpdf.Constants;

import eu.europa.esig.dss.pades.DSSFileFont;
import eu.europa.esig.dss.pades.DSSFont;
import eu.europa.esig.dss.pades.DSSJavaFont;

/**
 * Utilities for handling fonts in visible signature.
 *
 * @author Josef Cacek
 */
public class FontUtils {

    public static DSSFont l2baseFont;

    /**
     * Returns DSSFont for text of visible signature.
     *
     * @return
     */
    public static synchronized DSSFont getL2BaseFont() {
        if (l2baseFont == null) {
            final ConfigProvider conf = ConfigProvider.getInstance();
            try {
                String fontPath = conf.getNotEmptyProperty("font.path", null);
                if (fontPath != null) {
                    l2baseFont = new DSSFileFont(new FileInputStream(fontPath));
                } else {
                    InputStream tmpIs = FontUtils.class.getResourceAsStream(Constants.L2TEXT_FONT_PATH);
                    l2baseFont = new DSSFileFont(tmpIs);
                    tmpIs.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    l2baseFont = new DSSJavaFont(new Font("Helvetica", Font.PLAIN, 12));
                } catch (Exception ex) {
                    // where is the problem, dear Watson?
                }
            }
        }
        return l2baseFont;
    }

}
