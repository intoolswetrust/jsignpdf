package net.sf.jsignpdf.engine;

/**
 * Read-only view of the application-global advanced configuration, scoped to a single engine's
 * {@code engine.<id>.*} namespace. An engine reads its own knobs through this view without knowing
 * the namespacing convention &mdash; e.g. the DSS engine will read {@code getString("padesLevel", "B")}
 * which resolves {@code engine.dss.padesLevel} under the hood. The main module supplies the correctly
 * scoped instance to {@link SigningEngine#sign(net.sf.jsignpdf.BasicSignerOptions, EngineConfig)}.
 *
 * @author Josef Cacek
 */
public interface EngineConfig {

    /**
     * @param key engine-relative key (without the {@code engine.<id>.} prefix)
     * @return the value, or {@code null} if absent
     */
    String getString(String key);

    /**
     * @param key engine-relative key
     * @param fallback value returned when the key is absent
     * @return the value or {@code fallback}
     */
    String getString(String key, String fallback);

    /**
     * @param key engine-relative key
     * @param fallback value returned when the key is absent or not parseable
     * @return the boolean value or {@code fallback}
     */
    boolean getBoolean(String key, boolean fallback);

    /**
     * @param key engine-relative key
     * @param fallback value returned when the key is absent or not parseable
     * @return the int value or {@code fallback}
     */
    int getInt(String key, int fallback);
}
