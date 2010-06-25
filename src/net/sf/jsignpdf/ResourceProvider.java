package net.sf.jsignpdf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point to internationalization. Resource bundles has base
 * "translation/messages".
 * 
 * @author Josef Cacek [josef.cacek (at) gmail.com]
 * @author $Author: kwart $
 * @version $Revision: 1.4 $
 * @created $Date: 2010/06/25 15:30:45 $
 */
public class ResourceProvider extends AbstractBean {

	private static final ResourceProvider provider = new ResourceProvider();

	private Map<String, ResourceBundleBean> bundleMap = Collections
			.synchronizedMap(new HashMap<String, ResourceBundleBean>());

	private ResourceProvider() {
	}

	/**
	 * Returns singleton of ResourceProvider
	 * 
	 * @return singleton
	 */
	public static ResourceBundleBean getBundleBean() {
		return getBundleBean(Constants.RESOURCE_BUNDLE_BASE);
	}

	/**
	 * Returns singleton of ResourceProvider
	 * 
	 * @return singleton
	 */
	public static ResourceBundleBean getBundleBean(String aBaseName) {
		return provider.getBundleBeanInternal(aBaseName);
	}

	private ResourceBundleBean getBundleBeanInternal(String aBaseName) {
		if (!bundleMap.containsKey(aBaseName)) {
			synchronized (bundleMap) {
				if (!bundleMap.containsKey(aBaseName)) {
					bundleMap.put(aBaseName, new ResourceBundleBean(aBaseName));
				}
			}
		}
		return bundleMap.get(aBaseName);
	}

}
