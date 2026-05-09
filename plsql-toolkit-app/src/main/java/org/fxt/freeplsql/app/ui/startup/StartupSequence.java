package org.fxt.freeplsql.app.ui.startup;

import javafx.scene.control.Alert;
import org.fxt.freeplsql.appsvc.connection.ConnectionProfile;
import org.fxt.freeplsql.appsvc.connection.ProfileStore;
import org.fxt.freeplsql.appsvc.connection.WrongPasswordException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Drives the master-password dialog before the main window comes up. Returns
 * the decrypted profile list on success; returns {@code null} if the user
 * cancels or the file is irrecoverably broken — caller should then exit the
 * application.
 */
public final class StartupSequence {

    private final ProfileStore profileStore;

    public StartupSequence(ProfileStore profileStore) {
        this.profileStore = profileStore;
    }

    public List<ConnectionProfile> run() {
        return profileStore.exists() ? runUnlock() : runCreate();
    }

    private List<ConnectionProfile> runCreate() {
        var dialog = new MasterPasswordDialog(MasterPasswordDialog.Mode.CREATE);
        Optional<char[]> pw = dialog.showAndWait();
        if (pw.isEmpty()) {
            return null;
        }
        char[] pwd = pw.get();
        try {
            profileStore.initialize(pwd);
            return List.of();
        } catch (IOException e) {
            showError("Could not initialize profile store:\n" + e.getMessage());
            return null;
        } finally {
            Arrays.fill(pwd, '\0');
        }
    }

    private List<ConnectionProfile> runUnlock() {
        String prefilledError = null;
        while (true) {
            var dialog = new MasterPasswordDialog(MasterPasswordDialog.Mode.UNLOCK);
            if (prefilledError != null) {
                dialog.setInitialError(prefilledError);
            }
            Optional<char[]> pw = dialog.showAndWait();
            if (pw.isEmpty()) {
                return null;
            }
            char[] pwd = pw.get();
            try {
                return profileStore.unlock(pwd);
            } catch (WrongPasswordException e) {
                prefilledError = "Wrong master password — try again.";
            } catch (IOException e) {
                showError("Could not read profile store:\n" + e.getMessage());
                return null;
            } finally {
                Arrays.fill(pwd, '\0');
            }
        }
    }

    private static void showError(String msg) {
        var alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
