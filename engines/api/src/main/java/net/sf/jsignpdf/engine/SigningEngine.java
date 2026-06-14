package net.sf.jsignpdf.engine;

import java.util.Set;

import net.sf.jsignpdf.BasicSignerOptions;

/**
 * A pluggable PDF signing backend. Implementations are discovered at runtime via
 * {@link java.util.ServiceLoader}; each engine jar registers itself through a
 * {@code META-INF/services/net.sf.jsignpdf.engine.SigningEngine} file.
 *
 * @author Josef Cacek
 */
public interface SigningEngine {

    /** Stable, lower-case identifier used in config files and CLI args. */
    String id();

    /** Human-readable name shown in {@code --list-engines} and the FX dropdown. */
    String displayName();

    /**
     * Static capability set for this engine. The returned set is immutable and identical across all
     * calls and instances of the same engine.
     *
     * @return the supported capabilities
     */
    Set<Capability> capabilities();

    /**
     * Signs the file described by {@code options}. The implementation owns its own logging via the
     * same SLF4J/JUL bridge the rest of JSignPdf uses.
     *
     * @param options the signing options
     * @param engineConfig engine-scoped view of the advanced configuration
     * @return {@code true} on success, {@code false} on a recoverable error (already logged); throws
     *         for unrecoverable problems
     */
    boolean sign(BasicSignerOptions options, EngineConfig engineConfig);
}
