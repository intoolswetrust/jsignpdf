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
package net.sf.jsignpdf.ooo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.registry.XRegistryKey;

/**
 * 
 * @author Josef Cacek
 */
public class CentralRegistrationClass {

  public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
    String regClassesList = getRegistrationClasses();
    StringTokenizer t = new StringTokenizer(regClassesList, " ");
    while (t.hasMoreTokens()) {
      String className = t.nextToken();
      if (className != null && className.length() != 0) {
        try {
          @SuppressWarnings("rawtypes")
          Class regClass = Class.forName(className);
          Method writeRegInfo = regClass.getDeclaredMethod("__getComponentFactory", new Class[] { String.class });
          Object result = writeRegInfo.invoke(regClass, sImplementationName);
          if (result != null) {
            return (XSingleComponentFactory) result;
          }
        } catch (ClassNotFoundException ex) {
          ex.printStackTrace();
        } catch (ClassCastException ex) {
          ex.printStackTrace();
        } catch (SecurityException ex) {
          ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
          ex.printStackTrace();
        } catch (IllegalArgumentException ex) {
          ex.printStackTrace();
        } catch (InvocationTargetException ex) {
          ex.printStackTrace();
        } catch (IllegalAccessException ex) {
          ex.printStackTrace();
        }
      }
    }
    return null;
  }

  public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
    boolean bResult = true;
    String regClassesList = getRegistrationClasses();
    StringTokenizer t = new StringTokenizer(regClassesList, " ");
    while (t.hasMoreTokens()) {
      String className = t.nextToken();
      if (className != null && className.length() != 0) {
        try {
          @SuppressWarnings("rawtypes")
          Class regClass = Class.forName(className);
          Method writeRegInfo = regClass.getDeclaredMethod("__writeRegistryServiceInfo",
              new Class[] { XRegistryKey.class });
          Object result = writeRegInfo.invoke(regClass, xRegistryKey);
          bResult &= ((Boolean) result).booleanValue();
        } catch (ClassNotFoundException ex) {
          ex.printStackTrace();
        } catch (ClassCastException ex) {
          ex.printStackTrace();
        } catch (SecurityException ex) {
          ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
          ex.printStackTrace();
        } catch (IllegalArgumentException ex) {
          ex.printStackTrace();
        } catch (InvocationTargetException ex) {
          ex.printStackTrace();
        } catch (IllegalAccessException ex) {
          ex.printStackTrace();
        }
      }
    }
    return bResult;
  }

  private static String getRegistrationClasses() {
    CentralRegistrationClass c = new CentralRegistrationClass();
    String name = c.getClass().getCanonicalName().replace('.', '/').concat(".class");
    try {
      Enumeration<URL> urlEnum = c.getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
      while (urlEnum.hasMoreElements()) {
        URL url = urlEnum.nextElement();
        JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
        Manifest mf = jarConnection.getManifest();

        Attributes attrs = (Attributes) mf.getAttributes(name);
        if (attrs != null) {
          String classes = attrs.getValue("RegistrationClasses");
          return classes;
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    return "";
  }

  /** Creates a new instance of CentralRegistrationClass */
  private CentralRegistrationClass() {
  }
}
