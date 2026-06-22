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

    // Saves the screen pieces it needs (the top layer, the board and the track
    // where the totems live). Called once before any animation.
    void attach(StackPane rootLayer, Region board, Node centerTrack) {
        this.rootLayer = rootLayer;
        this.board = board;
        this.centerTrack = centerTrack;
    }

    // Call this BEFORE the board is redrawn: it takes a "photo" of where the totems
    // are now and figures out which players moved on/off the offer tile, plays a
    // sound if needed, and returns all that info to use later in finish().
    Pending begin() {
        Map<String, TotemAnchor> previousTotems = captureBoardTotems();
        Set<String> currentOnTile = totemsOnOfferTile();
        Set<String> logicalMoves = logicalTotemMoves(displayedOnOfferTile, currentOnTile);
        playTotemMoveSoundIfNeeded(previousTotems, logicalMoves);
        return new Pending(previousTotems, logicalMoves, currentOnTile);
    }

    // Call this AFTER the board is redrawn: it uses the old positions saved in
    // begin() to slide the totems from where they were to their new spot, and
    // remembers who is on the tile now for next time.
    void finish(Pending pending, Runnable onAnimationsFinished) {
        playTotemMoveAnimations(pending.previousTotems(), pending.logicalMoves(), onAnimationsFinished);
        displayedOnOfferTile = pending.currentOnTile();
    }

    // Tells if a totem animation is currently playing, so the GUI can wait.
    boolean isAnimating() {
        return animating;
    }

    // Looks at all the totem markers on the board and, for each one, saves its
    // owner and exact position/size into a map (the "photo" of the totems).
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

    // For one totem marker, converts its position into coordinates on the top
    // layer and works out its center point and size (null if it has no size yet).
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

    // Builds the set of players whose totem is currently on an offer tile
    // (ignoring disconnected players).
    private Set<String> totemsOnOfferTile() {
        Set<String> onTile = new HashSet<>();
        for (String nickname : model.getTotemOnTiles().values()) {
            if (nickname != null && !model.isPlayerDisconnected(nickname)) {
                onTile.add(nickname);
            }
        }
        return Set.copyOf(onTile);
    }

    // Compares "who was on the tile before" with "who is on it now" and returns
    // the players whose state changed (just stepped on, or just left the tile).
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

    // Plays the click sound, but only if a totem really moved and the board was
    // already on screen (so it stays silent on the first draw).
    private void playTotemMoveSoundIfNeeded(Map<String, TotemAnchor> previousTotems,
                                            Set<String> logicalMoves) {
        if (!logicalMoves.isEmpty()
                && !previousTotems.isEmpty()
                && rootLayer != null
                && rootLayer.getScene() != null) {
            GuiAudio.playActionClick();
        }
    }

    // The real animation. It re-reads where each totem is NOW, and for every
    // player that moved (and far enough to be worth it) it builds a slide from
    // the old spot to the new one. It runs all slides together, marks itself as
    // busy while they play, and calls the callback when they finish.
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

    // Builds the slide for one totem: it first "teleports" the totem back to its
    // old position and size, then animates it moving and scaling smoothly to its
    // real (new) position, so it looks like the same totem traveled across.
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

    // Data carried from begin() to finish(): the old totem positions, who moved,
    // and who is on the tile now.
    record Pending(Map<String, TotemAnchor> previousTotems,
                   Set<String> logicalMoves,
                   Set<String> currentOnTile) {
    }

    // One totem's position snapshot: the marker node, its center point and its size.
    private record TotemAnchor(Node node, Point2D center, double width, double height) {
    }

    // A single move to animate: where a totem comes from and where it goes to.
    private record TotemMove(TotemAnchor from, TotemAnchor to) {
    }
}
