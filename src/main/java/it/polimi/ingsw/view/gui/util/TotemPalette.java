package it.polimi.ingsw.view.gui.util;

import it.polimi.ingsw.model.enums.TotemColor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Single source of truth for how each {@link TotemColor} is rendered in the GUI.
 * {@code fillHex} is the darker shade used for solid markers/dots; {@code textHex}
 * is the brighter shade used for coloured text/accents; {@code assetPath} points
 * to the token image. Replaces the per-view {@code switch} statements that had
 * drifted out of sync.
 */
public final class TotemPalette {
    private static final String NEUTRAL_FILL_HEX = "#b8ad98";
    private static final Map<TotemColor, String> FILL_HEX = new EnumMap<>(TotemColor.class);
    private static final Map<TotemColor, String> TEXT_HEX = new EnumMap<>(TotemColor.class);
    private static final Map<TotemColor, String> ASSET_PATH = new EnumMap<>(TotemColor.class);

    static {
        FILL_HEX.put(TotemColor.RED, "#b94a3a");
        FILL_HEX.put(TotemColor.BLUE, "#3c6f9e");
        FILL_HEX.put(TotemColor.PURPLE, "#77529a");
        FILL_HEX.put(TotemColor.YELLOW, "#d7a93d");
        FILL_HEX.put(TotemColor.WHITE, "#f3ebdc");

        TEXT_HEX.put(TotemColor.RED, "#e05a46");
        TEXT_HEX.put(TotemColor.BLUE, "#5b9fd4");
        TEXT_HEX.put(TotemColor.PURPLE, "#a070cc");
        TEXT_HEX.put(TotemColor.YELLOW, "#e8be5a");
        TEXT_HEX.put(TotemColor.WHITE, "#f3ebdc");

        ASSET_PATH.put(TotemColor.RED, "assets/tokens/totem_red.png");
        ASSET_PATH.put(TotemColor.BLUE, "assets/tokens/totem_blue.png");
        ASSET_PATH.put(TotemColor.PURPLE, "assets/tokens/totem_purple.png");
        ASSET_PATH.put(TotemColor.YELLOW, "assets/tokens/totem_yellow.png");
        ASSET_PATH.put(TotemColor.WHITE, "assets/tokens/totem_white.png");
    }

    private TotemPalette() {
    }

    public static String fillHex(TotemColor color) {
        return color == null ? NEUTRAL_FILL_HEX : FILL_HEX.get(color);
    }

    public static String textHex(TotemColor color) {
        return TEXT_HEX.get(color == null ? TotemColor.WHITE : color);
    }

    public static String assetPath(TotemColor color) {
        return ASSET_PATH.get(color == null ? TotemColor.WHITE : color);
    }
}
