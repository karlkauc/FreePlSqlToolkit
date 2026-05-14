package org.fxt.freeplsql.app.ui.shell;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Centered "no content yet" placeholder.
 * Use {@link #builder()} to compose icon/title/body/action — all optional.
 */
public final class EmptyState extends VBox {

    private EmptyState() {
        getStyleClass().add("empty-state");
        setAlignment(Pos.CENTER);
        setSpacing(12);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Node icon;
        private String title;
        private String body;
        private String actionLabel;
        private Runnable onAction;

        public Builder featherIcon(Feather glyph) {
            FontIcon fi = new FontIcon(glyph);
            fi.setIconSize(36);
            fi.getStyleClass().add("empty-icon");
            this.icon = fi;
            return this;
        }

        public Builder logoMark(int size) {
            ImageView iv = new ImageView(Branding.markImage(size));
            iv.setOpacity(0.30);
            this.icon = iv;
            return this;
        }

        public Builder title(String t)      { this.title = t; return this; }
        public Builder body(String b)       { this.body = b; return this; }
        public Builder action(String label, Runnable r) {
            this.actionLabel = label; this.onAction = r; return this;
        }

        public EmptyState build() {
            EmptyState es = new EmptyState();
            if (icon != null) es.getChildren().add(icon);
            if (title != null) {
                Label t = new Label(title);
                t.getStyleClass().add("empty-title");
                es.getChildren().add(t);
            }
            if (body != null) {
                Label b = new Label(body);
                b.getStyleClass().add("empty-body");
                b.setWrapText(true);
                b.setMaxWidth(360);
                b.setStyle("-fx-text-alignment: center;");
                es.getChildren().add(b);
            }
            if (actionLabel != null && onAction != null) {
                Button btn = new Button(actionLabel);
                btn.getStyleClass().add("button-primary");
                btn.setOnAction(ev -> onAction.run());
                es.getChildren().add(btn);
            }
            return es;
        }
    }
}
