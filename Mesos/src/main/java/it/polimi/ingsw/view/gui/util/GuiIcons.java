package it.polimi.ingsw.view.gui.util;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

public final class GuiIcons {
    private GuiIcons() {
    }

    public static FontIcon icon(Ikon ikon, int size) {
        return icon(ikon, size, null);
    }

    public static FontIcon icon(Ikon ikon, int size, String color) {
        FontIcon fontIcon = FontIcon.of(ikon, size);
        fontIcon.getStyleClass().add("font-icon");
        if (color != null) {
            fontIcon.setIconColor(Color.web(color));
        }
        return fontIcon;
    }

    public static void decorateButton(Button button, Ikon ikon) {
        if (ikon == null) {
            return;
        }
        button.setGraphic(icon(ikon, 16));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(8);
    }

    public static FontIcon tileIcon(char tile) {
        return switch (tile) {
            case 'A' -> icon(MaterialDesignF.FOOD_DRUMSTICK, 14);
            case 'B', 'D' -> icon(MaterialDesignA.ARROW_DOWN_BOLD, 14, "#a7347b");
            case 'C', 'F' -> icon(MaterialDesignA.ARROW_UP_BOLD, 14, "#428047");
            case 'E', 'G' -> icon(MaterialDesignA.ARROW_UP_DOWN_BOLD, 14, "#5f6f35");
            default -> icon(MaterialDesignC.CARDS, 14);
        };
    }
}
