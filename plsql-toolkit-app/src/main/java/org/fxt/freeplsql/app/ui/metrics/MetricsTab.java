package org.fxt.freeplsql.app.ui.metrics;

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
import org.fxt.freeplsql.appsvc.lint.ProgressSink;
import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;
import org.fxt.freeplsql.appsvc.metrics.MetricsService;
import org.fxt.freeplsql.appsvc.metrics.ObjectMetrics;
import org.fxt.freeplsql.lint.DefaultRules;
import org.fxt.freeplsql.lint.LintEngine;

import java.sql.Connection;
import java.util.List;

public final class MetricsTab extends Tab {

    private final AppContext context;
    private final SchemaMetadataService metadata = new SchemaMetadataService();
    private final MetricsService metricsService = new MetricsService(new LintEngine(DefaultRules.all()));
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final ComboBox<ConnectionHandle> connectionCombo = new ComboBox<>();
    private final ComboBox<String> schemaCombo = new ComboBox<>();
    private final Button refreshButton = new Button("Refresh");
    private final Label statusLabel = new Label("Pick a connection and schema.");
    private final org.fxt.freeplsql.app.ui.shell.KpiCard kpiObjects =
            new org.fxt.freeplsql.app.ui.shell.KpiCard("OBJECTS", "—");
    private final org.fxt.freeplsql.app.ui.shell.KpiCard kpiLoc =
            new org.fxt.freeplsql.app.ui.shell.KpiCard("TOTAL LOC", "—");
    private final org.fxt.freeplsql.app.ui.shell.KpiCard kpiAvgCcn =
            new org.fxt.freeplsql.app.ui.shell.KpiCard("AVG CCN", "—");
    private final org.fxt.freeplsql.app.ui.shell.KpiCard kpiMaxCcn =
            new org.fxt.freeplsql.app.ui.shell.KpiCard("MAX CCN", "—");

    public MetricsTab(AppContext context) {
        super("Metrics");
        this.context = context;

        connectionCombo.getItems().setAll(context.connectionManager().activeConnections());
        connectionCombo.setConverter(handleConverter());
        connectionCombo.valueProperty().addListener((obs, was, h) -> loadSchemas(h));

        HBox controls = new HBox(8,
                new Label("Connection:"), connectionCombo,
                new Label("Schema:"), schemaCombo,
                refreshButton);
        controls.setPadding(new Insets(8));

        TableView<Row> table = buildTable();
        javafx.scene.layout.HBox kpis = new javafx.scene.layout.HBox(12,
                kpiObjects, kpiLoc, kpiAvgCcn, kpiMaxCcn);
        kpis.setPadding(new Insets(0, 8, 8, 8));
        VBox layout = new VBox(controls, kpis, table, statusLabel);
        VBox.setVgrow(table, Priority.ALWAYS);
        statusLabel.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.setStyle("-fx-text-fill: -fxt-fg-muted;");
        setContent(layout);

        refreshButton.setOnAction(e -> compute());
    }

    private TableView<Row> buildTable() {
        var table = new TableView<>(rows);
        TableColumn<Row, String> obj = new TableColumn<>("Object");
        obj.setCellValueFactory(new PropertyValueFactory<>("object"));
        obj.setPrefWidth(220);
        TableColumn<Row, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(140);
        TableColumn<Row, Number> loc = new TableColumn<>("LOC");
        loc.setCellValueFactory(new PropertyValueFactory<>("loc"));
        loc.setPrefWidth(80);
        TableColumn<Row, Number> sloc = new TableColumn<>("SLOC");
        sloc.setCellValueFactory(new PropertyValueFactory<>("sloc"));
        sloc.setPrefWidth(80);
        TableColumn<Row, Number> ccn = new TableColumn<>("CCN");
        ccn.setCellValueFactory(new PropertyValueFactory<>("ccn"));
        ccn.setPrefWidth(80);
        TableColumn<Row, Number> issues = new TableColumn<>("Issues");
        issues.setCellValueFactory(new PropertyValueFactory<>("issues"));
        issues.setPrefWidth(80);
        table.getColumns().setAll(obj, typeCol, loc, sloc, ccn, issues);
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
        Thread t = new Thread(task, "metrics-schemas");
        t.setDaemon(true);
        t.start();
    }

