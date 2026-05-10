package it.polimi.ingsw.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Persists and restores the {@link GameController} to and from disk so that
 * a running match can survive a server crash.
 *
 * <p>The state is written to {@value #SAVE_FILE_NAME} in the server's working
 * directory. Writes are atomic (write-to-temp + rename) to prevent half-written
 * files. If the on-disk file is unreadable on load, it is renamed with a
 * {@code .corrupted-<timestamp>.bak} suffix and the server restarts from
 * scratch, preserving the bad file for inspection.
 *
 * <p>This class is stateless and thread-safe: every method is synchronized on
 * the class lock to serialize concurrent saves.
 *
 * <p>The package-private overloads accept an explicit target {@link Path} and
 * are intended for unit tests, which use a temporary directory to avoid
 * polluting the working directory.
 */
public final class Persistence {

    private static final String SAVE_FILE_NAME = "mesos-savestate.bin";
    private static final DateTimeFormatter CORRUPTED_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Persistence() {
        // utility class, no instances
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Atomically writes the given controller to the default save file.
     * Failures are logged to stderr but never thrown: a failed save must not
     * crash the running game.
     *
     * @param controller the controller to persist; must not be null
     */
    public static synchronized void save(GameController controller) {
        save(controller, Path.of(SAVE_FILE_NAME));
    }

    /**
     * Reads the controller previously saved to the default save file, if any.
     * If the file is missing returns {@link Optional#empty()}.
     * If the file exists but is unreadable, the bad file is renamed with a
     * {@code .corrupted-<timestamp>.bak} suffix and {@link Optional#empty()}
     * is returned.
     *
     * @return the restored controller, or empty if no usable save is present
     */
    public static synchronized Optional<GameController> load() {
        return load(Path.of(SAVE_FILE_NAME));
    }

    /**
     * Deletes the default save file, if present. Silent no-op when the file
     * does not exist. Intended to be called when the match ends normally so
     * that the next server start begins with a clean slate.
     */
    public static synchronized void clear() {
        clear(Path.of(SAVE_FILE_NAME));
    }

    // ── Package-private API (for tests) ─────────────────────────────────────

    /**
     * Path-aware variant of {@link #save(GameController)}. Used by unit tests
     * that target a temporary directory.
     *
     * @param controller the controller to persist; must not be null
     * @param target     the destination file
     */
    static synchronized void save(GameController controller, Path target) {
        if (controller == null) {
            throw new IllegalArgumentException("Controller cannot be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target path cannot be null");
        }

        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");

        try (ObjectOutputStream out = new ObjectOutputStream(
                Files.newOutputStream(tempFile))) {
            out.writeObject(controller);
            out.flush();
        } catch (IOException exception) {
            System.err.println("[Persistence] save failed (write phase): "
                    + exception.getMessage());
            tryDelete(tempFile);
            return;
        }

        try {
            Files.move(tempFile, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException atomicException) {
            // fallback for filesystems that don't support atomic moves
            try {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackException) {
                System.err.println("[Persistence] save failed (rename phase): "
                        + fallbackException.getMessage());
                tryDelete(tempFile);
            }
        } catch (IOException exception) {
            System.err.println("[Persistence] save failed (rename phase): "
                    + exception.getMessage());
            tryDelete(tempFile);
        }
    }

    /**
     * Path-aware variant of {@link #load()}. Used by unit tests that target
     * a temporary directory.
     *
     * @param source the file to read
     * @return the restored controller, or empty if missing or corrupted
     */
    static synchronized Optional<GameController> load(Path source) {
        if (source == null) {
            throw new IllegalArgumentException("Source path cannot be null");
        }
        if (!Files.exists(source)) {
            return Optional.empty();
        }

        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(source))) {
            Object read = in.readObject();
            if (read instanceof GameController controller) {
                // Re-wire the transient Game.notifier that serialization stripped:
                // without this, every model-emitted event after restore (food/PP
                // changes from takeBuildingCard, slot effects, era changes...)
                // would be silently lost.
                if (controller.getGame() != null) {
                    controller.getGame().setNotifier(controller::notifyListeners);
                }
                return Optional.of(controller);
            }
            System.err.println("[Persistence] save file does not contain a "
                    + "GameController, ignoring it");
            quarantineCorruptedFile(source, "wrong-type");
            return Optional.empty();
        } catch (IOException | ClassNotFoundException exception) {
            System.err.println("[Persistence] save file is corrupted: "
                    + exception.getMessage() + " — quarantining");
            quarantineCorruptedFile(source, exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Path-aware variant of {@link #clear()}. Used by unit tests that target
     * a temporary directory.
     *
     * @param target the file to delete
     */
    static synchronized void clear(Path target) {
        if (target == null) {
            throw new IllegalArgumentException("Target path cannot be null");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            System.err.println("[Persistence] clear failed: "
                    + exception.getMessage());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void quarantineCorruptedFile(Path source, String reasonHint) {
        String timestamp = LocalDateTime.now().format(CORRUPTED_TIMESTAMP);
        String backupName = source.getFileName()
                + ".corrupted-" + timestamp + "-" + reasonHint + ".bak";
        Path backup = source.resolveSibling(backupName);
        try {
            Files.move(source, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveException) {
            // last resort: just delete the bad file
            System.err.println("[Persistence] could not quarantine corrupted file: "
                    + moveException.getMessage() + " — deleting it");
            tryDelete(source);
        }
    }

    private static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }
}