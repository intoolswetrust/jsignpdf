package net.sf.jsignpdf.engine;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.utils.AppConfig;

/**
 * Discovers the {@link SigningEngine} implementations available on the classpath through
 * {@link ServiceLoader} and resolves which one to use for a given invocation.
 *
 * <p>
 * Engines register themselves via a {@code META-INF/services/net.sf.jsignpdf.engine.SigningEngine}
 * file. The result is loaded once and cached for the JVM lifetime; duplicate ids are detected at
 * construction time, logged as a warning, and the first-loaded entry wins (deterministic by classpath
 * order).
 * </p>
 *
 * @author Josef Cacek
 */
public final class EngineRegistry {

    private static final EngineRegistry INSTANCE = new EngineRegistry();

    private final Map<String, SigningEngine> byId;
    private final List<SigningEngine> sorted;

    private EngineRegistry() {
        final Map<String, SigningEngine> map = new LinkedHashMap<>();
        for (SigningEngine engine : ServiceLoader.load(SigningEngine.class)) {
            final String id = engine.id();
            if (map.containsKey(id)) {
                LOGGER.warning("Duplicate signing-engine id '" + id + "' from " + engine.getClass().getName()
                        + " ignored; keeping " + map.get(id).getClass().getName());
                continue;
            }
            map.put(id, engine);
        }
        this.byId = Collections.unmodifiableMap(map);

        final String defaultId = AppConfig.DEFAULT_ENGINE_ID;
        final List<SigningEngine> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparing((SigningEngine e) -> !e.id().equals(defaultId)).thenComparing(SigningEngine::id));
        this.sorted = Collections.unmodifiableList(list);
    }

    /**
     * @return the shared registry instance
     */
    public static EngineRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * @param id the engine id (case-insensitive)
     * @return the engine, or empty if none is registered with that id
     */
    public Optional<SigningEngine> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        SigningEngine engine = byId.get(id);
        if (engine == null) {
            // be lenient about case so config files / CLI args are forgiving
            for (SigningEngine e : byId.values()) {
                if (e.id().equalsIgnoreCase(id)) {
                    engine = e;
                    break;
                }
            }
        }
        return Optional.ofNullable(engine);
    }

    /**
     * @return all registered engines, default first, then ordered by id
     */
    public List<SigningEngine> listAll() {
        return sorted;
    }

    /**
     * Resolves the default engine: the one selected in {@code advanced.properties} (falling back to the
     * bundled {@value net.sf.jsignpdf.utils.AppConfig#DEFAULT_ENGINE_ID}), or the first registered engine
     * if neither is available.
     *
     * @return the default engine, or empty if no engines are registered at all
     */
    public Optional<SigningEngine> getDefault() {
        Optional<SigningEngine> configured = findById(AppConfig.defaultEngineId());
        if (configured.isPresent()) {
            return configured;
        }
        Optional<SigningEngine> bundled = findById(AppConfig.DEFAULT_ENGINE_ID);
        if (bundled.isPresent()) {
            return bundled;
        }
        return sorted.isEmpty() ? Optional.empty() : Optional.of(sorted.get(0));
    }

    /**
     * Resolves the engine for a signing invocation following the precedence: the CLI override
     * ({@link BasicSignerOptions#getEngine()}), then {@code advanced.properties}, then the bundled default.
     *
     * @param options the signing options
     * @return the resolved engine
     * @throws IllegalArgumentException if an explicitly requested engine id is not registered
     */
    public SigningEngine resolve(BasicSignerOptions options) {
        final String requested = options.getEngine();
        if (requested != null && !requested.isEmpty()) {
            return findById(requested).orElseThrow(
                    () -> new IllegalArgumentException("Unknown signing engine '" + requested + "'"));
        }
        return getDefault().orElseThrow(() -> new IllegalStateException("No signing engine is registered"));
    }
}
