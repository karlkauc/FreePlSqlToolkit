package org.fxt.freeplsql.app.ui.dependency;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.appsvc.dependency.DependencyEdge;
import org.fxt.freeplsql.appsvc.dependency.DependencyService;
import org.fxt.freeplsql.sync.DbObject;

import java.sql.Connection;
import java.util.List;

/** Shows which objects this object calls and which objects call this one. */
public final class DependenciesTab extends Tab {

    private final DependencyService service = new DependencyService();
    private final ConnectionHandle handle;
    private final DbObject object;

    private final ObservableList<Row> calls = FXCollections.observableArrayList();
    private final ObservableList<Row> calledBy = FXCollections.observableArrayList();

    public DependenciesTab(ConnectionHandle handle, DbObject object) {
        super("Deps · " + object.schema() + "." + object.name());
        this.handle = handle;
        this.object = object;

        Label header = new Label("Dependencies for "
                + handle.profile().name() + " · " + object.schema() + "." + object.name()
                + " (" + object.type() + ")");
        header.setStyle("-fx-padding: 8 12 4 12; -fx-font-weight: bold;");

        TableView<Row> callsTable = buildTable("Calls", calls);
        TableView<Row> calledByTable = buildTable("Called by", calledBy);
        VBox callsBox = wrap("Calls (this → other)", callsTable);
        VBox calledByBox = wrap("Called by (other → this)", calledByTable);

        SplitPane split = new SplitPane(callsBox, calledByBox);
        split.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        split.setDividerPositions(0.5);

        VBox layout = new VBox(header, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        setContent(layout);

        load();
    }

    public static String tabKey(ConnectionHandle h, DbObject o) {
        return "deps:" + h.profile().id() + "/" + o.key();
    }

    private VBox wrap(String title, TableView<Row> table) {
        Label l = new Label(title);
        l.setStyle("-fx-padding: 6 8; -fx-font-weight: 600; -fx-text-fill: -color-fg-muted;");
        VBox box = new VBox(l, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private TableView<Row> buildTable(String label, ObservableList<Row> data) {
        var table = new TableView<>(data);
        TableColumn<Row, String> owner = new TableColumn<>("Owner");
        owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        owner.setPrefWidth(110);
        TableColumn<Row, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        name.setPrefWidth(200);
        TableColumn<Row, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        type.setPrefWidth(140);
        table.getColumns().setAll(owner, name, type);
        return table;
    }

    private void load() {
        Task<List<Row>[]> task = new Task<>() {
            @SuppressWarnings("unchecked")
            @Override
            protected List<Row>[] call() throws Exception {
                try (Connection c = handle.borrow()) {
                    var to = service.referencesOf(c, object).stream()
                            .map(Row::toTarget)
                            .toList();
                    var from = service.referencedBy(c, object).stream()
                            .map(Row::toSource)
                            .toList();
                    return new List[]{ to, from };
                }
            }
        };
        task.setOnSucceeded(ev -> {
            calls.setAll(task.getValue()[0]);
            calledBy.setAll(task.getValue()[1]);
        });
        task.setOnFailed(ev -> new Alert(Alert.AlertType.ERROR,
                "Could not load dependencies:\n" + task.getException().getMessage()).showAndWait());
        Thread t = new Thread(task, "deps-" + object.key());
        t.setDaemon(true);
        t.start();
    }

    public static final class Row {
        private final SimpleStringProperty owner;
        private final SimpleStringProperty name;
        private final SimpleStringProperty type;

        public Row(String owner, String name, String type) {
            this.owner = new SimpleStringProperty(owner);
            this.name = new SimpleStringProperty(name);
            this.type = new SimpleStringProperty(type);
        }

        static Row toTarget(DependencyEdge e) {
            return new Row(e.targetOwner(), e.targetName(), e.targetType());
        }

        static Row toSource(DependencyEdge e) {
            return new Row(e.sourceOwner(), e.sourceName(), e.sourceType());
        }

        public String getOwner() { return owner.get(); }
        public String getName() { return name.get(); }
        public String getType() { return type.get(); }
    }
}
