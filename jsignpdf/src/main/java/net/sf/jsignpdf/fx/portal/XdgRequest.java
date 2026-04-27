package net.sf.jsignpdf.fx.portal;

import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/** Proxy interface for {@code org.freedesktop.portal.Request}. */
@DBusInterfaceName("org.freedesktop.portal.Request")
interface XdgRequest extends DBusInterface {
    void Close();

    /**
     * DBus signal emitted by the XDG Desktop Portal on {@code org.freedesktop.portal.Request}.
     * Carries the result of an {@code OpenFile} or {@code SaveFile} call.
     * <p>
     * {@code response} values: 0 = success, 1 = user cancelled, 2 = error.
     */
    @DBusInterfaceName("org.freedesktop.portal.Request")
    public static class Response extends DBusSignal {

        private final UInt32 response;
        private final Map<String, Variant<?>> results;

        public Response(String path, UInt32 response, Map<String, Variant<?>> results) throws DBusException {
            super(path, response, results);
            this.response = response;
            this.results = results;
        }

        public UInt32 getResponse() {
            return response;
        }

        public Map<String, Variant<?>> getResults() {
            return results;
        }
    }

}
