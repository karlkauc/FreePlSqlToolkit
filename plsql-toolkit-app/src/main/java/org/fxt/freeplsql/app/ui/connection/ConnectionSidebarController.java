package org.fxt.freeplsql.app.ui.connection;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxt.freeplsql.app.AppContext;
import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.appsvc.connection.ConnectionManager;
import org.fxt.freeplsql.appsvc.connection.ConnectionProfile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public final class ConnectionSidebarController {

    @FXML private TitledPane root;
    @FXML private ListView<ConnectionProfile> profileList;
    @FXML private VBox emptyState;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    private final ObservableList<ConnectionProfile> items = FXCollections.observableArrayList();
    private AppContext context;
    private ConnectionManager connectionManager;

    public static TitledPane create(AppContext context) {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                ConnectionSidebarController.class.getResource("/fxml/ConnectionSidebar.fxml"),
                "ConnectionSidebar.fxml not found"));
        TitledPane pane;
        try {
            pane = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load ConnectionSidebar.fxml", e);
        }
        ConnectionSidebarController controller = loader.getController();
        controller.bind(context);
        return pane;
    }

    @FXML
    private void initialize() {
        profileList.setItems(items);
        var selected = profileList.getSelectionModel().selectedItemProperty();
        editButton.disableProperty().bind(selected.isNull());
        deleteButton.disableProperty().bind(selected.isNull());

        addButton.setOnAction(e -> onAdd());
        editButton.setOnAction(e -> onEdit());
        deleteButton.setOnAction(e -> onDelete());

        profileList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && profileList.getSelectionModel().getSelectedItem() != null) {
                onEdit();
            }
        });

        org.fxt.freeplsql.app.ui.shell.EmptyState empty =
                org.fxt.freeplsql.app.ui.shell.EmptyState.builder()
                        .featherIcon(org.kordamp.ikonli.feather.Feather.DATABASE)
                        .title("No connections yet")
                        .body("Add a database connection profile to start exploring schemas and running the linter.")
                        .action("+ Add connection", () -> addButton.fire())
                        .build();
        emptyState.getChildren().setAll(empty);
        Runnable updateEmptyState = () -> {
            boolean isEmpty = profileList.getItems().isEmpty();
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);
            profileList.setVisible(!isEmpty);
            profileList.setManaged(!isEmpty);
        };
        profileList.getItems().addListener(
                (javafx.collections.ListChangeListener<? super
                        org.fxt.freeplsql.appsvc.connection.ConnectionProfile>) c -> updateEmptyState.run());
        updateEmptyState.run();
    }

    private void bind(AppContext context) {
        this.context = context;
        this.connectionManager = context.connectionManager();
        items.setAll(context.profiles());
        profileList.setCellFactory(lv -> new ProfileCell(connectionManager, this::refreshList));
        connectionManager.addConnectListener(h -> Platform.runLater(this::refreshList));
        connectionManager.addDisconnectListener(id -> Platform.runLater(this::refreshList));
    }

    private void refreshList() {
        profileList.refresh();
    }

    private void onAdd() {
        new ProfileEditorDialog(null).showAndWait().ifPresent(p -> {
            items.add(p);
            persist();
        });
    }

    private void onEdit() {
        ConnectionProfile current = profileList.getSelectionModel().getSelectedItem();
        if (current == null) return;
        if (connectionManager.isActive(current.id())) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Disconnect '" + current.name() + "' before editing.").showAndWait();
            return;
        }
        Optional<ConnectionProfile> updated = new ProfileEditorDialog(current).showAndWait();
        updated.ifPresent(p -> {
            int idx = items.indexOf(current);
            items.set(idx, p);
            profileList.getSelectionModel().select(p);
            persist();
        });
    }

    private void onDelete() {
        ConnectionProfile current = profileList.getSelectionModel().getSelectedItem();
        if (current == null) return;
        if (connectionManager.isActive(current.id())) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Disconnect '" + current.name() + "' before deleting.").showAndWait();
            return;
        }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete profile '" + current.name() + "'?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(b -> {
                    items.remove(current);
                    persist();
                });
    }

    private void persist() {
        context.setProfiles(new ArrayList<>(items));
        try {
            context.saveProfiles();
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not save profiles:\n" + e.getMessage()).showAndWait();
        }
    }

    /** Custom row: status dot · profile name · spacer · Connect/Disconnect button. */
    private static final class ProfileCell extends ListCell<ConnectionProfile> {

        private final ConnectionManager mgr;
        private final Runnable refresh;
        private final HBox layout = new HBox(8);
        private final Label statusDot = new Label("○");
        private final Label nameLabel = new Label();
        private final Button connectButton = new Button("Connect");

        ProfileCell(ConnectionManager mgr, Runnable refresh) {
            this.mgr = mgr;
            this.refresh = refresh;
            layout.setAlignment(Pos.CENTER_LEFT);
            statusDot.getStyleClass().add("conn-dot");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            connectButton.getStyleClass().add("connect-button");
            layout.getChildren().addAll(statusDot, nameLabel, spacer, connectButton);
            connectButton.setOnAction(e -> toggle());
        }

        @Override
        protected void updateItem(ConnectionProfile p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            nameLabel.setText(p.name() + "  ·  " + p.authType().name());
            renderState();
            setText(null);
            setGraphic(layout);
        }

        private void renderState() {
            ConnectionProfile p = getItem();
            if (p == null) return;
            boolean active = mgr.isActive(p.id());
            statusDot.setText(active ? "●" : "○");
            statusDot.pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("connected"), active);
            connectButton.setText(active ? "Disconnect" : "Connect");
            connectButton.setDisable(false);
        }

        private void toggle() {
            ConnectionProfile p = getItem();
            if (p == null) return;
            if (mgr.isActive(p.id())) {
                mgr.disconnect(p.id());
                refresh.run();
                return;
            }
            connectButton.setDisable(true);
            connectButton.setText("Connecting…");
            Task<ConnectionHandle> task = new Task<>() {
                @Override
                protected ConnectionHandle call() throws Exception {
                    return mgr.connect(p);
                }
            };
            task.setOnSucceeded(ev -> refresh.run());
            task.setOnFailed(ev -> {
                Throwable err = task.getException();
                renderState();
                Alert a = new Alert(Alert.AlertType.ERROR,
                        "Could not connect to '" + p.name() + "':\n"
                                + (err != null ? err.getMessage() : "unknown error"));
                a.setHeaderText(null);
                a.showAndWait();
            });
            Thread t = new Thread(task, "connect-" + p.id());
            t.setDaemon(true);
            t.start();
        }
    }
}
