package org.fxt.freeplsql.app;

import org.fxt.freeplsql.appsvc.connection.ConnectionProfile;
import org.fxt.freeplsql.appsvc.persistence.AppPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppContextTest {

    @Test
    void profilesPersistAcrossInstances(@TempDir Path tmp) throws Exception {
        var paths = new AppPaths(tmp);

        var ctx1 = new AppContext(paths);
        ctx1.profileStore().initialize("master1!!".toCharArray());
        var p1 = ConnectionProfile.easyConnect("id-1", "Local-XE",
                "localhost", 1521, "XEPDB1", "scott", "tiger", 2);
        var p2 = ConnectionProfile.tnsNames("id-2", "Prod",
                "PROD_RW", "app", "secret", 4);
        ctx1.setProfiles(List.of(p1, p2));
        ctx1.saveProfiles();

        var ctx2 = new AppContext(paths);
        var unlocked = ctx2.profileStore().unlock("master1!!".toCharArray());
        ctx2.setProfiles(unlocked);

        assertEquals(2, ctx2.profiles().size());
        assertTrue(ctx2.profiles().contains(p1));
        assertTrue(ctx2.profiles().contains(p2));
    }

    @Test
    void settingsPersistAcrossInstances(@TempDir Path tmp) {
        var paths = new AppPaths(tmp);

        var ctx1 = new AppContext(paths);
        ctx1.settings().setDark(true);
        ctx1.saveSettings();

        var ctx2 = new AppContext(paths);
        assertTrue(ctx2.settings().isDark());
    }
}
