package org.fxt.freeplsql.app.lint;

import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintEngine;
import org.fxt.freeplsql.lint.LintRule;
import org.fxt.freeplsql.parser.PlSqlParserService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Runs the linter on a background thread; only the result of the latest
 * submission is delivered to the callback (older runs are dropped).
 */
public final class LiveLintService {

    private final PlSqlParserService parser = new PlSqlParserService();
    private final LintEngine engine;
    private final ExecutorService executor;
    private final AtomicLong sequence = new AtomicLong();

    public LiveLintService(List<LintRule> rules, ExecutorService executor) {
        this.engine = new LintEngine(rules);
        this.executor = executor;
    }

    public void lint(String source, Consumer<List<Issue>> callback) {
        long mySeq = sequence.incrementAndGet();
        executor.execute(() -> {
            try {
                List<Issue> issues = engine.run(parser.parse(source), "buffer.sql");
                if (mySeq == sequence.get()) {
                    callback.accept(issues);
                }
            } catch (RuntimeException ex) {
                if (mySeq == sequence.get()) {
                    callback.accept(List.of());
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
