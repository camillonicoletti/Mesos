package it.polimi.ingsw.view.gui.component;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

// Handles the popup panels shown on top of the game board.
// It builds them, shows them with an animation and removes them again.
final class OverlayController {
    private static final String OVERLAY_STYLE = "inline-overlay";

    private StackPane rootLayer;

    /** Binds the controller to the root layer the overlays are drawn on. */
    void attach(StackPane rootLayer) {
        this.rootLayer = rootLayer;
    }

    VBox inlinePanel(String titleText) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("inline-panel");
        Label title = new Label(titleText);
        title.getStyleClass().add("inline-panel-title");
        panel.getChildren().add(title);
        return panel;
    }

    Button inlineButton(String text, boolean primary) {
        Button button = new Button(text);
        button.getStyleClass().add(primary ? "inline-button-primary" : "inline-button");
        return button;
    }

    void showInlinePanel(Node panel, Pos alignment) {
        if (rootLayer == null) {
            return;
        }
        hideInlinePanel();

        StackPane overlay = new StackPane(panel);
        overlay.getStyleClass().add(OVERLAY_STYLE);
        overlay.setPadding(new Insets(24));
        overlay.setOnMouseClicked(event -> {
            if (event.getTarget() == overlay) {
                hideInlinePanel();
            }
        });
        StackPane.setAlignment(panel, alignment);
        rootLayer.getChildren().add(overlay);

        panel.setOpacity(0);
        panel.setTranslateX(alignment == Pos.CENTER_RIGHT ? 44 : 0);
        panel.setTranslateY(alignment == Pos.CENTER_RIGHT ? 0 : 20);

        // Render the panel (with its heavy drop-shadows) to a bitmap for the
        // duration of the open animation, so the blur isn't recomputed on every
        // frame of the fade/slide. Restored afterwards for a crisp static panel.
        panel.setCache(true);
        panel.setCacheHint(CacheHint.SPEED);

        FadeTransition fade = new FadeTransition(Duration.millis(160), panel);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(180), panel);
        slide.setToX(0);
        slide.setToY(0);
        ParallelTransition openAnimation = new ParallelTransition(fade, slide);
        openAnimation.setOnFinished(event -> {
            panel.setCacheHint(CacheHint.DEFAULT);
            panel.setCache(false);
        });
        openAnimation.play();
    }

    void hideInlinePanel() {
        if (rootLayer == null) {
            return;
        }
        rootLayer.getChildren().removeIf(node -> node.getStyleClass().contains(OVERLAY_STYLE));
    }
}
