package org.fxt.freeplsql.app;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fxt.freeplsql.app.ui.WorkspaceController;
import org.fxt.freeplsql.app.ui.shell.Branding;
import org.fxt.freeplsql.app.ui.shell.FontLoader;
import org.fxt.freeplsql.app.ui.startup.StartupSequence;
import org.fxt.freeplsql.appsvc.connection.ConnectionProfile;
import org.fxt.freeplsql.appsvc.persistence.WorkspaceState;

import java.util.List;
import java.util.Objects;

public final class MainApp extends Application {

    public static final String APP_NAME = "FreePlSqlToolkit";

    private final AppContext context = new AppContext();

    @Override
    public void start(Stage stage) throws Exception {
        applyTheme(context.settings().isDark());
        FontLoader.loadAll();

        List<ConnectionProfile> unlocked = new StartupSequence(context.profileStore()).run();
        if (unlocked == null) {
            Platform.exit();
            return;
        }
        context.setProfiles(unlocked);

        WorkspaceState ws = context.workspace();

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(MainApp.class.getResource("/fxml/MainView.fxml"),
                        "MainView.fxml not found on classpath"));
        Scene scene = new Scene(loader.load(), ws.getWindowWidth(), ws.getWindowHeight());
        scene.getStylesheets().add(
                Objects.requireNonNull(MainApp.class.getResource("/css/syntax.css")).toExternalForm());
        scene.getStylesheets().add(
                Objects.requireNonNull(MainApp.class.getResource("/css/app.css")).toExternalForm());

        WorkspaceController controller = loader.getController();
        controller.bind(stage, scene, context);

        if (ws.getWindowX() >= 0 && ws.getWindowY() >= 0) {
            stage.setX(ws.getWindowX());
            stage.setY(ws.getWindowY());
        }
        if (ws.isMaximized()) {
            stage.setMaximized(true);
        }

        stage.setOnCloseRequest(ev -> {
            if (!stage.isMaximized()) {
                ws.setWindowX(stage.getX());
                ws.setWindowY(stage.getY());
                ws.setWindowWidth(stage.getWidth());
                ws.setWindowHeight(stage.getHeight());
            }
            ws.setMaximized(stage.isMaximized());
            controller.captureWorkspaceState();
            try {
                context.saveWorkspace();
            } catch (RuntimeException ignored) {
                // non-fatal — startup will fall back to defaults
            }
            context.shutdown();
        });

        stage.setTitle(APP_NAME);
        stage.getIcons().addAll(Branding.stageIcons());
        stage.setScene(scene);
        stage.show();

        controller.restoreOpenTabs();
    }

    public static void applyTheme(boolean dark) {
        Application.setUserAgentStylesheet(
                dark ? new PrimerDark().getUserAgentStylesheet()
                     : new PrimerLight().getUserAgentStylesheet());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
