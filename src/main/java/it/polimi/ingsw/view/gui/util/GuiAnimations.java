package it.polimi.ingsw.view.gui.util;

import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.util.Duration;

// Small helpers for simple UI animations shared by the GUI.
// For example: grow a little on mouse hover, or pulse gently forever.
public final class GuiAnimations {
    private GuiAnimations() {
    }

    public static void addHoverScale(Node node) {
        node.setOnMouseEntered(event -> {
            // Cache the node (with its drop-shadow) as a bitmap while it is
            // scaled, so the shadow isn't re-rendered on every animation frame.
            node.setCache(true);
            node.setCacheHint(CacheHint.SPEED);
            scaleTo(node, 1.035, 140, null);
        });
        node.setOnMouseExited(event -> scaleTo(node, 1.0, 140, () -> {
            node.setCacheHint(CacheHint.DEFAULT);
            node.setCache(false);
        }));
    }

    private static void scaleTo(Node node, double scale, int millis, Runnable onFinished) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
        transition.setToX(scale);
        transition.setToY(scale);
        if (onFinished != null) {
            transition.setOnFinished(event -> onFinished.run());
        }
        transition.play();
    }
}
