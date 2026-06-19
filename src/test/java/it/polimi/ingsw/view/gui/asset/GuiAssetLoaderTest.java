package it.polimi.ingsw.view.gui.asset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiAssetLoaderTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearConfiguredAssetRoot() {
        System.clearProperty(GuiAssetLoader.ASSETS_DIR_PROPERTY);
    }

    @Test
    void opensAssetFromConfiguredLocalDirectory() throws Exception {
        Path localAsset = tempDir.resolve("ui/test-asset.txt");
        Files.createDirectories(localAsset.getParent());
        Files.writeString(localAsset, "local asset", StandardCharsets.UTF_8);
        System.setProperty(GuiAssetLoader.ASSETS_DIR_PROPERTY, tempDir.toString());

        try (InputStream stream = GuiAssetLoader.open("assets/ui/test-asset.txt").orElseThrow()) {
            assertEquals("local asset", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertTrue(GuiAssetLoader.url("assets/ui/test-asset.txt").isPresent());
    }
}
