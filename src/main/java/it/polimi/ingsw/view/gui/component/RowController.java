package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientModel;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Decides when and how the two market bands need redrawing and plays the card
 * dealing animations. It owns the "displayed" snapshot of what is currently on
 * screen so it can diff against the model and avoid rebuilding the rows when
 * nothing relevant changed.
 *
 * <ul>
 *   <li>Initial deal (round 0): both rows are dealt from the deck.</li>
 *   <li>Round-end transition (round &gt; 0): the upper row slides down onto the
 *       lower row, then the upper row is refilled from the deck. Buildings only
 *       move at an era change (when their ids differ).</li>
 *   <li>Otherwise, bands are rebuilt in place only when they are out of date.</li>
 * </ul>
 */
final class RowController {
    private final ClientModel model;
    private final CardMarketView cardMarketView;
    private final DeckView deckView;

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

    RowController(ClientModel model, CardMarketView cardMarketView, DeckView deckView) {
        this.model = model;
        this.cardMarketView = cardMarketView;
        this.deckView = deckView;
    }

    void attach(StackPane rootLayer, Region board, HBox upperMarketBand, HBox lowerMarketBand, BoardScaler boardScaler) {
        this.rootLayer = rootLayer;
        this.board = board;
        this.upperMarketBand = upperMarketBand;
        this.lowerMarketBand = lowerMarketBand;
        this.boardScaler = boardScaler;
    }

    void update() {
        int currentRound = model.getLastFinishedRound();
        if (currentRound > 0 && currentRound > lastSeenRound) {
            lastSeenRound = currentRound;
        }

        if (roundTransitionPlaying) {
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

    private boolean hasNewId(List<Integer> currentIds, Set<Integer> displayed) {
        for (Integer id : currentIds) {
            if (!displayed.contains(id)) return true;
        }
        return false;
    }

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

    private void rememberMarketState() {
        displayedActionPlayer = model.getCurrentActionPlayer();
        displayedGameState = model.getGameState();
        displayedMyTurn = model.isMyTurn();
        displayedCanPickUpper = model.canCurrentActionPlayerPickFromRow(true);
        displayedCanPickLower = model.canCurrentActionPlayerPickFromRow(false);
    }

    private void refreshMarketBand(HBox band, VBox characters, VBox buildings) {
        band.getChildren().setAll(characters, buildings);
        HBox.setHgrow(characters, Priority.ALWAYS);
        HBox.setHgrow(buildings, Priority.ALWAYS);
    }

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

    private void dealCardsFromDeck(List<Node> upperCards, List<Node> lowerCards) {
        // Interleave upper/lower like a real dealer
        List<Node> dealOrder = new ArrayList<>();
        int maxLen = Math.max(upperCards.size(), lowerCards.size());
        for (int i = 0; i < maxLen; i++) {
            if (i < upperCards.size()) dealOrder.add(upperCards.get(i));
            if (i < lowerCards.size()) dealOrder.add(lowerCards.get(i));
        }

        long staggerMs = 280;
        Duration dealDur = Duration.millis(420);

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
            PauseTransition wait = new PauseTransition(Duration.millis(i * staggerMs));
            wait.setOnFinished(ev -> {
                card.setOpacity(1);
                TranslateTransition slide = new TranslateTransition(dealDur, card);
                slide.setToX(0);
                slide.setToY(0);
                slide.setInterpolator(Interpolator.EASE_OUT);
                slide.play();
            });
            wait.play();
        }

        // Release the transition lock and sync the displayed-state tracker once
        // everything has landed, so a subsequent refresh() won't redraw the bands.
        long totalMs = dealOrder.size() * staggerMs + (long) dealDur.toMillis() + 80;
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
        });
        done.play();
    }

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

    private List<Node> collectCardNodes(HBox marketBand) {
        List<Node> cards = new ArrayList<>();
        for (Node child : marketBand.getChildren()) {
            if (child instanceof VBox row) {
                for (Node rowChild : row.getChildren()) {
                    if (rowChild instanceof HBox hbox) {
                        cards.addAll(hbox.getChildren());
                    }
                }
            }
        }
        return cards;
    }

    private List<Node> collectCardNodesFromSection(HBox marketBand, int sectionIndex) {
        List<Node> cards = new ArrayList<>();
        if (marketBand.getChildren().size() > sectionIndex
                && marketBand.getChildren().get(sectionIndex) instanceof VBox row) {
            for (Node rowChild : row.getChildren()) {
                if (rowChild instanceof HBox hbox) {
                    cards.addAll(hbox.getChildren());
                }
            }
        }
        return cards;
    }

    private List<Node> collectCharacterCardNodes(HBox marketBand) {
        return collectCardNodesFromSection(marketBand, 0);
    }

    private List<Node> collectBuildingCardNodes(HBox marketBand) {
        return collectCardNodesFromSection(marketBand, 1);
    }
}
