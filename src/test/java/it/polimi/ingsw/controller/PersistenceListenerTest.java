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
 * Verifies that PersistenceListener correctly drives the Persistence facade
 * in response to controller lifecycle events.
 *
 * Persistence resolves its save file against the JVM working directory
 * (Path.of("mesos-savestate.bin")). The JDK caches that directory at startup,
 * so it cannot be redirected to a temp directory without changing production
 * code. The test writes to the real working directory instead and guarantees
 * cleanup via the cleanup() method, run both before and after each test.
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
