package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.GamePhase;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import it.polimi.ingsw.view.gui.util.GuiAudio;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;

// Small helper methods shared by the GUI components (spacers, titles, etc.).
// Just shortcuts so the same little pieces of code are not repeated everywhere.
final class GuiViews {
    private GuiViews() {
    }

    static String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    static void setFixedSize(Region region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }

    static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    static String currentTurnNickname(ClientModel model) {
        GamePhase phase = GamePhase.fromStateName(model.getGameState());
        if (phase.isTotemPlacement()) {
            return model.getCurrentTotemPlacementPlayer();
        }
        if (phase.isActionResolution()) {
            return model.getCurrentActionResolutionPlayer();
        }
        return null;
    }

    static MaterialDesignV audioIcon() {
        return GuiAudio.isEnabled() ? MaterialDesignV.VOLUME_HIGH : MaterialDesignV.VOLUME_OFF;
    }


    static Button menuAudioToggleButton(String... extraStyleClasses) {
        Button button = new Button("Audio");
        button.getStyleClass().addAll("secondary-button", "game-rules-button");
        button.getStyleClass().addAll(extraStyleClasses);
        button.setAccessibleText("Toggle audio");
        GuiIcons.decorateButton(button, audioIcon());
        GuiAnimations.addHoverScale(button);
        button.setOnAction(event -> {
            boolean enabled = GuiAudio.toggleEnabled();
            if (enabled) {
                GuiAudio.playMenuMusic();
            }
            GuiIcons.decorateButton(button, audioIcon());
        });
        return button;
    }
}
