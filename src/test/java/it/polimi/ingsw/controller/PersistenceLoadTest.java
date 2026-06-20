package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.state.LobbyState;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Persistence.load(): verifies save-file deserialization,
 * missing-file and corrupted-file handling (controller persistence facade).
 */
class PersistenceLoadTest {

    @Test
    void loadReturnsEmptyWhenFileMissing(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("does-not-exist.bin");

        Optional<GameController> loaded = Persistence.load(nonExistent);

        assertTrue(loaded.isEmpty());
    }

    @Test
    void roundTripPreservesLobbyState(@TempDir Path tempDir) {
        Path target = tempDir.resolve("savestate.bin");

        GameController original = new GameController();
        original.onMessageReceived("Anna",
                new JoinGameAction(new Totem(TotemColor.RED, null)));
        original.onMessageReceived("Anna",
                new SetPlayerCountAction(2));

        Persistence.save(original, target);

        Optional<GameController> loaded = Persistence.load(target);

        assertTrue(loaded.isPresent(), "load() must return the saved controller");
        GameController restored = loaded.get();

        assertEquals(2, restored.getChosenPlayersCount());
        assertEquals(1, restored.getLobbyPlayers().size());
        assertEquals("Anna", restored.getLobbyPlayers().get(0).getNickname());
        assertTrue(restored.getCurrentState() instanceof LobbyState);
    }

    @Test
    void corruptedFileIsQuarantinedAndLoadReturnsEmpty(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("savestate.bin");
        // write garbage that is not a valid serialized object
        Files.writeString(target, "this is definitely not a serialized GameController");

        Optional<GameController> loaded = Persistence.load(target);

        assertTrue(loaded.isEmpty(), "corrupted file must yield empty result");
        assertFalse(Files.exists(target), "corrupted file must be moved to .bak");

        // verify a backup file appeared next to it
        try (var stream = Files.list(tempDir)) {
            boolean backupExists = stream.anyMatch(
                    p -> p.getFileName().toString().contains(".corrupted-"));
            assertTrue(backupExists, "corrupted file must be quarantined as .bak");
        }
    }
}