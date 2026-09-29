package net.sf.jsignpdf.pkcs11;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * The bundled, read-only catalog of known PKCS#11 drivers ({@code net/sf/jsignpdf/pkcs11/catalog.json}). Entries are
 * hostile input: config keys are allowlisted and values validated; an entry failing validation is dropped.
 */
public final class Pkcs11Catalog {

    public static final int SCHEMA_VERSION = 1;
    public static final String RESOURCE = "/net/sf/jsignpdf/pkcs11/catalog.json";

    static final Set<String> OS_VALUES = Set.of("linux", "windows", "macos");
    static final Set<String> ARCH_VALUES = Set.of("x86_64", "aarch64");
    static final Set<String> CONFIG_KEYS = Set.of("slot", "slotListIndex", "description");
    private static final Pattern DESCRIPTION = Pattern.compile("[A-Za-z0-9 ._:/+~-]{1,64}");
    private static final Pattern SINGLE_LINE = Pattern.compile("[^\\p{Cntrl}]{1,200}");

    public record Library(String os, List<String> arch, List<String> paths) {
    }

    public record Attestation(String os, String osVersion, String driver, String jsignpdf, String reporter,
            String date) {
    }

    public record Entry(String id, int revision, String label, String vendor, String infoUrl,
            List<Library> libraries, Map<String, Object> config, ProviderMode provider, String notes,
            List<Attestation> tested, String discussion) {

        /** The {@code #jsignpdf:catalog=} value. */
        public String catalogRef() {
            return id + "@" + revision;
        }

        public List<Library> librariesFor(String os, String arch) {
            List<Library> result = new ArrayList<>();
            for (Library l : libraries) {
                if (l.os().equals(os) && l.arch().contains(arch)) {
                    result.add(l);
                }
            }
            return result;
        }
    }

    private final String updated;
    private final List<Entry> entries;

    private Pkcs11Catalog(String updated, List<Entry> entries) {
        this.updated = updated;
        this.entries = Collections.unmodifiableList(entries);
    }

    public String updated() {
        return updated;
    }

    public List<Entry> entries() {
        return entries;
    }

    /**
     * Loads the bundled catalog. Returns an empty catalog when it cannot be read.
     */
    public static Pkcs11Catalog loadBundled() {
        try (InputStream is = Pkcs11Catalog.class.getResourceAsStream(RESOURCE)) {
            if (is == null) {
                LOGGER.warning("PKCS#11 catalog missing: " + RESOURCE);
                return new Pkcs11Catalog(null, List.of());
            }
            return parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Cannot read the PKCS#11 catalog", e);
            return new Pkcs11Catalog(null, List.of());
        }
    }

