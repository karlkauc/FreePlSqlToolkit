package org.fxt.freeplsql.cli.output;

import org.fxt.freeplsql.lint.Issue;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ReportWriter {

    void write(Map<Path, List<Issue>> resultsByFile, PrintStream out);
}
