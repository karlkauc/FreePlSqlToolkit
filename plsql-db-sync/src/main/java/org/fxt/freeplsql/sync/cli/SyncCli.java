package org.fxt.freeplsql.sync.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "plsqlsync",
        mixinStandardHelpOptions = true,
        version = "FreePlSqlToolkit 0.1.0",
        description = "Sync Oracle PL/SQL source from a database to a Git repository.",
        subcommands = {InitCommand.class, OnceCommand.class, RunCommand.class}
)
public final class SyncCli implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SyncCli()).execute(args);
        System.exit(exitCode);
    }
}
