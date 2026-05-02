package it.polimi.ingsw.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceSaveTest {

    @Test
    void saveCreatesFileOnDisk(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("savestate.bin");
        GameController controller = new GameController();

        Persistence.save(controller, target);

        assertTrue(Files.exists(target), "save file must exist after save()");
        assertTrue(Files.size(target) > 0, "save file must not be empty");
    }

    @Test
    void saveOverwritesExistingFile(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("savestate.bin");
        GameController controller = new GameController();

        Persistence.save(controller, target);
        long firstSize = Files.size(target);

        Persistence.save(controller, target);
        long secondSize = Files.size(target);

        assertTrue(Files.exists(target));
        // sizes are similar but the file was rewritten
        assertTrue(secondSize > 0);
        // and no leftover .tmp
        assertFalse(Files.exists(tempDir.resolve("savestate.bin.tmp")),
                "temporary file must be cleaned up");
    }
}