    /**
     * Parses a catalog document. Invalid entries are skipped; a malformed document throws.
     */
    public static Pkcs11Catalog parse(String json) {
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map<?, ?> doc)) {
            throw new IllegalArgumentException("Catalog root is not an object");
        }
        long schema = asLong(doc.get("schemaVersion"), "schemaVersion");
        if (schema < 1) {
            throw new IllegalArgumentException("Unsupported schemaVersion " + schema);
        }
        List<Entry> entries = new ArrayList<>();
        Object list = doc.get("entries");
        if (list instanceof List<?> l) {
            for (Object o : l) {
                try {
                    Entry e = parseEntry(o);
                    if (e != null) {
                        entries.add(e);
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.warning("PKCS#11 catalog entry rejected: " + ex.getMessage());
                }
            }
        }
        return new Pkcs11Catalog(doc.get("updated") instanceof String s ? s : null, entries);
    }

    static Entry parseEntry(Object o) {
        if (!(o instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("entry is not an object");
        }
        String id = requiredString(m, "id");
        if (!Pkcs11Profiles.isValidId(id)) {
            throw new IllegalArgumentException("invalid id " + id);
        }
        long minSchema = m.containsKey("minSchemaVersion") ? asLong(m.get("minSchemaVersion"), "minSchemaVersion") : 1;
        if (minSchema > SCHEMA_VERSION) {
            LOGGER.fine("PKCS#11 catalog entry " + id + " needs schema " + minSchema + ", skipped");
            return null;
        }
        long revision = asLong(m.get("revision"), "revision");
        if (revision < 1 || revision > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(id + ": invalid revision");
        }
        String label = singleLine(requiredString(m, "label"), id + ": label");
        String vendor = optionalSingleLine(m, "vendor", id);
        String infoUrl = optionalUrl(m, "infoUrl", id);
        String discussion = optionalUrl(m, "discussion", id);
        String notes = m.get("notes") instanceof String s ? s : null;
        ProviderMode provider = ProviderMode.SUN;
        if (m.containsKey("provider")) {
            provider = ProviderMode.parse(requiredString(m, "provider"));
            if (provider == null) {
                throw new IllegalArgumentException(id + ": invalid provider");
            }
        }
        return new Entry(id, (int) revision, label, vendor, infoUrl, parseLibraries(m.get("libraries"), id),
                parseConfig(m.get("config"), id), provider, notes, parseTested(m.get("tested"), id), discussion);
    }

    private static List<Library> parseLibraries(Object o, String id) {
        if (!(o instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException(id + ": libraries missing");
        }
        List<Library> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> lm)) {
                throw new IllegalArgumentException(id + ": library is not an object");
            }
            String os = requiredString(lm, "os");
            if (!OS_VALUES.contains(os)) {
                throw new IllegalArgumentException(id + ": invalid os " + os);
            }
            List<String> arch = stringList(lm.get("arch"), id + ": arch");
            for (String a : arch) {
                if (!ARCH_VALUES.contains(a)) {
                    throw new IllegalArgumentException(id + ": invalid arch " + a);
                }
            }
            List<String> paths = stringList(lm.get("paths"), id + ": paths");
            for (String p : paths) {
                singleLine(p, id + ": path");
                if (p.contains("\"")) {
                    throw new IllegalArgumentException(id + ": invalid path " + p);
                }
            }
            result.add(new Library(os, List.copyOf(arch), List.copyOf(paths)));
        }
        return List.copyOf(result);
    }

    static Map<String, Object> parseConfig(Object o, String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (o == null) {
            return result;
        }
        if (!(o instanceof Map<?, ?> cm)) {
            throw new IllegalArgumentException(id + ": config is not an object");
        }
        for (Map.Entry<?, ?> e : cm.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (!CONFIG_KEYS.contains(key)) {
                throw new IllegalArgumentException(id + ": config key not allowed: " + key);
            }
            Object v = e.getValue();
            if ("description".equals(key)) {
                if (!(v instanceof String s) || !DESCRIPTION.matcher(s).matches()) {
                    throw new IllegalArgumentException(id + ": invalid description");
                }
                result.put(key, s);
            } else {
                if (!(v instanceof Long n) || n < 0 || n > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(id + ": " + key + " must be a non-negative integer");
                }
                result.put(key, n);
            }
        }
        if (result.containsKey("slot") && result.containsKey("slotListIndex")) {
            throw new IllegalArgumentException(id + ": slot and slotListIndex are mutually exclusive");
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<Attestation> parseTested(Object o, String id) {
        if (o == null) {
            return List.of();
        }
        if (!(o instanceof List<?> list)) {
            throw new IllegalArgumentException(id + ": tested is not an array");
        }
        List<Attestation> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> tm)) {
                throw new IllegalArgumentException(id + ": tested item is not an object");
            }
            result.add(new Attestation(requiredString(tm, "os"), optionalSingleLine(tm, "osVersion", id),
                    optionalSingleLine(tm, "driver", id), optionalSingleLine(tm, "jsignpdf", id),
                    optionalSingleLine(tm, "reporter", id), optionalSingleLine(tm, "date", id)));
        }
        return List.copyOf(result);
    }

    /**
     * Builds the config text of a draft profile created from an entry.
     */
    public static String toProfileBody(Entry entry, String profileId, String libraryPath) {
        StringBuilder sb = new StringBuilder();
        sb.append(Pkcs11ConfigText.META_PREFIX).append(Pkcs11ConfigText.META_LABEL).append('=').append(entry.label())
                .append('\n');
        sb.append(Pkcs11ConfigText.META_PREFIX).append(Pkcs11ConfigText.META_PROVIDER).append('=')
                .append(entry.provider().value()).append('\n');
        sb.append(Pkcs11ConfigText.META_PREFIX).append(Pkcs11ConfigText.META_CATALOG).append('=')
                .append(entry.catalogRef()).append('\n');
        sb.append("name=").append(profileId).append('\n');
        sb.append("library=").append(Pkcs11ConfigText.formatLibraryValue(libraryPath)).append('\n');
        for (Map.Entry<String, Object> e : entry.config().entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Returns the most recent attestation, or {@code null} when the entry has none.
     */
    public static Attestation latestAttestation(Entry entry) {
        Attestation latest = null;
        for (Attestation a : entry.tested()) {
            if (latest == null || (a.date() != null && (latest.date() == null || a.date().compareTo(latest.date()) > 0))) {
                latest = a;
            }
        }
        return latest;
    }

    private static String requiredString(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return s;
    }

    private static String optionalSingleLine(Map<?, ?> m, String key, String id) {
        Object v = m.get(key);
        if (v == null) {
            return null;
        }
        if (!(v instanceof String s)) {
            throw new IllegalArgumentException(id + ": " + key + " is not a string");
        }
        return singleLine(s, id + ": " + key);
    }

    private static String optionalUrl(Map<?, ?> m, String key, String id) {
        String v = optionalSingleLine(m, key, id);
        if (v != null && !v.startsWith("https://")) {
            throw new IllegalArgumentException(id + ": " + key + " must be an https URL");
        }
        return v;
    }

    private static String singleLine(String s, String what) {
        if (!SINGLE_LINE.matcher(s).matches()) {
            throw new IllegalArgumentException(what + " must be a single line");
        }
        return s;
    }

    private static List<String> stringList(Object o, String what) {
        if (!(o instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException(what + " missing");
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String s) || s.isBlank()) {
                throw new IllegalArgumentException(what + " must hold strings");
            }
            result.add(s);
        }
        return result;
    }

    private static long asLong(Object o, String what) {
        if (!(o instanceof Long n)) {
            throw new IllegalArgumentException(what + " must be an integer");
        }
        return n;
    }
}
