package org.fxt.freeplsql.app.ui.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxt.freeplsql.app.AppContext;
import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.appsvc.diff.DiffResult;
import org.fxt.freeplsql.appsvc.diff.SchemaDiffService;
import org.fxt.freeplsql.appsvc.lint.ProgressSink;
import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

/** Compares the same schema across two connections and shows added/removed/modified. */
public final class SchemaDiffTab extends Tab {

    private final AppContext context;
    private final SchemaDiffService diffService = new SchemaDiffService();
    private final SchemaMetadataService metadata = new SchemaMetadataService();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final TextArea diffArea = new TextArea();

    private final ComboBox<ConnectionHandle> connA = new ComboBox<>();
    private final ComboBox<String> schemaA = new ComboBox<>();
    private final ComboBox<ConnectionHandle> connB = new ComboBox<>();
    private final ComboBox<String> schemaB = new ComboBox<>();
    private final Button compareButton = new Button("Compare");
    private final Label statusLabel = new Label();

    public SchemaDiffTab(AppContext context) {
        super("Schema Diff");
        this.context = context;

        var converter = new javafx.util.StringConverter<ConnectionHandle>() {
            @Override public String toString(ConnectionHandle h) { return h == null ? "" : h.profile().name(); }
            @Override public ConnectionHandle fromString(String s) { return null; }
        };
        connA.setConverter(converter);
        connB.setConverter(converter);
        connA.getItems().setAll(context.connectionManager().activeConnections());
        connB.getItems().setAll(context.connectionManager().activeConnections());
        connA.valueProperty().addListener((obs, was, h) -> loadSchemas(h, schemaA));
        connB.valueProperty().addListener((obs, was, h) -> loadSchemas(h, schemaB));

        HBox controls = new HBox(8,
                new Label("A:"), connA, schemaA,
                new Label("B:"), connB, schemaB,
                compareButton);
        controls.setPadding(new Insets(8));

        TableView<Row> table = buildTable();
        diffArea.setEditable(false);
        diffArea.setStyle("-fx-font-family: 'Monospace'; -fx-font-size: 12px;");

        SplitPane split = new SplitPane(table, diffArea);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.55);

        VBox layout = new VBox(controls, split, statusLabel);
        VBox.setVgrow(split, Priority.ALWAYS);
        statusLabel.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.setStyle("-fx-text-fill: -color-fg-muted;");
        setContent(layout);

        compareButton.setOnAction(e -> runDiff());
        table.getSelectionModel().selectedItemProperty().addListener((obs, was, sel) -> {
            if (sel != null) diffArea.setText(sel.getDiffText());
        });
    }

    private TableView<Row> buildTable() {
        var table = new TableView<>(rows);
        TableColumn<Row, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        status.setPrefWidth(100);
        TableColumn<Row, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        type.setPrefWidth(140);
        TableColumn<Row, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        name.setPrefWidth(280);
        table.getColumns().setAll(status, type, name);
        return table;
    }

    private void loadSchemas(ConnectionHandle handle, ComboBox<String> target) {
        target.getItems().clear();
        if (handle == null) return;
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                try (Connection c = handle.borrow()) {
                    return metadata.listSchemas(c);
                }
            }
        };
        task.setOnSucceeded(ev -> target.getItems().setAll(task.getValue()));
        Thread t = new Thread(task, "diff-schemas");
        t.setDaemon(true);
        t.start();
    }

    private void runDiff() {
        ConnectionHandle a = connA.getValue();
        ConnectionHandle b = connB.getValue();
        String sA = schemaA.getValue();
        String sB = schemaB.getValue();
        if (a == null || b == null || sA == null || sB == null) return;
        statusLabel.setText("Comparing…");
        rows.clear();
        diffArea.clear();

        Task<DiffResult> task = new Task<>() {
            @Override
            protected DiffResult call() throws Exception {
                try (Connection cA = a.borrow(); Connection cB = b.borrow()) {
                    return diffService.diff(cA, a.profile().name(), sA,
                            cB, b.profile().name(), sB,
                            ProgressSink.noop());
                }
            }
        };
        task.setOnSucceeded(ev -> populate(task.getValue()));
        task.setOnFailed(ev -> {
            statusLabel.setText("Failed.");
            new Alert(Alert.AlertType.ERROR,
                    "Schema diff failed:\n" + task.getException().getMessage()).showAndWait();
        });
        Thread t = new Thread(task, "schema-diff");
        t.setDaemon(true);
        t.start();
    }

    private void populate(DiffResult result) {
        for (var o : result.added()) {
            rows.add(new Row("Added", o.type(), o.name(),
                    "Object only in " + result.labelB() + ":\n" + o.name()));
        }
        for (var o : result.removed()) {
            rows.add(new Row("Removed", o.type(), o.name(),
                    "Object only in " + result.labelA() + ":\n" + o.name()));
        }
        for (var m : result.modified()) {
            String unified = unifiedDiff(m.objectA().name(), m.ddlA(), m.ddlB(),
                    result.labelA(), result.labelB());
            rows.add(new Row("Modified", m.objectA().type(), m.objectA().name(), unified));
        }
        statusLabel.setText(result.totalChanges() + " change(s) — "
                + result.added().size() + " added, "
                + result.removed().size() + " removed, "
                + result.modified().size() + " modified");
    }

    private static String unifiedDiff(String name, String a, String b, String labelA, String labelB) {
        List<String> aLines = Arrays.asList(a.split("\\r?\\n", -1));
        List<String> bLines = Arrays.asList(b.split("\\r?\\n", -1));
        var patch = DiffUtils.diff(aLines, bLines);
        List<String> diff = UnifiedDiffUtils.generateUnifiedDiff(
                labelA + "/" + name, labelB + "/" + name, aLines, patch, 3);
        return String.join("\n", diff);
    }

    public static final class Row {
        private final SimpleStringProperty status;
        private final SimpleStringProperty type;
        private final SimpleStringProperty name;
        private final String diffText;

        public Row(String status, String type, String name, String diffText) {
            this.status = new SimpleStringProperty(status);
            this.type = new SimpleStringProperty(type);
            this.name = new SimpleStringProperty(name);
            this.diffText = diffText;
        }

        public String getStatus() { return status.get(); }
        public String getType() { return type.get(); }
        public String getName() { return name.get(); }
        public String getDiffText() { return diffText; }
    }
}
