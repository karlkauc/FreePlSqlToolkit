package org.fxt.freeplsql.app.ui.editor;

import javafx.application.Platform;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintEngine;
import org.fxt.freeplsql.parser.PlSqlParserService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Runs the linter in the background. Newer submissions supersede older ones —
 * only the latest result is delivered to the consumer (always on FX thread).
 */
public final class LiveLintBinding {

    private final PlSqlParserService parser = new PlSqlParserService();
    private final LintEngine engine;
    private final ExecutorService executor;
    private final AtomicLong sequence = new AtomicLong();
    private final String sourcePath;
    private final Consumer<List<Issue>> consumer;

    public LiveLintBinding(LintEngine engine, String sourcePath, Consumer<List<Issue>> consumer) {
        this.engine = engine;
        this.sourcePath = sourcePath;
        this.consumer = consumer;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "fpl-livelint");
            t.setDaemon(true);
            return t;
        });
    }

    public void lint(String source) {
        long mySeq = sequence.incrementAndGet();
        executor.execute(() -> {
            List<Issue> result;
            try {
                result = engine.run(parser.parse(source), sourcePath);
            } catch (RuntimeException e) {
                result = List.of();
            }
            if (mySeq == sequence.get()) {
                final List<Issue> finalResult = result;
                Platform.runLater(() -> consumer.accept(finalResult));
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
