package net.sf.jsignpdf.fx;

import static net.sf.jsignpdf.Constants.RES;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;

import net.sf.jsignpdf.engine.Capability;
import net.sf.jsignpdf.engine.SigningEngine;

/**
 * Holds the active {@link SigningEngine} as an observable property and turns engine capabilities into
 * JavaFX bindings so controls can disable themselves (and show a shared "not supported" tooltip) when
 * the selected engine cannot honour them.
 *
 * <p>
 * Capability gating is driven entirely off {@link #activeEngineProperty()}; switching the engine in the
 * toolbar re-evaluates every binding created here. A control is treated as unsupported when the active
 * engine declares <em>none</em> of the capabilities passed to {@link #unsupported(Capability...)} —
 * this gives umbrella controls (e.g. the encryption section, gated on
 * {@code ENCRYPTION_PASSWORD ∪ ENCRYPTION_CERTIFICATE}) union semantics, and single-capability controls
 * the obvious "missing means disabled" behaviour.
 * </p>
 *
 * @author Josef Cacek
 */
public final class EngineCapabilities {

    private final ObjectProperty<SigningEngine> activeEngine = new SimpleObjectProperty<>();
    private final Tooltip unsupportedTooltip = new Tooltip(RES.get("jfx.gui.engine.unsupported"));

    /**
     * @return the property holding the engine whose capabilities gate the UI
     */
    public ObjectProperty<SigningEngine> activeEngineProperty() {
        return activeEngine;
    }

    /**
     * Returns a binding that is {@code true} when the active engine supports none of the given
     * capabilities (i.e. the gated control should be disabled).
     *
     * @param caps the capabilities the control needs (any one of them is sufficient)
     * @return the "unsupported" binding
     */
    public BooleanBinding unsupported(Capability... caps) {
        return Bindings.createBooleanBinding(() -> {
            SigningEngine engine = activeEngine.get();
            if (engine == null) {
                return false;
            }
            for (Capability c : caps) {
                if (engine.capabilities().contains(c)) {
                    return false;
                }
            }
            return true;
        }, activeEngine);
    }

    /**
     * Binds the control's {@code disableProperty} to {@link #unsupported(Capability...)} and shows the
     * shared "not supported by the selected engine" tooltip while it is disabled.
     *
     * @param control the control to gate
     * @param caps the capabilities it requires
     */
    public void gate(Control control, Capability... caps) {
        BooleanBinding unsupported = unsupported(caps);
        control.disableProperty().bind(unsupported);
        // Show the shared "not supported" tooltip while disabled, otherwise keep whatever tooltip the
        // control already had (e.g. the informative tooltip declared in FXML).
        final Tooltip original = control.getTooltip();
        control.tooltipProperty().bind(Bindings.when(unsupported).then(unsupportedTooltip).otherwise(original));
    }
}
