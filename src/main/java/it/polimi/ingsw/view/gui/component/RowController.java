package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientModel;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Decides when the card rows need redrawing and plays the card-dealing animations.
// It remembers what is on screen so it only rebuilds the rows when something changed.
final class RowController {
    private static final long DEAL_STAGGER_MILLIS = 90;
    private static final Duration DEAL_DURATION = Duration.millis(300);
    private static final long DEAL_FINISH_BUFFER_MILLIS = 60;

    private final ClientModel model;
    private final CardMarketView cardMarketView;
    private final DeckView deckView;
    private final PlayerDockView playerDockView;
    private final Runnable onAnimationsFinished;

    private StackPane rootLayer;
    private Region board;
    private HBox upperMarketBand;
    private HBox lowerMarketBand;
    private BoardScaler boardScaler;

    private int lastSeenRound = 0;
    private boolean roundTransitionPlaying = false;
    private List<Integer> displayedUpperCharIds  = List.of();
    private List<Integer> displayedUpperBuildIds = List.of();
    private List<Integer> displayedLowerCharIds  = List.of();
    private List<Integer> displayedLowerBuildIds = List.of();
    private String displayedActionPlayer = null;
    private String displayedGameState    = null;
    private boolean displayedMyTurn;
    private boolean displayedCanPickUpper;
    private boolean displayedCanPickLower;
    private boolean cardTakeAnimationPlaying;
    private long lastHandledTakenCardSequence;

    RowController(ClientModel model,
                  CardMarketView cardMarketView,
                  DeckView deckView,
                  PlayerDockView playerDockView,
                  Runnable onAnimationsFinished) {
        this.model = model;
        this.cardMarketView = cardMarketView;
        this.deckView = deckView;
        this.playerDockView = playerDockView;
        this.onAnimationsFinished = onAnimationsFinished;
    }

    // Saves the screen pieces this controller needs to work on (the layers,
    // the board, the two card rows and the scaler). Called once before use.
    void attach(StackPane rootLayer, Region board, HBox upperMarketBand, HBox lowerMarketBand, BoardScaler boardScaler) {
        this.rootLayer = rootLayer;
        this.board = board;
        this.upperMarketBand = upperMarketBand;
        this.lowerMarketBand = lowerMarketBand;
        this.boardScaler = boardScaler;
    }

    // The main "brain": decides what to do each time the game changes.
    // If an animation is already running it waits; if a card was just taken it
    // flies it away; if new cards appeared it deals them (initial deal or a
    // round-end slide); otherwise, if the rows are stale, it just redraws them.
    void update() {
        int currentRound = model.getLastFinishedRound();
        if (currentRound > 0 && currentRound > lastSeenRound) {
            lastSeenRound = currentRound;
        }

        if (roundTransitionPlaying || cardTakeAnimationPlaying) {
            return;
        }

        if (playPendingCardTakeAnimation()) {
            return;
        }

        // Trigger an animation whenever the model has cards that weren't
        // shown before. Initial deal (round 0) fills both rows from the deck;
        // round-end transitions (round > 0) slide upper to lower then refill
        // only the upper row.
        if (newCardsAppeared()) {
            if (lastSeenRound > 0) {
                // Buildings only move at era change (when their IDs differ).
                boolean buildingsChanged = !model.getUpperRowBuildingIds().equals(displayedUpperBuildIds)
                                        || !model.getLowerRowBuildingIds().equals(displayedLowerBuildIds);
                Runnable rebuild = buildingsChanged
                        ? () -> {
                            refreshMarketBandTracked(upperMarketBand, true);
                            refreshMarketBandTracked(lowerMarketBand, false);
                        }
                        : () -> {
                            refreshCharactersOnly(upperMarketBand, true);
                            refreshCharactersOnly(lowerMarketBand, false);
                        };
                playRoundEndAnimation(buildingsChanged, rebuild);
            } else {
                playInitialDealAnimation(() -> {
                    refreshMarketBandTracked(upperMarketBand, true);
                    refreshMarketBandTracked(lowerMarketBand, false);
                });
            }
        } else if (marketBandsOutOfDate()) {
            refreshMarketBandTracked(upperMarketBand, true);
            refreshMarketBandTracked(lowerMarketBand, false);
        }
    }

