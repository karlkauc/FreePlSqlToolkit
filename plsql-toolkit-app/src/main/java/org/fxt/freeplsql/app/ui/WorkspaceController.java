package org.fxt.freeplsql.app.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxt.freeplsql.app.AppContext;
import org.fxt.freeplsql.app.ui.connection.ConnectionSidebarController;
import org.fxt.freeplsql.app.ui.diff.SchemaDiffTab;
import org.fxt.freeplsql.app.ui.editor.LocalFileEditorTab;
import org.fxt.freeplsql.app.ui.invalid.InvalidObjectsTab;
import org.fxt.freeplsql.app.ui.metrics.MetricsTab;
import org.fxt.freeplsql.app.ui.navigator.LocalFileNavigatorController;
import org.fxt.freeplsql.app.ui.navigator.SchemaNavigatorController;
import org.fxt.freeplsql.app.ui.search.DbSearchTab;
import org.fxt.freeplsql.app.ui.snapshot.SnapshotDialog;
import org.fxt.freeplsql.app.ui.tabs.TabManager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WorkspaceController {

    @FXML private BorderPane root;
    @FXML private Accordion sidebarAccordion;
    @FXML private TabPane workspaceTabs;
    @FXML private Label statusLabel;
    @FXML private org.fxt.freeplsql.app.ui.shell.ActivityBarController activityBarController;

    private Stage stage;
    private AppContext context;
    private ThemeManager themeManager;
    private TabManager tabManager;

    public void bind(Stage stage, Scene scene, AppContext context) {
        this.stage = stage;
        this.context = context;
        this.tabManager = new TabManager(workspaceTabs);
        this.themeManager = new ThemeManager(scene, context.settings().isDark(), () -> {
            context.settings().setDark(themeManager.isDark());
            context.saveSettings();
        });

        TitledPane connections = ConnectionSidebarController.create(context);
        TitledPane schema = SchemaNavigatorController.create(context, tabManager);
        TitledPane files = LocalFileNavigatorController.create(tabManager, stage);
        sidebarAccordion.getPanes().addAll(connections, schema, files);
        sidebarAccordion.setExpandedPane(connections);

        activityBarController.bindSidebar(sidebarAccordion, connections, schema, files);
        activityBarController.onSearch(this::onDbSearch);
        activityBarController.onMetrics(this::onMetrics);
        activityBarController.onInvalid(this::onInvalidObjects);
        activityBarController.onDiff(this::onSchemaDiff);
        activityBarController.onTheme(this::onToggleTheme);
        activityBarController.setDarkIcon(themeManager.isDark());
        themeManager.darkProperty().addListener((o, was, dark) ->
                activityBarController.setDarkIcon(dark));
    }

    /** Re-opens the local-file tabs that were open last session. Called after stage.show(). */
    public void restoreOpenTabs() {
        for (String filePath : context.workspace().getOpenLocalFiles()) {
            Path p = Path.of(filePath);
            if (!Files.exists(p)) continue;
            String key = "file://" + p.toAbsolutePath();
            try {
                tabManager.openOrFocus(key, () -> {
                    try {
                        return new LocalFileEditorTab(p);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (UncheckedIOException ignored) {
                // file became unreadable between persist and now — drop silently
            }
        }
    }

    /** Snapshots the current open-tab list into the WorkspaceState (called on close). */
    public void captureWorkspaceState() {
        List<String> openFiles = new ArrayList<>();
        for (Tab t : workspaceTabs.getTabs()) {
            if (t instanceof LocalFileEditorTab lf) {
                openFiles.add(lf.file().toAbsolutePath().toString());
            }
        }
        context.workspace().setOpenLocalFiles(openFiles);
    }

    @FXML
    private void onOpenFile() {
        var chooser = new FileChooser();
        chooser.setTitle("Open PL/SQL file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "PL/SQL files", "*.sql", "*.pks", "*.pkb", "*.plsql"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        File f = chooser.showOpenDialog(stage);
        if (f == null) return;
        String key = "file://" + f.getAbsolutePath();
        try {
            tabManager.openOrFocus(key, () -> {
                try {
                    return new LocalFileEditorTab(f.toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not open file:\n" + e.getCause().getMessage()).showAndWait();
        }
        statusLabel.setText("Opened " + f.getName());
    }

    @FXML
    private void onToggleTheme() {
        themeManager.toggle();
        statusLabel.setText("Theme: " + (themeManager.isDark() ? "dark" : "light"));
    }

    @FXML
    private void onDbSearch() {
        if (!ensureActiveConnection()) return;
        tabManager.openOrFocus("tool:db-search", () -> new DbSearchTab(context, tabManager));
    }

    @FXML
    private void onMetrics() {
        if (!ensureActiveConnection()) return;
        tabManager.openOrFocus("tool:metrics", () -> new MetricsTab(context));
    }

    @FXML
    private void onInvalidObjects() {
        if (!ensureActiveConnection()) return;
        tabManager.openOrFocus("tool:invalid", () -> new InvalidObjectsTab(context));
    }

    @FXML
    private void onSchemaDiff() {
        if (context.connectionManager().activeConnections().size() < 2) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Schema Diff needs at least two active connections.").showAndWait();
            return;
        }
        tabManager.openOrFocus("tool:schema-diff", () -> new SchemaDiffTab(context));
    }

    @FXML
    private void onSnapshot() {
        if (!ensureActiveConnection()) return;
        new SnapshotDialog(context).showAndWait();
    }

    @FXML
    private void onAbout() {
        var alert = new Alert(Alert.AlertType.INFORMATION,
                "FreePlSqlToolkit v0.2\n\nMulti-database PL/SQL analysis workbench.\n"
                        + "Apache 2.0 — github.com/karlkauc/FreePlSqlToolkit");
        alert.setHeaderText("About");
        alert.showAndWait();
    }

    private boolean ensureActiveConnection() {
        if (context.connectionManager().activeConnections().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Connect to at least one database first.").showAndWait();
            return false;
        }
        return true;
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }
}
