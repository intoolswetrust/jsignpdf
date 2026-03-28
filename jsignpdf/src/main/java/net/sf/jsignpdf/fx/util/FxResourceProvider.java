package net.sf.jsignpdf.fx.util;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.ResourceProvider;

/**
 * Adapts ResourceProvider for JavaFX StringBindings,
 * enabling easy binding of i18n strings to JavaFX UI properties.
 */
public final class FxResourceProvider {

    private static final ResourceProvider RES = Constants.RES;

    private FxResourceProvider() {
    }

    /**
     * Get a localized string by key.
     */
    public static String get(String key) {
        return RES.get(key);
    }

    /**
     * Get a localized string with parameters.
     */
    public static String get(String key, String... args) {
        return RES.get(key, args);
    }

    /**
     * Create a StringBinding for a resource key.
     */
    public static StringBinding createStringBinding(String key) {
        return new StringBinding() {
            @Override
            protected String computeValue() {
                return RES.get(key);
            }
        };
    }
}
