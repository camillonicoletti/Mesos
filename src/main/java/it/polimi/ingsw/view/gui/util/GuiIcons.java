package it.polimi.ingsw.view.gui.util;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

// Small helpers to build the icons used in the GUI (font icons, button icons).
// Keeps icon creation in one place so every view uses the same look.
public final class GuiIcons {


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


}