    private void compute() {
        ConnectionHandle handle = connectionCombo.getValue();
        String schema = schemaCombo.getValue();
        if (handle == null || schema == null) return;
        statusLabel.setText("Computing…");
        rows.clear();
        kpiObjects.setValue("—");
        kpiLoc.setValue("—");
        kpiAvgCcn.setValue("—");
        kpiMaxCcn.setValue("—");
        Task<List<ObjectMetrics>> task = new Task<>() {
            @Override
            protected List<ObjectMetrics> call() throws Exception {
                try (Connection c = handle.borrow()) {
                    var objects = metadata.listAllObjects(c, schema);
                    return metricsService.measure(c, schema, objects, ProgressSink.noop());
                }
            }
        };
        task.setOnSucceeded(ev -> {
            for (ObjectMetrics m : task.getValue()) {
                rows.add(Row.from(m));
            }
            long total = task.getValue().size();
            long loc   = task.getValue().stream().mapToLong(ObjectMetrics::loc).sum();
            double avgCcn = task.getValue().stream().mapToInt(ObjectMetrics::ccn).average().orElse(0);
            int maxCcn = task.getValue().stream().mapToInt(ObjectMetrics::ccn).max().orElse(0);
            statusLabel.setText(rows.size() + " object(s)");
            kpiObjects.setValue(Long.toString(total));
            kpiLoc.setValue(Long.toString(loc));
            kpiAvgCcn.setValue(String.format("%.1f", avgCcn));
            kpiMaxCcn.setValue(Integer.toString(maxCcn));
        });
        task.setOnFailed(ev -> {
            statusLabel.setText("Failed.");
            new Alert(Alert.AlertType.ERROR,
                    "Could not compute metrics:\n" + task.getException().getMessage()).showAndWait();
        });
        Thread t = new Thread(task, "metrics-compute");
        t.setDaemon(true);
        t.start();
    }

    private static javafx.util.StringConverter<ConnectionHandle> handleConverter() {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(ConnectionHandle h) { return h == null ? "" : h.profile().name(); }
            @Override public ConnectionHandle fromString(String s) { return null; }
        };
    }

    public static final class Row {
        private final SimpleStringProperty object;
        private final SimpleStringProperty type;
        private final javafx.beans.property.SimpleIntegerProperty loc;
        private final javafx.beans.property.SimpleIntegerProperty sloc;
        private final javafx.beans.property.SimpleIntegerProperty ccn;
        private final javafx.beans.property.SimpleIntegerProperty issues;

        public Row(String object, String type, int loc, int sloc, int ccn, int issues) {
            this.object = new SimpleStringProperty(object);
            this.type = new SimpleStringProperty(type);
            this.loc = new javafx.beans.property.SimpleIntegerProperty(loc);
            this.sloc = new javafx.beans.property.SimpleIntegerProperty(sloc);
            this.ccn = new javafx.beans.property.SimpleIntegerProperty(ccn);
            this.issues = new javafx.beans.property.SimpleIntegerProperty(issues);
        }

        public static Row from(ObjectMetrics m) {
            return new Row(m.dbObject().name(), m.dbObject().type(),
                    m.loc(), m.sloc(), m.ccn(), m.issueCount());
        }

        public String getObject() { return object.get(); }
        public String getType() { return type.get(); }
        public int getLoc() { return loc.get(); }
        public int getSloc() { return sloc.get(); }
        public int getCcn() { return ccn.get(); }
        public int getIssues() { return issues.get(); }
    }
}
