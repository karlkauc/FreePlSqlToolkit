package org.fxt.freeplsql.app.editor;

import javafx.application.Platform;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RichTextFX {@link CodeArea} that re-highlights its content asynchronously
 * after the user stops typing for ~150 ms.
 */
public final class PlSqlCodeArea extends CodeArea {

    private final PlSqlSyntaxHighlighter highlighter = new PlSqlSyntaxHighlighter();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "plsql-highlighter");
        t.setDaemon(true);
        return t;
    });

    public PlSqlCodeArea() {
        setParagraphGraphicFactory(LineNumberFactory.get(this));
        getStyleClass().add("plsql-code-area");

        multiPlainChanges()
                .successionEnds(Duration.ofMillis(150))
                .subscribe(ignore -> rehighlight());
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void rehighlight() {
        String text = getText();
        executor.execute(() -> {
            List<HighlightSpan> spans = highlighter.compute(text);
            StyleSpans<Collection<String>> styleSpans = toStyleSpans(spans, text.length());
            Platform.runLater(() -> {
                if (text.equals(getText())) {
                    setStyleSpans(0, styleSpans);
                }
            });
        });
    }

    private static StyleSpans<Collection<String>> toStyleSpans(List<HighlightSpan> spans, int textLength) {
        var builder = new StyleSpansBuilder<Collection<String>>();
        int cursor = 0;
        for (HighlightSpan span : spans) {
            if (span.start() < cursor) {
                continue; // overlapping or out-of-order tokens — skip
            }
            if (span.start() > cursor) {
                builder.add(Collections.emptyList(), span.start() - cursor);
            }
            builder.add(Collections.singletonList(span.cssClass()), span.length());
            cursor = span.start() + span.length();
        }
        if (cursor < textLength) {
            builder.add(Collections.emptyList(), textLength - cursor);
        }
        if (cursor == 0 && textLength == 0) {
            builder.add(Collections.emptyList(), 0);
        }
        return builder.create();
    }
}
