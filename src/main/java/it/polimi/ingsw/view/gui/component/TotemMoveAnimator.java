package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.gui.util.GuiAudio;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Plays the animation of totems sliding across the board when players place them.
final class TotemMoveAnimator {
    private static final double TOTEM_MOVE_MIN_DISTANCE = 10;

    private final ClientModel model;
    private StackPane rootLayer;
    private Region board;
    private Node centerTrack;
    private Set<String> displayedOnOfferTile;
    private boolean animating;

    TotemMoveAnimator(ClientModel model) {
        this.model = model;
    }

    void attach(StackPane rootLayer, Region board, Node centerTrack) {
        this.rootLayer = rootLayer;
        this.board = board;
        this.centerTrack = centerTrack;
    }

    Pending begin() {
        Map<String, TotemAnchor> previousTotems = captureBoardTotems();
        Set<String> currentOnTile = totemsOnOfferTile();
        Set<String> logicalMoves = logicalTotemMoves(displayedOnOfferTile, currentOnTile);
        playTotemMoveSoundIfNeeded(previousTotems, logicalMoves);
        return new Pending(previousTotems, logicalMoves, currentOnTile);
    }

    void finish(Pending pending, Runnable onAnimationsFinished) {
        playTotemMoveAnimations(pending.previousTotems(), pending.logicalMoves(), onAnimationsFinished);
        displayedOnOfferTile = pending.currentOnTile();
    }

    boolean isAnimating() {
        return animating;
    }

    private Map<String, TotemAnchor> captureBoardTotems() {
        Map<String, TotemAnchor> anchors = new HashMap<>();
        if (centerTrack == null || centerTrack.getScene() == null || rootLayer == null) {
            return anchors;
        }
        for (Node node : centerTrack.lookupAll(".tile-totem-marker")) {
            String owner = TotemMarkerFactory.boardTotemOwner(node);
            if (owner == null) {
                continue;
            }
            TotemAnchor anchor = anchorFor(node);
            if (anchor != null) {
                anchors.put(owner, anchor);
            }
        }
        return anchors;
    }

    private TotemAnchor anchorFor(Node node) {
        if (node == null || node.getScene() == null || rootLayer == null) {
            return null;
        }
        Bounds sceneBounds = node.localToScene(node.getBoundsInLocal());
        Bounds localBounds = rootLayer.sceneToLocal(sceneBounds);
        double width = localBounds.getWidth();
        double height = localBounds.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        Point2D center = new Point2D(
                (localBounds.getMinX() + localBounds.getMaxX()) / 2.0,
                (localBounds.getMinY() + localBounds.getMaxY()) / 2.0);
        return new TotemAnchor(node, center, width, height);
    }

    private Set<String> totemsOnOfferTile() {
        Set<String> onTile = new HashSet<>();
        for (String nickname : model.getTotemOnTiles().values()) {
            if (nickname != null && !model.isPlayerDisconnected(nickname)) {
                onTile.add(nickname);
            }
        }
        return Set.copyOf(onTile);
    }

    private Set<String> logicalTotemMoves(Set<String> previousOnTile, Set<String> currentOnTile) {
        if (previousOnTile == null) {
            return Set.of();
        }

        Set<String> moves = new HashSet<>();
        for (String nickname : model.getPlayers().keySet()) {
            if (previousOnTile.contains(nickname) != currentOnTile.contains(nickname)) {
                moves.add(nickname);
            }
        }
        return moves;
    }

    private void playTotemMoveSoundIfNeeded(Map<String, TotemAnchor> previousTotems,
                                            Set<String> logicalMoves) {
        if (!logicalMoves.isEmpty()
                && !previousTotems.isEmpty()
                && rootLayer != null
                && rootLayer.getScene() != null) {
            GuiAudio.playActionClick();
        }
    }

    private boolean playTotemMoveAnimations(Map<String, TotemAnchor> previousTotems,
                                            Set<String> logicalMoves,
                                            Runnable onAnimationsFinished) {
        if (logicalMoves.isEmpty()
                || previousTotems.isEmpty()
                || rootLayer == null
                || rootLayer.getScene() == null) {
            return false;
        }

        rootLayer.applyCss();
        rootLayer.layout();
        Map<String, TotemAnchor> currentTotems = captureBoardTotems();
        List<TotemMove> moves = new ArrayList<>();
        for (Map.Entry<String, TotemAnchor> entry : currentTotems.entrySet()) {
            String nickname = entry.getKey();
            if (!logicalMoves.contains(nickname)) {
                continue;
            }
            TotemAnchor current = entry.getValue();
            TotemAnchor previous = previousTotems.get(nickname);
            if (previous != null && previous.center.distance(current.center) >= TOTEM_MOVE_MIN_DISTANCE) {
                moves.add(new TotemMove(previous, current));
            }
        }

        if (moves.isEmpty()) {
            return false;
        }

        animating = true;
        ParallelTransition allMoves = new ParallelTransition();
        for (TotemMove move : moves) {
            allMoves.getChildren().add(totemMoveAnimation(move.from(), move.to()));
        }
        allMoves.setOnFinished(event -> {
            animating = false;
            if (onAnimationsFinished != null) {
                onAnimationsFinished.run();
            }
        });
        allMoves.play();
        return true;
    }

    private ParallelTransition totemMoveAnimation(TotemAnchor from, TotemAnchor to) {
        Node destination = to.node;
        double boardScale = board == null || board.getScaleX() <= 0 ? 1.0 : board.getScaleX();
        double baseTranslateX = destination.getTranslateX();
        double baseTranslateY = destination.getTranslateY();
        double baseScaleX = destination.getScaleX();
        double baseScaleY = destination.getScaleY();
        double startScale = to.height <= 0 ? 1.0 : from.height / to.height;

        destination.setTranslateX(baseTranslateX + (from.center.getX() - to.center.getX()) / boardScale);
        destination.setTranslateY(baseTranslateY + (from.center.getY() - to.center.getY()) / boardScale);
        destination.setScaleX(baseScaleX * startScale);
        destination.setScaleY(baseScaleY * startScale);

        TranslateTransition move = new TranslateTransition(Duration.millis(520), destination);
        move.setToX(baseTranslateX);
        move.setToY(baseTranslateY);
        move.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(Duration.millis(520), destination);
        scale.setToX(baseScaleX);
        scale.setToY(baseScaleY);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        return new ParallelTransition(move, scale);
    }

    record Pending(Map<String, TotemAnchor> previousTotems,
                   Set<String> logicalMoves,
                   Set<String> currentOnTile) {
    }

    private record TotemAnchor(Node node, Point2D center, double width, double height) {
    }

    private record TotemMove(TotemAnchor from, TotemAnchor to) {
    }
}
