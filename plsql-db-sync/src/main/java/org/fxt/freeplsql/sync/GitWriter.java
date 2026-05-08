package org.fxt.freeplsql.sync;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin wrapper around JGit. Always operates on a working tree at {@code repoRoot}.
 */
public final class GitWriter {

    private final Path repoRoot;

    public GitWriter(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    /**
     * Initializes the repo if it doesn't exist, otherwise opens the existing one.
     */
    public Git initOrOpen() throws GitAPIException, IOException {
        Files.createDirectories(repoRoot);
        Path gitDir = repoRoot.resolve(".git");
        if (!Files.exists(gitDir)) {
            return Git.init()
                    .setDirectory(repoRoot.toFile())
                    .setInitialBranch("main")
                    .call();
        }
        return Git.open(repoRoot.toFile());
    }

    /** Writes the given content to {@code <repoRoot>/<relativePath>}, creating parent dirs. */
    public void writeFile(String relativePath, String content) throws IOException {
        Path target = repoRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    /**
     * Stages all changes and commits with the given message + author. Returns the commit hash,
     * or {@code null} if there is nothing to commit.
     */
    public String stageAndCommit(Git git, String message, PersonIdent author)
            throws GitAPIException {
        git.add().addFilepattern(".").call();
        var status = git.status().call();
        boolean dirty = !status.getAdded().isEmpty()
                || !status.getChanged().isEmpty()
                || !status.getRemoved().isEmpty()
                || !status.getModified().isEmpty()
                || !status.getMissing().isEmpty()
                || !status.getUntracked().isEmpty();
        if (!dirty) {
            return null;
        }
        // Re-stage to also pick up deletions
        git.add().addFilepattern(".").setUpdate(true).call();
        var commit = git.commit()
                .setAuthor(author)
                .setCommitter(author)
                .setMessage(message)
                .call();
        return commit.getName();
    }

    /**
     * Parses {@code "Name <email@host>"} into a {@link PersonIdent}. Falls back to a plain
     * "PLSQLSync" identity when the input isn't recognizable.
     */
    public static PersonIdent parseAuthor(String authorString) {
        Pattern p = Pattern.compile("^\\s*(.*?)\\s*<([^>]+)>\\s*$");
        Matcher m = p.matcher(authorString);
        if (m.matches()) {
            String name = m.group(1).isBlank() ? "PLSQLSync" : m.group(1);
            return new PersonIdent(name, m.group(2));
        }
        return new PersonIdent("PLSQLSync", "plsqlsync@example.com");
    }
}
