package org.fxt.freeplsql.app.ui.snapshot;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.fxt.freeplsql.app.AppContext;
import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;
import org.fxt.freeplsql.appsvc.snapshot.SnapshotRunner;
import org.fxt.freeplsql.sync.SyncResult;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

/** Modal dialog: pick connection + schema + repo, run a single DB→Git snapshot. */
public final class SnapshotDialog extends Dialog<SyncResult> {

    private final AppContext context;
    private final SnapshotRunner runner = new SnapshotRunner();
    private final SchemaMetadataService metadata = new SchemaMetadataService();

    private final ComboBox<ConnectionHandle> connectionCombo = new ComboBox<>();
    private final ComboBox<String> schemaCombo = new ComboBox<>();
    private final TextField repoField = new TextField();
    private final Button browseButton = new Button("Browse…");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Label statusLabel = new Label("Ready.");

    public SnapshotDialog(AppContext context) {
        this.context = context;
        setTitle("Snapshot to Git");
        setGraphic(brandMark());
        setHeaderText("Dumps each PL/SQL object's DDL into a Git repository "
                + "and commits the changed files.");

        connectionCombo.getItems().setAll(context.connectionManager().activeConnections());
        connectionCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ConnectionHandle h) { return h == null ? "" : h.profile().name(); }
            @Override public ConnectionHandle fromString(String s) { return null; }
        });
        connectionCombo.valueProperty().addListener((obs, was, h) -> loadSchemas(h));

        var grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Connection:"), 0, 0);
        grid.add(connectionCombo, 1, 0, 2, 1);
        grid.add(new Label("Schema:"), 0, 1);
        grid.add(schemaCombo, 1, 1, 2, 1);
        grid.add(new Label("Git repository:"), 0, 2);
        repoField.setPromptText("/path/to/repo (will be initialised if empty)");
        HBox.setHgrow(repoField, Priority.ALWAYS);
        grid.add(repoField, 1, 2);
        grid.add(browseButton, 2, 2);
        grid.add(statusLabel, 0, 3, 2, 1);
        grid.add(progress, 2, 3);
        progress.setVisible(false);
        progress.setPrefSize(20, 20);
        statusLabel.setStyle("-fx-text-fill: -fxt-fg-muted;");

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().setAll(
                new ButtonType("Run", javafx.scene.control.ButtonBar.ButtonData.OK_DONE),
                ButtonType.CLOSE);

        browseButton.setOnAction(e -> chooseDirectory());

        var runButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        runButton.getStyleClass().add("button-primary");
        runButton.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            ev.consume();   // run async, don't close dialog yet
            runSnapshot(runButton);
        });

        setResultConverter(b -> null);
    }

    private void chooseDirectory() {
        var chooser = new DirectoryChooser();
        chooser.setTitle("Select repository directory");
        if (!repoField.getText().isBlank()) {
            File f = new File(repoField.getText());
            if (f.isDirectory()) chooser.setInitialDirectory(f);
        }
        Window owner = getDialogPane().getScene() != null
                ? getDialogPane().getScene().getWindow() : null;
        File picked = chooser.showDialog(owner);
        if (picked != null) {
            repoField.setText(picked.getAbsolutePath());
        }
    }

    private void loadSchemas(ConnectionHandle handle) {
        schemaCombo.getItems().clear();
        if (handle == null) return;
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                try (Connection c = handle.borrow()) {
                    return metadata.listSchemas(c);
                }
            }
        };
        task.setOnSucceeded(ev -> schemaCombo.getItems().setAll(task.getValue()));
        Thread t = new Thread(task, "snapshot-schemas");
        t.setDaemon(true);
        t.start();
    }

    private static javafx.scene.image.ImageView brandMark() {
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                org.fxt.freeplsql.app.ui.shell.Branding.markImage(40));
        iv.setFitWidth(40);
        iv.setFitHeight(40);
        iv.setPreserveRatio(true);
        return iv;
    }

    private void runSnapshot(Button runButton) {
        ConnectionHandle handle = connectionCombo.getValue();
        String schema = schemaCombo.getValue();
        String repoText = repoField.getText() == null ? "" : repoField.getText().trim();
        if (handle == null || schema == null || repoText.isEmpty()) {
            statusLabel.setText("Pick a connection, schema, and repository first.");
            return;
        }
        statusLabel.setText("Snapshot running…");
        progress.setVisible(true);
        runButton.setDisable(true);

        Task<SyncResult> task = new Task<>() {
            @Override
            protected SyncResult call() throws Exception {
                return runner.run(handle.profile(), Path.of(repoText), List.of(schema));
            }
        };
        task.setOnSucceeded(ev -> {
            progress.setVisible(false);
            runButton.setDisable(false);
            SyncResult r = task.getValue();
            statusLabel.setText("Done · scanned " + r.objectsScanned()
                    + " · updated " + r.updatedRelativePaths().size()
                    + (r.commitHash() != null ? " · commit " + r.commitHash().substring(0, 8) : ""));
        });
        task.setOnFailed(ev -> {
            progress.setVisible(false);
            runButton.setDisable(false);
            statusLabel.setText("Failed.");
            new Alert(Alert.AlertType.ERROR,
                    "Snapshot failed:\n" + task.getException().getMessage()).showAndWait();
        });
        Thread t = new Thread(task, "snapshot");
        t.setDaemon(true);
        t.start();
    }
}
