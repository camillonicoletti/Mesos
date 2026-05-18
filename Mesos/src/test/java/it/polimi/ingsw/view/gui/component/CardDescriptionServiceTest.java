package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardDescriptionServiceTest {

    private final CardDescriptionService descriptions =
            new CardDescriptionService(GuiAssetCatalog.loadDefault());

    @Test
    void describesCharacterWithoutDependingOnGameBoardView() {
        List<String> lines = descriptions.describeCard(1, -1, null);

        assertTrue(lines.contains("Personaggio - Cacciatori | Era I"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("ottieni subito 1 cibo")));
    }

    @Test
    void describesBuildingEffectAndSummaryLabel() {
        List<String> lines = descriptions.describeCard(109, -1, null);

        assertEquals("PP finali da set completo", descriptions.summaryLabelFor(109));
        assertTrue(lines.stream().anyMatch(line -> line.contains("6 PP per ogni set completo")));
    }

    @Test
    void appendsLastResolvedEventDetailsOnlyForMatchingCard() {
        List<String> lines = descriptions.describeCard(85, 85, "Alice guadagna 2 PP");

        assertTrue(lines.contains("Ultimo effetto risolto: Alice guadagna 2 PP"));
    }
}
