package net.sf.jsignpdf.fx.portal;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.types.UInt32;

import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;

/**
 * Maps {@link ExtensionFilter} values (facade representation) to
 * {@link PortalFilter} structs for the {@code a(sa(us))} portal option.
 * Insertion order of the input list is preserved — the "All Files" filter
 * at index 0 in site 3 (CertificateSettingsController) must not be reshuffled.
 */
final class FilterMarshaller {

    private static final UInt32 GLOB_TYPE = new UInt32(0);

    private FilterMarshaller() {}

    static List<PortalFilter> marshal(List<ExtensionFilter> filters) {
        List<PortalFilter> result = new ArrayList<>(filters.size());
        for (ExtensionFilter f : filters) {
            result.add(toPortalFilter(f));
        }
        return result;
    }

    static PortalFilter toPortalFilter(ExtensionFilter f) {
        List<FilterPattern> patterns = new ArrayList<>(f.extensions().size());
        for (String ext : f.extensions()) {
            patterns.add(new FilterPattern(GLOB_TYPE, ext));
        }
        return new PortalFilter(f.description(), patterns);
    }
}
