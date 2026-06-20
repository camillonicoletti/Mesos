package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CardDescriptionService (view.gui.component).
 *
 * Verifies human-readable description of cards for the GUI layer.
 */
class CardDescriptionServiceTest {

    private final CardDescriptionService descriptions =
            new CardDescriptionService(GuiAssetCatalog.loadDefault());

    @Test
    void describesCharacterWithoutDependingOnGameBoardView() {
        List<String> lines = descriptions.describeCard(1, -1, null);

        assertTrue(lines.contains("Hunters"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("immediately gain 1 food")));
    }

    @Test
    void describesBuildingEffectAndSummaryLabel() {
        List<String> lines = descriptions.describeCard(109, -1, null);

        assertEquals("End PP full set", lines.get(0));
        assertEquals("End PP full set", descriptions.summaryLabelFor(109));
        assertTrue(lines.stream().anyMatch(line -> line.contains("gain 6 PP")));
    }

    @Test
    void appendsLastResolvedEventDetailsOnlyForMatchingCard() {
        List<String> lines = descriptions.describeCard(85, 85, "Alice guadagna 2 PP");

        assertEquals("Event - Hunt", lines.get(0));
        assertTrue(lines.contains("Last resolved effect: Alice guadagna 2 PP"));
    }
}
