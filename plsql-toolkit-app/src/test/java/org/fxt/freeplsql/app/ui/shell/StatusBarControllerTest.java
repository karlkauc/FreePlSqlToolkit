package org.fxt.freeplsql.app.ui.shell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatusBarControllerTest {

    @Test
    void buildsConnectionSummaryForZeroActive() {
        assertEquals("● no connection", StatusBarController.connectionSummary(0, null));
    }

    @Test
    void buildsConnectionSummaryForOneActive() {
        assertEquals("● prod_ora19", StatusBarController.connectionSummary(1, "prod_ora19"));
    }

    @Test
    void buildsConnectionSummaryForMultipleActive() {
        assertEquals("● 3 connected", StatusBarController.connectionSummary(3, "prod_ora19"));
    }

    @Test
    void caretLabelFormat() {
        assertEquals("Ln 42, Col 7", StatusBarController.caretLabel(41, 6));
        assertEquals("Ln 1, Col 1",  StatusBarController.caretLabel(0, 0));
    }
}
