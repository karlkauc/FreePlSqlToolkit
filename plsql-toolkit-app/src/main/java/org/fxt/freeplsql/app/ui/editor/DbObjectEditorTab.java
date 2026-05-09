package org.fxt.freeplsql.app.ui.editor;

import org.fxt.freeplsql.appsvc.connection.ConnectionHandle;
import org.fxt.freeplsql.sync.DbObject;

/**
 * Read-only editor tab showing the DDL of a database object. Lint runs once
 * after the DDL is loaded — content cannot change so a debounced live-lint
 * would just burn cycles.
 */
public final class DbObjectEditorTab extends EditorTab {

    private final ConnectionHandle handle;
    private final DbObject dbObject;

    public DbObjectEditorTab(ConnectionHandle handle, DbObject dbObject, String ddl) {
        super(handle.profile().name() + " · " + dbObject.schema() + "." + dbObject.name(),
                handle.profile().name() + "/" + dbObject.relativePath());
        this.handle = handle;
        this.dbObject = dbObject;
        codeArea.replaceText(ddl);
        codeArea.setEditable(false);
        runLint();
    }

    /** Replaces the placeholder text with the actual DDL once it's been fetched. */
    public void replaceContent(String ddl) {
        codeArea.setEditable(true);
        codeArea.replaceText(ddl);
        codeArea.setEditable(false);
        runLint();
    }

    public ConnectionHandle handle() {
        return handle;
    }

    public DbObject dbObject() {
        return dbObject;
    }

    public static String tabKey(String profileId, DbObject obj) {
        return "db:" + profileId + "/" + obj.key();
    }

    public static String tabKeyOf(String profileId, DbObject obj) {
        return tabKey(profileId, obj);
    }
}
