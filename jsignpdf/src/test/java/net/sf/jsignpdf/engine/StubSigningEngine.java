package net.sf.jsignpdf.engine;

import java.util.EnumSet;
import java.util.Set;

import net.sf.jsignpdf.BasicSignerOptions;

/**
 * Test-only {@link SigningEngine} with a configurable capability set and a no-op {@link #sign} so the
 * engine-mismatch validator can be exercised against deliberately reduced capabilities. Phase 1 ships
 * only the all-capable OpenPDF engine, so the validator's failure paths are only reachable via a stub.
 */
public final class StubSigningEngine implements SigningEngine {

    private final String id;
    private final Set<Capability> capabilities;

    public StubSigningEngine(String id, Capability... caps) {
        this.id = id;
        this.capabilities = caps.length == 0 ? EnumSet.noneOf(Capability.class) : EnumSet.copyOf(Set.of(caps));
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return "Stub(" + id + ")";
    }

    @Override
    public Set<Capability> capabilities() {
        return capabilities;
    }

    @Override
    public boolean sign(BasicSignerOptions options, EngineConfig engineConfig) {
        return true;
    }
}
