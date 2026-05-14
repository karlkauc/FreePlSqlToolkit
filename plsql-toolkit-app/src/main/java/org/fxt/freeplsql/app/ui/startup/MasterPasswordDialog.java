package org.fxt.freeplsql.app.ui.startup;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/**
 * Modal dialog that asks for the master password — either to set one up
 * (CREATE) or to unlock an existing profile file (UNLOCK). Returns the entered
 * password as {@code char[]} so the caller can zero it after the KDF runs.
 */
public final class MasterPasswordDialog extends Dialog<char[]> {

    public enum Mode { CREATE, UNLOCK }

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final Mode mode;
    private final PasswordField pw1 = new PasswordField();
    private final PasswordField pw2 = new PasswordField();
    private final Label errorLabel = new Label();

    public MasterPasswordDialog(Mode mode) {
        this.mode = mode;
        setTitle("FreePlSqlToolkit");
        setGraphic(brandMark());
        setHeaderText(mode == Mode.CREATE
                ? "Set a master password to encrypt your connection profiles.\n"
                  + "If you lose it, the profiles cannot be recovered."
                : "Enter the master password to unlock your connection profiles.");

        VBox content = new VBox(8);
        content.setPadding(new Insets(20, 20, 12, 20));
        content.setPrefWidth(420);

        content.getChildren().addAll(new Label("Master password:"), pw1);
        if (mode == Mode.CREATE) {
            content.getChildren().addAll(new Label("Confirm:"), pw2);
        }
        errorLabel.getStyleClass().add("dialog-error");
        errorLabel.setWrapText(true);
        content.getChildren().add(errorLabel);

        getDialogPane().setContent(content);

        ButtonType primary = mode == Mode.CREATE
                ? new ButtonType("Create", ButtonBar.ButtonData.OK_DONE)
                : new ButtonType("Unlock", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(primary, ButtonType.CANCEL);

        Node primaryButton = getDialogPane().lookupButton(primary);
        primaryButton.getStyleClass().add("button-primary");
        primaryButton.addEventFilter(ActionEvent.ACTION, ev -> {
            if (!validate()) {
                ev.consume();
            }
        });

        setResultConverter(button -> button == primary ? pw1.getText().toCharArray() : null);

        Platform.runLater(pw1::requestFocus);
    }

    /** Pre-fills the inline error label, e.g. after a previous wrong-password attempt. */
    public void setInitialError(String message) {
        if (message != null) {
            errorLabel.setText(message);
        }
    }

    private static javafx.scene.image.ImageView brandMark() {
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                org.fxt.freeplsql.app.ui.shell.Branding.markImage(40));
        iv.setFitWidth(40);
        iv.setFitHeight(40);
        iv.setPreserveRatio(true);
        return iv;
    }

    private boolean validate() {
        String p1 = pw1.getText();
        if (p1 == null || p1.isEmpty()) {
            errorLabel.setText("Password cannot be empty.");
            return false;
        }
        if (mode == Mode.CREATE) {
            if (p1.length() < MIN_PASSWORD_LENGTH) {
                errorLabel.setText("Master password must be at least "
                        + MIN_PASSWORD_LENGTH + " characters.");
                return false;
            }
            if (!p1.equals(pw2.getText())) {
                errorLabel.setText("Passwords do not match.");
                return false;
            }
        }
        errorLabel.setText("");
        return true;
    }
}
