package net.sf.jsignpdf.fx.preset;

import static net.sf.jsignpdf.Constants.RES;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.sf.jsignpdf.BasicSignerOptions;

/**
 * Dialog that lists all presets and offers per-row rename / overwrite / delete actions.
 */
public class ManagePresetsDialog extends Dialog<Void> {

    private final PresetManager manager;
    private final BasicSignerOptions options;

    public ManagePresetsDialog(PresetManager manager, BasicSignerOptions options, Stage owner) {
        this.manager = manager;
        this.options = options;

        setTitle(RES.get("jfx.gui.preset.manage.title"));
        setHeaderText(null);
        if (owner != null) {
            initOwner(owner);
        }

        TableView<Preset> table = buildTable();
        table.setPrefSize(620, 320);
        table.setPlaceholder(new Label(RES.get("jfx.gui.preset.manage.empty")));

        HBox content = new HBox(table);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((javafx.scene.control.Button) getDialogPane().lookupButton(ButtonType.CLOSE))
                .setText(RES.get("jfx.gui.preset.manage.button.close"));
        setResultConverter(bt -> null);
    }

    private TableView<Preset> buildTable() {
        TableView<Preset> table = new TableView<>(manager.getPresets());

        TableColumn<Preset, String> nameCol = new TableColumn<>(RES.get("jfx.gui.preset.manage.column.name"));
        nameCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getDisplayName()));
        nameCol.setPrefWidth(220);

        TableColumn<Preset, Instant> createdCol = new TableColumn<>(RES.get("jfx.gui.preset.manage.column.created"));
        createdCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getCreatedAt()));
        createdCol.setCellFactory(col -> new TableCell<>() {
            private final DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

            @Override
            protected void updateItem(Instant item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : fmt.format(java.util.Date.from(item)));
            }
        });
        createdCol.setPrefWidth(150);

        TableColumn<Preset, Void> actionsCol = new TableColumn<>(RES.get("jfx.gui.preset.manage.column.actions"));
        actionsCol.setCellFactory(buildActionsCellFactory());
        actionsCol.setPrefWidth(230);
        actionsCol.setSortable(false);
        actionsCol.setReorderable(false);

        table.getColumns().add(nameCol);
        table.getColumns().add(createdCol);
        table.getColumns().add(actionsCol);
        return table;
    }

    private Callback<TableColumn<Preset, Void>, TableCell<Preset, Void>> buildActionsCellFactory() {
        return column -> new TableCell<>() {
            private final javafx.scene.control.Button renameBtn =
                    new javafx.scene.control.Button(RES.get("jfx.gui.preset.manage.button.rename"));
            private final javafx.scene.control.Button overwriteBtn =
                    new javafx.scene.control.Button(RES.get("jfx.gui.preset.manage.button.overwrite"));
            private final javafx.scene.control.Button deleteBtn =
                    new javafx.scene.control.Button(RES.get("jfx.gui.preset.manage.button.delete"));
            private final HBox box = new HBox(6, renameBtn, overwriteBtn, deleteBtn);

            {
                renameBtn.setOnAction(e -> onRename(getTableRow().getItem()));
                overwriteBtn.setOnAction(e -> onOverwrite(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> onDelete(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getTableRow() == null || getTableRow().getItem() == null) ? null : box);
            }
        };
    }

    private void onRename(Preset preset) {
        if (preset == null) {
            return;
        }
        TextInputDialog dlg = new TextInputDialog(preset.getDisplayName());
        dlg.setTitle(RES.get("jfx.gui.preset.dialog.rename.title"));
        dlg.setHeaderText(RES.get("jfx.gui.preset.dialog.rename.header"));
        dlg.setContentText(RES.get("jfx.gui.preset.dialog.rename.prompt"));
        dlg.initOwner(getDialogPane().getScene().getWindow());

        while (true) {
            Optional<String> result = dlg.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            String name = result.get();
            PresetValidation.Result validation = PresetValidation.validate(name,
                    n -> manager.hasDisplayName(n, preset));
            if (validation != PresetValidation.Result.OK) {
                showAlert(Alert.AlertType.ERROR,
                        RES.get("jfx.gui.preset.dialog.rename.title"),
                        validationMessage(validation));
                dlg.getEditor().setText(PresetValidation.trim(name));
                continue;
            }
            manager.rename(preset, PresetValidation.trim(name));
            return;
        }
    }

    private void onOverwrite(Preset preset) {
        if (preset == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(RES.get("jfx.gui.preset.dialog.overwrite.title"));
        confirm.setHeaderText(null);
        confirm.setContentText(MessageFormat.format(
                RES.get("jfx.gui.preset.dialog.overwrite.confirm"), preset.getDisplayName()));
        confirm.initOwner(getDialogPane().getScene().getWindow());
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            manager.overwrite(preset, options);
        }
    }

    private void onDelete(Preset preset) {
        if (preset == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(RES.get("jfx.gui.preset.dialog.delete.title"));
        confirm.setHeaderText(null);
        confirm.setContentText(MessageFormat.format(
                RES.get("jfx.gui.preset.dialog.delete.confirm"), preset.getDisplayName()));
        confirm.initOwner(getDialogPane().getScene().getWindow());
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            manager.delete(preset);
        }
    }

    private String validationMessage(PresetValidation.Result result) {
        switch (result) {
            case EMPTY: return RES.get("jfx.gui.preset.validation.empty");
            case ILLEGAL_CHAR: return RES.get("jfx.gui.preset.validation.illegalChar");
            case TOO_LONG: return RES.get("jfx.gui.preset.validation.tooLong");
            case DUPLICATE: return RES.get("jfx.gui.preset.validation.duplicate");
            default: return "";
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }
}
