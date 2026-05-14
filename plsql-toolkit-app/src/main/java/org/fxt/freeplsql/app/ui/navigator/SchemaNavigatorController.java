package org.fxt.freeplsql.app.ui.navigator;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import org.fxt.freeplsql.app.AppContext;
import org.fxt.freeplsql.app.ui.dependency.DependenciesTab;
import org.fxt.freeplsql.app.ui.editor.DbObjectEditorTab;
import org.fxt.freeplsql.app.ui.lint.LintReportTab;
import org.fxt.freeplsql.app.ui.tabs.TabManager;
import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.appsvc.lint.BatchLintService;
import org.fxt.freeplsql.appsvc.lint.LintReport;
import org.fxt.freeplsql.appsvc.lint.ProgressSink;
import org.fxt.freeplsql.appsvc.metadata.SchemaMetadataService;
import org.fxt.freeplsql.lint.DefaultRules;
import org.fxt.freeplsql.lint.LintEngine;
import org.fxt.freeplsql.sync.DbObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class SchemaNavigatorController {

    private static final Map<String, String> TYPE_LABELS = new LinkedHashMap<>();
    static {
        TYPE_LABELS.put("PACKAGE", "Packages");
        TYPE_LABELS.put("PACKAGE BODY", "Package Bodies");
        TYPE_LABELS.put("PROCEDURE", "Procedures");
        TYPE_LABELS.put("FUNCTION", "Functions");
        TYPE_LABELS.put("TRIGGER", "Triggers");
        TYPE_LABELS.put("VIEW", "Views");
        TYPE_LABELS.put("TYPE", "Types");
        TYPE_LABELS.put("TYPE BODY", "Type Bodies");
    }

    @FXML private TitledPane root;
    @FXML private TreeView<NavNode> tree;
    @FXML private javafx.scene.layout.VBox emptyState;

    private final SchemaMetadataService metadata = new SchemaMetadataService();
    private final BatchLintService batchLint = new BatchLintService(new LintEngine(DefaultRules.all()));

    private AppContext context;
    private TabManager tabManager;

    public static TitledPane create(AppContext context, TabManager tabManager) {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                SchemaNavigatorController.class.getResource("/fxml/SchemaNavigator.fxml"),
                "SchemaNavigator.fxml not found"));
        TitledPane pane;
        try {
            pane = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load SchemaNavigator.fxml", e);
        }
        SchemaNavigatorController c = loader.getController();
        c.context = context;
        c.tabManager = tabManager;
        c.attachListeners();
        return pane;
    }

    @FXML
    private void initialize() {
        tree.setRoot(new TreeItem<>());
        tree.setCellFactory(tv -> new NavCell(this::lintSchema, this::showDependencies));
        tree.setOnMouseClicked(this::onTreeClick);
        org.fxt.freeplsql.app.ui.shell.EmptyState empty =
                org.fxt.freeplsql.app.ui.shell.EmptyState.builder()
                        .featherIcon(org.kordamp.ikonli.feather.Feather.FOLDER)
                        .title("No schema loaded")
                        .body("Connect to a database to browse its packages, views, tables and triggers.")
                        .build();
        emptyState.getChildren().setAll(empty);
        Runnable updateEmptyState = () -> {
            boolean isEmpty = tree.getRoot() == null
                    || tree.getRoot().getChildren().isEmpty();
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);
            tree.setVisible(!isEmpty);
            tree.setManaged(!isEmpty);
        };
        updateEmptyState.run();
        tree.getRoot().getChildren().addListener(
                (javafx.collections.ListChangeListener<? super javafx.scene.control.TreeItem<NavNode>>)
                c -> updateEmptyState.run());
    }

    private void showDependencies(NavNode.Obj objNode) {
        ConnectionHandle handle = context.connectionManager().get(objNode.profileId()).orElse(null);
        if (handle == null) return;
        String key = DependenciesTab.tabKey(handle, objNode.dbObject());
        tabManager.openOrFocus(key, () -> new DependenciesTab(handle, objNode.dbObject()));
    }

    private void attachListeners() {
        var mgr = context.connectionManager();
        mgr.addConnectListener(handle ->
                Platform.runLater(() -> addConnection(handle)));
        mgr.addDisconnectListener(profileId ->
                Platform.runLater(() -> removeConnection(profileId)));
        for (ConnectionHandle h : mgr.activeConnections()) {
            addConnection(h);
        }
    }

    private void addConnection(ConnectionHandle handle) {
        var connItem = new TreeItem<NavNode>(
                new NavNode.Conn(handle.profile().id(), handle.profile().name()));
        connItem.getChildren().add(loadingItem("Loading schemas…"));
        attachLazyLoader(connItem, () -> loadSchemas(connItem, handle));
        tree.getRoot().getChildren().add(connItem);
    }

    private void removeConnection(String profileId) {
        tree.getRoot().getChildren().removeIf(i ->
                i.getValue() instanceof NavNode.Conn c && c.profileId().equals(profileId));
        if (tabManager != null) {
            tabManager.closeAllForProfile(profileId);
        }
    }

    private void loadSchemas(TreeItem<NavNode> connItem, ConnectionHandle handle) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                try (Connection c = handle.borrow()) {
                    return metadata.listSchemas(c);
                }
            }
        };
        task.setOnSucceeded(ev -> {
            connItem.getChildren().clear();
            for (String schema : task.getValue()) {
                var schemaItem = new TreeItem<NavNode>(
                        new NavNode.Schema(handle.profile().id(), schema));
                for (var entry : TYPE_LABELS.entrySet()) {
                    var typeNode = new NavNode.Type(handle.profile().id(),
                            schema, entry.getKey(), entry.getValue());
                    var typeItem = new TreeItem<NavNode>(typeNode);
                    typeItem.getChildren().add(loadingItem("Loading…"));
                    attachLazyLoader(typeItem,
                            () -> loadObjects(typeItem, handle, schema, entry.getKey()));
                    schemaItem.getChildren().add(typeItem);
                }
                connItem.getChildren().add(schemaItem);
            }
        });
        task.setOnFailed(ev -> {
            connItem.getChildren().clear();
            connItem.getChildren().add(new TreeItem<>(
                    new NavNode.Loading("Failed: " + task.getException().getMessage())));
        });
        runDaemon(task, "schemas-" + handle.profile().id());
    }

    private void loadObjects(TreeItem<NavNode> typeItem,
                             ConnectionHandle handle, String schema, String type) {
        Task<List<DbObject>> task = new Task<>() {
            @Override
            protected List<DbObject> call() throws Exception {
                try (Connection c = handle.borrow()) {
                    return metadata.listObjectsByType(c, schema, type);
                }
            }
        };
        task.setOnSucceeded(ev -> {
            typeItem.getChildren().clear();
            for (DbObject obj : task.getValue()) {
                typeItem.getChildren().add(new TreeItem<>(
                        new NavNode.Obj(handle.profile().id(), obj)));
            }
            if (typeItem.getChildren().isEmpty()) {
                typeItem.getChildren().add(new TreeItem<>(new NavNode.Loading("(none)")));
            }
        });
        task.setOnFailed(ev -> {
            typeItem.getChildren().clear();
            typeItem.getChildren().add(new TreeItem<>(
                    new NavNode.Loading("Failed: " + task.getException().getMessage())));
        });
        runDaemon(task, "objects-" + schema + "-" + type);
    }

    private void onTreeClick(MouseEvent ev) {
        if (ev.getClickCount() != 2) return;
        TreeItem<NavNode> item = tree.getSelectionModel().getSelectedItem();
        if (item == null || !(item.getValue() instanceof NavNode.Obj on)) return;
        ConnectionHandle handle = context.connectionManager().get(on.profileId()).orElse(null);
        if (handle == null) return;
        openObjectTab(handle, on.dbObject());
    }

    private void openObjectTab(ConnectionHandle handle, DbObject obj) {
        String key = DbObjectEditorTab.tabKey(handle.profile().id(), obj);
        tabManager.openOrFocus(key, () -> {
            DbObjectEditorTab tab = new DbObjectEditorTab(handle, obj,
                    "-- Loading DDL for " + obj.schema() + "." + obj.name() + " …");
            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    try (Connection c = handle.borrow()) {
                        return metadata.getDdl(c, obj);
                    }
                }
            };
            task.setOnSucceeded(ev -> tab.replaceContent(task.getValue()));
            task.setOnFailed(ev -> tab.replaceContent(
                    "-- Failed to load DDL: " + task.getException().getMessage()));
            runDaemon(task, "ddl-" + obj.key());
            return tab;
        });
    }

    private void lintSchema(NavNode.Schema schemaNode) {
        ConnectionHandle handle = context.connectionManager().get(schemaNode.profileId()).orElse(null);
        if (handle == null) return;

        Task<LintReport> task = new Task<>() {
            @Override
            protected LintReport call() throws Exception {
                Task<LintReport> selfTask = this;
                try (Connection c = handle.borrow()) {
                    List<DbObject> objects = metadata.listAllObjects(c, schemaNode.schemaName());
                    if (objects.isEmpty()) {
                        return new LintReport(handle.profile().name(),
                                schemaNode.schemaName(), Instant.now(), Map.of());
                    }
                    ProgressSink progress = new ProgressSink() {
                        @Override
                        public void update(int current, int total, String message) {
                            updateProgress(current, total);
                            updateMessage(current + "/" + total + " · " + message);
                        }
                        @Override
                        public boolean isCancelled() {
                            return selfTask.isCancelled();
                        }
                    };
                    return batchLint.lint(c, handle.profile().name(),
                            schemaNode.schemaName(), objects, progress);
                }
            }
        };
        task.setOnSucceeded(ev -> openReportTab(task.getValue()));
        task.setOnFailed(ev -> {
            Throwable err = task.getException();
            new Alert(Alert.AlertType.ERROR,
                    "Lint failed:\n" + (err != null ? err.getMessage() : "unknown")).showAndWait();
        });
        runDaemon(task, "lint-" + schemaNode.schemaName());
    }

    private void openReportTab(LintReport report) {
        String key = "lint:" + report.connectionName() + ":" + report.schemaName()
                + ":" + System.currentTimeMillis();
        tabManager.openOrFocus(key, () -> new LintReportTab(report));
    }

    private static TreeItem<NavNode> loadingItem(String text) {
        return new TreeItem<>(new NavNode.Loading(text));
    }

    private static void attachLazyLoader(TreeItem<NavNode> item, Runnable loader) {
        ChangeListener<Boolean> listener = new ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs,
                                Boolean was, Boolean now) {
                if (Boolean.TRUE.equals(now) && hasLoadingPlaceholder(item)) {
                    item.expandedProperty().removeListener(this);
                    loader.run();
                }
            }
        };
        item.expandedProperty().addListener(listener);
    }

    private static boolean hasLoadingPlaceholder(TreeItem<NavNode> item) {
        return item.getChildren().size() == 1
                && item.getChildren().get(0).getValue() instanceof NavNode.Loading;
    }

    private static void runDaemon(Task<?> task, String name) {
        Thread t = new Thread(task, name);
        t.setDaemon(true);
        t.start();
    }

    private static final class NavCell extends TreeCell<NavNode> {

        private final ContextMenu schemaMenu;
        private final ContextMenu objectMenu;

        NavCell(Consumer<NavNode.Schema> onLintSchema, Consumer<NavNode.Obj> onDependencies) {
            this.schemaMenu = new ContextMenu();
            MenuItem lint = new MenuItem("Lint Schema…");
            lint.setOnAction(e -> {
                if (getItem() instanceof NavNode.Schema s) onLintSchema.accept(s);
            });
            schemaMenu.getItems().add(lint);

            this.objectMenu = new ContextMenu();
            MenuItem deps = new MenuItem("Show Dependencies");
            deps.setOnAction(e -> {
                if (getItem() instanceof NavNode.Obj o) onDependencies.accept(o);
            });
            objectMenu.getItems().add(deps);
        }

        @Override
        protected void updateItem(NavNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
                setContextMenu(null);
                return;
            }
            setText(item.label());
            setGraphic(iconFor(item));
            switch (item) {
                case NavNode.Schema s -> setContextMenu(schemaMenu);
                case NavNode.Obj o -> setContextMenu(objectMenu);
                default -> setContextMenu(null);
            }
            switch (item) {
                case NavNode.Conn ignored -> setStyle("-fx-font-weight: bold;");
                case NavNode.Schema ignored -> setStyle("-fx-font-weight: bold; -fx-text-fill: -fxt-primary;");
                case NavNode.Type ignored -> setStyle("-fx-text-fill: -fxt-fg-muted;");
                case NavNode.Loading ignored -> setStyle("-fx-text-fill: -fxt-fg-muted; -fx-font-style: italic;");
                case NavNode.Obj ignored -> setStyle("");
            }
        }

        private static org.kordamp.ikonli.javafx.FontIcon iconFor(NavNode n) {
            org.kordamp.ikonli.feather.Feather glyph = switch (n) {
                case NavNode.Conn ignored -> org.kordamp.ikonli.feather.Feather.DATABASE;
                case NavNode.Schema ignored -> org.kordamp.ikonli.feather.Feather.FOLDER;
                case NavNode.Type t -> glyphForType(t.objectType());
                case NavNode.Obj o -> glyphForType(o.dbObject().type());
                case NavNode.Loading ignored -> org.kordamp.ikonli.feather.Feather.LOADER;
            };
            org.kordamp.ikonli.javafx.FontIcon fi = new org.kordamp.ikonli.javafx.FontIcon(glyph);
            fi.setIconSize(14);
            return fi;
        }

        private static org.kordamp.ikonli.feather.Feather glyphForType(String typeName) {
            String t = typeName == null ? "" : typeName.toUpperCase();
            return switch (t) {
                case "PACKAGE", "PACKAGE BODY" -> org.kordamp.ikonli.feather.Feather.PACKAGE;
                case "FUNCTION", "PROCEDURE"   -> org.kordamp.ikonli.feather.Feather.PLAY;
                case "VIEW"                    -> org.kordamp.ikonli.feather.Feather.EYE;
                case "TABLE"                   -> org.kordamp.ikonli.feather.Feather.GRID;
                case "TRIGGER"                 -> org.kordamp.ikonli.feather.Feather.ZAP;
                case "TYPE", "TYPE BODY"       -> org.kordamp.ikonli.feather.Feather.BOX;
                case "SEQUENCE"                -> org.kordamp.ikonli.feather.Feather.HASH;
                case "INDEX"                   -> org.kordamp.ikonli.feather.Feather.LIST;
                case "SYNONYM"                 -> org.kordamp.ikonli.feather.Feather.LINK_2;
                default                        -> org.kordamp.ikonli.feather.Feather.FILE;
            };
        }
    }
}
