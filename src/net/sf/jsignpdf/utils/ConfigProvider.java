package net.sf.jsignpdf.utils;

import net.sf.jsignpdf.Constants;

/**
 * Property holder for tweak file.
 * 
 * @author Josef Cacek
 */
public class ConfigProvider extends PropertyProvider {

	private static final ConfigProvider provider = new ConfigProvider();

	protected ConfigProvider() {
	}

	/**
	 * Returns instance of this class. (singleton)
	 * 
	 * @return
	 */
	public static ConfigProvider getInstance() {
		return provider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jsignpdf.PropertyProvider#loadDefault()
	 */
	@Override
	public void loadDefault() throws ProperyProviderException {
		loadProperties(Constants.CONF_FILE);
	}
}
