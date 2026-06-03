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
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Animates a player's totem sliding between the Turn Order Tile and an Offer
 * Tile. It tracks each totem's logical location across refreshes and, when one
 * has moved, tweens its on-screen marker from the old position to the new one.
 *
 * <p>Usage spans a single refresh: call {@link #begin()} before the board is
 * rebuilt to snapshot the current positions, then {@link #finish(Pending)}
 * once the new layout is in place to play the animations.
 */
final class TotemMoveAnimator {
    private static final double TOTEM_MOVE_MIN_DISTANCE = 10;

    private final ClientModel model;
    private StackPane rootLayer;
    private Region board;
    private Node centerTrack;
    private Map<String, TotemLocation> displayedTotemLocations = Map.of();
    private boolean animating;

    TotemMoveAnimator(ClientModel model) {
        this.model = model;
    }

    void attach(StackPane rootLayer, Region board, Node centerTrack) {
        this.rootLayer = rootLayer;
        this.board = board;
        this.centerTrack = centerTrack;
    }

    /**
     * Snapshots on-screen totem positions and which totems moved since the last
     * refresh, playing the move sound if any did. Call before the center track
     * is rebuilt; hand the result to {@link #finish(Pending)} afterwards.
     */
    Pending begin() {
        Map<String, TotemAnchor> previousTotems = captureBoardTotems();
        Map<String, TotemLocation> currentLocations = modelTotemLocations();
        Set<String> logicalMoves = logicalTotemMoves(displayedTotemLocations, currentLocations);
        playTotemMoveSoundIfNeeded(previousTotems, logicalMoves);
        return new Pending(previousTotems, logicalMoves, currentLocations);
    }

    /** Plays the move animations using the freshly laid-out positions. */
    void finish(Pending pending, Runnable onAnimationsFinished) {
        playTotemMoveAnimations(pending.previousTotems(), pending.logicalMoves(), onAnimationsFinished);
        displayedTotemLocations = pending.currentLocations();
    }

    boolean isAnimating() {
        return animating;
    }

    private Map<String, TotemAnchor> captureBoardTotems() {
        Map<String, TotemAnchor> anchors = new HashMap<>();
        if (centerTrack == null || centerTrack.getScene() == null || rootLayer == null) {
            return anchors;
        }
        collectBoardTotems(centerTrack, anchors);
        return anchors;
    }

    private void collectBoardTotems(Node node, Map<String, TotemAnchor> anchors) {
        String owner = TotemMarkerFactory.boardTotemOwner(node);
        if (owner != null) {
            TotemAnchor anchor = anchorFor(node);
            if (anchor != null) {
                anchors.put(owner, anchor);
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectBoardTotems(child, anchors);
            }
        }
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

    private Map<String, TotemLocation> modelTotemLocations() {
        Map<String, TotemLocation> locations = new HashMap<>();
        for (String nickname : model.getTurnOrder()) {
            if (!model.isPlayerDisconnected(nickname)) {
                locations.put(nickname, TotemLocation.TURN_ORDER);
            }
        }
        for (String nickname : model.getTotemOnTiles().values()) {
            if (nickname != null && !model.isPlayerDisconnected(nickname)) {
                locations.put(nickname, TotemLocation.OFFER_TILE);
            }
        }
        return Map.copyOf(locations);
    }

    private Set<String> logicalTotemMoves(Map<String, TotemLocation> previous,
                                          Map<String, TotemLocation> current) {
        if (previous.isEmpty()) {
            return Set.of();
        }

        Set<String> moves = new HashSet<>();
        for (String nickname : model.getPlayers().keySet()) {
            TotemLocation from = previous.get(nickname);
            TotemLocation to = current.get(nickname);
            if (isOrderOfferMove(from, to)) {
                moves.add(nickname);
            }
        }
        return moves;
    }

    private boolean isOrderOfferMove(TotemLocation from, TotemLocation to) {
        return from == TotemLocation.TURN_ORDER && to == TotemLocation.OFFER_TILE
                || from == TotemLocation.OFFER_TILE && to == TotemLocation.TURN_ORDER;
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

    /** Opaque token carrying the pre-rebuild snapshot between begin and finish. */
    record Pending(Map<String, TotemAnchor> previousTotems,
                   Set<String> logicalMoves,
                   Map<String, TotemLocation> currentLocations) {
    }

    private record TotemAnchor(Node node, Point2D center, double width, double height) {
    }

    private record TotemMove(TotemAnchor from, TotemAnchor to) {
    }

    private enum TotemLocation {
        TURN_ORDER,
        OFFER_TILE
    }
}
