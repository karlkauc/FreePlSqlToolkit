package org.fxt.freeplsql.app.ui.shell;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Compact "label over value" stat card. Styled via .kpi-card in components.css. */
public final class KpiCard extends VBox {

    private final Label valueLabel;

    public KpiCard(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");
        valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");
        getChildren().addAll(l, valueLabel);
        getStyleClass().add("kpi-card");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(2);
    }

    /** Mutates the displayed value (lets callers reuse the same card instance). */
    public void setValue(String value) {
        valueLabel.setText(value);
    }

    /** Convenience: build from a long. */
    public static KpiCard of(String label, long value) {
        return new KpiCard(label, Long.toString(value));
    }
}
