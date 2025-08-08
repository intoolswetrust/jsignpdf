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


/**
 * Utilities for handling fonts in visible signature.
 * 
 * @deprecated This class is no longer used since the DSS migration.
 * Signature appearance and fonts are now handled by the DSS framework.
 * This class can be safely removed in a future version.
 * 
 * @author Josef Cacek
 */
@Deprecated
public class FontUtils {

    /**
     * @deprecated This method is no longer functional since OpenPdf removal.
     * Font handling is now managed by the DSS framework.
     * @return null always - method is deprecated
     */
    @Deprecated
    public static synchronized Object getL2BaseFont() {
        // This method is deprecated and no longer functional
        // Font handling is now done by DSS framework
        return null;
    }

}
