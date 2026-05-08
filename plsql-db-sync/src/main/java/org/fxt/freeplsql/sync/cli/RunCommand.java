package org.fxt.freeplsql.sync.cli;

import org.fxt.freeplsql.sync.SyncResult;
import org.fxt.freeplsql.sync.SyncService;
import org.fxt.freeplsql.sync.config.SyncConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "run", description = "Run sync on a fixed interval (loops until interrupted).")
public final class RunCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, description = "Path to plsqlsync.yaml", required = true)
    Path configFile;

    @Override
    public Integer call() throws Exception {
        SyncConfig config = SyncConfig.load(configFile);
        var service = new SyncService();
        long intervalMs = Math.max(1, config.schedule().intervalMinutes()) * 60_000L;
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.out.println("Stopping plsqlsync.")));
        while (!Thread.currentThread().isInterrupted()) {
            try {
                SyncResult result = service.syncOnce(config);
                if (result.hasChanges()) {
                    System.out.printf("[%s] %d file(s) updated, commit %s%n",
                            java.time.LocalDateTime.now(),
                            result.updatedRelativePaths().size(),
                            result.commitHash());
                } else {
                    System.out.printf("[%s] no changes (%d scanned)%n",
                            java.time.LocalDateTime.now(),
                            result.objectsScanned());
                }
            } catch (Exception e) {
                System.err.println("Sync failed: " + e.getMessage());
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        return 0;
    }
}
