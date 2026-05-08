package org.fxt.freeplsql.sync.cli;

import org.fxt.freeplsql.sync.SyncResult;
import org.fxt.freeplsql.sync.SyncService;
import org.fxt.freeplsql.sync.config.SyncConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "once", description = "Run a single sync pass and exit.")
public final class OnceCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, description = "Path to plsqlsync.yaml", required = true)
    Path configFile;

    @Override
    public Integer call() throws Exception {
        SyncConfig config = SyncConfig.load(configFile);
        var service = new SyncService();
        SyncResult result = service.syncOnce(config);
        System.out.printf("Scanned %d object(s); wrote %d file(s)%n",
                result.objectsScanned(), result.updatedRelativePaths().size());
        if (result.commitHash() != null) {
            System.out.println("Commit: " + result.commitHash());
        } else {
            System.out.println("No changes to commit.");
        }
        return 0;
    }
}
