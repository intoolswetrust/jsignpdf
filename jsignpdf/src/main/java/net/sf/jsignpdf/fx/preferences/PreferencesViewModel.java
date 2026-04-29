package net.sf.jsignpdf.fx.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.sf.jsignpdf.utils.AdvancedConfig;

/**
 * Backing model for the Preferences dialog. Loaded from {@link AdvancedConfig} on dialog open and written back on OK.
 * Properties match the keys in {@code advanced.properties}; the PDF-library order is encoded as three pairs (checkbox + 1/2/3
 * order index) so the UI can render an ordered list with up/down buttons.
 */
public class PreferencesViewModel {

    public static final String LIB_JPEDAL = "jpedal";
    public static final String LIB_PDFBOX = "pdfbox";
    public static final String LIB_OPENPDF = "openpdf";

    private final StringProperty fontPath = new SimpleStringProperty("");
    private final StringProperty fontName = new SimpleStringProperty("");
    private final StringProperty fontEncoding = new SimpleStringProperty("");

    private final BooleanProperty checkValidity = new SimpleBooleanProperty(true);
    private final BooleanProperty checkKeyUsage = new SimpleBooleanProperty(true);
    private final BooleanProperty checkCriticalExtensions = new SimpleBooleanProperty(false);

    private final BooleanProperty relaxSslSecurity = new SimpleBooleanProperty(false);

    private final BooleanProperty pdfLibJpedal = new SimpleBooleanProperty(true);
    private final BooleanProperty pdfLibPdfbox = new SimpleBooleanProperty(true);
    private final BooleanProperty pdfLibOpenpdf = new SimpleBooleanProperty(true);
    private final IntegerProperty pdfLibJpedalOrder = new SimpleIntegerProperty(1);
    private final IntegerProperty pdfLibPdfboxOrder = new SimpleIntegerProperty(2);
    private final IntegerProperty pdfLibOpenpdfOrder = new SimpleIntegerProperty(3);

    private final StringProperty tsaHashAlgorithm = new SimpleStringProperty("SHA-256");

    private final StringProperty pkcs11Body = new SimpleStringProperty("");

    /** Loads the VM from the given snapshot of {@link AdvancedConfig} and a pkcs11 file body. */
    public void loadFrom(AdvancedConfig cfg, String pkcs11FileBody) {
        fontPath.set(orEmpty(cfg.getProperty("font.path")));
        fontName.set(orEmpty(cfg.getProperty("font.name")));
        fontEncoding.set(orEmpty(cfg.getProperty("font.encoding")));

        checkValidity.set(cfg.getAsBool("certificate.checkValidity", true));
        checkKeyUsage.set(cfg.getAsBool("certificate.checkKeyUsage", true));
        checkCriticalExtensions.set(cfg.getAsBool("certificate.checkCriticalExtensions", false));

        relaxSslSecurity.set(cfg.getAsBool("relax.ssl.security", false));

        decodePdfLibraries(cfg.getNotEmptyProperty("pdf2image.libraries", "jpedal,pdfbox,openpdf"));

        tsaHashAlgorithm.set(cfg.getNotEmptyProperty("tsa.hashAlgorithm", "SHA-256"));

        pkcs11Body.set(pkcs11FileBody == null ? "" : pkcs11FileBody);
    }

    /** Writes the VM back into the given {@link AdvancedConfig} (does not persist; caller should call {@code save()}). */
    public void writeTo(AdvancedConfig cfg) {
        writeStringOrRemove(cfg, "font.path", fontPath.get());
        writeStringOrRemove(cfg, "font.name", fontName.get());
        writeStringOrRemove(cfg, "font.encoding", fontEncoding.get());
        cfg.setProperty("certificate.checkValidity", checkValidity.get());
        cfg.setProperty("certificate.checkKeyUsage", checkKeyUsage.get());
        cfg.setProperty("certificate.checkCriticalExtensions", checkCriticalExtensions.get());
        cfg.setProperty("relax.ssl.security", relaxSslSecurity.get());
        cfg.setProperty("pdf2image.libraries", encodePdfLibraries());
        cfg.setProperty("tsa.hashAlgorithm", tsaHashAlgorithm.get());
    }

    /** Resets every VM property to the bundled-default value (read from the given snapshot of bundled defaults). */
    public void applyDefaults(AdvancedConfig defaults) {
        fontPath.set(orEmpty(defaults.getBundledDefault("font.path")));
        fontName.set(orEmpty(defaults.getBundledDefault("font.name")));
        fontEncoding.set(orEmpty(defaults.getBundledDefault("font.encoding")));
        checkValidity.set(parseBool(defaults.getBundledDefault("certificate.checkValidity"), true));
        checkKeyUsage.set(parseBool(defaults.getBundledDefault("certificate.checkKeyUsage"), true));
        checkCriticalExtensions.set(parseBool(defaults.getBundledDefault("certificate.checkCriticalExtensions"), false));
        relaxSslSecurity.set(parseBool(defaults.getBundledDefault("relax.ssl.security"), false));
        decodePdfLibraries(orFallback(defaults.getBundledDefault("pdf2image.libraries"), "jpedal,pdfbox,openpdf"));
        tsaHashAlgorithm.set(orFallback(defaults.getBundledDefault("tsa.hashAlgorithm"), "SHA-256"));
    }

