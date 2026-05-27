package it.polimi.ingsw.view.tui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity checks on the TUI style constants and offer-tile tables.
 * These guard against accidental edits that would silently shift the layout
 * or break the offer-tile presentation contract consumed by Renderer.
 */
class TuiStyleTest {

    @Test
    void ansiResetIsTheStandardCsiZero() {
        assertEquals("\033[0m", TuiStyle.RST);
    }

    @Test
    void cardDimensionsMatchRendererExpectations() {
        // Renderer hardcodes CARD_H lines per card and CARD_W cells per card
        // in many places; bumping these silently would break every card row.
        assertEquals(10, TuiStyle.CARD_H);
        assertEquals(16, TuiStyle.CARD_W);
        assertTrue(TuiStyle.CARD_GAP >= 1);
    }

    @Test
    void mainPanelWidthIs140() {
        // W = 140 is the screen-wide reference used by header/footer and
        // every centerVisual(...) call site.
        assertEquals(140, TuiStyle.W);
    }

    @Test
    void tileOrderContainsSevenTilesInAlphabeticOrder() {
        assertEquals(7, TuiStyle.TILE_ORDER.size());
        assertEquals('A', TuiStyle.TILE_ORDER.get(0));
        assertEquals('G', TuiStyle.TILE_ORDER.get(6));
    }

    @Test
    void tileArrowsAndTileMinHaveSameKeyset() {
        assertEquals(TuiStyle.TILE_ARROWS.keySet(), TuiStyle.TILE_MIN.keySet());
    }

    @Test
    void tileMinIsBoundedTo2_5() {
        for (Integer v : TuiStyle.TILE_MIN.values()) {
            assertNotNull(v);
            assertTrue(v >= 2 && v <= 5, "TILE_MIN entry out of range: " + v);
        }
    }

    @Test
    void tileArrowsArePairs() {
        for (String[] arrows : TuiStyle.TILE_ARROWS.values()) {
            assertEquals(2, arrows.length,
                    "TILE_ARROWS entry must be a length-2 array (primary, secondary)");
        }
    }

    @Test
    void ansiConstantsAreNonEmpty() {
        // A regression where a color collapsed to "" would silently strip
        // styling without breaking compilation. Catch it here.
        for (String s : new String[]{TuiStyle.RST, TuiStyle.BOLD, TuiStyle.DIM,
                TuiStyle.CYN, TuiStyle.YLW, TuiStyle.GRN, TuiStyle.RED,
                TuiStyle.MGN, TuiStyle.BLU, TuiStyle.ORG, TuiStyle.WHT,
                TuiStyle.GRY, TuiStyle.BLINK}) {
            assertFalse(s.isEmpty());
            assertTrue(s.startsWith("\033["));
        }
    }
}
