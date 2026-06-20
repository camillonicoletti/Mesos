package it.polimi.ingsw.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Persistence.clear(): verifies save-file deletion
 * (controller persistence facade).
 */
class PersistenceClearTest {

    @Test
    void clearRemovesExistingFile(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("savestate.bin");
        Persistence.save(new GameController(), target);
        assertTrue(Files.exists(target), "precondition: file must exist before clear()");

        Persistence.clear(target);

        assertFalse(Files.exists(target));
    }

    @Test
    void clearIsNoOpWhenFileMissing(@TempDir Path tempDir) {
        Path target = tempDir.resolve("never-existed.bin");

        // must not throw
        Persistence.clear(target);

        assertFalse(Files.exists(target));
    }
}