package net.sf.jsignpdf.fx.portal;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * DBus struct representing a single filter pattern: {@code (us)} — type code + glob.
 * Type 0 = glob (e.g. "*.pdf"), type 1 = MIME type.
 */
public final class FilterPattern extends Struct {

    @Position(0)
    private final UInt32 type;

    @Position(1)
    private final String pattern;

    public FilterPattern(UInt32 type, String pattern) {
        this.type = type;
        this.pattern = pattern;
    }

    public UInt32 getType() {
        return type;
    }

    public String getPattern() {
        return pattern;
    }
}
