package it.polimi.ingsw.view.tui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralizes the styling primitives used by the TUI: ANSI escape codes,
 * fixed layout dimensions (panel/card widths and heights) and the offer-tile
 * presentation tables. All members are constants — no behavior lives here.
 */
final class TuiStyle {

    private TuiStyle() { }

    static final String RST  = "\033[0m";
    static final String BLINK = "\033[5m";
    static final String BOLD = "\033[1m";
    static final String DIM  = "\033[2m";
    static final String CYN  = "\033[96m";
    static final String YLW  = "\033[93m";
    static final String GRN  = "\033[92m";
    static final String RED  = "\033[91m";
    static final String MGN  = "\033[95m";
    static final String BLU  = "\033[94m";
    static final String ORG  = "\033[38;5;208m";
    static final String WHT  = "\033[97m";
    static final String GRY  = "\033[90m";

    static final int W = 140;
    static final int CARD_W   = 16;
    static final int CARD_H   = 10;
    static final int CARD_GAP = 2;
    static final int OFFER_W  = 14;
    static final int OFFER_H  = 10;
    static final int ORDER_W  = 38;
    static final int LEFT_COL_W  = 88;
    static final int TRIBE_BOX_W = 46;
    static final int TOP_TRIBE_ROW_W = 192;
    static final int END_PLAYER_W = 24;
    static final int END_PP_W = 18;
    static final int END_FOOD_W = 12;

    static final List<Character> TILE_ORDER = List.of('A','B','C','D','E','F','G');

    // Offer-tile presentation data consumed by the TUI renderer.
    // TILE_ARROWS: per-tile arrow tokens displayed inside the tile box.
    // TILE_MIN:    minimum player count for which a tile is shown.
    static final Map<Character, String[]> TILE_ARROWS = new LinkedHashMap<>() {{
        put('A', new String[]{"+3c", ""});
        put('B', new String[]{"down", ""});
        put('C', new String[]{"up", ""});
        put('D', new String[]{"down2", ""});
        put('E', new String[]{"up", "down"});
        put('F', new String[]{"up2", ""});
        put('G', new String[]{"up2", "down"});
    }};

    static final Map<Character, Integer> TILE_MIN = Map.of(
            'A', 5, 'B', 2, 'C', 2, 'D', 3, 'E', 2, 'F', 2, 'G', 4
    );
}
