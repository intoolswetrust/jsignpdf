package net.sf.jsignpdf.fx.portal;

import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/** Proxy interface for {@code org.freedesktop.portal.FileChooser}. */
@DBusInterfaceName("org.freedesktop.portal.FileChooser")
interface XdgFileChooser extends DBusInterface {

    DBusPath OpenFile(String parentWindow, String title, Map<String, Variant<?>> options);

    DBusPath SaveFile(String parentWindow, String title, Map<String, Variant<?>> options);
}
