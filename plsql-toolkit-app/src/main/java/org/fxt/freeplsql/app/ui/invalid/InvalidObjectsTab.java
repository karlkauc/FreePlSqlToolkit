package org.fxt.freeplsql.app.ui.invalid;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxt.freeplsql.app.AppContext;
import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.appsvc.invalid.InvalidEntry;
import org.fxt.freeplsql.appsvc.invalid.InvalidObjectsService;
import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;

import java.sql.Connection;
import java.util.List;

public final class InvalidObjectsTab extends Tab {

    private final AppContext context;
    private final InvalidObjectsService service = new InvalidObjectsService();
    private final SchemaMetadataService metadata = new SchemaMetadataService();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final ComboBox<ConnectionHandle> connectionCombo = new ComboBox<>();
    private final ComboBox<String> schemaCombo = new ComboBox<>();
    private final Button refreshButton = new Button("Refresh");
    private final Label statusLabel = new Label("Pick a connection and schema.");

    public InvalidObjectsTab(AppContext context) {
        super("Invalid Objects");
        this.context = context;

        connectionCombo.getItems().setAll(context.connectionManager().activeConnections());
        connectionCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ConnectionHandle h) { return h == null ? "" : h.profile().name(); }
            @Override public ConnectionHandle fromString(String s) { return null; }
        });
        connectionCombo.valueProperty().addListener((obs, was, h) -> loadSchemas(h));

        HBox controls = new HBox(8,
                new Label("Connection:"), connectionCombo,
                new Label("Schema:"), schemaCombo,
                refreshButton);
        controls.setPadding(new Insets(8));

        TableView<Row> table = buildTable();
        VBox layout = new VBox(controls, table, statusLabel);
        VBox.setVgrow(table, Priority.ALWAYS);
        statusLabel.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.setStyle("-fx-text-fill: -color-fg-muted;");
        setContent(layout);

        refreshButton.setOnAction(e -> refresh());
    }

    private TableView<Row> buildTable() {
        var table = new TableView<>(rows);
        TableColumn<Row, String> owner = new TableColumn<>("Owner");
        owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        owner.setPrefWidth(110);
        TableColumn<Row, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        name.setPrefWidth(180);
        TableColumn<Row, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        type.setPrefWidth(120);
        TableColumn<Row, Number> line = new TableColumn<>("Line");
        line.setCellValueFactory(new PropertyValueFactory<>("line"));
        line.setPrefWidth(60);
        TableColumn<Row, Number> pos = new TableColumn<>("Pos");
        pos.setCellValueFactory(new PropertyValueFactory<>("position"));
        pos.setPrefWidth(60);
        TableColumn<Row, String> attr = new TableColumn<>("Attribute");
        attr.setCellValueFactory(new PropertyValueFactory<>("attribute"));
        attr.setPrefWidth(90);
        TableColumn<Row, String> text = new TableColumn<>("Message");
        text.setCellValueFactory(new PropertyValueFactory<>("text"));
        text.setPrefWidth(700);
        table.getColumns().setAll(owner, name, type, line, pos, attr, text);
        return table;
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
        Thread t = new Thread(task, "invalid-schemas");
        t.setDaemon(true);
        t.start();
    }

    private void refresh() {
        ConnectionHandle handle = connectionCombo.getValue();
        String schema = schemaCombo.getValue();
        if (handle == null || schema == null) return;
        statusLabel.setText("Loading…");
        rows.clear();
        Task<List<InvalidEntry>> task = new Task<>() {
            @Override
            protected List<InvalidEntry> call() throws Exception {
                try (Connection c = handle.borrow()) {
                    return service.listErrors(c, schema);
                }
            }
        };
        task.setOnSucceeded(ev -> {
            for (InvalidEntry e : task.getValue()) {
                rows.add(Row.from(e));
            }
            statusLabel.setText(rows.size() + " error(s)");
        });
        task.setOnFailed(ev -> {
            statusLabel.setText("Failed.");
            new Alert(Alert.AlertType.ERROR,
                    "Could not load errors:\n" + task.getException().getMessage()).showAndWait();
        });
        Thread t = new Thread(task, "invalid-load");
        t.setDaemon(true);
        t.start();
    }

    public static final class Row {
        private final SimpleStringProperty owner;
        private final SimpleStringProperty name;
        private final SimpleStringProperty type;
        private final javafx.beans.property.SimpleIntegerProperty line;
        private final javafx.beans.property.SimpleIntegerProperty position;
        private final SimpleStringProperty text;
        private final SimpleStringProperty attribute;

        public Row(String owner, String name, String type, int line, int position,
                   String text, String attribute) {
            this.owner = new SimpleStringProperty(owner);
            this.name = new SimpleStringProperty(name);
            this.type = new SimpleStringProperty(type);
            this.line = new javafx.beans.property.SimpleIntegerProperty(line);
            this.position = new javafx.beans.property.SimpleIntegerProperty(position);
            this.text = new SimpleStringProperty(text);
            this.attribute = new SimpleStringProperty(attribute);
        }

        public static Row from(InvalidEntry e) {
            return new Row(e.owner(), e.name(), e.type(), e.line(), e.position(),
                    e.text(), e.attribute());
        }

        public String getOwner() { return owner.get(); }
        public String getName() { return name.get(); }
        public String getType() { return type.get(); }
        public int getLine() { return line.get(); }
        public int getPosition() { return position.get(); }
        public String getText() { return text.get(); }
        public String getAttribute() { return attribute.get(); }
    }
}
