package net.sf.jsignpdf.fx;

import javafx.application.Platform;

import org.junit.Assume;

/**
 * Guard for the tests that need a running JavaFX toolkit. Those rely on the headless Monocle platform the
 * surefire configuration selects with {@code -Dglass.platform=Monocle}, which only works when JavaFX itself
 * comes from the class path &mdash; as it does in CI, where the build runs on a Temurin JDK and JavaFX is
 * pulled in as ordinary Maven jars.
 *
 * <p>
 * On a JDK that <em>bundles</em> JavaFX (e.g. Zulu {@code ca-fx}), {@code javafx.graphics} is instead resolved
 * as a named platform module and cannot see the {@code openjfx-monocle} test jar on the class path. Toolkit
 * startup then dies on the launcher thread with {@code PlatformFactory.getPlatformFactory()} returning
 * {@code null}. That is worse than a failing test: {@code PlatformImpl}'s internal startup latch is never
 * counted down while the toolkit still counts as initialised, so the next {@link Platform#runLater} parks
 * forever in {@code waitForStart()} and the build hangs instead of failing. Skipping up front keeps such a
 * JDK usable for everything else.
 * </p>
 */
public final class MonocleAssumption {

    private MonocleAssumption() {
    }

    /** Skips the calling test (or class, from a {@code @BeforeClass}) unless Monocle can be applied. */
    public static void assumeUsable() {
        Assume.assumeFalse("JavaFX is bundled in this JDK (javafx.graphics is a named module), so the Monocle"
                + " headless platform cannot be applied - build with a JDK without JavaFX modules to run"
                + " these tests", Platform.class.getModule().isNamed());
    }
}
