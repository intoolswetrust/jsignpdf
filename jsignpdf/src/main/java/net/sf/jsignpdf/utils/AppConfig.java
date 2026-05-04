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

import net.sf.jsignpdf.Constants;

/**
 * Static facade over {@link AdvancedConfig}. Call sites read app-global toggles through these typed accessors so they stay
 * compact and don't depend on the singleton-resolution path.
 */
public final class AppConfig {

    private AppConfig() {
    }

    public static boolean relaxSslSecurity() {
        return cfg().getAsBool("relax.ssl.security", false);
    }

    public static String pdf2imageLibraries() {
        return cfg().getNotEmptyProperty("pdf2image.libraries", Constants.PDF2IMAGE_LIBRARIES_DEFAULT);
    }

    public static String defaultTsaHashAlg() {
        return cfg().getNotEmptyProperty("tsa.hashAlgorithm", "SHA-256");
    }

    public static boolean checkValidity() {
        return cfg().getAsBool("certificate.checkValidity", true);
    }

    public static boolean checkKeyUsage() {
        return cfg().getAsBool("certificate.checkKeyUsage", true);
    }

    public static boolean checkCriticalExtensions() {
        return cfg().getAsBool("certificate.checkCriticalExtensions", false);
    }

    public static String fontPath() {
        return cfg().getNotEmptyProperty("font.path", null);
    }

    public static String fontName() {
        return cfg().getNotEmptyProperty("font.name", null);
    }

    public static String fontEncoding() {
        return cfg().getNotEmptyProperty("font.encoding", null);
    }

    private static AdvancedConfig cfg() {
        return PropertyStoreFactory.getInstance().advancedConfig();
    }
}
