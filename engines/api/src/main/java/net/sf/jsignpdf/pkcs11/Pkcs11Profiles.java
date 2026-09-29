package net.sf.jsignpdf.pkcs11;

import static net.sf.jsignpdf.Constants.LOGGER;
import static net.sf.jsignpdf.Constants.RES;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import net.sf.jsignpdf.utils.ConfigLocationResolver;

/**
 * Discovers PKCS#11 profiles ({@code <cfg>/pkcs11.cfg} as {@code default}, plus {@code <cfg>/pkcs11/*.cfg}) and
 * registers their security providers lazily, on first use. Failures never propagate out of discovery; resolution and
 * registration throw {@link Pkcs11Exception} with a user-facing message.
 */
public final class Pkcs11Profiles {

    public static final String DEFAULT_ID = "default";
    public static final String PROFILES_DIR_NAME = "pkcs11";
    public static final String LEGACY_FILE_NAME = "pkcs11.cfg";
    public static final String FILE_SUFFIX = ".cfg";
    public static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,32}");

    /**
     * Creates (but does not register) a provider for the given config file.
     */
    @FunctionalInterface
    public interface ProviderFactory {
        Provider create(Pkcs11Backend backend, Path configFile) throws Exception;
    }

    private static volatile Pkcs11Profiles instance;

    private final Path configDir;
    private final ProviderFactory factory;
    private long unregisterDelayMillis = 1000L;

    private final Map<String, Pkcs11Profile> profiles = new LinkedHashMap<>();
    private final Map<String, ProfileStatus> invalid = new LinkedHashMap<>();
    private final Map<String, Map<Pkcs11Backend, Provider>> registered = new LinkedHashMap<>();
    private final Map<String, String> failures = new LinkedHashMap<>();
    private final Map<String, String> renames = new LinkedHashMap<>();

    /**
     * Profiles under the given config directory, registered with the real SunPKCS11 / JSignPKCS11 providers.
     */
    public Pkcs11Profiles(Path configDir) {
        this(configDir, Pkcs11Profiles::createProvider);
    }

    public Pkcs11Profiles(Path configDir, ProviderFactory factory) {
        this.configDir = configDir;
        this.factory = factory;
        reload();
    }

    public static Pkcs11Profiles getInstance() {
        Pkcs11Profiles ref = instance;
        if (ref == null) {
            synchronized (Pkcs11Profiles.class) {
                ref = instance;
                if (ref == null) {
                    ref = new Pkcs11Profiles(ConfigLocationResolver.getInstance().getConfigDir());
                    instance = ref;
                }
            }
        }
        return ref;
    }

    /** Test seam. */
    public static void setInstance(Pkcs11Profiles profiles) {
        instance = profiles;
    }

    void setUnregisterDelayMillis(long millis) {
        this.unregisterDelayMillis = millis;
    }

    public Path getConfigDir() {
        return configDir;
    }

    public Path getProfilesDir() {
        return configDir == null ? null : configDir.resolve(PROFILES_DIR_NAME);
    }

    public Path getLegacyFile() {
        return configDir == null ? null : configDir.resolve(LEGACY_FILE_NAME);
    }

    /**
     * Returns the file a profile with the given id is stored in.
     */
    public Path fileFor(String id) {
        if (configDir == null) {
            return null;
        }
        return DEFAULT_ID.equalsIgnoreCase(id) ? getLegacyFile() : getProfilesDir().resolve(id + FILE_SUFFIX);
    }

    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    /**
     * {@code true} for the keystore types a PKCS#11 profile can provide ({@code PKCS11}, {@code JSIGNPKCS11}).
     */
    public static boolean isPkcs11Type(String keyStoreType) {
        return Pkcs11Backend.forKeyStoreType(keyStoreType) != null;
    }

    /**
     * Re-reads the profile files. Profiles that were removed or whose file content changed are unregistered.
     */
    public synchronized void reload() {
        Map<String, Pkcs11Profile> oldProfiles = new LinkedHashMap<>(profiles);
        profiles.clear();
        invalid.clear();
        if (configDir != null) {
            Path legacy = getLegacyFile();
            if (Files.isRegularFile(legacy)) {
                readProfile(DEFAULT_ID, legacy, true);
            }
            Path dir = getProfilesDir();
            if (Files.isDirectory(dir)) {
                List<Path> files = new ArrayList<>();
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*" + FILE_SUFFIX)) {
                    ds.forEach(files::add);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Cannot list " + dir, e);
                }
                files.sort(Comparator.comparing((Path f) -> f.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(f -> f.getFileName().toString()));
                for (Path f : files) {
                    if (Files.isRegularFile(f)) {
                        String fileName = f.getFileName().toString();
                        readProfile(fileName.substring(0, fileName.length() - FILE_SUFFIX.length()), f, false);
                    }
                }
            }
        }
        for (Map.Entry<String, Pkcs11Profile> e : oldProfiles.entrySet()) {
            Pkcs11Profile now = profiles.get(e.getKey());
            if (now == null || !now.body().equals(e.getValue().body()) || !now.file().equals(e.getValue().file())) {
                unregisterInternal(e.getKey());
                failures.remove(e.getKey());
            }
        }
    }

    private void readProfile(String id, Path file, boolean legacy) {
        String fileName = file.getFileName().toString();
        if (!isValidId(id)) {
            markInvalid(fileName, RES.get("console.pkcs11.invalidId", fileName));
            return;
        }
        if (!legacy && DEFAULT_ID.equalsIgnoreCase(id)) {
            markInvalid(fileName, RES.get("console.pkcs11.reservedId", fileName));
            return;
        }
        String key = key(id);
        if (profiles.containsKey(key)) {
            markInvalid(id, RES.get("console.pkcs11.caseCollision", id, profiles.get(key).id()));
            return;
        }
        String body;
        try {
            body = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            markInvalid(id, RES.get("console.pkcs11.unreadable", file.toString(), String.valueOf(e.getMessage())));
            return;
        }
        Pkcs11Profile profile = new Pkcs11Profile(id, file, legacy, body);
        if (profile.cfgName() != null) {
            for (Pkcs11Profile other : profiles.values()) {
                if (profile.cfgName().equals(other.cfgName())) {
                    markInvalid(id, RES.get("console.pkcs11.duplicateName", id, profile.cfgName(), other.id()));
                    return;
                }
            }
            if (!legacy && !profile.cfgName().equals(id)) {
                LOGGER.warning(RES.get("console.pkcs11.nameMismatch", id, profile.cfgName()));
            }
        }
        profiles.put(key, profile);
    }

    private void markInvalid(String id, String message) {
        LOGGER.warning(message);
        invalid.put(key(id), new ProfileStatus(id, ProfileStatus.State.INVALID, message));
    }

    /**
     * Usable profiles, the legacy {@code default} first, then by id.
     */
    public synchronized List<Pkcs11Profile> list() {
        return Collections.unmodifiableList(new ArrayList<>(profiles.values()));
    }

    /**
     * Status of every profile file, usable ones first.
     */
    public synchronized List<ProfileStatus> statuses() {
        List<ProfileStatus> result = new ArrayList<>();
        for (Pkcs11Profile p : profiles.values()) {
            result.add(status(p.id()));
        }
        result.addAll(invalid.values());
        return result;
    }

    public synchronized ProfileStatus status(String id) {
        String key = key(id);
        if (invalid.containsKey(key) && !profiles.containsKey(key)) {
            return invalid.get(key);
        }
        Pkcs11Profile p = profiles.get(key);
        if (p == null) {
            return null;
        }
        if (failures.containsKey(key)) {
            return new ProfileStatus(p.id(), ProfileStatus.State.FAILED, failures.get(key));
        }
        return new ProfileStatus(p.id(),
                registered.containsKey(key) ? ProfileStatus.State.OK : ProfileStatus.State.NOT_LOADED, null);
    }

    public synchronized Optional<Pkcs11Profile> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(profiles.get(key(id)));
    }

    /**
     * Keystore types offered by at least one usable profile.
     */
    public synchronized Set<String> offeredTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Pkcs11Profile p : profiles.values()) {
            types.addAll(p.offeredTypes());
        }
        return types;
    }

    /**
     * Records a rename done through the Preferences dialog, so a selection holding the old id can follow it.
     */
    public synchronized void recordRename(String oldId, String newId) {
        renames.replaceAll((from, to) -> to.equalsIgnoreCase(oldId) ? newId : to);
        renames.put(key(oldId), newId);
    }

    /**
     * Returns the id the given one was renamed to in this session, or {@code null}.
     */
    public synchronized String renamedTo(String id) {
        return id == null ? null : renames.get(key(id));
    }

    /**
     * Resolves the profile for a keystore selection. Returns {@code null} when the type is not a PKCS#11 type.
     *
     * @throws Pkcs11Exception when the named profile is missing or unusable, when no profile offers the type, or when
     *         several do and none is named
     */
    public synchronized Pkcs11Profile resolve(String profileId, String keyStoreType) {
        if (!isPkcs11Type(keyStoreType)) {
            return null;
        }
        if (StringUtils.isNotBlank(profileId)) {
            String id = profileId.trim();
            Pkcs11Profile p = profiles.get(key(id));
            if (p == null) {
                ProfileStatus st = invalid.get(key(id));
                if (st != null) {
                    throw new Pkcs11Exception(RES.get("console.pkcs11.profileInvalid", id, st.message()));
                }
                throw new Pkcs11Exception(RES.get("console.pkcs11.profileNotFound", id, availableIds()));
            }
            if (!p.accepts(keyStoreType)) {
                throw new Pkcs11Exception(RES.get("console.pkcs11.typeNotOffered", p.id(), keyStoreType));
            }
            return p;
        }
        List<Pkcs11Profile> candidates = profiles.values().stream().filter(p -> p.accepts(keyStoreType))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            throw new Pkcs11Exception(RES.get("console.pkcs11.noProfile", keyStoreType));
        }
        if (candidates.size() > 1) {
            throw new Pkcs11Exception(RES.get("console.pkcs11.ambiguous",
                    candidates.stream().map(Pkcs11Profile::id).collect(Collectors.joining(", "))));
        }
        return candidates.get(0);
    }

    /**
     * Resolves the profile and returns its registered provider for the keystore type, registering it on first use.
     * Returns {@code null} when the type is not a PKCS#11 type.
     */
    public synchronized Provider provider(String profileId, String keyStoreType) {
        Pkcs11Profile p = resolve(profileId, keyStoreType);
        if (p == null) {
            return null;
        }
        return register(p, p.backendsFor(keyStoreType).iterator().next());
    }

    /**
     * Registers every provider of the profile. Returns the resulting status; never throws.
     */
    public synchronized ProfileStatus register(String id) {
        Pkcs11Profile p = profiles.get(key(id));
        if (p == null) {
            return status(id);
        }
        try {
            for (Pkcs11Backend b : p.mode().backends()) {
                register(p, b);
            }
        } catch (Pkcs11Exception e) {
            // recorded in failures
        }
        return status(id);
    }

    private Provider register(Pkcs11Profile p, Pkcs11Backend backend) {
        String key = key(p.id());
        Map<Pkcs11Backend, Provider> byBackend = registered.get(key);
        if (byBackend != null && byBackend.containsKey(backend)) {
            return byBackend.get(backend);
        }
        failures.remove(key);
        try {
            checkLibrary(p);
            Provider provider = factory.create(backend, p.file());
            if (provider == null) {
                throw new Pkcs11Exception(RES.get("console.pkcs11.registrationFailed", backend.providerClass()));
            }
            if (Security.getProvider(provider.getName()) != null) {
                throw new Pkcs11Exception(RES.get("console.pkcs11.providerExists", provider.getName()));
            }
            Security.addProvider(provider);
            registered.computeIfAbsent(key, k -> new EnumMap<>(Pkcs11Backend.class)).put(backend, provider);
            LOGGER.info(RES.get("console.pkcs11.registered", p.id(), provider.getName()));
            return provider;
        } catch (Pkcs11Exception e) {
            failures.put(key, e.getMessage());
            throw e;
        } catch (Throwable e) {
            String message = describe(p, e);
            LOGGER.log(Level.FINE, message, e);
            failures.put(key, message);
            throw new Pkcs11Exception(message, e);
        }
    }

    private static void checkLibrary(Pkcs11Profile p) {
        String lib = p.library();
        if (lib == null || lib.contains("$") || lib.contains("%")) {
            return;
        }
        Path libPath = Path.of(lib);
        if (libPath.isAbsolute() && !Files.exists(libPath)) {
            throw new Pkcs11Exception(RES.get("console.pkcs11.libraryMissing", lib));
        }
    }

    static String describe(Pkcs11Profile p, Throwable e) {
        StringBuilder all = new StringBuilder();
        String root = null;
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t.getMessage() != null) {
                all.append(t.getMessage()).append('\n');
                root = t.getMessage();
            }
        }
        String text = all.toString().toLowerCase(Locale.ROOT);
        if (text.contains("elfclass") || text.contains("wrong elf") || text.contains("not a valid win32")
                || text.contains("32-bit") || text.contains("wrong architecture")
                || text.contains("incompatible architecture")) {
            return RES.get("console.pkcs11.archMismatch", String.valueOf(p.library()),
                    System.getProperty("os.arch", "?"));
        }
        if (text.contains("slotlistindex") || text.contains("slot")) {
            return RES.get("console.pkcs11.noSlot", String.valueOf(root));
        }
        return RES.get("console.pkcs11.registrationFailed",
                root != null ? root : e.getClass().getName());
    }

    /**
     * Forgets recorded registration failures so the next use retries.
     */
    public synchronized void clearFailures() {
        failures.clear();
    }

    /**
     * Unregisters the providers of one profile.
     */
    public synchronized void unregister(String id) {
        unregisterInternal(key(id));
    }

    /**
     * Unregisters every registered provider; some tokens need a short pause afterwards, paid once.
     */
    public synchronized void unregisterAll() {
        boolean any = !registered.isEmpty();
        for (String key : new ArrayList<>(registered.keySet())) {
            unregisterInternal(key);
        }
        if (any && unregisterDelayMillis > 0) {
            try {
                Thread.sleep(unregisterDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void unregisterInternal(String key) {
        Map<Pkcs11Backend, Provider> byBackend = registered.remove(key);
        if (byBackend == null) {
            return;
        }
        for (Provider p : byBackend.values()) {
            LOGGER.fine("Removing security provider with name " + p.getName());
            try {
                Security.removeProvider(p.getName());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Removing provider failed", e);
            }
        }
    }

    /**
     * One profile as edited in the Preferences dialog.
     *
     * @param originalId the id the profile was loaded with, {@code null} for a new profile
     * @param id the id to save it under
     * @param body the config text
     */
    public record ProfileEdit(String originalId, String id, String body) {
    }

    /**
     * Checks a full set of edited profiles. Returns a user-facing message for the first problem, or {@code null}.
     */
    public synchronized String validateEdits(List<ProfileEdit> edits) {
        Map<String, String> seen = new LinkedHashMap<>();
        for (ProfileEdit e : edits) {
            if (!isValidId(e.id())) {
                return RES.get("console.pkcs11.invalidId", String.valueOf(e.id()));
            }
            boolean wasDefault = DEFAULT_ID.equalsIgnoreCase(e.originalId());
            if (DEFAULT_ID.equalsIgnoreCase(e.id()) != wasDefault) {
                return RES.get("console.pkcs11.reservedId", e.id());
            }
            String previous = seen.putIfAbsent(key(e.id()), e.id());
            if (previous != null) {
                return RES.get("console.pkcs11.caseCollision", e.id(), previous);
            }
            boolean sameAsOriginal = e.originalId() != null && key(e.originalId()).equals(key(e.id()));
            Path target = fileFor(e.id());
            if (!sameAsOriginal && target != null && Files.exists(target)
                    && edits.stream().noneMatch(o -> o.originalId() != null && key(o.originalId()).equals(key(e.id())))) {
                return RES.get("console.pkcs11.fileExists", target.toString());
            }
        }
        return null;
    }

    /**
     * Writes a full set of edited profiles: profiles missing from the list are deleted, renamed ones are moved, and
     * every other one is written with {@code name=<id>} forced (except the legacy file). Invalid profile files are left
     * alone. Reloads afterwards, which unregisters every changed profile.
     */
    public synchronized void applyEdits(List<ProfileEdit> edits) throws IOException {
        if (configDir == null) {
            return;
        }
        Set<String> kept = edits.stream().map(ProfileEdit::originalId).filter(Objects::nonNull).map(Pkcs11Profiles::key)
                .collect(Collectors.toSet());
        for (Pkcs11Profile p : new ArrayList<>(profiles.values())) {
            if (!kept.contains(key(p.id()))) {
                Files.deleteIfExists(p.file());
            }
        }
        for (ProfileEdit e : edits) {
            Pkcs11Profile old = e.originalId() == null ? null : profiles.get(key(e.originalId()));
            if (old != null && !old.file().equals(fileFor(e.id()))) {
                Files.deleteIfExists(old.file());
            }
        }
        for (ProfileEdit e : edits) {
            Path target = fileFor(e.id());
            boolean legacy = DEFAULT_ID.equalsIgnoreCase(e.id());
            String body = e.body() == null ? "" : e.body();
            if (legacy && body.isBlank()) {
                Files.deleteIfExists(target);
                continue;
            }
            if (!legacy) {
                body = Pkcs11ConfigText.withValue(body, Pkcs11ConfigText.KEY_NAME, e.id());
            }
            Files.createDirectories(target.getParent());
            Pkcs11Profile old = e.originalId() == null ? null : profiles.get(key(e.originalId()));
            if (old == null || !old.file().equals(target) || !old.body().equals(body)) {
                Files.writeString(target, body, StandardCharsets.UTF_8);
            }
            if (e.originalId() != null && !key(e.originalId()).equals(key(e.id()))) {
                recordRename(e.originalId(), e.id());
            }
        }
        reload();
    }

    private String availableIds() {
        return profiles.isEmpty() ? "-"
                : profiles.values().stream().map(Pkcs11Profile::id).collect(Collectors.joining(", "));
    }

    private static String key(String id) {
        return Objects.requireNonNull(id).trim().toLowerCase(Locale.ROOT);
    }

    static Provider createProvider(Pkcs11Backend backend, Path configFile) throws Exception {
        String path = configFile.toAbsolutePath().toString();
        if (backend == Pkcs11Backend.SUN) {
            Provider base = Security.getProvider("SunPKCS11");
            if (base != null) {
                return base.configure(path);
            }
        }
        Class<?> cls = Class.forName(backend.providerClass());
        try {
            return (Provider) cls.getConstructor(String.class).newInstance(path);
        } catch (NoSuchMethodException e) {
            Provider p = (Provider) cls.getConstructor().newInstance();
            return p.configure(path);
        }
    }
}
