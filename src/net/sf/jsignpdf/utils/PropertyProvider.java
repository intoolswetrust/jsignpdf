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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

import net.sf.jsignpdf.Constants;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * This class provides basic functionality for work with property files. It
 * encapsulates work with java.util.Properties class.
 * 
 * @author Josef Cacek
 * @see java.util.Properties
 */
public class PropertyProvider {

  /**
   * Value used as replacement for null in Null Sensitive properties
   */
  public static final String NS_NULL_VALUE = "$$NULL$$";

  private final static Logger LOGGER = Logger.getLogger(KeyStoreUtils.class);

  /**
   * Unchecked exception used in PropertyProviders check.
   * 
   * @author Josef Cacek
   */
  public static class ProperyProviderException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProperyProviderException() {
      super();
    }

    public ProperyProviderException(String aMessage) {
      super(aMessage);
    }

    public ProperyProviderException(Throwable aCause) {
      super(aCause);
    }

    public ProperyProviderException(String aMessage, Throwable aCause) {
      super(aMessage, aCause);
    }
  }

  /**
   * <code>PROPERTY_FILE</code> contains default filename for property file.
   */
  public static final String PROPERTY_FILE = Constants.USER_HOME + "/" + Constants.PROPERTIES_FILE;

  /**
   * Singleton instance
   */
  private static PropertyProvider provider = new PropertyProvider();
  private Properties properties;

  /**
   * Creates new instance of PropertyProvider and loads default properties - if
   * exist.
   */
  protected PropertyProvider() {
    properties = new Properties();
    try {
      loadDefault();
    } catch (ProperyProviderException e) {
      // default file probably doesn't exist
      LOGGER.warn(e);
    }
  }

  /**
   * Returns singleton of this class (first call tries to load properties from
   * default file, if it exists).
   * 
   * @return instance of PropertyProvider
   * @see PropertyProvider#PROPERTY_FILE
   */
  public static PropertyProvider getInstance() {
    return provider;
  }

  /**
   * Loads properties from file with given filename.
   * 
   * @param aFileName
   *          name of file from which are properties loaded
   * @throws ProperyProviderException
   */
  public void loadProperties(String aFileName) throws ProperyProviderException {
    if (aFileName != null) {
      loadProperties(new File(aFileName));
    } else {
      throw new ProperyProviderException("Property filename is null!");
    }
  }

  /**
   * Loads properties from given file.
   * 
   * @param aFile
   * @throws ProperyProviderException
   */
  public void loadProperties(File aFile) throws ProperyProviderException {
    if (aFile != null && aFile.canRead()) {
      try {
        properties.load(new FileInputStream(aFile));
      } catch (Exception e) {
        throw new ProperyProviderException("Properties cannot be loaded", e);
      }
    } else {
      throw new ProperyProviderException("Property file " + aFile.getPath() + " doesn't exist.");
    }
  }

  /**
   * Loads properties from given inputstream.
   * 
   * @param anIS
   *          input stream to read properties from
   * @throws ProperyProviderException
   */
  public void loadProperties(InputStream anIS) throws ProperyProviderException {
    if (anIS != null) {
      try {
        properties.load(anIS);
      } catch (Exception e) {
        throw new ProperyProviderException("Properties cannot be loaded", e);
      }
    } else {
      throw new ProperyProviderException("InputStream can't be null.");
    }
  }

  /**
   * Sets property from given parameter. An expression must be in this form:
   * <code>propertyName=propertyValue</code>
   * 
   * @param anExpr
   *          string in this form: key=value
   * @throws ProperyProviderException
   *           parameter is not in desired form
   */
  public void setProperty(String anExpr) throws ProperyProviderException {
    int tmpPos;
    if (anExpr != null && (tmpPos = anExpr.indexOf('=')) > -1) {
      setProperty(anExpr.substring(0, tmpPos), anExpr.substring(tmpPos + 1));
    } else {
      throw new ProperyProviderException("Wrong setProperty(...) expression.");
    }
  }

  /**
   * Sets property with given name to given value
   * 
   * @param aKey
   *          name of a property
   * @param aValue
   *          value of a property
   */
  public void setProperty(String aKey, String aValue) {
    properties.setProperty(aKey, aValue == null ? "" : aValue);
  }

  /**
   * Sets null sensitive property.
   * 
   * @param aKey
   *          property name
   * @param aValue
   *          property value
   */
  public void setPropNullSensitive(final String aKey, final String aValue) {
    setProperty(aKey, aValue == null ? NS_NULL_VALUE : aValue);
  }

  /**
   * Sets boolean property with given name to given value
   * 
   * @param aKey
   *          name of a property
   * @param aValue
   *          value of a property
   */
  public void setProperty(String aKey, boolean aValue) {
    properties.setProperty(aKey, String.valueOf(aValue));
  }

  /**
   * Sets integer property with given name to given value
   * 
   * @param aKey
   *          name of a property
   * @param aValue
   *          value of a property
   */
  public void setProperty(String aKey, int aValue) {
    properties.setProperty(aKey, String.valueOf(aValue));
  }

  /**
   * Sets integer property with given name to given value
   * 
   * @param aKey
   *          name of a property
   * @param aValue
   *          value of a property
   */
  public void setProperty(String aKey, float aValue) {
    properties.setProperty(aKey, String.valueOf(aValue));
  }

  /**
   * Removes property.
   * 
   * @param aKey
   *          property name
   */
  public void removeProperty(final String aKey) {
    properties.remove(aKey);
  }

  /**
   * Returns value of property with given name.
   * 
   * @param aKey
   *          name of a property
   * @return value of property or null if property doesn't exist
   */
  public String getProperty(String aKey) {
    return properties.getProperty(aKey);
  }

  /**
   * Gets null sensitive property
   * 
   * @param aKey
   *          name of property
   * @return value of property. If value doesn't exist null is returned
   */
  public String getPropNullSensitive(final String aKey) {
    final String tmpValue = getProperty(aKey);
    return NS_NULL_VALUE.equals(tmpValue) ? null : tmpValue;
  }

  /**
   * Deletes all properties from PropertyProvider.
   */
  public void clear() {
    synchronized (properties) {
      properties.clear();
    }
  }

  /**
   * Loads properties from default file.
   * 
   * @throws ProperyProviderException
   */
  public void loadDefault() throws ProperyProviderException {
    loadProperties(PROPERTY_FILE);
  }

  /**
   * Save current set of properties holded by PropertyProvider to a given file.
   * 
   * @param aFile
   *          file to which will be properties saved
   * @throws ProperyProviderException
   */
  public void saveProperties(File aFile) throws ProperyProviderException {
    if (aFile != null) {
      try {
        synchronized (properties) {
          properties.store(new FileOutputStream(aFile), "Properties saved by PropertyProvider");
        }
      } catch (Exception e) {
        throw new ProperyProviderException("Properties cannot be stored", e);
      }
    } else {
      throw new ProperyProviderException("Property-file is null!");
    }
  }

  /**
   * Save current set of properties holded by PropertyProvider to a file with
   * given filename.
   * 
   * @param aFileName
   * @throws ProperyProviderException
   */
  public void saveProperties(String aFileName) throws ProperyProviderException {
    if (aFileName != null) {
      saveProperties(new File(aFileName));
    } else {
      throw new ProperyProviderException("Property filename is null!");
    }
  }

  /**
   * Save current set of properties to default file.
   * 
   * @throws ProperyProviderException
   */
  public void saveDefault() throws ProperyProviderException {
    saveProperties(PROPERTY_FILE);
  }

  /**
   * Returns value for given key converted to integer;
   * 
   * @param aKey
   * @return value from properties converted to integer (if value doesn't exist,
   *         0 is returned)
   * @see #exists(String)
   */
  public int getAsInt(String aKey) {
    return getAsInt(aKey, 0);
  }

  /**
   * Returns value for given key converted to integer; If property doesn't exist
   * default value is returned.
   * 
   * @param aKey
   * @param aDefault
   * @return value for given key as integer
   */
  public int getAsInt(String aKey, int aDefault) {
    int tmpResult = aDefault;
    synchronized (properties) {
      if (properties.containsKey(aKey)) {
        tmpResult = ConvertUtils.toInt(properties.getProperty(aKey), aDefault);
      }
    }
    return tmpResult;
  }

  /**
   * Returns value for given key converted to float;
   * 
   * @param aKey
   * @return value from properties converted to integer (if value doesn't exist,
   *         0 is returned)
   * @see #exists(String)
   */
  public float getAsFloat(String aKey) {
    return getAsFloat(aKey, 0f);
  }

  /**
   * Returns value for given key converted to float; If property doesn't exist
   * default value is returned.
   * 
   * @param aKey
   * @param aDefault
   * @return value for given key as integer
   */
  public float getAsFloat(String aKey, float aDefault) {
    float tmpResult = aDefault;
    synchronized (properties) {
      if (properties.containsKey(aKey)) {
        tmpResult = ConvertUtils.toFloat(properties.getProperty(aKey), aDefault);
      }
    }
    return tmpResult;
  }

  /**
   * Returns value for given key converted to long;
   * 
   * @param aKey
   * @return value from properties converted to long (if value doesn't exist, 0
   *         is returned)
   * @see #exists(String)
   */
  public long getAsLong(String aKey) {
    String tmpValue = properties.getProperty(aKey, "0");
    return Long.parseLong(tmpValue);
  }

  /**
   * Returns value for given key converted to boolean;
   * 
   * @param aKey
   * @return value from properties converted to boolean (if value doesn't exist,
   *         false is returned)
   * @see #exists(String)
   */
  public boolean getAsBool(String aKey) {
    return getAsBool(aKey, false);
  }

  /**
   * Returns value for given key converted to boolean - if key doesn't exists
   * returns default value
   * 
   * @param aKey
   *          property name
   * @param aDefault
   *          default value
   * @return value from properties converted to boolean (if value doesn't exist,
   *         default is returned)
   * @see #exists(String)
   */
  public boolean getAsBool(String aKey, boolean aDefault) {
    boolean tmpResult = aDefault;
    synchronized (properties) {
      if (properties.containsKey(aKey)) {
        tmpResult = ConvertUtils.toBoolean(properties.getProperty(aKey), aDefault);
      }
    }
    return tmpResult;
  }

  /**
   * Tests if exists property with given name.
   * 
   * @param aKey
   * @return true if exists property with given name
   */
  public boolean exists(String aKey) {
    return properties.containsKey(aKey);
  }

  /**
   * Throws PEException if given key doesn't exist.
   * 
   * @param aKey
   *          property name which must be included in properties, if not
   *          exception is thrown
   * @throws ProperyProviderException
   *           key doesn't exist
   */
  public void checkMandatory(String aKey) throws ProperyProviderException {
    if (!properties.containsKey(aKey)) {
      throw new ProperyProviderException("Mandatory property '" + aKey + "' is missing!");
    }
  }

  /**
   * Returns property value for the given name. If not found returns default
   * value (2nd param).
   * 
   * @param aKey
   *          property name
   * @param aDefault
   *          default value
   * @return value for given key
   */
  public String getProperty(String aKey, String aDefault) {
    return properties.getProperty(aKey, aDefault);
  }

  /**
   * Returns not-empty property value for the given name. If not found returns
   * default value (2nd param).
   * 
   * @param aKey
   *          property name
   * @param aDefault
   *          default value
   * @return value for given key
   */
  public String getNotEmptyProperty(String aKey, String aDefault) {
    final String tmpVal = properties.getProperty(aKey);
    return StringUtils.isEmpty(tmpVal) ? aDefault : tmpVal;
  }

}
