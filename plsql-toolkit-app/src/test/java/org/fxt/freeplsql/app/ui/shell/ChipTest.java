package org.fxt.freeplsql.app.ui.shell;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class ChipTest {

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException ignored) {
            // Platform already initialised — that is fine
        }
    }

    @Test
    void warnChipHasWarnAndChipStyleClass() {
        Label chip = Chip.warn("3");
        assertEquals("3", chip.getText());
        assertTrue(chip.getStyleClass().contains("chip"));
        assertTrue(chip.getStyleClass().contains("chip-warn"));
    }

    @Test
    void dangerChipHasDangerStyleClass() {
        assertTrue(Chip.danger("2").getStyleClass().contains("chip-danger"));
    }

    @Test
    void successChipHasSuccessStyleClass() {
        assertTrue(Chip.success("OK").getStyleClass().contains("chip-success"));
    }

    @Test
    void mutedChipFallback() {
        assertTrue(Chip.muted("x").getStyleClass().contains("chip-muted"));
    }
}
