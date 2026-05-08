package org.fxt.freeplsql.app;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public final class MainApp extends Application {

    public static final String APP_NAME = "FreePlSqlToolkit";

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(MainApp.class.getResource("/fxml/MainView.fxml"),
                        "MainView.fxml not found on classpath"));
        Scene scene = new Scene(loader.load(), 1100, 750);
        scene.getStylesheets().add(
                Objects.requireNonNull(MainApp.class.getResource("/css/syntax.css")).toExternalForm());
        scene.getStylesheets().add(
                Objects.requireNonNull(MainApp.class.getResource("/css/app.css")).toExternalForm());

        MainController controller = loader.getController();
        controller.bind(stage, scene);

        stage.setTitle(APP_NAME);
        stage.setScene(scene);
        stage.show();
    }

    /** Switches between AtlantaFX Primer Light and Dark themes. */
    public static void applyTheme(boolean dark) {
        Application.setUserAgentStylesheet(
                dark ? new PrimerDark().getUserAgentStylesheet()
                     : new PrimerLight().getUserAgentStylesheet());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
