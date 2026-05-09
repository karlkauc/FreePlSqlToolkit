package org.fxt.freeplsql.app.ui.editor;

import javafx.scene.control.Alert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Editor tab backed by a local file. Live-lints on every text change
 * (debounced 300 ms).
 */
public final class LocalFileEditorTab extends EditorTab {

    private final Path file;

    public LocalFileEditorTab(Path file) throws IOException {
        super(file.getFileName().toString(), file.toString());
        this.file = file;
        codeArea.replaceText(Files.readString(file, StandardCharsets.UTF_8));

        codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(300))
                .subscribe(ignored -> runLint());

        runLint();
    }

    public Path file() {
        return file;
    }

    public void save() {
        try {
            Files.writeString(file, codeArea.getText(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not save '" + file + "':\n" + e.getMessage()).showAndWait();
        }
    }
}
