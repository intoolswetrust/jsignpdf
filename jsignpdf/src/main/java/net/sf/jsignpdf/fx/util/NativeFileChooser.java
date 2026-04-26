package net.sf.jsignpdf.fx.util;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import net.sf.jsignpdf.fx.portal.PortalFileChooserBackend;

import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

/**
 * Drop-in replacement for {@link javafx.stage.FileChooser} that transparently routes
 * file-picker calls through the XDG Desktop Portal when running inside a Flatpak or
 * Snap sandbox on Linux. On all other platforms, and on non-sandboxed Linux, it
 * delegates directly to the JavaFX {@code FileChooser}.
 *
 * <p>Developer override: {@code -Djsignpdf.filechooser=fx|portal|auto} (default {@code auto}).
 * Documented in the Flatpak/Snap troubleshooting section only. On non-Linux, setting
 * {@code portal} silently degrades to FX.
 */
public final class NativeFileChooser {

    /** A filter entry: a human-readable description and a list of glob patterns. */
    public record ExtensionFilter(String description, List<String> extensions) {

        public static ExtensionFilter of(String desc, String... exts) {
            return new ExtensionFilter(desc, List.of(exts));
        }
    }

    // -----------------------------------------------------------------------
    // Per-session static state
    // -----------------------------------------------------------------------

    private enum Override { FX, PORTAL, AUTO }

    private static final Override OVERRIDE = resolveOverride();

    /** Cached portal reachability (null = not yet probed). */
    private static volatile Boolean portalReachable = null;

    /** Set to true after MAX_PORTAL_FAILURES consecutive failures; causes session-wide FX fallback. */
    private static volatile boolean portalDisabled = false;

    private static final int MAX_PORTAL_FAILURES = 3;
    private static final AtomicInteger portalFailureCount = new AtomicInteger(0);

    /** Set after the one-time fallback Alert has been shown. */
    private static volatile boolean portalAlertShown = false;

    // -----------------------------------------------------------------------
    // Per-instance state (mirrors FileChooser API)
    // -----------------------------------------------------------------------

    private String title;
    private final List<ExtensionFilter> filters = new ArrayList<>();
    private ExtensionFilter selectedFilter;
    private File initialDirectory;
    private String initialFileName;

    // -----------------------------------------------------------------------
    // Builder-style setters (fluent)
    // -----------------------------------------------------------------------

    public NativeFileChooser setTitle(String t) {
        this.title = t;
        return this;
    }

    public NativeFileChooser addFilter(ExtensionFilter f) {
        filters.add(f);
        return this;
    }

    /** Sets the pre-selected filter. Only populate portal {@code current_filter} when called. */
    public NativeFileChooser setSelectedFilter(ExtensionFilter f) {
        this.selectedFilter = f;
        return this;
    }

    public NativeFileChooser setInitialDirectory(File d) {
        this.initialDirectory = d;
        return this;
    }

    public NativeFileChooser setInitialFileName(String n) {
        this.initialFileName = n;
        return this;
    }

    // -----------------------------------------------------------------------
    // Show methods — same contract as JavaFX FileChooser
    // -----------------------------------------------------------------------

    public File showOpenDialog(Window owner) {
        if (shouldUsePortal()) {
            try {
                Optional<List<Path>> result =
                        PortalFileChooserBackend.openFile(title, filters, selectedFilter, initialDirectory, false);
                markPortalSuccess();
                if (result.isEmpty()) return null;
                List<Path> paths = result.get();
                if (paths.isEmpty()) return null;
                return toFileIfOwnerShowing(paths.get(0), owner);
            } catch (Exception e) {
                markPortalFailed(e);
            }
        }
        File result = fxOpenDialog(owner);
        if (portalDisabled) showFallbackAlertOnce(owner);
        return result;
    }

    public List<File> showOpenMultipleDialog(Window owner) {
        if (shouldUsePortal()) {
            try {
                Optional<List<Path>> result =
                        PortalFileChooserBackend.openFile(title, filters, selectedFilter, initialDirectory, true);
                markPortalSuccess();
                if (result.isEmpty()) return null;
                List<Path> paths = result.get();
                if (owner != null && !owner.isShowing()) return null;
                List<File> files = new ArrayList<>(paths.size());
                for (Path p : paths) files.add(p.toFile());
                return files;
            } catch (Exception e) {
                markPortalFailed(e);
            }
        }
        List<File> result = fxOpenMultipleDialog(owner);
        if (portalDisabled) showFallbackAlertOnce(owner);
        return result;
    }

    public File showSaveDialog(Window owner) {
        if (shouldUsePortal()) {
            try {
                Optional<List<Path>> result = PortalFileChooserBackend.saveFile(
                        title, filters, selectedFilter, initialDirectory, initialFileName);
                markPortalSuccess();
                if (result.isEmpty()) return null;
                List<Path> paths = result.get();
                if (paths.isEmpty()) return null;
                Path p = appendExtensionIfNeeded(paths.get(0));
                return toFileIfOwnerShowing(p, owner);
            } catch (Exception e) {
                markPortalFailed(e);
            }
        }
        File result = fxSaveDialog(owner);
        if (portalDisabled) showFallbackAlertOnce(owner);
        return result;
    }

