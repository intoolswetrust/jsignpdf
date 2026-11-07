package net.sf.jsignpdf.pkcs11;

/**
 * State of one PKCS#11 profile.
 *
 * @param id the profile id (or the file name when the stem is not a valid id)
 * @param state the state
 * @param message the reason for {@link State#FAILED} and {@link State#INVALID}, otherwise {@code null}
 */
public record ProfileStatus(String id, State state, String message) {

    public enum State {
        /** Parsed, no provider registered yet. */
        NOT_LOADED,
        /** All providers of the profile registered. */
        OK,
        /** Registration failed. */
        FAILED,
        /** The file cannot be used as a profile. */
        INVALID
    }

    public boolean isUsable() {
        return state != State.INVALID;
    }
}