    // Tells if an animation is currently playing, so the rest of the GUI can wait.
    boolean isAnimating() {
        return roundTransitionPlaying || cardTakeAnimationPlaying;
    }

    // Checks if a new "card taken" event arrived (using a sequence number so each
    // event is handled only once). If so, finds the card on the row and its target
    // on the player's dock, then starts the fly-away animation. Returns true if it did.
    private boolean playPendingCardTakeAnimation() {
        ClientModel.TakenCardEvent taken = model.getLastTakenCardEvent();
        if (taken == null || taken.sequence() <= lastHandledTakenCardSequence) {
            return false;
        }

        lastHandledTakenCardSequence = taken.sequence();
        Node source = findMarketCardNode(taken.cardId(), taken.fromUpperRow(), taken.building());
        if (source == null || source.getScene() == null || rootLayer == null || rootLayer.getScene() == null) {
            return false;
        }

        Node target = playerDockView.deckTargetFor(taken.nickname(), taken.cardId());
        playCardTakeAnimation(source, target, taken.cardId());
        return true;
    }

    // Plays the "take a card" animation. It hides the real card and creates a
    // copy on a top layer, then moves it in two steps: first it lifts to the
    // center growing and spinning, then it flies to the player's area shrinking
    // and fading. When done it cleans up, redraws the rows if needed and unlocks.
    private void playCardTakeAnimation(Node source, Node target, int cardId) {
        cardTakeAnimationPlaying = true;
        source.setOpacity(0);

        Bounds sourceBounds = rootLayer.sceneToLocal(source.localToScene(source.getBoundsInLocal()));
        double cardWidth = Math.max(72, sourceBounds.getWidth());
        double cardHeight = Math.max(101, sourceBounds.getHeight());
        Node flyingCard = cardMarketView.animationCard(cardId, cardWidth, cardHeight);
        flyingCard.getStyleClass().add("card-take-flight");
        flyingCard.setMouseTransparent(true);
        flyingCard.setRotationAxis(Rotate.Y_AXIS);

        Pane flightLayer = new Pane(flyingCard);
        flightLayer.getStyleClass().add("card-take-flight-layer");
        flightLayer.setMouseTransparent(true);
        flightLayer.setPickOnBounds(false);
        flightLayer.prefWidthProperty().bind(rootLayer.widthProperty());
        flightLayer.prefHeightProperty().bind(rootLayer.heightProperty());
        rootLayer.getChildren().add(flightLayer);

        double startX = centerX(sourceBounds) - cardWidth / 2.0;
        double startY = centerY(sourceBounds) - cardHeight / 2.0;
        Point2D center = new Point2D(rootLayer.getWidth() / 2.0 - cardWidth / 2.0,
                rootLayer.getHeight() / 2.0 - cardHeight / 2.0);
        Point2D targetPoint = targetPoint(target, cardWidth, cardHeight);

        flyingCard.setLayoutX(startX);
        flyingCard.setLayoutY(startY);

        Duration liftDuration = Duration.millis(520);
        TranslateTransition liftMove = new TranslateTransition(liftDuration, flyingCard);
        liftMove.setToX(center.getX() - startX);
        liftMove.setToY(center.getY() - startY);
        liftMove.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition liftScale = new ScaleTransition(liftDuration, flyingCard);
        liftScale.setToX(1.72);
        liftScale.setToY(1.72);
        liftScale.setInterpolator(Interpolator.EASE_BOTH);

        RotateTransition liftRotate = new RotateTransition(liftDuration, flyingCard);
        liftRotate.setFromAngle(0);
        liftRotate.setToAngle(360);
        liftRotate.setInterpolator(Interpolator.EASE_BOTH);

        Duration landDuration = Duration.millis(540);
        TranslateTransition landMove = new TranslateTransition(landDuration, flyingCard);
        landMove.setToX(targetPoint.getX() - startX);
        landMove.setToY(targetPoint.getY() - startY);
        landMove.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition landScale = new ScaleTransition(landDuration, flyingCard);
        landScale.setToX(0.32);
        landScale.setToY(0.32);
        landScale.setInterpolator(Interpolator.EASE_IN);

        RotateTransition landRotate = new RotateTransition(landDuration, flyingCard);
        landRotate.setFromAngle(360);
        landRotate.setToAngle(720);
        landRotate.setInterpolator(Interpolator.EASE_IN);

        FadeTransition landFade = new FadeTransition(landDuration, flyingCard);
        landFade.setToValue(0.18);

        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(liftMove, liftScale, liftRotate),
                new PauseTransition(Duration.millis(90)),
                new ParallelTransition(landMove, landScale, landRotate, landFade)
        );
        sequence.setOnFinished(event -> {
            rootLayer.getChildren().remove(flightLayer);
            source.setOpacity(1);
            if (marketBandsOutOfDate()) {
                refreshMarketBandTracked(upperMarketBand, true);
                refreshMarketBandTracked(lowerMarketBand, false);
            }
            cardTakeAnimationPlaying = false;
            Platform.runLater(() -> {
                boardScaler.update();
                update();
                if (onAnimationsFinished != null) {
                    onAnimationsFinished.run();
                }
            });
        });
        sequence.play();
    }

    // Works out the screen spot where the flying card should land: the center of
    // the player's card if we have one, otherwise the bottom of the board.
    private Point2D targetPoint(Node target, double cardWidth, double cardHeight) {
        if (target != null && target.getScene() != null) {
            Bounds targetBounds = rootLayer.sceneToLocal(target.localToScene(target.getBoundsInLocal()));
            return new Point2D(centerX(targetBounds) - cardWidth / 2.0,
                    centerY(targetBounds) - cardHeight / 2.0);
        }

        Bounds boardBounds = rootLayer.sceneToLocal(board.localToScene(board.getBoundsInLocal()));
        return new Point2D(centerX(boardBounds) - cardWidth / 2.0,
                boardBounds.getMaxY() - cardHeight);
    }

    // Looks through the right row and returns the on-screen card that matches the
    // given id/row/type (each card carries this info in its userData).
    private Node findMarketCardNode(int cardId, boolean upper, boolean building) {
        HBox band = upper ? upperMarketBand : lowerMarketBand;
        int sectionIndex = building ? 1 : 0;
        for (Node node : collectCardNodesFromSection(band, sectionIndex)) {
            CardMarketView.MarketCardRef ref = (CardMarketView.MarketCardRef) node.getUserData();
            if (ref != null
                    && ref.cardId() == cardId
                    && ref.upper() == upper
                    && ref.building() == building) {
                return node;
            }
        }
        return null;
    }

    // Returns the horizontal middle point of a box.
    private double centerX(Bounds bounds) {
        return (bounds.getMinX() + bounds.getMaxX()) / 2.0;
    }

    // Returns the vertical middle point of a box.
    private double centerY(Bounds bounds) {
        return (bounds.getMinY() + bounds.getMaxY()) / 2.0;
    }

    // Puts all the card ids currently on screen into one set, then checks if the
    // model now has any id that is not in that set (i.e. a brand new card showed up).
    private boolean newCardsAppeared() {
        Set<Integer> displayed = new HashSet<>();
        displayed.addAll(displayedUpperCharIds);
        displayed.addAll(displayedUpperBuildIds);
        displayed.addAll(displayedLowerCharIds);
        displayed.addAll(displayedLowerBuildIds);

        return hasNewId(model.getUpperRowCharacterIds(), displayed)
            || hasNewId(model.getUpperRowBuildingIds(), displayed)
            || hasNewId(model.getLowerRowCharacterIds(), displayed)
            || hasNewId(model.getLowerRowBuildingIds(), displayed);
    }

    // Helper: returns true if any id in the list is not already shown on screen.
    private boolean hasNewId(List<Integer> currentIds, Set<Integer> displayed) {
        for (Integer id : currentIds) {
            if (!displayed.contains(id)) return true;
        }
        return false;
    }

    // Compares what is on screen with the model (card ids, current player, game
    // state, whose turn, which rows are pickable) and returns true if anything
    // differs, meaning the rows need to be redrawn.
    private boolean marketBandsOutOfDate() {
        return !model.getUpperRowCharacterIds().equals(displayedUpperCharIds)
            || !model.getUpperRowBuildingIds().equals(displayedUpperBuildIds)
            || !model.getLowerRowCharacterIds().equals(displayedLowerCharIds)
            || !model.getLowerRowBuildingIds().equals(displayedLowerBuildIds)
            || !Objects.equals(model.getCurrentActionPlayer(), displayedActionPlayer)
            || !Objects.equals(model.getGameState(), displayedGameState)
            || model.isMyTurn() != displayedMyTurn
            || model.canCurrentActionPlayerPickFromRow(true) != displayedCanPickUpper
            || model.canCurrentActionPlayerPickFromRow(false) != displayedCanPickLower;
    }

    // Rebuilds a whole row (both characters and buildings) and then saves the new
    // ids as "what is shown", so later checks know the row is up to date.
    private void refreshMarketBandTracked(HBox band, boolean upper) {
        refreshMarketBand(band,
                cardMarketView.characterRow(upper),
                cardMarketView.buildingRow(upper));
        if (upper) {
            displayedUpperCharIds  = new ArrayList<>(model.getUpperRowCharacterIds());
            displayedUpperBuildIds = new ArrayList<>(model.getUpperRowBuildingIds());
        } else {
            displayedLowerCharIds  = new ArrayList<>(model.getLowerRowCharacterIds());
            displayedLowerBuildIds = new ArrayList<>(model.getLowerRowBuildingIds());
        }
        rememberMarketState();
    }

    // Rebuilds only the character part of a row (leaving the buildings as they are)
    // and updates the saved ids. Used when buildings did not change.
    private void refreshCharactersOnly(HBox band, boolean upper) {
        VBox newCharacters = cardMarketView.characterRow(upper);
        HBox.setHgrow(newCharacters, Priority.ALWAYS);
        if (!band.getChildren().isEmpty()) {
            band.getChildren().set(0, newCharacters);
        } else {
            band.getChildren().add(newCharacters);
        }
        if (upper) {
            displayedUpperCharIds = new ArrayList<>(model.getUpperRowCharacterIds());
        } else {
            displayedLowerCharIds = new ArrayList<>(model.getLowerRowCharacterIds());
        }
        rememberMarketState();
    }

    // Saves the current "context" (player, game state, turn, pickable rows) as the
    // shown state, so marketBandsOutOfDate() can compare against it next time.
    private void rememberMarketState() {
        displayedActionPlayer = model.getCurrentActionPlayer();
        displayedGameState = model.getGameState();
        displayedMyTurn = model.isMyTurn();
        displayedCanPickUpper = model.canCurrentActionPlayerPickFromRow(true);
        displayedCanPickLower = model.canCurrentActionPlayerPickFromRow(false);
    }

    // Replaces a row's content with the new character and building boxes and lets
    // them stretch to share the width evenly.
    private void refreshMarketBand(HBox band, VBox characters, VBox buildings) {
        band.getChildren().setAll(characters, buildings);
        HBox.setHgrow(characters, Priority.ALWAYS);
        HBox.setHgrow(buildings, Priority.ALWAYS);
    }

    // Plays the end-of-round animation: the old lower cards fade out while the old
    // upper cards slide down to take their place. When that finishes it rebuilds
    // the upper row and deals fresh cards into it from the deck.
    private void playRoundEndAnimation(boolean includeBuildings, Runnable onRebuildBands) {
        roundTransitionPlaying = true;
        upperMarketBand.setOpacity(1);
        lowerMarketBand.setOpacity(1);

        // Old upper: always characters, plus buildings only on era change
        List<Node> oldUpperCards = new ArrayList<>(collectCharacterCardNodes(upperMarketBand));
        List<Node> oldLowerCards = new ArrayList<>(collectCharacterCardNodes(lowerMarketBand));
        if (includeBuildings) {
            oldUpperCards.addAll(collectBuildingCardNodes(upperMarketBand));
            oldLowerCards.addAll(collectBuildingCardNodes(lowerMarketBand));
        }

        double scale = board.getScaleX();
        if (scale <= 0) scale = 1.0;
        double translateDeltaY = computeBandsDistanceY() / scale;

        Duration slideDur = Duration.millis(600);

        // Old lower row: fade out (they're being overwritten by the slide-down)
        ParallelTransition fadeLower = new ParallelTransition();
        for (Node c : oldLowerCards) {
            FadeTransition ft = new FadeTransition(slideDur, c);
            ft.setToValue(0);
            fadeLower.getChildren().add(ft);
        }

        // Old upper row: slide down to the lower row's vertical position
        ParallelTransition slideUpper = new ParallelTransition();
        for (Node card : oldUpperCards) {
            TranslateTransition tt = new TranslateTransition(slideDur, card);
            tt.setToY(translateDeltaY);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            slideUpper.getChildren().add(tt);
        }

        ParallelTransition phase1 = new ParallelTransition(slideUpper, fadeLower);
        phase1.setOnFinished(ev -> {
            // Replace the affected sections (characters always; buildings only on era change)
            onRebuildBands.run();
            upperMarketBand.setOpacity(1);
            lowerMarketBand.setOpacity(1);

            // Deal only the upper row sections that we actually rebuilt
            List<Node> newUpperCards = new ArrayList<>(collectCharacterCardNodes(upperMarketBand));
            if (includeBuildings) {
                newUpperCards.addAll(collectBuildingCardNodes(upperMarketBand));
            }
            for (Node c : newUpperCards) c.setOpacity(0);

            Platform.runLater(() -> dealCardsFromDeck(newUpperCards, List.of()));
        });
        phase1.play();
    }

    // Plays the first deal of the game: builds both rows, hides every card, then
    // deals them all out from the deck one by one.
    private void playInitialDealAnimation(Runnable onRebuildBands) {
        roundTransitionPlaying = true;
        upperMarketBand.setOpacity(1);
        lowerMarketBand.setOpacity(1);

        onRebuildBands.run();

        List<Node> upperCards = collectCardNodes(upperMarketBand);
        List<Node> lowerCards = collectCardNodes(lowerMarketBand);
        for (Node c : upperCards) c.setOpacity(0);
        for (Node c : lowerCards) c.setOpacity(0);

        Platform.runLater(() -> dealCardsFromDeck(upperCards, lowerCards));
    }

    // Measures the vertical distance between the upper and lower rows, so the
    // slide-down animation knows how far to move the cards.
    private double computeBandsDistanceY() {
        if (upperMarketBand.getScene() == null || lowerMarketBand.getScene() == null) {
            return 0;
        }
        Bounds upperScene = upperMarketBand.localToScene(upperMarketBand.getBoundsInLocal());
        Bounds lowerScene = lowerMarketBand.localToScene(lowerMarketBand.getBoundsInLocal());
        double upperCenterY = (upperScene.getMinY() + upperScene.getMaxY()) / 2.0;
        double lowerCenterY = (lowerScene.getMinY() + lowerScene.getMaxY()) / 2.0;
        return lowerCenterY - upperCenterY;
    }

    // The actual dealing animation. It orders the cards upper/lower like a real
    // dealer, moves each one so it starts on top of the deck (still invisible),
    // then makes them appear and slide to their slot one after another (staggered).
    // After everything lands it resets the cards, saves the state and unlocks.
    private void dealCardsFromDeck(List<Node> upperCards, List<Node> lowerCards) {
        // Interleave upper/lower like a real dealer
        List<Node> dealOrder = new ArrayList<>();
        int maxLen = Math.max(upperCards.size(), lowerCards.size());
        for (int i = 0; i < maxLen; i++) {
            if (i < upperCards.size()) dealOrder.add(upperCards.get(i));
            if (i < lowerCards.size()) dealOrder.add(lowerCards.get(i));
        }

        StackPane deckCard = deckView.getCardBackNode();
        Point2D deckCenter = computeDeckCenter(deckCard);
        double scale = board.getScaleX();
        if (scale <= 0) scale = 1.0;

        // Pre-position every card so it visually sits on the deck (still invisible)
        for (Node card : dealOrder) {
            if (card.getScene() == null) continue;
            Bounds cb = card.localToScene(card.getBoundsInLocal());
            Bounds cr = rootLayer.sceneToLocal(cb);
            double cardCX = (cr.getMinX() + cr.getMaxX()) / 2.0;
            double cardCY = (cr.getMinY() + cr.getMaxY()) / 2.0;
            // Translate happens in parent coords (un-scaled) -> divide scene delta by board scale
            card.setTranslateX((deckCenter.getX() - cardCX) / scale);
            card.setTranslateY((deckCenter.getY() - cardCY) / scale);
        }

        // Animate each card from deck to its final slot, staggered
        for (int i = 0; i < dealOrder.size(); i++) {
            final Node card = dealOrder.get(i);
            PauseTransition wait = new PauseTransition(Duration.millis(i * DEAL_STAGGER_MILLIS));
            wait.setOnFinished(ev -> {
                card.setOpacity(1);
                TranslateTransition slide = new TranslateTransition(DEAL_DURATION, card);
                slide.setToX(0);
                slide.setToY(0);
                slide.setInterpolator(Interpolator.EASE_OUT);
                slide.play();
            });
            wait.play();
        }

        // Release the transition lock and sync the displayed-state tracker once
        // everything has landed, so a subsequent refresh() won't redraw the bands.
        long totalMs = dealOrder.size() * DEAL_STAGGER_MILLIS
                + (long) DEAL_DURATION.toMillis()
                + DEAL_FINISH_BUFFER_MILLIS;
        PauseTransition done = new PauseTransition(Duration.millis(totalMs));
        done.setOnFinished(ev -> {
            for (Node card : dealOrder) {
                card.setTranslateX(0);
                card.setTranslateY(0);
                card.setOpacity(1);
            }
            displayedActionPlayer = model.getCurrentActionPlayer();
            displayedGameState    = model.getGameState();
            roundTransitionPlaying = false;
            Platform.runLater(boardScaler::update);
            if (onAnimationsFinished != null) {
                onAnimationsFinished.run();
            }
        });
        done.play();
    }

    // Finds the center point of the deck on screen (so cards can start from there);
    // if the deck is not visible it falls back to the center of the board.
    private Point2D computeDeckCenter(StackPane deckCardNode) {
        if (deckCardNode != null && deckCardNode.getScene() != null) {
            Bounds db = deckCardNode.localToScene(deckCardNode.getBoundsInLocal());
            Bounds dr = rootLayer.sceneToLocal(db);
            return new Point2D((dr.getMinX() + dr.getMaxX()) / 2.0,
                               (dr.getMinY() + dr.getMaxY()) / 2.0);
        }
        Bounds bb = board.localToScene(board.getBoundsInLocal());
        Bounds br = rootLayer.sceneToLocal(bb);
        return new Point2D((br.getMinX() + br.getMaxX()) / 2.0,
                           (br.getMinY() + br.getMaxY()) / 2.0);
    }

    // Walks through a whole row and gathers every single card node into one list.
    private List<Node> collectCardNodes(HBox marketBand) {
        List<Node> cards = new ArrayList<>();
        for (Node section : marketBand.getChildren()) {
            for (Node row : childrenOf(section)) {
                cards.addAll(childrenOf(row));
            }
        }
        return cards;
    }

    // Gathers the card nodes from just one part of a row (0 = characters, 1 = buildings).
    private List<Node> collectCardNodesFromSection(HBox marketBand, int sectionIndex) {
        List<Node> cards = new ArrayList<>();
        if (marketBand.getChildren().size() > sectionIndex) {
            Node section = marketBand.getChildren().get(sectionIndex);
            for (Node row : childrenOf(section)) {
                cards.addAll(childrenOf(row));
            }
        }
        return cards;
    }

    // Small helper: returns the child nodes of a container.
    private List<Node> childrenOf(Node node) {
        return ((Parent) node).getChildrenUnmodifiable();
    }

    // Shortcut: gets only the character cards of a row.
    private List<Node> collectCharacterCardNodes(HBox marketBand) {
        return collectCardNodesFromSection(marketBand, 0);
    }

    // Shortcut: gets only the building cards of a row.
    private List<Node> collectBuildingCardNodes(HBox marketBand) {
        return collectCardNodesFromSection(marketBand, 1);
    }
}
