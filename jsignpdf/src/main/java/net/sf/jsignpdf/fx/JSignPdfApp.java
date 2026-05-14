package net.sf.jsignpdf.fx;

import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.view.MainWindowController;

/**
 * JavaFX Application entry point for JSignPdf.
 */
public class JSignPdfApp extends Application {

    private static BasicSignerOptions initialOptions;

    static void setInitialOptions(BasicSignerOptions opts) {
        initialOptions = opts;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE_BASE);
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/net/sf/jsignpdf/fx/view/MainWindow.fxml"), bundle);
        Parent root = loader.load();

        MainWindowController controller = loader.getController();
        controller.setStage(primaryStage);

        // Create options and load persisted configuration
        BasicSignerOptions opts = initialOptions != null ? initialOptions : new BasicSignerOptions();
        opts.loadOptions();
        controller.initFromOptions(opts);

        Scene scene = new Scene(root, 1100, 750);
        scene.getStylesheets().add(
                getClass().getResource("/net/sf/jsignpdf/fx/styles/jsignpdf.css").toExternalForm());

        primaryStage.setTitle("JSignPdf " + Constants.VERSION);
        primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/net/sf/jsignpdf/signedpdf32.png")));
        primaryStage.setScene(scene);

        // Store configuration on window close
        primaryStage.setOnCloseRequest(event -> controller.storeAndCleanup());

        primaryStage.show();

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        if (primaryStage.getWidth() > visualBounds.getWidth() || primaryStage.getHeight() > visualBounds.getHeight()) {
            primaryStage.setMaximized(true);
        }
    }
}
