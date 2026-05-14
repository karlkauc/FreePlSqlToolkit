package org.fxt.freeplsql.app.ui.shell;

import javafx.scene.control.Label;

/** Factory for small inline status chips. Style is driven by components.css. */
public final class Chip {

    private Chip() {}

    public static Label success(String text) { return chip(text, "chip-success"); }
    public static Label warn(String text)    { return chip(text, "chip-warn"); }
    public static Label danger(String text)  { return chip(text, "chip-danger"); }
    public static Label info(String text)    { return chip(text, "chip-info"); }
    public static Label muted(String text)   { return chip(text, "chip-muted"); }

    /**
     * Returns a chip styled for the given lint severity name
     * (case-insensitive: ERROR/WARN/INFO/HINT or anything else).
     */
    public static Label forSeverity(String severityName) {
        String s = severityName == null ? "" : severityName.toUpperCase();
        return switch (s) {
            case "ERROR" -> danger(s);
            case "WARN", "WARNING" -> warn(s);
            case "INFO" -> info(s);
            case "HINT" -> muted(s);
            default -> muted(s);
        };
    }

    private static Label chip(String text, String severityClass) {
        Label l = new Label(text);
        l.getStyleClass().setAll("chip", severityClass);
        return l;
    }
}
