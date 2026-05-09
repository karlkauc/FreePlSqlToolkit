package org.fxt.freeplsql.app.ui.tabs;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.fxt.freeplsql.app.ui.editor.EditorTab;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Opens tabs by stable key, focusing existing tabs instead of duplicating. */
public final class TabManager {

    private final TabPane tabs;
    private final Map<String, Tab> byKey = new HashMap<>();

    public TabManager(TabPane tabs) {
        this.tabs = tabs;
    }

    public void openOrFocus(String key, Supplier<Tab> factory) {
        Tab existing = byKey.get(key);
        if (existing != null) {
            tabs.getSelectionModel().select(existing);
            return;
        }
        Tab tab = factory.get();
        EventHandler<Event> previous = tab.getOnClosed();
        tab.setOnClosed(ev -> {
            if (previous != null) previous.handle(ev);
            byKey.remove(key);
        });
        byKey.put(key, tab);
        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
    }

    /** Programmatically closes every tab owned by the given profile (e.g. on disconnect). */
    public void closeAllForProfile(String profileId) {
        var prefix = "db:" + profileId + "/";
        for (Map.Entry<String, Tab> e : Map.copyOf(byKey).entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                tabs.getTabs().remove(e.getValue());
                if (e.getValue() instanceof EditorTab editor) {
                    editor.dispose();
                }
                byKey.remove(e.getKey());
            }
        }
    }

    public TabPane tabPane() {
        return tabs;
    }
}
