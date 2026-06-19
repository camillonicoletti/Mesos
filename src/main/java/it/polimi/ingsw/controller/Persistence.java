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

// Persists and restores the GameController to/from disk so a running match can survive a server crash.
// Writes are atomic (write-to-temp + rename) to prevent half-written files. If the on-disk file is
// unreadable on load, it is renamed with a .corrupted-<timestamp>.bak suffix (preserved for inspection)
// and the server restarts from scratch. Stateless and thread-safe: every method synchronizes on the class lock.
public final class Persistence {

    private static final String SAVE_FILE_NAME = "mesos-savestate.bin";
    private static final DateTimeFormatter CORRUPTED_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Persistence() {
        // utility class, no instances
    }

    // ── Public API ──────────────────────────────────────────────────────────

    // Atomically writes the given controller to the default save file. Failures are logged to
    // stderr but never thrown: a failed save must not crash the running game.
    public static synchronized void save(GameController controller) {
        save(controller, Path.of(SAVE_FILE_NAME));
    }

    // Reads the controller previously saved to the default save file; empty if missing or corrupted.
    public static synchronized Optional<GameController> load() {
        return load(Path.of(SAVE_FILE_NAME));
    }

    // Deletes the default save file, if present. Called when the match ends normally so the
    // next server start begins with a clean slate.
    public static synchronized void clear() {
        clear(Path.of(SAVE_FILE_NAME));
    }

    // ── Package-private API (for tests) ─────────────────────────────────────

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