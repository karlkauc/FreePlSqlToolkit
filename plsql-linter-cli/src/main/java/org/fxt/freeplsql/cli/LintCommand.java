package org.fxt.freeplsql.cli;

import org.fxt.freeplsql.cli.output.JsonReportWriter;
import org.fxt.freeplsql.cli.output.ReportWriter;
import org.fxt.freeplsql.cli.output.SarifReportWriter;
import org.fxt.freeplsql.cli.output.TextReportWriter;
import org.fxt.freeplsql.lint.Issue;
import org.fxt.freeplsql.lint.LintEngine;
import org.fxt.freeplsql.lint.LintRule;
import org.fxt.freeplsql.lint.RuleLoader;
import org.fxt.freeplsql.lint.Severity;
import org.fxt.freeplsql.parser.ParseResult;
import org.fxt.freeplsql.parser.PlSqlParserService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
        name = "check",
        description = "Run lint checks against PL/SQL source files."
)
public final class LintCommand implements Callable<Integer> {

    enum Format { text, json, sarif }

    @Parameters(arity = "1..*", description = "Files or directories to scan.")
    List<Path> inputs;

    @Option(names = {"-f", "--format"}, description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})", defaultValue = "text")
    Format format;

    @Option(names = {"-o", "--output"}, description = "Output file (default: stdout).")
    Path output;

    @Option(names = "--fail-on", description = "Minimum severity that causes a non-zero exit code: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})", defaultValue = "ERROR")
    Severity failOn;

    @Option(names = {"-r", "--rules"}, description = "YAML file with custom rules.")
    Path rulesFile;

    @Override
    public Integer call() throws Exception {
        var parser = new PlSqlParserService();
        List<LintRule> rules = RuleLoader.loadAll(
                rulesFile,
                Thread.currentThread().getContextClassLoader()
        );
        var engine = new LintEngine(rules);

        var sources = collectSources(inputs);
        var resultsByFile = new LinkedHashMap<Path, List<Issue>>();

        for (Path file : sources) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            ParseResult parsed = parser.parse(content);
            List<Issue> issues = engine.run(parsed, file.toString());
            resultsByFile.put(file, issues);
        }

        ReportWriter writer = switch (format) {
            case text -> new TextReportWriter();
            case json -> new JsonReportWriter();
            case sarif -> new SarifReportWriter();
        };

        try (var out = openOutput()) {
            writer.write(resultsByFile, out);
        }

        return shouldFail(resultsByFile) ? 1 : 0;
    }

    private boolean shouldFail(Map<Path, List<Issue>> results) {
        return results.values().stream()
                .flatMap(List::stream)
                .anyMatch(issue -> issue.severity().ordinal() >= failOn.ordinal());
    }

    private PrintStream openOutput() throws IOException {
        if (output == null) {
            return new PrintStream(new UnclosableOutputStream(System.out), true, StandardCharsets.UTF_8);
        }
        Files.createDirectories(output.toAbsolutePath().getParent());
        return new PrintStream(Files.newOutputStream(output), true, StandardCharsets.UTF_8);
    }

    private List<Path> collectSources(List<Path> inputs) throws IOException {
        var collected = new ArrayList<Path>();
        for (Path input : inputs) {
            if (Files.isDirectory(input)) {
                try (Stream<Path> walk = Files.walk(input)) {
                    walk.filter(Files::isRegularFile)
                            .filter(LintCommand::isSqlFile)
                            .sorted()
                            .forEach(collected::add);
                }
            } else if (Files.isRegularFile(input)) {
                collected.add(input);
            } else {
                throw new IOException("Path does not exist: " + input);
            }
        }
        return collected;
    }

    private static boolean isSqlFile(Path path) {
        var name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".sql") || name.endsWith(".pkb") || name.endsWith(".pks") || name.endsWith(".plsql");
    }

    private static final class UnclosableOutputStream extends OutputStream {
        private final OutputStream delegate;

        UnclosableOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.flush();
        }
    }
}
