package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.listener.PersistenceListener;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link PersistenceListener} correctly drives the
 * {@link Persistence} facade in response to controller lifecycle events.
 *
 * <p>Components under test: {@link PersistenceListener} (controller layer)
 * and its interaction with the {@link Persistence} save-file API.
 *
 * <p>Note on isolation: {@link Persistence} resolves its save file against the
 * JVM working directory through {@code Path.of("mesos-savestate.bin")}. Because
 * the JDK caches the working directory at startup, neither {@code user.dir}
 * nor a JUnit {@code @TempDir} can redirect it without changing production
 * code. The test therefore writes to the working directory and guarantees
 * cleanup via {@code @BeforeEach} and {@code @AfterEach}. Migrating this to
 * a real {@code @TempDir} requires exposing a path-aware overload on
 * {@link PersistenceListener} and is tracked as a P1 cleanup item.
 */
class PersistenceListenerTest {

    private static final Path SAVE_FILE = Path.of("mesos-savestate.bin");

    @BeforeEach
    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(SAVE_FILE);
    }

    @Test
    void onStateChangedTriggersSave() {
        GameController controller = new GameController();
        PersistenceListener listener = new PersistenceListener(controller);

        listener.onStateChanged(new TotemPlacementState());

        assertTrue(Files.exists(SAVE_FILE),
                "save file must be created on state change");
    }

    @Test
    void onGameEndedTriggersClear() throws Exception {
        GameController controller = new GameController();
        Persistence.save(controller); // create the file first
        assertTrue(Files.exists(SAVE_FILE), "precondition: file must exist");

        new PersistenceListener(controller).onGameEnded(null, java.util.Map.of());

        assertFalse(Files.exists(SAVE_FILE),
                "save file must be deleted on game end");
    }
}
