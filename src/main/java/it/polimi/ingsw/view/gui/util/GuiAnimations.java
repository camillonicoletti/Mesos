package it.polimi.ingsw.view.gui.util;

import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public final class GuiAnimations {
    private GuiAnimations() {
    }

    public static void addHoverScale(Node node) {
        node.setOnMouseEntered(event -> scaleTo(node, 1.035, 140));
        node.setOnMouseExited(event -> scaleTo(node, 1.0, 140));
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

    private static void scaleTo(Node node, double scale, int millis) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }
}
