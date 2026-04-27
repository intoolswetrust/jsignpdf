package net.sf.jsignpdf.fx.portal;

import java.util.List;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * DBus struct representing one entry in the {@code a(sa(us))} filters option:
 * a human-readable label plus a list of {@link FilterPattern}s.
 */
public final class PortalFilter extends Struct {

    @Position(0)
    private final String label;

    @Position(1)
    private final List<FilterPattern> patterns;

    public PortalFilter(String label, List<FilterPattern> patterns) {
        this.label = label;
        this.patterns = patterns;
    }

    public String getLabel() {
        return label;
    }

    public List<FilterPattern> getPatterns() {
        return patterns;
    }
}
