package net.sf.jsignpdf.fx.view;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.EngineConfig;
import net.sf.jsignpdf.engine.SigningEngine;
import net.sf.jsignpdf.fx.EngineCapabilities;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;

public class SignaturePropertiesControllerTest {

    @BeforeClass
    public static void initFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void persistedOverwriteIsForcedBackToAppendForNonOverwriteEngine() throws Exception {
        AtomicReference<Boolean> appendAfterLoad = new AtomicReference<>();
        runOnFxThread(() -> {
            SigningOptionsViewModel vm = new SigningOptionsViewModel();

            SignaturePropertiesController controller = new SignaturePropertiesController();
            setField(controller, "cmbHashAlgorithm", new ComboBox<>());
            setField(controller, "cmbCertLevel", new ComboBox<>());
            setField(controller, "txtSignerName", new TextField());
            setField(controller, "txtReason", new TextField());
            setField(controller, "txtLocation", new TextField());
            setField(controller, "txtContact", new TextField());
            setField(controller, "chkAppend", new CheckBox());
            setField(controller, "txtOutFile", new TextField());
            controller.setViewModel(vm);

            EngineCapabilities caps = new EngineCapabilities();
            caps.activeEngineProperty().set(new FakeEngine(EnumSet.of(Capability.PADES_BASELINE_B)));

            controller.gateCapabilities(caps);

            vm.appendProperty().set(false);
            appendAfterLoad.set(vm.appendProperty().get());
        });
        assertTrue("append must be forced on for a non-overwrite engine", appendAfterLoad.get());
    }

    @Test
    public void overwriteIsAllowedForOverwriteCapableEngine() throws Exception {
        AtomicReference<Boolean> appendAfterLoad = new AtomicReference<>();
        runOnFxThread(() -> {
            SigningOptionsViewModel vm = new SigningOptionsViewModel();

            SignaturePropertiesController controller = new SignaturePropertiesController();
            setField(controller, "cmbHashAlgorithm", new ComboBox<>());
            setField(controller, "cmbCertLevel", new ComboBox<>());
            setField(controller, "txtSignerName", new TextField());
            setField(controller, "txtReason", new TextField());
            setField(controller, "txtLocation", new TextField());
            setField(controller, "txtContact", new TextField());
            setField(controller, "chkAppend", new CheckBox());
            setField(controller, "txtOutFile", new TextField());
            controller.setViewModel(vm);

            EngineCapabilities caps = new EngineCapabilities();
            caps.activeEngineProperty().set(new FakeEngine(EnumSet.of(Capability.OVERWRITE_MODE)));

            controller.gateCapabilities(caps);

            vm.appendProperty().set(false);
            appendAfterLoad.set(vm.appendProperty().get());
        });
        assertFalse("overwrite must survive for an overwrite-capable engine", appendAfterLoad.get());
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class FakeEngine implements SigningEngine {
        private final Set<Capability> capabilities;

        FakeEngine(Set<Capability> capabilities) {
            this.capabilities = Set.copyOf(capabilities);
        }

        @Override
        public String id() {
            return "fake";
        }

        @Override
        public String displayName() {
            return "Fake";
        }

        @Override
        public Set<Capability> capabilities() {
            return capabilities;
        }

        @Override
        public boolean sign(BasicSignerOptions options, EngineConfig engineConfig) {
            return false;
        }
    }
}
