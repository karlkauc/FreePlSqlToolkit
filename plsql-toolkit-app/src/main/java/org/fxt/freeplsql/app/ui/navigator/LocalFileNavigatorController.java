package org.fxt.freeplsql.app.ui.navigator;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.fxt.freeplsql.app.ui.editor.LocalFileEditorTab;
import org.fxt.freeplsql.app.ui.tabs.TabManager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

/** Sidebar pane that browses the local filesystem and opens .sql/.pks/.pkb files in tabs. */
public final class LocalFileNavigatorController {

    @FXML private TitledPane root;
    @FXML private TreeView<Path> tree;
    @FXML private Button openFolderButton;
    @FXML private Button refreshButton;
    @FXML private Label rootLabel;
    @FXML private javafx.scene.layout.VBox pane;
    @FXML private javafx.scene.layout.VBox emptyState;

    private TabManager tabManager;
    private Stage stage;
    private Path currentRoot;

    public static TitledPane create(TabManager tabManager, Stage stage) {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                LocalFileNavigatorController.class.getResource("/fxml/LocalFileNavigator.fxml"),
                "LocalFileNavigator.fxml not found"));
        TitledPane pane;
        try {
            pane = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load LocalFileNavigator.fxml", e);
        }
        LocalFileNavigatorController c = loader.getController();
        c.tabManager = tabManager;
        c.stage = stage;
        return pane;
    }

    @FXML
    private void initialize() {
        tree.setRoot(new TreeItem<>());
        tree.setCellFactory(tv -> new PathCell());

        openFolderButton.setOnAction(e -> chooseFolder());
        refreshButton.setOnAction(e -> rebuildTree());
        refreshButton.setDisable(true);

        tree.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) return;
            TreeItem<Path> sel = tree.getSelectionModel().getSelectedItem();
            if (sel == null || sel.getValue() == null) return;
            Path p = sel.getValue();
            if (Files.isRegularFile(p)) {
                openFile(p);
            }
        });

        // Empty state
        org.fxt.freeplsql.app.ui.shell.EmptyState es =
                org.fxt.freeplsql.app.ui.shell.EmptyState.builder()
                        .featherIcon(org.kordamp.ikonli.feather.Feather.HARD_DRIVE)
                        .title("No files yet")
                        .body("Drop .sql, .pks or .pkb files here, or use Open Folder…")
                        .action("Open Folder…", () -> openFolderButton.fire())
                        .build();
        emptyState.getChildren().setAll(es);
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
                (javafx.collections.ListChangeListener<? super javafx.scene.control.TreeItem<java.nio.file.Path>>)
                c -> updateEmptyState.run());

        // Drag-and-drop on the pane
        pane.setOnDragOver(ev -> {
            if (ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            ev.consume();
        });
        pane.setOnDragDropped(ev -> {
            boolean success = false;
            if (ev.getDragboard().hasFiles()) {
                for (var f : ev.getDragboard().getFiles()) {
                    java.nio.file.Path p = f.toPath();
                    if (java.nio.file.Files.isRegularFile(p) && isPlSqlFile(p)) {
                        openFile(p);
                        success = true;
                    }
                }
            }
            ev.setDropCompleted(success);
            ev.consume();
        });
    }

    private void chooseFolder() {
        var chooser = new DirectoryChooser();
        chooser.setTitle("Open folder");
        if (currentRoot != null && Files.isDirectory(currentRoot)) {
            chooser.setInitialDirectory(currentRoot.toFile());
        }
        File picked = chooser.showDialog(stage);
        if (picked == null) return;
        currentRoot = picked.toPath();
        refreshButton.setDisable(false);
        rootLabel.setText(currentRoot.toString());
        rebuildTree();
    }

    private void rebuildTree() {
        tree.getRoot().getChildren().clear();
        if (currentRoot == null) return;
        TreeItem<Path> rootItem = buildItem(currentRoot);
        rootItem.setExpanded(true);
        tree.getRoot().getChildren().add(rootItem);
    }

    private TreeItem<Path> buildItem(Path path) {
        TreeItem<Path> item = new TreeItem<>(path);
        if (Files.isDirectory(path)) {
            item.getChildren().add(new TreeItem<>(null));  // placeholder
            ChangeListener<Boolean> listener = new ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs,
                                    Boolean was, Boolean now) {
                    if (Boolean.TRUE.equals(now)
                            && item.getChildren().size() == 1
                            && item.getChildren().get(0).getValue() == null) {
                        item.expandedProperty().removeListener(this);
                        loadChildren(item);
                    }
                }
            };
            item.expandedProperty().addListener(listener);
        }
        return item;
    }

    private void loadChildren(TreeItem<Path> parent) {
        parent.getChildren().clear();
        try (Stream<Path> stream = Files.list(parent.getValue())) {
            stream
                    .filter(p -> Files.isDirectory(p) || isPlSqlFile(p))
                    .sorted(Comparator
                            .comparing((Path p) -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(p -> parent.getChildren().add(buildItem(p)));
        } catch (IOException e) {
            parent.getChildren().add(new TreeItem<>(null));
        }
    }

    private static boolean isPlSqlFile(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".sql") || name.endsWith(".pks")
                || name.endsWith(".pkb") || name.endsWith(".plsql");
    }

    private void openFile(Path file) {
        String key = "file://" + file.toAbsolutePath();
        try {
            tabManager.openOrFocus(key, () -> {
                try {
                    return new LocalFileEditorTab(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not open file:\n" + e.getCause().getMessage()).showAndWait();
        }
    }

    private static final class PathCell extends TreeCell<Path> {
        @Override
        protected void updateItem(Path item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
                return;
            }
            setText(item.getFileName() != null ? item.getFileName().toString() : item.toString());
            if (Files.isDirectory(item)) {
                setStyle("-fx-font-weight: bold;");
            } else {
                setStyle("");
            }
        }
    }
}
