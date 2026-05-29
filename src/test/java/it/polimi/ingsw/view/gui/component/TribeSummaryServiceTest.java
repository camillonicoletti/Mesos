package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for TribeSummaryService (view.gui.component).
 *
 * <p>Verifies: GUI-facing summary of a tribe state.
 */
class TribeSummaryServiceTest {

    private final TribeSummaryService summaries =
            new TribeSummaryService(GuiAssetCatalog.loadDefault());

    @Test
    void computesCharacterCountersAndDerivedValues() {
        PlayerView player = new PlayerView(
                "milo",
                TotemColor.BLUE,
                0,
                0,
                List.of(1, 6, 7, 10, 11, 14, 19, 20, 22, 27, 28),
                List.of()
        );

        TribeSummaryStats stats = summaries.statsFor(player);

        assertEquals(2, stats.gatherers());
        assertEquals(6, stats.gathererFoodDiscount());
        assertEquals(1, stats.hunters());
        assertEquals(2, stats.builders());
        assertEquals(3, stats.builderFoodDiscount());
        assertEquals(2, stats.shamans());
        assertEquals(3, stats.shamanStars());
        assertEquals(1, stats.artists());
        assertEquals(3, stats.inventors());
        assertEquals(2, stats.distinctInventorIcons());
    }
}
