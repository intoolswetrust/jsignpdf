package net.sf.jsignpdf.fx.control;

import java.io.File;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/** Text field for the suffix of the next signed PDF (e.g. DL or EM). */
public final class OutputSuffixField extends TextField {
    public OutputSuffixField() {
        super();
        textProperty().addListener((obs, oldValue, newValue) -> {
            OutputSuffixSupport.setUserValue(newValue);
            refreshVisibleOutput();
        });
    }

    private void refreshVisibleOutput() {
        String output = OutputSuffixSupport.suggestedForLastInput();
        if (output == null) return;
        Scene scene = getScene();
        if (scene == null) return;
        Node node = scene.lookup("#lblOutputPath");
        if (!(node instanceof Label label)) return;
        label.setText("→ " + new File(output).getName());
        Tooltip tooltip = label.getTooltip();
        if (tooltip == null) {
            tooltip = new Tooltip(output);
            label.setTooltip(tooltip);
        } else {
            tooltip.setText(output);
        }
    }
}
