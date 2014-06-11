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
import java.security.Provider;
import java.security.Security;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Methods for handling PKCS#11 security providers.
 * 
 * @author Josef Cacek
 */
public class PKCS11Utils {

	private static final Logger LOGGER = Logger.getLogger(PKCS11Utils.class);

	/**
	 * Tries to register the sun.security.pkcs11.SunPKCS11 provider with
	 * configuration provided in the given file.
	 * 
	 * @param configPath
	 *            path to PKCS#11 provider configuration file
	 * @return newly registered PKCS#11 provider name if provider successfully
	 *         registered; <code>null</code> otherwise
	 */
	public static String registerProvider(final String configPath) {
		if (StringUtils.isEmpty(configPath)) {
			return null;
		}
		LOGGER.debug("Registering SunPKCS11 provider from configuration in " + configPath);
		final File cfgFile = new File(configPath);
		final String absolutePath = cfgFile.getAbsolutePath();
		if (cfgFile.isFile()) {
			try {
				Provider pkcs11Provider = (Provider) Class.forName("sun.security.pkcs11.SunPKCS11")
						.getConstructor(String.class).newInstance(absolutePath);
				Security.addProvider(pkcs11Provider);
				final String name = pkcs11Provider.getName();
				LOGGER.debug("SunPKCS11 provider registered with name " + name);
				return name;
			} catch (Exception e) {
				System.err.println("Unable to register SunPKCS11 security provider.");
				e.printStackTrace();
			}
		} else {
			System.err.println("The PKCS#11 provider is not registered. Configuration file doesn't exist: "
					+ absolutePath);
		}
		return null;
	}

	/**
	 * Unregisters security provider with given name.
	 * <p>
	 * Some tokens/card-readers hangs during second usage of the program, they
	 * have to be unplugged and plugged again following code should prevent this
	 * issue.
	 * </p>
	 * 
	 * @param providerName
	 */
	public static void unregisterProvider(final String providerName) {
		if (providerName != null) {
			LOGGER.debug("Removing security provider with name " + providerName);
			Security.removeProvider(providerName);
			//we should wait a little bit to de-register provider correctly (is it a driver issue?)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
