package org.fxt.freeplsql.app.ui.connection;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.fxt.freeplsql.appsvc.connection.AuthType;
import org.fxt.freeplsql.appsvc.connection.ConnectionProfile;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Modal dialog for creating or editing a {@link ConnectionProfile}. Renders
 * one tab per {@link AuthType}; the active tab decides which fields the
 * resulting profile is built from.
 */
public final class ProfileEditorDialog extends Dialog<ConnectionProfile> {

    @FXML private TextField nameField;
    @FXML private Spinner<Integer> poolSpinner;
    @FXML private TabPane authTabs;

    @FXML private TextField ecHost;
    @FXML private Spinner<Integer> ecPort;
    @FXML private TextField ecService;
    @FXML private TextField ecUsername;
    @FXML private PasswordField ecPassword;

    @FXML private TextField tnsAlias;
    @FXML private TextField tnsUsername;
    @FXML private PasswordField tnsPassword;

    @FXML private TextField walletAlias;
    @FXML private TextField walletPath;
    @FXML private Button walletBrowseButton;
    @FXML private TextField walletUsername;
    @FXML private PasswordField walletPassword;

    @FXML private TextField kerbHost;
    @FXML private Spinner<Integer> kerbPort;
    @FXML private TextField kerbService;
    @FXML private TextField kerbPrincipal;

    @FXML private Label errorLabel;

    private final String existingId;

