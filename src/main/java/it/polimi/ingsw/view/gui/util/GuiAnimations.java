package it.polimi.ingsw.view.gui.util;

import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.util.Duration;

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

    public static void pulse(Node node) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(820), node);
        transition.setFromX(1.0);
        transition.setFromY(1.0);
        transition.setToX(1.025);
        transition.setToY(1.025);
        transition.setAutoReverse(true);
        transition.setCycleCount(Animation.INDEFINITE);
        transition.play();
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
