package net.sf.jsignpdf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link Signer#launchGui(BasicSignerOptions)}, in particular the JavaFX-init failure fallback path:
 * when the JavaFX launcher throws, a warning notifier is invoked and the legacy Swing UI is started.
 */
public class SignerLaunchGuiTest {

    private Consumer<BasicSignerOptions> originalFx;
    private Consumer<BasicSignerOptions> originalSwing;
    private Consumer<Throwable> originalNotifier;
    private String originalSwingProp;

    @Before
    public void saveHooks() {
        originalFx = Signer.fxLauncher;
        originalSwing = Signer.swingLauncher;
        originalNotifier = Signer.fxFallbackNotifier;
        originalSwingProp = System.getProperty("jsignpdf.swing");
        System.clearProperty("jsignpdf.swing");
    }

    @After
    public void restoreHooks() {
        Signer.fxLauncher = originalFx;
        Signer.swingLauncher = originalSwing;
        Signer.fxFallbackNotifier = originalNotifier;
        if (originalSwingProp == null) {
            System.clearProperty("jsignpdf.swing");
        } else {
            System.setProperty("jsignpdf.swing", originalSwingProp);
        }
    }

    @Test
    public void fxFailure_triggersWarningAndSwingFallback() {
        BasicSignerOptions opts = new BasicSignerOptions();
        UnsatisfiedLinkError fxError = new UnsatisfiedLinkError("incompatible JavaFX native");

        AtomicReference<BasicSignerOptions> fxCalledWith = new AtomicReference<>();
        AtomicReference<BasicSignerOptions> swingCalledWith = new AtomicReference<>();
        AtomicReference<Throwable> notifierCalledWith = new AtomicReference<>();

        Signer.fxLauncher = o -> {
            fxCalledWith.set(o);
            throw fxError;
        };
        Signer.fxFallbackNotifier = notifierCalledWith::set;
        Signer.swingLauncher = swingCalledWith::set;

        Signer.launchGui(opts);

        assertSame("FX launcher should have been called with the provided opts", opts, fxCalledWith.get());
        assertSame("Notifier must receive the original FX failure", fxError, notifierCalledWith.get());
        assertSame("Swing fallback must run after FX failure", opts, swingCalledWith.get());
    }

    @Test
    public void fxSuccess_doesNotInvokeNotifierOrSwing() {
        BasicSignerOptions opts = new BasicSignerOptions();
        AtomicReference<BasicSignerOptions> fxCalledWith = new AtomicReference<>();
        AtomicReference<BasicSignerOptions> swingCalledWith = new AtomicReference<>();
        AtomicReference<Throwable> notifierCalledWith = new AtomicReference<>();

        Signer.fxLauncher = fxCalledWith::set;
        Signer.fxFallbackNotifier = notifierCalledWith::set;
        Signer.swingLauncher = swingCalledWith::set;

        Signer.launchGui(opts);

        assertSame("FX launcher should run on the happy path", opts, fxCalledWith.get());
        assertNull("Swing fallback must not run when FX succeeds", swingCalledWith.get());
        assertNull("Notifier must not fire when FX succeeds", notifierCalledWith.get());
    }

    @Test
    public void swingSystemProperty_skipsFxEntirely() {
        BasicSignerOptions opts = new BasicSignerOptions();
        AtomicReference<BasicSignerOptions> fxCalledWith = new AtomicReference<>();
        AtomicReference<BasicSignerOptions> swingCalledWith = new AtomicReference<>();
        AtomicReference<Throwable> notifierCalledWith = new AtomicReference<>();

        Signer.fxLauncher = fxCalledWith::set;
        Signer.fxFallbackNotifier = notifierCalledWith::set;
        Signer.swingLauncher = swingCalledWith::set;

        System.setProperty("jsignpdf.swing", "true");
        try {
            Signer.launchGui(opts);
        } finally {
            System.clearProperty("jsignpdf.swing");
        }

        assertSame("Swing launcher should run when -Djsignpdf.swing=true is set", opts, swingCalledWith.get());
        assertNull("FX launcher must not be invoked when forced to Swing", fxCalledWith.get());
        assertNull("No fallback notifier when FX wasn't even attempted", notifierCalledWith.get());
    }

    @Test
    public void notifierFailure_doesNotPreventSwingFallback() {
        BasicSignerOptions opts = new BasicSignerOptions();
        AtomicReference<BasicSignerOptions> swingCalledWith = new AtomicReference<>();

        Signer.fxLauncher = o -> {
            throw new UnsatisfiedLinkError("missing native");
        };
        Signer.fxFallbackNotifier = t -> {
            throw new java.awt.HeadlessException("no display");
        };
        Signer.swingLauncher = swingCalledWith::set;

        Signer.launchGui(opts);

        assertSame("Swing fallback must run even if the warning dialog itself can't be shown",
                opts, swingCalledWith.get());
    }

    @Test
    public void fallbackMessage_containsGithubIssuesUrl() {
        String message = Signer.buildFxFallbackMessage(new UnsatisfiedLinkError("incompatible architecture"));
        assertNotNull(message);
        assertTrue("Fallback dialog message must include the GitHub issues URL: " + message,
                message.contains(Signer.GITHUB_ISSUES_URL));
        assertTrue("Fallback dialog message should describe the failure cause: " + message,
                message.contains("incompatible architecture"));
    }

    @Test
    public void fallbackMessage_handlesNullCauseGracefully() {
        String message = Signer.buildFxFallbackMessage(null);
        assertNotNull(message);
        assertTrue("Even with a null cause, the GitHub issues URL must appear in the message: " + message,
                message.contains(Signer.GITHUB_ISSUES_URL));
    }
}
