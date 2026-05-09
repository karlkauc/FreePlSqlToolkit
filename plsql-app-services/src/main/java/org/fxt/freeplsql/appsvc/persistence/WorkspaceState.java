package org.fxt.freeplsql.appsvc.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted UI state — window geometry, sidebar layout, list of locally-open
 * editor tabs. Mutable POJO so the FX-thread can update fields and save.
 */
public final class WorkspaceState {

    private double windowX = -1;
    private double windowY = -1;
    private double windowWidth = 1280;
    private double windowHeight = 800;
    private boolean maximized = false;
    private List<String> openLocalFiles = new ArrayList<>();

    public double getWindowX() { return windowX; }
    public void setWindowX(double v) { this.windowX = v; }

    public double getWindowY() { return windowY; }
    public void setWindowY(double v) { this.windowY = v; }

    public double getWindowWidth() { return windowWidth; }
    public void setWindowWidth(double v) { this.windowWidth = v; }

    public double getWindowHeight() { return windowHeight; }
    public void setWindowHeight(double v) { this.windowHeight = v; }

    public boolean isMaximized() { return maximized; }
    public void setMaximized(boolean v) { this.maximized = v; }

    public List<String> getOpenLocalFiles() { return openLocalFiles; }
    public void setOpenLocalFiles(List<String> files) {
        this.openLocalFiles = files == null ? new ArrayList<>() : new ArrayList<>(files);
    }

    public static WorkspaceState defaults() {
        return new WorkspaceState();
    }
}
