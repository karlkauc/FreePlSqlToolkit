package org.fxt.freeplsql.app.ui.search;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxt.freeplsql.app.AppContext;
import org.fxt.freeplsql.app.ui.editor.DbObjectEditorTab;
import org.fxt.freeplsql.app.ui.tabs.TabManager;
import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.appsvc.lint.ProgressSink;
import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;
import org.fxt.freeplsql.appsvc.search.DbSourceSearchService;
import org.fxt.freeplsql.appsvc.search.SearchHit;
import org.fxt.freeplsql.appsvc.search.SearchQuery;
import org.fxt.freeplsql.sync.DbObject;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;

public final class DbSearchTab extends Tab {

    private final AppContext context;
    private final TabManager tabManager;
    private final DbSourceSearchService searchService = new DbSourceSearchService();
    private final SchemaMetadataService metadata = new SchemaMetadataService();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final TextField patternField = new TextField();
    private final CheckBox regexBox = new CheckBox("Regex");
    private final CheckBox caseBox = new CheckBox("Case-sensitive");
    private final Button searchButton = new Button("Search");
    private final Label statusLabel = new Label("Ready.");

    public DbSearchTab(AppContext context, TabManager tabManager) {
        super("DB Search");
        this.context = context;
        this.tabManager = tabManager;

        patternField.setPromptText("Pattern (literal substring or regex)");
        HBox controls = new HBox(8, new Label("Pattern:"), patternField,
                regexBox, caseBox, searchButton);
        HBox.setHgrow(patternField, Priority.ALWAYS);
        controls.setPadding(new Insets(8));

        TableView<Row> table = buildTable();
        VBox layout = new VBox(controls, table, statusLabel);
        VBox.setVgrow(table, Priority.ALWAYS);
        statusLabel.setPadding(new Insets(4, 8, 4, 8));
        statusLabel.setStyle("-fx-text-fill: -fxt-fg-muted;");
        setContent(layout);

        searchButton.setOnAction(e -> runSearch());
        patternField.setOnAction(e -> runSearch());
    }

