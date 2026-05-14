package org.fxt.freeplsql.app.ui.editor;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxt.freeplsql.app.ui.editor.PlSqlCodeArea;
import org.fxt.freeplsql.lint.DefaultRules;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintEngine;

/** Common shell for an editor tab: code area on top, lint-issue table below. */
public abstract class EditorTab extends Tab {

    protected final PlSqlCodeArea codeArea = new PlSqlCodeArea();
    protected final ObservableList<IssueRow> issueRows = FXCollections.observableArrayList();
    protected final TableView<IssueRow> issuesTable = new TableView<>(issueRows);
    protected final LiveLintBinding lintBinding;

    /** Public read access for shell wiring. */
    public PlSqlCodeArea codeArea() { return codeArea; }

    /** Public read access for shell wiring. */
    public javafx.collections.ObservableList<IssueRow> issueRows() { return issueRows; }

    protected EditorTab(String title, String sourcePath) {
        setText(title);
        configureIssuesTable();

        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.VERTICAL);
        split.getItems().addAll(new VirtualizedScrollPane<>(codeArea), issuesTable);
        split.setDividerPositions(0.72);
        setContent(split);

        lintBinding = new LiveLintBinding(
                new LintEngine(DefaultRules.all()),
                sourcePath,
                issues -> issueRows.setAll(issues.stream().map(IssueRow::from).toList()));

        setOnClosed(ev -> dispose());
    }

    protected final void runLint() {
        lintBinding.lint(codeArea.getText());
    }

    public void dispose() {
        lintBinding.shutdown();
        codeArea.shutdown();
    }

    private void configureIssuesTable() {
        TableColumn<IssueRow, String> sev = new TableColumn<>("Severity");
        sev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        sev.setPrefWidth(90);
        sev.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                setGraphic(org.fxt.freeplsql.app.ui.shell.Chip.forSeverity(item));
                setText(null);
            }
        });
        TableColumn<IssueRow, String> rule = new TableColumn<>("Rule");
        rule.setCellValueFactory(new PropertyValueFactory<>("ruleId"));
        rule.setPrefWidth(80);
        TableColumn<IssueRow, Number> line = new TableColumn<>("Line");
        line.setCellValueFactory(new PropertyValueFactory<>("line"));
        line.setPrefWidth(60);
        TableColumn<IssueRow, String> msg = new TableColumn<>("Message");
        msg.setCellValueFactory(new PropertyValueFactory<>("message"));
        msg.setPrefWidth(700);
        issuesTable.getColumns().setAll(sev, rule, line, msg);

        issuesTable.setRowFactory(tv -> {
            var row = new javafx.scene.control.TableRow<IssueRow>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getClickCount() == 2) {
                    jumpTo(row.getItem().getLine());
                }
            });
            return row;
        });
    }

    private void jumpTo(int line) {
        if (line < 1 || line > codeArea.getParagraphs().size()) return;
        int caret = codeArea.position(line - 1, 0).toOffset();
        codeArea.moveTo(caret);
        codeArea.requestFollowCaret();
        codeArea.requestFocus();
    }

    /** Bean exposed to TableView via {@link PropertyValueFactory}. */
    public static final class IssueRow {
        private final SimpleStringProperty severity;
        private final SimpleStringProperty ruleId;
        private final javafx.beans.property.SimpleIntegerProperty line;
        private final SimpleStringProperty message;

        public IssueRow(String severity, String ruleId, int line, String message) {
            this.severity = new SimpleStringProperty(severity);
            this.ruleId = new SimpleStringProperty(ruleId);
            this.line = new javafx.beans.property.SimpleIntegerProperty(line);
            this.message = new SimpleStringProperty(message);
        }

        public static IssueRow from(Issue issue) {
            return new IssueRow(
                    issue.severity().name(),
                    issue.ruleId(),
                    issue.position().line(),
                    issue.message());
        }

        public String getSeverity() { return severity.get(); }
        public String getRuleId() { return ruleId.get(); }
        public int getLine() { return line.get(); }
        public String getMessage() { return message.get(); }
    }
}