    /** {@code initial == null} → create new profile; otherwise edit a copy. */
    public ProfileEditorDialog(ConnectionProfile initial) {
        this.existingId = initial != null ? initial.id() : null;

        setTitle(initial == null ? "New Connection Profile" : "Edit Connection Profile");
        setHeaderText("Connection profile");
        setGraphic(brandMark());

        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/fxml/ProfileEditorDialog.fxml"),
                "ProfileEditorDialog.fxml not found"));
        loader.setController(this);
        try {
            getDialogPane().setContent(loader.load());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load ProfileEditorDialog.fxml", e);
        }

        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        configureSpinners();
        configureBrowse();
        applyInitial(initial);

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("button-primary");
        okButton.addEventFilter(ActionEvent.ACTION, ev -> {
            if (!validate()) {
                ev.consume();
            }
        });

        setResultConverter(button -> button == ButtonType.OK ? buildProfile() : null);

        Platform.runLater(nameField::requestFocus);
    }

    private void configureSpinners() {
        poolSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 32, 2));
        poolSpinner.setEditable(true);
        ecPort.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 65535, 1521));
        ecPort.setEditable(true);
        kerbPort.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 65535, 1521));
        kerbPort.setEditable(true);
    }

    private void configureBrowse() {
        walletBrowseButton.setOnAction(e -> {
            var chooser = new DirectoryChooser();
            chooser.setTitle("Select Oracle Wallet directory");
            String current = walletPath.getText();
            if (current != null && !current.isBlank()) {
                File dir = new File(current);
                if (dir.isDirectory()) {
                    chooser.setInitialDirectory(dir);
                }
            }
            File picked = chooser.showDialog(getDialogPane().getScene().getWindow());
            if (picked != null) {
                walletPath.setText(picked.getAbsolutePath());
            }
        });
    }

    private void applyInitial(ConnectionProfile p) {
        if (p == null) {
            authTabs.getSelectionModel().select(0);
            return;
        }
        nameField.setText(nullToEmpty(p.name()));
        poolSpinner.getValueFactory().setValue(p.poolSize() <= 0 ? 2 : p.poolSize());

        switch (p.authType()) {
            case EASY_CONNECT -> {
                authTabs.getSelectionModel().select(0);
                ecHost.setText(nullToEmpty(p.host()));
                ecPort.getValueFactory().setValue(p.port() != null ? p.port() : 1521);
                ecService.setText(nullToEmpty(p.service()));
                ecUsername.setText(nullToEmpty(p.username()));
                ecPassword.setText(nullToEmpty(p.password()));
            }
            case TNS_NAMES -> {
                authTabs.getSelectionModel().select(1);
                tnsAlias.setText(nullToEmpty(p.tnsAlias()));
                tnsUsername.setText(nullToEmpty(p.username()));
                tnsPassword.setText(nullToEmpty(p.password()));
            }
            case WALLET -> {
                authTabs.getSelectionModel().select(2);
                walletAlias.setText(nullToEmpty(p.tnsAlias()));
                walletPath.setText(nullToEmpty(p.walletPath()));
                walletUsername.setText(nullToEmpty(p.username()));
                walletPassword.setText(nullToEmpty(p.password()));
            }
            case KERBEROS -> {
                authTabs.getSelectionModel().select(3);
                kerbHost.setText(nullToEmpty(p.host()));
                kerbPort.getValueFactory().setValue(p.port() != null ? p.port() : 1521);
                kerbService.setText(nullToEmpty(p.service()));
                kerbPrincipal.setText(nullToEmpty(p.kerberosPrincipal()));
            }
        }
    }

    private boolean validate() {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            return fail("Profile name is required.");
        }
        switch (selectedAuth()) {
            case EASY_CONNECT -> {
                if (ecHost.getText().isBlank()) return fail("Host is required.");
                if (ecService.getText().isBlank()) return fail("Service name is required.");
                if (ecUsername.getText().isBlank()) return fail("Username is required.");
            }
            case TNS_NAMES -> {
                if (tnsAlias.getText().isBlank()) return fail("TNS alias is required.");
                if (tnsUsername.getText().isBlank()) return fail("Username is required.");
            }
            case WALLET -> {
                if (walletAlias.getText().isBlank()) return fail("TNS alias (from the wallet) is required.");
                if (walletPath.getText().isBlank()) return fail("Wallet directory is required.");
            }
            case KERBEROS -> {
                if (kerbHost.getText().isBlank()) return fail("Host is required.");
                if (kerbService.getText().isBlank()) return fail("Service name is required.");
            }
        }
        errorLabel.setText("");
        return true;
    }

    private boolean fail(String msg) {
        errorLabel.setText(msg);
        return false;
    }

    private ConnectionProfile buildProfile() {
        String id = existingId != null ? existingId : UUID.randomUUID().toString();
        String name = nameField.getText().trim();
        int pool = poolSpinner.getValue();
        return switch (selectedAuth()) {
            case EASY_CONNECT -> ConnectionProfile.easyConnect(
                    id, name,
                    ecHost.getText().trim(), ecPort.getValue(), ecService.getText().trim(),
                    ecUsername.getText().trim(), ecPassword.getText(), pool);
            case TNS_NAMES -> ConnectionProfile.tnsNames(
                    id, name, tnsAlias.getText().trim(),
                    tnsUsername.getText().trim(), tnsPassword.getText(), pool);
            case WALLET -> ConnectionProfile.wallet(
                    id, name,
                    walletAlias.getText().trim(),
                    walletPath.getText().trim(),
                    walletUsername.getText().trim(),
                    walletPassword.getText() == null ? null : walletPassword.getText(),
                    pool);
            case KERBEROS -> ConnectionProfile.kerberos(
                    id, name,
                    kerbHost.getText().trim(), kerbPort.getValue(), kerbService.getText().trim(),
                    kerbPrincipal.getText() == null ? null : kerbPrincipal.getText().trim(),
                    pool);
        };
    }

    private AuthType selectedAuth() {
        Tab t = authTabs.getSelectionModel().getSelectedItem();
        if (t == null) return AuthType.EASY_CONNECT;
        return switch (t.getText()) {
            case "TNS" -> AuthType.TNS_NAMES;
            case "Wallet" -> AuthType.WALLET;
            case "Kerberos" -> AuthType.KERBEROS;
            default -> AuthType.EASY_CONNECT;
        };
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static javafx.scene.image.ImageView brandMark() {
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                org.fxt.freeplsql.app.ui.shell.Branding.markImage(40));
        iv.setFitWidth(40);
        iv.setFitHeight(40);
        iv.setPreserveRatio(true);
        return iv;
    }
}
