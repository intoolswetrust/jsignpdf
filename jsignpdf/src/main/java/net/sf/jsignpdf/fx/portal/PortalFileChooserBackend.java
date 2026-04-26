package net.sf.jsignpdf.fx.portal;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;

import static net.sf.jsignpdf.Constants.LOGGER;

/**
 * Portal client that talks to {@code org.freedesktop.portal.FileChooser} over D-Bus.
 * Maintains a lazy singleton session-bus connection for the lifetime of the JVM.
 */
public final class PortalFileChooserBackend {

    private static final String PORTAL_BUS  = "org.freedesktop.portal.Desktop";
    private static final String PORTAL_PATH = "/org/freedesktop/portal/desktop";
    private static final int    HANDLE_TIMEOUT_SEC = 10;

    private static volatile DBusConnection CONN;

    private static final ExecutorService PORTAL_CALL_EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "jsignpdf-portal-call");
        t.setDaemon(true);
        return t;
    });

    private PortalFileChooserBackend() {}

    // -----------------------------------------------------------------------
    // Portal availability probe (no internal cache — caller caches)
    // -----------------------------------------------------------------------

    public static boolean checkPortalReachable() {
        try {
            DBus dbus = conn().getRemoteObject(
                    "org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
            return dbus.NameHasOwner(PORTAL_BUS);
        } catch (Exception e) {
            LOGGER.fine("D-Bus portal probe failed: " + e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Open / Save
    // -----------------------------------------------------------------------

    public static Optional<List<Path>> openFile(
            String title,
            List<ExtensionFilter> filters,
            ExtensionFilter selectedFilter,
            File initialDirectory,
            boolean multiple) throws Exception {

        DBusConnection c = conn();
        String[] handle = computeHandle(c);
        String token       = handle[0];
        String requestPath = handle[1];

        Map<String, Variant<?>> options = buildCommonOptions(token, filters, selectedFilter);
        if (multiple) {
            options.put("multiple", new Variant<>(Boolean.TRUE));
        }
        if (initialDirectory != null) {
            options.put("current_folder",
                    new Variant<>(nullTerminatedUtf8(initialDirectory.getAbsolutePath()), "ay"));
        }

        return callPortal(c, requestPath, () -> {
            XdgFileChooser fc = c.getRemoteObject(PORTAL_BUS, PORTAL_PATH, XdgFileChooser.class);
            fc.OpenFile("", title, options);
        });
    }

    public static Optional<List<Path>> saveFile(
            String title,
            List<ExtensionFilter> filters,
            ExtensionFilter selectedFilter,
            File initialDirectory,
            String initialFileName) throws Exception {

        DBusConnection c = conn();
        String[] handle = computeHandle(c);
        String token       = handle[0];
        String requestPath = handle[1];

        Map<String, Variant<?>> options = buildCommonOptions(token, filters, selectedFilter);
        if (initialFileName != null && !initialFileName.isEmpty()) {
            options.put("current_name", new Variant<>(initialFileName));
        }
        if (initialDirectory != null) {
            options.put("current_folder",
                    new Variant<>(nullTerminatedUtf8(initialDirectory.getAbsolutePath()), "ay"));
        }

        return callPortal(c, requestPath, () -> {
            XdgFileChooser fc = c.getRemoteObject(PORTAL_BUS, PORTAL_PATH, XdgFileChooser.class);
            fc.SaveFile("", title, options);
        });
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static Map<String, Variant<?>> buildCommonOptions(
            String token,
            List<ExtensionFilter> filters,
            ExtensionFilter selectedFilter) {

        Map<String, Variant<?>> opts = new HashMap<>();
        opts.put("handle_token", new Variant<>(token));
        opts.put("modal", new Variant<>(Boolean.TRUE));
        if (!filters.isEmpty()) {
            List<PortalFilter> pf = FilterMarshaller.marshal(filters);
            opts.put("filters", new Variant<>(pf, "a(sa(us))"));
            if (selectedFilter != null) {
                opts.put("current_filter",
                        new Variant<>(FilterMarshaller.toPortalFilter(selectedFilter), "(sa(us))"));
            }
        }
        return opts;
    }

    @FunctionalInterface
    private interface PortalCall {
        void invoke() throws DBusException;
    }

    private static Optional<List<Path>> callPortal(
            DBusConnection c,
            String requestPath,
            PortalCall call) throws Exception {

        CompletableFuture<Optional<List<Path>>> future = new CompletableFuture<>();
        AtomicBoolean settled = new AtomicBoolean(false);

        // Subscribe BEFORE the method call to avoid the handle_token race (design §3).
        AutoCloseable handler = c.addSigHandler(Response.class, signal -> {
            if (!requestPath.equals(signal.getPath())) return;
            if (!settled.compareAndSet(false, true)) return;
            completeFromSignal(signal, future);
        });

        try {
            // 10 s timeout for the initial method call — a flaky bus manifests here.
            CompletableFuture.runAsync(() -> {
                try { call.invoke(); } catch (DBusException e) { throw new RuntimeException(e); }
            }, PORTAL_CALL_EXEC).get(HANDLE_TIMEOUT_SEC, TimeUnit.SECONDS);

            // Wait indefinitely for the Response signal — the user is browsing.
            return future.get();

        } catch (Exception e) {
            settled.set(true);
            future.cancel(false);
            try {
                XdgRequest req = c.getRemoteObject(PORTAL_BUS, requestPath, XdgRequest.class);
                req.Close();
            } catch (Exception closeEx) {
                LOGGER.fine("Failed to close abandoned portal request: " + closeEx.getMessage());
            }
            throw e;
        } finally {
            try { handler.close(); } catch (Exception ignored) {}
        }
    }

    private static void completeFromSignal(Response signal, CompletableFuture<Optional<List<Path>>> future) {
        int code = signal.getResponse().intValue();
        if (code == 0) {
            try {
                List<String> uris = extractUris(signal.getResults());
                List<Path> paths = new ArrayList<>(uris.size());
                for (String uri : uris) {
                    URI u = URI.create(uri);
                    if (!"file".equals(u.getScheme())) {
                        throw new IllegalStateException("Portal returned non-file URI: " + uri);
                    }
                    paths.add(Path.of(u));
                }
                future.complete(Optional.of(paths));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else if (code == 1) {
            future.complete(Optional.empty()); // user cancelled
        } else {
            future.completeExceptionally(
                    new Exception("XDG portal returned error response: " + code));
        }
    }

    private static List<String> extractUris(Map<String, Variant<?>> results) {
        if (results == null) return List.of();
        Variant<?> v = results.get("uris");
        if (v == null) return List.of();
        Object val = v.getValue();
        if (!(val instanceof List)) return List.of();
        List<?> raw = (List<?>) val;
        List<String> uris = new ArrayList<>(raw.size());
        for (Object element : raw) {
            if (!(element instanceof String)) {
                throw new IllegalStateException(
                        "Portal returned malformed 'uris': expected String, got " +
                        (element == null ? "null" : element.getClass().getName()));
            }
            uris.add((String) element);
        }
        return uris;
    }

    private static String[] computeHandle(DBusConnection c) {
        // Unique name is e.g. ":1.234" → sender "1_234"
        String unique = c.getUniqueName();
        String sender = unique.substring(1).replace('.', '_');
        String token  = "jsignpdf_" + ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        String path   = "/org/freedesktop/portal/desktop/request/" + sender + "/" + token;
        return new String[]{token, path};
    }

    private static byte[] nullTerminatedUtf8(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        byte[] r = new byte[b.length + 1];
        System.arraycopy(b, 0, r, 0, b.length);
        // last byte is 0 by default
        return r;
    }

    // -----------------------------------------------------------------------
    // Lazy singleton connection — closed via JVM shutdown hook
    // -----------------------------------------------------------------------

    private static DBusConnection conn() throws DBusException {
        DBusConnection c = CONN;
        if (c == null) {
            synchronized (PortalFileChooserBackend.class) {
                c = CONN;
                if (c == null) {
                    c = DBusConnectionBuilder.forSessionBus().withShared(false).build();
                    final DBusConnection toClose = c;
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try { toClose.disconnect(); } catch (Exception ignored) {}
                    }, "jsignpdf-dbus-shutdown"));
                    CONN = c;
                }
            }
        }
        return c;
    }
}