    private TableView<Row> buildTable() {
        var table = new TableView<>(rows);
        TableColumn<Row, String> conn = new TableColumn<>("Connection");
        conn.setCellValueFactory(new PropertyValueFactory<>("connection"));
        conn.setPrefWidth(140);
        TableColumn<Row, String> owner = new TableColumn<>("Owner");
        owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        owner.setPrefWidth(110);
        TableColumn<Row, String> name = new TableColumn<>("Object");
        name.setCellValueFactory(new PropertyValueFactory<>("object"));
        name.setPrefWidth(180);
        TableColumn<Row, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        type.setPrefWidth(110);
        type.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                org.kordamp.ikonli.feather.Feather glyph = switch (item.toUpperCase()) {
                    case "PACKAGE", "PACKAGE BODY" -> org.kordamp.ikonli.feather.Feather.PACKAGE;
                    case "FUNCTION", "PROCEDURE"   -> org.kordamp.ikonli.feather.Feather.PLAY;
                    case "VIEW"                    -> org.kordamp.ikonli.feather.Feather.EYE;
                    case "TABLE"                   -> org.kordamp.ikonli.feather.Feather.GRID;
                    case "TRIGGER"                 -> org.kordamp.ikonli.feather.Feather.ZAP;
                    case "TYPE", "TYPE BODY"       -> org.kordamp.ikonli.feather.Feather.BOX;
                    default                        -> org.kordamp.ikonli.feather.Feather.FILE;
                };
                org.kordamp.ikonli.javafx.FontIcon fi =
                        new org.kordamp.ikonli.javafx.FontIcon(glyph);
                fi.setIconSize(14);
                setGraphic(fi);
                setGraphicTextGap(8);
            }
        });
        TableColumn<Row, Number> line = new TableColumn<>("Line");
        line.setCellValueFactory(new PropertyValueFactory<>("line"));
        line.setPrefWidth(60);
        TableColumn<Row, String> snippet = new TableColumn<>("Source line");
        snippet.setCellValueFactory(new PropertyValueFactory<>("snippet"));
        snippet.setPrefWidth(640);
        table.getColumns().setAll(conn, owner, name, type, line, snippet);
        table.setPlaceholder(
                org.fxt.freeplsql.app.ui.shell.EmptyState.builder()
                        .featherIcon(org.kordamp.ikonli.feather.Feather.SEARCH)
                        .title("Type to search")
                        .body("Search runs across SOURCE for every connected schema. Wildcards: % and _.")
                        .build());

        table.setRowFactory(tv -> {
            var r = new javafx.scene.control.TableRow<Row>();
            r.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !r.isEmpty()) {
                    openHit(r.getItem());
                }
            });
            return r;
        });
        return table;
    }

    private void runSearch() {
        String pattern = patternField.getText() == null ? "" : patternField.getText().trim();
        if (pattern.isEmpty()) return;

        List<ConnectionHandle> handles = List.copyOf(context.connectionManager().activeConnections());
        if (handles.isEmpty()) {
            statusLabel.setText("No active connections.");
            return;
        }

        searchButton.setDisable(true);
        statusLabel.setText("Searching…");
        rows.clear();

        SearchQuery query = new SearchQuery(pattern, regexBox.isSelected(),
                caseBox.isSelected(), List.of());

        Task<List<SearchHit>> task = new Task<>() {
            @Override
            protected List<SearchHit> call() {
                return searchService.search(handles, query, ProgressSink.noop());
            }
        };
        task.setOnSucceeded(ev -> {
            for (SearchHit h : task.getValue()) {
                rows.add(Row.from(h));
            }
            statusLabel.setText(rows.size() + " hit(s) at " + Instant.now());
            searchButton.setDisable(false);
        });
        task.setOnFailed(ev -> {
            searchButton.setDisable(false);
            statusLabel.setText("Search failed.");
            new Alert(Alert.AlertType.ERROR,
                    "Search failed:\n" + task.getException().getMessage()).showAndWait();
        });
        Thread t = new Thread(task, "db-search");
        t.setDaemon(true);
        t.start();
    }

    private void openHit(Row r) {
        ConnectionHandle handle = context.connectionManager().get(r.profileId).orElse(null);
        if (handle == null) return;
        Task<DbObject> resolve = new Task<>() {
            @Override
            protected DbObject call() throws Exception {
                try (Connection c = handle.borrow()) {
                    return metadata.listObjectsByType(c, r.getOwner(), r.getType()).stream()
                            .filter(o -> o.name().equalsIgnoreCase(r.getObject()))
                            .findFirst()
                            .orElse(null);
                }
            }
        };
        resolve.setOnSucceeded(ev -> {
            DbObject obj = resolve.getValue();
            if (obj == null) return;
            String key = DbObjectEditorTab.tabKey(handle.profile().id(), obj);
            tabManager.openOrFocus(key, () -> {
                DbObjectEditorTab tab = new DbObjectEditorTab(handle, obj,
                        "-- Loading DDL …");
                Task<String> ddlTask = new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        try (Connection c = handle.borrow()) {
                            return metadata.getDdl(c, obj);
                        }
                    }
                };
                ddlTask.setOnSucceeded(ev2 -> tab.replaceContent(ddlTask.getValue()));
                ddlTask.setOnFailed(ev2 -> tab.replaceContent(
                        "-- Failed to load DDL: " + ddlTask.getException().getMessage()));
                Thread t2 = new Thread(ddlTask, "ddl-" + obj.key());
                t2.setDaemon(true);
                t2.start();
                return tab;
            });
        });
        Thread t = new Thread(resolve, "resolve-hit");
        t.setDaemon(true);
        t.start();
    }

    public static final class Row {
        private final SimpleStringProperty connection;
        private final String profileId;
        private final SimpleStringProperty owner;
        private final SimpleStringProperty object;
        private final SimpleStringProperty type;
        private final javafx.beans.property.SimpleIntegerProperty line;
        private final SimpleStringProperty snippet;

        public Row(String connection, String profileId, String owner, String object,
                   String type, int line, String snippet) {
            this.connection = new SimpleStringProperty(connection);
            this.profileId = profileId;
            this.owner = new SimpleStringProperty(owner);
            this.object = new SimpleStringProperty(object);
            this.type = new SimpleStringProperty(type);
            this.line = new javafx.beans.property.SimpleIntegerProperty(line);
            this.snippet = new SimpleStringProperty(snippet);
        }

        public static Row from(SearchHit hit) {
            return new Row(hit.connectionName(), hit.profileId(), hit.owner(),
                    hit.objectName(), hit.objectType(), hit.line(), hit.snippet());
        }

        public String getConnection() { return connection.get(); }
        public String getOwner() { return owner.get(); }
        public String getObject() { return object.get(); }
        public String getType() { return type.get(); }
        public int getLine() { return line.get(); }
        public String getSnippet() { return snippet.get(); }
    }
}
