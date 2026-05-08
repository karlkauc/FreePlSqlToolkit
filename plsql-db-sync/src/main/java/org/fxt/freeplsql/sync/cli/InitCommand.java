package org.fxt.freeplsql.sync.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "init", description = "Write a starter plsqlsync.yaml in the current directory.")
public final class InitCommand implements Callable<Integer> {

    @Option(names = {"-o", "--output"}, description = "Target file (default: ./plsqlsync.yaml)",
            defaultValue = "plsqlsync.yaml")
    Path output;

    @Override
    public Integer call() throws Exception {
        if (Files.exists(output)) {
            System.err.println("Refusing to overwrite existing " + output);
            return 1;
        }
        String template = """
                connection:
                  url: jdbc:oracle:thin:@localhost:1521/XEPDB1
                  user: ${DB_USER}
                  password: ${DB_PASSWORD}
                schemas:
                  - HR
                output:
                  repo: ./plsql-source
                  branch: main
                  commitAuthor: "PLSQLSync <plsqlsync@example.com>"
                  push: false
                schedule:
                  intervalMinutes: 5
                """;
        Files.writeString(output, template);
        System.out.println("Wrote " + output);
        return 0;
    }
}
