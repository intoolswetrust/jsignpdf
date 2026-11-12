package net.sf.jsignpdf.fx.view;

import java.util.Objects;

/**
 * One entry of the keystore-type combo: a plain keystore type, or a PKCS#11 type bound to a profile.
 *
 * @param type the keystore type
 * @param profileId the PKCS#11 profile id, {@code null} for other types
 * @param label the profile label shown next to the type
 * @param note tooltip text: why the entry is disabled, or the last registration failure of a selectable one
 * @param enabled {@code false} for a profile file that cannot be used
 */
public record KeystoreChoice(String type, String profileId, String label, String note, boolean enabled) {

    public static KeystoreChoice plain(String type) {
        return new KeystoreChoice(type, null, null, null, true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean matches(String otherType, String otherProfileId) {
        if (type == null || !type.equalsIgnoreCase(otherType)) {
            return false;
        }
        return profileId == null ? otherProfileId == null
                : otherProfileId != null && profileId.equalsIgnoreCase(otherProfileId);
    }

    public String display() {
        return profileId == null ? Objects.toString(type, "") : type + " — " + label;
    }
}
