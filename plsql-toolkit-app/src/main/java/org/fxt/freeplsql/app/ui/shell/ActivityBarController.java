package org.fxt.freeplsql.app.ui.shell;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Vertical icon bar to the left of the Accordion sidebar.
 * Hands off clicks to the parent — sidebar expansion and tool-tab opening
 * stay in WorkspaceController so this class has no AppContext dependency.
 */
public final class ActivityBarController {

    @FXML private VBox root;
    @FXML private Button btnConnections, btnSchema, btnFiles,
            btnSearch, btnMetrics, btnInvalid, btnDiff, btnTheme;
    @FXML private FontIcon themeIcon;

    private Accordion sidebarAccordion;
    private TitledPane connectionsPane, schemaPane, filesPane;

    public static VBox load() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    ActivityBarController.class.getResource("/fxml/ActivityBar.fxml")));
            VBox node = loader.load();
            node.setUserData(loader.getController());
            return node;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void bindSidebar(Accordion accordion,
                            TitledPane connections, TitledPane schema, TitledPane files) {
        this.sidebarAccordion = accordion;
        this.connectionsPane = connections;
        this.schemaPane = schema;
        this.filesPane = files;
        btnConnections.setOnAction(e -> expand(connections));
        btnSchema.setOnAction(e -> expand(schema));
        btnFiles.setOnAction(e -> expand(files));
    }

    public void onSearch(Runnable r)  { btnSearch.setOnAction(e -> r.run()); }
    public void onMetrics(Runnable r) { btnMetrics.setOnAction(e -> r.run()); }
    public void onInvalid(Runnable r) { btnInvalid.setOnAction(e -> r.run()); }
    public void onDiff(Runnable r)    { btnDiff.setOnAction(e -> r.run()); }
    public void onTheme(Runnable r)   { btnTheme.setOnAction(e -> r.run()); }

    public void setDarkIcon(boolean dark) {
        themeIcon.setIconLiteral(dark ? "fth-sun" : "fth-moon");
    }

    private void expand(TitledPane pane) {
        if (sidebarAccordion != null) sidebarAccordion.setExpandedPane(pane);
    }
}
