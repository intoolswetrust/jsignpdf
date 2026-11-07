package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.util.logging.Level;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.pkcs11.Pkcs11Profiles;

/**
 * Methods for handling PKCS#11 security providers. Delegates to {@link Pkcs11Profiles}.
 *
 * @author Josef Cacek
 */
public class PKCS11Utils {

    private static final String SAMPLE_RESOURCE = "/net/sf/jsignpdf/conf/pkcs11.cfg.sample";

    /**
     * Parses the PKCS#11 profiles. No native library is loaded; providers register on first use.
     */
    public static void discoverProfiles() {
        Pkcs11Profiles.getInstance();
    }

    /**
     * Returns the bundled PKCS#11 sample as a String.
     */
    public static String getSampleConfig() {
        try (InputStream is = PKCS11Utils.class.getResourceAsStream(SAMPLE_RESOURCE)) {
            if (is == null) {
                LOGGER.warning("Bundled PKCS#11 sample missing: " + SAMPLE_RESOURCE);
                return "";
            }
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read bundled PKCS#11 sample", e);
            return "";
        }
    }

    /**
     * Unregisters every PKCS#11 provider registered by the profiles.
     * <p>
     * Some tokens/card-readers hang during second usage of the program, they have to be unplugged and plugged again;
     * this should prevent the issue.
     * </p>
     */
    public static void unregisterProviders() {
        Pkcs11Profiles.getInstance().unregisterAll();
    }

    public static boolean isPkcs11Type(String keyStoreType) {
        return Pkcs11Profiles.isPkcs11Type(keyStoreType);
    }

    /**
     * Returns the provider of the PKCS#11 profile selected in the options, registering it on first use, or
     * {@code null} when the keystore type is not a PKCS#11 type.
     *
     * @throws net.sf.jsignpdf.pkcs11.Pkcs11Exception when the profile cannot be resolved or registered
     */
    public static Provider getProvider(BasicSignerOptions options) {
        return Pkcs11Profiles.getInstance().provider(options.getKsProvider(), options.getKsType());
    }

    /**
     * Name of the provider returned by {@link #getProvider(BasicSignerOptions)}, or {@code null}.
     */
    public static String getProviderName(BasicSignerOptions options) {
        Provider p = getProvider(options);
        return p == null ? null : p.getName();
    }
}
