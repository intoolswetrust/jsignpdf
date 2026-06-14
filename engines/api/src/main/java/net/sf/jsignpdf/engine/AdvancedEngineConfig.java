package net.sf.jsignpdf.engine;

import net.sf.jsignpdf.utils.AdvancedConfig;

/**
 * {@link EngineConfig} backed by {@link AdvancedConfig}, resolving engine-relative keys under a fixed
 * {@code engine.<id>.} prefix.
 *
 * @author Josef Cacek
 */
public final class AdvancedEngineConfig implements EngineConfig {

    private final AdvancedConfig cfg;
    private final String prefix;

    /**
     * @param cfg the backing advanced configuration
     * @param prefix the {@code engine.<id>.} key prefix (including the trailing dot)
     */
    public AdvancedEngineConfig(AdvancedConfig cfg, String prefix) {
        this.cfg = cfg;
        this.prefix = prefix;
    }

    @Override
    public String getString(String key) {
        return cfg.getProperty(prefix + key);
    }

    @Override
    public String getString(String key, String fallback) {
        return cfg.getProperty(prefix + key, fallback);
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return cfg.getAsBool(prefix + key, fallback);
    }

    @Override
    public int getInt(String key, int fallback) {
        return cfg.getAsInt(prefix + key, fallback);
    }
}