    // -----------------------------------------------------------------------
    // Extension auto-append for Save (portal does not do this unlike JavaFX)
    // -----------------------------------------------------------------------

    Path appendExtensionIfNeeded(Path p) {
        String name = p.getFileName() != null ? p.getFileName().toString() : "";
        if (name.contains(".")) return p; // has extension — honor user intent
        String ext = singleGlobExtension();
        if (ext == null) return p;
        Path parent = p.getParent();
        String newName = name + "." + ext;
        return parent != null ? parent.resolve(newName) : Path.of(newName);
    }

    private String singleGlobExtension() {
        ExtensionFilter f = selectedFilter != null ? selectedFilter
                : (filters.isEmpty() ? null : filters.get(0));
        if (f == null) return null;
        List<String> exts = f.extensions();
        if (exts.size() != 1) return null; // multi-glob — do not guess
        String glob = exts.get(0);
        if (!glob.startsWith("*.")) return null;
        String bare = glob.substring(2);
        if (bare.isEmpty() || bare.contains(".") || bare.contains("*")) return null;
        return bare;
    }

    // -----------------------------------------------------------------------
    // Backend dispatch
    // -----------------------------------------------------------------------

    private boolean shouldUsePortal() {
        switch (OVERRIDE) {
            case FX:     return false;
            case PORTAL: {
                if (!Sandbox.isLinux()) {
                    LOGGER.fine("jsignpdf.filechooser=portal but not Linux; using FX");
                    return false;
                }
                return !portalDisabled && isPortalReachable();
            }
            default: // AUTO
                return Sandbox.isLinux()
                        && Sandbox.isSandboxed()
                        && !portalDisabled
                        && isPortalReachable();
        }
    }

    private static boolean isPortalReachable() {
        Boolean cached = portalReachable;
        if (cached != null) return cached;
        synchronized (NativeFileChooser.class) {
            cached = portalReachable;
            if (cached != null) return cached;
            try {
                boolean reachable = PortalFileChooserBackend.checkPortalReachable();
                if (!reachable) {
                    LOGGER.fine("XDG Desktop Portal not reachable; file chooser will use JavaFX");
                }
                portalReachable = reachable;
                return reachable;
            } catch (Exception e) {
                LOGGER.fine("Portal reachability probe error: " + e.getMessage());
                portalReachable = Boolean.FALSE;
                return false;
            }
        }
    }

    private static void markPortalSuccess() {
        portalFailureCount.set(0);
    }

    private static void markPortalFailed(Exception e) {
        String uuid = UUID.randomUUID().toString();
        LOGGER.log(Level.WARNING,
                "XDG portal file chooser failed [" + uuid + "]; falling back to JavaFX", e);
        if (portalFailureCount.incrementAndGet() >= MAX_PORTAL_FAILURES) {
            portalDisabled = true;
        }
    }

    private static void showFallbackAlertOnce(Window owner) {
        if (portalAlertShown) return;
        portalAlertShown = true;
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(RES.get("jfx.gui.dialog.portalFallback.title"));
            alert.setHeaderText(null);
            alert.setContentText(RES.get("jfx.gui.dialog.portalFallback.text"));
            if (owner != null) alert.initOwner(owner);
            alert.show(); // non-modal
        });
    }

    private static File toFileIfOwnerShowing(Path p, Window owner) {
        if (owner != null && !owner.isShowing()) return null;
        return p.toFile();
    }

    // -----------------------------------------------------------------------
    // JavaFX fallback backend
    // -----------------------------------------------------------------------

    private File fxOpenDialog(Window owner) {
        return buildFxChooser().showOpenDialog(owner);
    }

    private List<File> fxOpenMultipleDialog(Window owner) {
        return buildFxChooser().showOpenMultipleDialog(owner);
    }

    private File fxSaveDialog(Window owner) {
        FileChooser fc = buildFxChooser();
        if (initialFileName != null) {
            fc.setInitialFileName(initialFileName);
        }
        return fc.showSaveDialog(owner);
    }

    private FileChooser buildFxChooser() {
        FileChooser fc = new FileChooser();
        if (title != null) fc.setTitle(title);
        for (ExtensionFilter f : filters) {
            FileChooser.ExtensionFilter fxf =
                    new FileChooser.ExtensionFilter(f.description(), f.extensions());
            fc.getExtensionFilters().add(fxf);
        }
        if (selectedFilter != null) {
            for (FileChooser.ExtensionFilter fxf : fc.getExtensionFilters()) {
                if (fxf.getDescription().equals(selectedFilter.description())) {
                    fc.setSelectedExtensionFilter(fxf);
                    break;
                }
            }
        }
        if (initialDirectory != null && initialDirectory.isDirectory()) {
            fc.setInitialDirectory(initialDirectory);
        }
        return fc;
    }

    // -----------------------------------------------------------------------
    // Initialisation
    // -----------------------------------------------------------------------

    private static Override resolveOverride() {
        String prop = System.getProperty("jsignpdf.filechooser", "auto").trim();
        if ("fx".equalsIgnoreCase(prop))     return Override.FX;
        if ("portal".equalsIgnoreCase(prop)) return Override.PORTAL;
        return Override.AUTO;
    }
}
