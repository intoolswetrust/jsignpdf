package net.sf.jsignpdf.fx.portal;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

/** Proxy interface for {@code org.freedesktop.portal.Request}. */
@DBusInterfaceName("org.freedesktop.portal.Request")
interface XdgRequest extends DBusInterface {
    void Close();
}
