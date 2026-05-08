package org.fxt.freeplsql.sync;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitWriterTest {

    @Test
    void initsRepoAndCommitsFile(@TempDir Path tempDir) throws Exception {
        var writer = new GitWriter(tempDir);
        try (Git git = writer.initOrOpen()) {
            writer.writeFile("hr/procedure/p_demo.sql", "BEGIN NULL; END;");
            String hash = writer.stageAndCommit(git, "first commit",
                    new PersonIdent("Test", "test@example.com"));
            assertNotNull(hash);
            assertTrue(Files.exists(tempDir.resolve("hr/procedure/p_demo.sql")));

            List<String> messages = new ArrayList<>();
            git.log().call().forEach(c -> messages.add(c.getShortMessage()));
            assertEquals(1, messages.size());
            assertEquals("first commit", messages.get(0));
        }
    }

    @Test
    void noopCommitReturnsNull(@TempDir Path tempDir) throws Exception {
        var writer = new GitWriter(tempDir);
        try (Git git = writer.initOrOpen()) {
            String first = writer.stageAndCommit(git, "first", new PersonIdent("T", "t@x"));
            // Initial empty repo: nothing staged → null
            assertNull(first);
        }
    }

    @Test
    void parseAuthorHandlesNameAndEmail() {
        var ident = GitWriter.parseAuthor("Alice Example <alice@example.com>");
        assertEquals("Alice Example", ident.getName());
        assertEquals("alice@example.com", ident.getEmailAddress());
    }

    @Test
    void parseAuthorFallsBack() {
        var ident = GitWriter.parseAuthor("not-a-valid-author-string");
        assertEquals("PLSQLSync", ident.getName());
    }
}