    public String encodePdfLibraries() {
        // Build (libname, order, enabled) tuples and emit the enabled ones in ascending order.
        record Lib(String name, int order, boolean enabled) {}
        List<Lib> libs = new ArrayList<>();
        libs.add(new Lib(LIB_JPEDAL, pdfLibJpedalOrder.get(), pdfLibJpedal.get()));
        libs.add(new Lib(LIB_PDFBOX, pdfLibPdfboxOrder.get(), pdfLibPdfbox.get()));
        libs.add(new Lib(LIB_OPENPDF, pdfLibOpenpdfOrder.get(), pdfLibOpenpdf.get()));
        libs.sort((a, b) -> Integer.compare(a.order, b.order));
        StringBuilder sb = new StringBuilder();
        for (Lib lib : libs) {
            if (lib.enabled) {
                if (sb.length() > 0) sb.append(',');
                sb.append(lib.name);
            }
        }
        return sb.toString();
    }

    void decodePdfLibraries(String csv) {
        Set<String> known = new LinkedHashSet<>(Arrays.asList(LIB_JPEDAL, LIB_PDFBOX, LIB_OPENPDF));
        Set<String> seen = new LinkedHashSet<>();
        if (csv != null) {
            for (String token : csv.split("\\s*,\\s*")) {
                if (known.contains(token)) {
                    seen.add(token);
                }
            }
        }
        // Append any known libs missing from the input at the end (disabled).
        Set<String> remaining = new LinkedHashSet<>(known);
        remaining.removeAll(seen);

        int order = 1;
        for (String lib : seen) {
            assignOrder(lib, order++);
            assignEnabled(lib, true);
        }
        for (String lib : remaining) {
            assignOrder(lib, order++);
            assignEnabled(lib, false);
        }
    }

    private void assignOrder(String lib, int order) {
        switch (lib) {
            case LIB_JPEDAL -> pdfLibJpedalOrder.set(order);
            case LIB_PDFBOX -> pdfLibPdfboxOrder.set(order);
            case LIB_OPENPDF -> pdfLibOpenpdfOrder.set(order);
        }
    }

    private void assignEnabled(String lib, boolean enabled) {
        switch (lib) {
            case LIB_JPEDAL -> pdfLibJpedal.set(enabled);
            case LIB_PDFBOX -> pdfLibPdfbox.set(enabled);
            case LIB_OPENPDF -> pdfLibOpenpdf.set(enabled);
        }
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String orFallback(String s, String fallback) {
        return (s == null || s.isEmpty()) ? fallback : s;
    }

    private static boolean parseBool(String s, boolean def) {
        if (s == null) return def;
        String t = s.trim().toLowerCase();
        return t.equals("true") || t.equals("yes") || t.equals("1") || t.equals("on");
    }

    private static void writeStringOrRemove(AdvancedConfig cfg, String key, String value) {
        if (value == null || value.isEmpty()) {
            cfg.removeProperty(key);
        } else {
            cfg.setProperty(key, value);
        }
    }

    public StringProperty fontPathProperty() { return fontPath; }
    public StringProperty fontNameProperty() { return fontName; }
    public StringProperty fontEncodingProperty() { return fontEncoding; }
    public BooleanProperty checkValidityProperty() { return checkValidity; }
    public BooleanProperty checkKeyUsageProperty() { return checkKeyUsage; }
    public BooleanProperty checkCriticalExtensionsProperty() { return checkCriticalExtensions; }
    public BooleanProperty relaxSslSecurityProperty() { return relaxSslSecurity; }
    public BooleanProperty pdfLibJpedalProperty() { return pdfLibJpedal; }
    public BooleanProperty pdfLibPdfboxProperty() { return pdfLibPdfbox; }
    public BooleanProperty pdfLibOpenpdfProperty() { return pdfLibOpenpdf; }
    public IntegerProperty pdfLibJpedalOrderProperty() { return pdfLibJpedalOrder; }
    public IntegerProperty pdfLibPdfboxOrderProperty() { return pdfLibPdfboxOrder; }
    public IntegerProperty pdfLibOpenpdfOrderProperty() { return pdfLibOpenpdfOrder; }
    public StringProperty tsaHashAlgorithmProperty() { return tsaHashAlgorithm; }
    public StringProperty pkcs11BodyProperty() { return pkcs11Body; }

    /** Move the given library one slot up (order--), swapping with the lib currently at that order. */
    public void moveUp(String lib) {
        int curr = orderOf(lib);
        if (curr <= 1) return;
        String other = libAtOrder(curr - 1);
        assignOrder(other, curr);
        assignOrder(lib, curr - 1);
    }

    /** Move the given library one slot down (order++), swapping with the lib currently at that order. */
    public void moveDown(String lib) {
        int curr = orderOf(lib);
        if (curr >= 3) return;
        String other = libAtOrder(curr + 1);
        assignOrder(other, curr);
        assignOrder(lib, curr + 1);
    }

    public int orderOf(String lib) {
        return switch (lib) {
            case LIB_JPEDAL -> pdfLibJpedalOrder.get();
            case LIB_PDFBOX -> pdfLibPdfboxOrder.get();
            case LIB_OPENPDF -> pdfLibOpenpdfOrder.get();
            default -> -1;
        };
    }

    private String libAtOrder(int order) {
        if (pdfLibJpedalOrder.get() == order) return LIB_JPEDAL;
        if (pdfLibPdfboxOrder.get() == order) return LIB_PDFBOX;
        if (pdfLibOpenpdfOrder.get() == order) return LIB_OPENPDF;
        return null;
    }
}
