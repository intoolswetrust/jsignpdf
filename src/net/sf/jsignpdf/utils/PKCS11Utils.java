package net.sf.jsignpdf.utils;

import java.io.File;
import java.security.Provider;
import java.security.Security;

/**
 * Methods for handling PKCS#11 security providers.
 * 
 * @author Josef Cacek
 */
public class PKCS11Utils {

  /**
   * Tries to register the sun.security.pkcs11.SunPKCS11 provider with
   * configuration provided in the given file.
   * 
   * @param configPath
   *          path to PKCS#11 provider configuration file
   * @return true if provider succesfully registered; false otherwise
   */
  public static boolean registerProvider(final String configPath) {
    if (StringUtils.isEmpty(configPath)) {
      return false;
    }
    final File cfgFile = new File(configPath);
    final String absolutePath = cfgFile.getAbsolutePath();
    if (cfgFile.isFile()) {
      try {
        Security.addProvider((Provider) Class.forName("sun.security.pkcs11.SunPKCS11").getConstructor(String.class)
            .newInstance(absolutePath));
        return true;
      } catch (Exception e) {
        System.err.println("Unable to register SunPKCS11 security provider.");
        e.printStackTrace();
      }
    } else {
      System.err.println("The PKCS#11 provider is not registered. Configuration file doesn't exist: " + absolutePath);
    }
    return false;
  }
}
