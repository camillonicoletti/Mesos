package it.polimi.ingsw.controller.listener;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.Persistence;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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