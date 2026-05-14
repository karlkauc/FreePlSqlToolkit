package org.fxt.freeplsql.app.ui.shell;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.fxt.freeplsql.appsvc.connection.ConnectionManager;

/**
 * Owns the four logical status-bar segments:
 *   [connection]  [lint chips]  [caret]  [encoding/theme]
 *
 * Listens to ConnectionManager connect/disconnect events so the dot stays
 * in sync without polling.
 */
public final class StatusBarController {

    private final Label connectionLabel;
    private final Label lintWarnChip;
    private final Label lintErrorChip;
    private final Label caretLabel;
    private final ConnectionManager connections;

    public StatusBarController(Label connectionLabel,
                               Label lintWarnChip,
                               Label lintErrorChip,
                               Label caretLabel,
                               ConnectionManager connections) {
        this.connectionLabel = connectionLabel;
        this.lintWarnChip = lintWarnChip;
        this.lintErrorChip = lintErrorChip;
        this.caretLabel = caretLabel;
        this.connections = connections;
        lintWarnChip.getStyleClass().setAll("chip", "chip-warn");
        lintErrorChip.getStyleClass().setAll("chip", "chip-danger");
        connectionLabel.getStyleClass().add("conn-dot");
        connectionLabel.setTooltip(new Tooltip("Active database connections"));
        refreshConnection();
        connections.addConnectListener(h -> Platform.runLater(this::refreshConnection));
        connections.addDisconnectListener(name -> Platform.runLater(this::refreshConnection));
        setLintCounts(0, 0);
        setCaret(0, 0);
    }

    public void refreshConnection() {
        var active = connections.activeConnections();
        int count = active.size();
        String first = active.stream()
                .findFirst()
                .map(h -> h.profile().name())
                .orElse(null);
        connectionLabel.setText(connectionSummary(count, first));
        if (count > 0) {
            connectionLabel.getStyleClass().add("connected");
        } else {
            connectionLabel.getStyleClass().remove("connected");
        }
    }

    public void setLintCounts(int warn, int error) {
        lintWarnChip.setText("⚠ " + warn);
        lintErrorChip.setText("✕ " + error);
        lintWarnChip.setVisible(warn > 0);
        lintWarnChip.setManaged(warn > 0);
        lintErrorChip.setVisible(error > 0);
        lintErrorChip.setManaged(error > 0);
    }

    public void setCaret(int lineZeroBased, int colZeroBased) {
        caretLabel.setText(caretLabel(lineZeroBased, colZeroBased));
    }

    /* --- Pure helpers, tested in isolation --- */

    static String connectionSummary(int count, String firstName) {
        if (count <= 0) return "● no connection";
        if (count == 1) return "● " + firstName;
        return "● " + count + " connected";
    }

    static String caretLabel(int lineZeroBased, int colZeroBased) {
        return "Ln " + (lineZeroBased + 1) + ", Col " + (colZeroBased + 1);
    }
}
