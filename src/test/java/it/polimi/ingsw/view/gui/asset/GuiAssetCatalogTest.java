package it.polimi.ingsw.view.gui.asset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for GuiAssetCatalog (view.gui.asset).
 *
 * Verifies lookup of GUI image and style assets by key.
 */
class GuiAssetCatalogTest {

    @Test
    void loadsAllPlayableCardsFromManifest() {
        GuiAssetCatalog catalog = GuiAssetCatalog.loadDefault();

        assertEquals(117, catalog.getCards().size());
        assertTrue(catalog.findCard(1).isPresent());
        assertTrue(catalog.findCard(85).isPresent());
        assertTrue(catalog.findCard(117).isPresent());
        assertTrue(catalog.findCard(118).isEmpty());
    }

    @Test
    void resolvesSemanticBacksByCardTypeAndEra() {
        GuiAssetCatalog catalog = GuiAssetCatalog.loadDefault();

        assertEquals("assets/cards/backs/back_character_era_i.png",
                catalog.getCard(1).getBackPath());
        assertEquals("assets/cards/backs/back_character_era_ii.png",
                catalog.getCard(30).getBackPath());
        assertEquals("assets/cards/backs/back_character_era_iii.png",
                catalog.getCard(58).getBackPath());
        assertEquals("assets/cards/backs/back_event_final.png",
                catalog.getCard(95).getBackPath());
        assertEquals("assets/cards/backs/back_building_era_iii.png",
                catalog.getCard(117).getBackPath());
    }

    @Test
    void keepsWorkingWhenImagesAreOnlyLocalOptionalResources() {
        GuiAssetCatalog catalog = GuiAssetCatalog.loadDefault();
        CardAsset card = catalog.getCard(1);

        assertEquals("assets/cards/characters/card_001.png", card.getFrontPath());
        assertFalse(catalog.placeholderLabelFor(card).isBlank());
    }
}
