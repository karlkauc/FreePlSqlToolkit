package org.fxt.freeplsql.app;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxt.freeplsql.app.editor.PlSqlCodeArea;
import org.fxt.freeplsql.app.lint.LiveLintService;
import org.fxt.freeplsql.lint.DefaultRules;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainController {

    @FXML private BorderPane root;
    @FXML private StackPane editorPane;
    @FXML private TableView<IssueRow> lintTable;
    @FXML private TableColumn<IssueRow, String> colSeverity;
    @FXML private TableColumn<IssueRow, String> colRuleId;
    @FXML private TableColumn<IssueRow, Number> colLine;
    @FXML private TableColumn<IssueRow, String> colMessage;
    @FXML private TableView<RuleRow> guidelinesTable;
    @FXML private TableColumn<RuleRow, String> guidelineId;
    @FXML private TableColumn<RuleRow, String> guidelineName;
    @FXML private TableColumn<RuleRow, String> guidelineSeverity;
    @FXML private Label statusLabel;

    private final PlSqlCodeArea codeArea = new PlSqlCodeArea();
    private final ObservableList<IssueRow> issues = FXCollections.observableArrayList();
    private final ExecutorService lintExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "plsql-linter");
        t.setDaemon(true);
        return t;
    });
    private final LiveLintService liveLint = new LiveLintService(DefaultRules.all(), lintExecutor);
    private boolean darkTheme = false;
    private Path currentFile;
    private Stage stage;

    @FXML
    private void initialize() {
        editorPane.getChildren().add(new VirtualizedScrollPane<>(codeArea));
        codeArea.replaceText(0, 0, """
                -- Welcome to FreePlSqlToolkit. Open a .sql file or start typing.
                CREATE OR REPLACE PROCEDURE p_demo IS
                    l_count NUMBER;
                BEGIN
                    SELECT count(*) INTO l_count FROM dual;
                END;
                /
                """);

        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colRuleId.setCellValueFactory(new PropertyValueFactory<>("ruleId"));
        colLine.setCellValueFactory(new PropertyValueFactory<>("line"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        lintTable.setItems(issues);

        lintTable.setRowFactory(tv -> {
            var row = new javafx.scene.control.TableRow<IssueRow>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getClickCount() == 2) {
                    jumpTo(row.getItem());
                }
            });
            return row;
        });

        guidelineId.setCellValueFactory(new PropertyValueFactory<>("id"));
        guidelineName.setCellValueFactory(new PropertyValueFactory<>("name"));
        guidelineSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        var ruleRows = FXCollections.<RuleRow>observableArrayList();
        for (LintRule rule : DefaultRules.all()) {
            ruleRows.add(new RuleRow(rule.id(), rule.name(), rule.defaultSeverity().name()));
        }
        guidelinesTable.setItems(ruleRows);

        codeArea.multiPlainChanges()
                .successionEnds(java.time.Duration.ofMillis(300))
                .subscribe(ignore -> runLint());

        runLint();
    }

    void bind(Stage stage, Scene scene) {
        this.stage = stage;
    }

    @FXML
    private void onOpen() {
        var chooser = new FileChooser();
        chooser.setTitle("Open PL/SQL file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PL/SQL", "*.sql", "*.pks", "*.pkb", "*.plsql"));
        var file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            codeArea.replaceText(text);
            currentFile = file.toPath();
            stage.setTitle(MainApp.APP_NAME + " — " + file.getName());
            statusLabel.setText("Opened " + file.getAbsolutePath());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not open file: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onSave() {
        if (currentFile == null) {
            onSaveAs();
            return;
        }
        try {
            Files.writeString(currentFile, codeArea.getText(), StandardCharsets.UTF_8);
            statusLabel.setText("Saved " + currentFile);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not save: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onSaveAs() {
        var chooser = new FileChooser();
        chooser.setTitle("Save As");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PL/SQL", "*.sql"));
        var file = chooser.showSaveDialog(stage);
        if (file == null) return;
        currentFile = file.toPath();
        onSave();
    }

    @FXML
    private void onToggleTheme() {
        darkTheme = !darkTheme;
        MainApp.applyTheme(darkTheme);
        statusLabel.setText("Theme: " + (darkTheme ? "dark" : "light"));
    }

    @FXML
    private void onRunLint() {
        runLint();
    }

    @FXML
    private void onExit() {
        liveLint.shutdown();
        codeArea.shutdown();
        Platform.exit();
    }

    private void runLint() {
        String text = codeArea.getText();
        liveLint.lint(text, results -> Platform.runLater(() -> applyResults(results)));
    }

    private void applyResults(List<Issue> results) {
        issues.setAll(results.stream().map(IssueRow::from).toList());
        statusLabel.setText(results.isEmpty()
                ? "No issues found."
                : results.size() + " issue(s) found.");
    }

    private void jumpTo(IssueRow row) {
        if (row == null) return;
        int line = row.line.get();
        if (line >= 1 && line <= codeArea.getParagraphs().size()) {
            int paraIndex = line - 1;
            int caret = codeArea.position(paraIndex, 0).toOffset();
            codeArea.moveTo(caret);
            codeArea.requestFollowCaret();
            codeArea.requestFocus();
        }
    }

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

    public static final class RuleRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty severity;

        public RuleRow(String id, String name, String severity) {
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.severity = new SimpleStringProperty(severity);
        }

        public String getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getSeverity() { return severity.get(); }
    }
}
