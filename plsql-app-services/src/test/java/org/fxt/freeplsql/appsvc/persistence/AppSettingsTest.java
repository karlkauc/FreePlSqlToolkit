package org.fxt.freeplsql.appsvc.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingsTest {

    @Test
    void defaultsAreLightTheme() {
        var settings = AppSettings.defaults();
        assertNotNull(settings);
        assertFalse(settings.isDark(), "defaults should be light theme");
    }

    @Test
    void darkFieldIsMutable() {
        var settings = AppSettings.defaults();
        settings.setDark(true);
        assertTrue(settings.isDark());
        settings.setDark(false);
        assertFalse(settings.isDark());
    }
}
