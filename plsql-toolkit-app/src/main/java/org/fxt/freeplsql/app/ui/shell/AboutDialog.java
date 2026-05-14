package org.fxt.freeplsql.app.ui.shell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class AboutDialog {

    private AboutDialog() {}

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("About FreePlSqlToolkit");

        ImageView mark = new ImageView(Branding.markImage(96));
        mark.setFitWidth(96); mark.setFitHeight(96); mark.setPreserveRatio(true);

        Label name = new Label("FreePlSqlToolkit");
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");
        Label version = new Label("Version 0.2");
        version.setStyle("-fx-text-fill: -fxt-fg-muted;");
        Label tag = new Label("PL/SQL · LINT · WORKBENCH");
        tag.setStyle("-fx-text-fill: -fxt-fg-muted; -fx-font-size: 11px;");
        Label license = new Label("Apache License 2.0");
        license.setStyle("-fx-text-fill: -fxt-fg-muted;");

        Hyperlink repo = new Hyperlink("github.com/karlkauc/FreePlSqlToolkit");
        repo.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                        new java.net.URI("https://github.com/karlkauc/FreePlSqlToolkit"));
            } catch (Exception ignored) {}
        });

        Button close = new Button("Close");
        close.getStyleClass().add("button-primary");
        close.setOnAction(e -> stage.close());

        VBox root = new VBox(12, mark, name, version, tag, license, repo, close);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("about-dialog");
        Scene scene = new Scene(root, 360, 460);
        // Inherit owner stylesheets so theme + tokens apply
        if (owner instanceof Stage os && os.getScene() != null) {
            scene.getStylesheets().setAll(os.getScene().getStylesheets());
            if (os.getScene().getRoot().getStyleClass().contains("dark")) {
                scene.getRoot().getStyleClass().add("dark");
            }
        }
        stage.setScene(scene);
        stage.showAndWait();
    }
}
