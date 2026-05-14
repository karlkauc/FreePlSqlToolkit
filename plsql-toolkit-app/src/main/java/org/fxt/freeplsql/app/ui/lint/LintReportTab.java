package org.fxt.freeplsql.app.ui.lint;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.fxt.freeplsql.appsvc.lint.LintReport;
import org.fxt.freeplsql.appsvc.lint.LintReportRenderer;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.sync.DbObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Tab that displays a {@link LintReport} as a flat issue table with three
 * export buttons (Markdown / HTML / SARIF).
 */
public final class LintReportTab extends Tab {

    private final LintReport report;
    private final LintReportRenderer renderer = new LintReportRenderer();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    public LintReportTab(LintReport report) {
        super("Lint: " + report.schemaName() + " · " + report.totalIssues() + " issue(s)");
        this.report = report;
        populateRows();

        Label header = new Label(
                report.connectionName() + " · " + report.schemaName()
                        + " · " + report.totalObjects() + " object(s) · "
                        + report.totalIssues() + " issue(s) · "
                        + report.runAt());
        header.setStyle("-fx-padding: 8 12 4 12; -fx-text-fill: -fxt-fg-muted;");

        TableView<Row> table = buildTable();

        ToolBar toolbar = new ToolBar();
        Button md = new Button("Export Markdown…");
        Button html = new Button("Export HTML…");
        Button sarif = new Button("Export SARIF…");
        md.setOnAction(e -> export("Markdown", "*.md", ".md", renderer.renderMarkdown(report)));
        html.setOnAction(e -> export("HTML", "*.html", ".html", renderer.renderHtml(report)));
        sarif.setOnAction(e -> export("SARIF", "*.sarif", ".sarif", renderer.renderSarif(report)));
        toolbar.getItems().addAll(md, html, sarif);

        VBox layout = new VBox(toolbar, header, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        setContent(layout);
    }

    public LintReport report() {
        return report;
    }

    private void populateRows() {
        for (Map.Entry<DbObject, java.util.List<Issue>> e : report.issuesByObject().entrySet()) {
            DbObject obj = e.getKey();
            for (Issue i : e.getValue()) {
                rows.add(new Row(
                        obj.type() + " " + obj.name(),
                        i.severity().name(),
                        i.ruleId(),
                        i.position().line(),
                        i.message()));
            }
        }
    }

    private TableView<Row> buildTable() {
        var table = new TableView<>(rows);
        TableColumn<Row, String> objectCol = new TableColumn<>("Object");
        objectCol.setCellValueFactory(new PropertyValueFactory<>("object"));
        objectCol.setPrefWidth(220);
        TableColumn<Row, String> sev = new TableColumn<>("Severity");
        sev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        sev.setPrefWidth(80);
        sev.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                setGraphic(org.fxt.freeplsql.app.ui.shell.Chip.forSeverity(item));
                setText(null);
            }
        });
        TableColumn<Row, String> rule = new TableColumn<>("Rule");
        rule.setCellValueFactory(new PropertyValueFactory<>("ruleId"));
        rule.setPrefWidth(70);
        TableColumn<Row, Number> line = new TableColumn<>("Line");
        line.setCellValueFactory(new PropertyValueFactory<>("line"));
        line.setPrefWidth(60);
        TableColumn<Row, String> msg = new TableColumn<>("Message");
        msg.setCellValueFactory(new PropertyValueFactory<>("message"));
        msg.setPrefWidth(700);
        table.getColumns().setAll(objectCol, sev, rule, line, msg);
        return table;
    }

    private void export(String label, String pattern, String extension, String content) {
        var chooser = new FileChooser();
        chooser.setTitle("Export " + label);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(label, pattern));
        chooser.setInitialFileName("lint-" + report.schemaName().toLowerCase() + extension);
        Window owner = getTabPane() != null && getTabPane().getScene() != null
                ? getTabPane().getScene().getWindow() : null;
        var file = chooser.showSaveDialog(owner);
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Export failed:\n" + e.getMessage()).showAndWait();
        }
    }

    public static final class Row {
        private final SimpleStringProperty object;
        private final SimpleStringProperty severity;
        private final SimpleStringProperty ruleId;
        private final javafx.beans.property.SimpleIntegerProperty line;
        private final SimpleStringProperty message;

        public Row(String object, String severity, String ruleId, int line, String message) {
            this.object = new SimpleStringProperty(object);
            this.severity = new SimpleStringProperty(severity);
            this.ruleId = new SimpleStringProperty(ruleId);
            this.line = new javafx.beans.property.SimpleIntegerProperty(line);
            this.message = new SimpleStringProperty(message);
        }

        public String getObject() { return object.get(); }
        public String getSeverity() { return severity.get(); }
        public String getRuleId() { return ruleId.get(); }
        public int getLine() { return line.get(); }
        public String getMessage() { return message.get(); }
    }
}
