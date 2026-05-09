package org.fxt.freeplsql.appsvc.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Generic Jackson-backed JSON store with atomic write (temp file + rename).
 * Not thread-safe; callers serialise access on a single thread (FX-Thread for
 * UI-driven settings).
 */
public final class JsonStore<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path path;
    private final Class<T> type;

    public JsonStore(Path path, Class<T> type) {
        this.path = path;
        this.type = type;
    }

    public Path path() {
        return path;
    }

    public Optional<T> load() throws IOException {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(MAPPER.readValue(path.toFile(), type));
    }

    /**
     * Loads the file. If it does not exist or cannot be parsed, returns the
     * fallback. Use only for non-critical state where corruption is recoverable
     * (settings, workspace) — not for credentials.
     */
    public T loadOrDefault(T fallback) {
        try {
            return load().orElse(fallback);
        } catch (IOException e) {
            return fallback;
        }
    }

    public void save(T value) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), value);
            try {
                Files.move(tmp, path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + path, e);
        }
    }
}
