package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CardDescriptionService (view.gui.component).
 *
 * <p>Verifies: human-readable description of cards for the GUI layer.
 */
class CardDescriptionServiceTest {

    private final CardDescriptionService descriptions =
            new CardDescriptionService(GuiAssetCatalog.loadDefault());

    @Test
    void describesCharacterWithoutDependingOnGameBoardView() {
        List<String> lines = descriptions.describeCard(1, -1, null);

        assertTrue(lines.contains("Character - Hunters | Era I"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("immediately gain 1 food")));
    }

    @Test
    void describesBuildingEffectAndSummaryLabel() {
        List<String> lines = descriptions.describeCard(109, -1, null);

        assertEquals("End PP full set", descriptions.summaryLabelFor(109));
        assertTrue(lines.stream().anyMatch(line -> line.contains("gain 6 PP")));
    }

    @Test
    void appendsLastResolvedEventDetailsOnlyForMatchingCard() {
        List<String> lines = descriptions.describeCard(85, 85, "Alice guadagna 2 PP");

        assertTrue(lines.contains("Last resolved effect: Alice guadagna 2 PP"));
    }
}
