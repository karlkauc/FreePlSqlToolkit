package org.fxt.freeplsql.sync;

import java.util.List;

public record SyncResult(int objectsScanned, List<String> updatedRelativePaths, String commitHash) {

    public boolean hasChanges() {
        return !updatedRelativePaths.isEmpty();
    }
}
