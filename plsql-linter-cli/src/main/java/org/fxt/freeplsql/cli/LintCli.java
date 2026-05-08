package org.fxt.freeplsql.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "plsqllint",
        mixinStandardHelpOptions = true,
        version = "FreePlSqlToolkit 0.1.0",
        description = "Lint PL/SQL source files against built-in coding rules.",
        subcommands = {LintCommand.class}
)
public final class LintCli implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new LintCli()).execute(args);
        System.exit(exitCode);
    }
}
