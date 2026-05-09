package org.fxt.freeplsql.appsvc.lint;

/** Lightweight progress + cancel hook handed to long-running services. */
public interface ProgressSink {

    void update(int current, int total, String message);

    boolean isCancelled();

    static ProgressSink noop() {
        return new ProgressSink() {
            @Override public void update(int current, int total, String message) {}
            @Override public boolean isCancelled() { return false; }
        };
    }
}
