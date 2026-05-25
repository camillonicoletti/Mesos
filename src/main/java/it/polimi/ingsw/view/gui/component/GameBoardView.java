package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.GuiApplication;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GameBoardView {
    private static final double DETAIL_CARD_WIDTH = 330;
    private static final double DETAIL_CARD_HEIGHT = 466;
    private static final double MARKET_CARD_W = 96;
    private static final double MARKET_CARD_H = 135;

    private final ClientModel model;
    private final CardDescriptionService cardDescriptions;
    private final CardImageFactory cardImages;
    private final TotemMarkerFactory totemMarkers;
    private final CardMarketView cardMarketView;
    private final TurnOrderView turnOrderView;
    private final OfferTilesView offerTilesView;
    private final DeckView deckView;
    private final PlayerDockView playerDockView;
    private StackPane rootLayer;
    private VBox board;
    private boolean boardShellBuilt;
    private Label instructionLabel;
    private HBox upperMarketBand;
    private HBox centerTrack;
    private HBox lowerMarketBand;
    private HBox playerDockSlot;
    private boolean fitBindingInstalled;
    private int lastSeenRound = 0;
    private boolean roundTransitionPlaying = false;
    private List<Integer> displayedUpperCharIds  = List.of();
    private List<Integer> displayedUpperBuildIds = List.of();
    private List<Integer> displayedLowerCharIds  = List.of();
    private List<Integer> displayedLowerBuildIds = List.of();
    private String displayedActionPlayer = null;
    private String displayedGameState    = null;

    public GameBoardView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.model = model;
        this.cardDescriptions = new CardDescriptionService(assetCatalog);
        this.cardImages = new CardImageFactory(assetCatalog);
        this.totemMarkers = new TotemMarkerFactory(assetCatalog);
        this.cardMarketView = new CardMarketView(controller, model, cardImages, this::showCardPanel);
        this.turnOrderView = new TurnOrderView(model, assetCatalog, totemMarkers,
                this::inlinePanel,
                this::inlineButton,
                this::showInlinePanel,
                this::hideInlinePanel);
        this.offerTilesView = new OfferTilesView(controller, model, assetCatalog, totemMarkers);
        this.deckView = new DeckView(model, assetCatalog);
        this.playerDockView = new PlayerDockView(controller, model, assetCatalog, cardDescriptions, totemMarkers,
                cardImages,
                cardId -> showCardPanel(cardId, false, null),
                this::inlinePanel,
                this::inlineButton,
                this::showInlinePanel,
                this::hideInlinePanel);
    }

    public Node render() {
        if (rootLayer != null) {
            refresh();
            return rootLayer;
        }

        board = new VBox(8);
        board.getStyleClass().add("game-board");
        board.setPadding(new Insets(6, 10, 6, 10));
        board.setAlignment(Pos.CENTER);
        board.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        rootLayer = new StackPane(board);
        rootLayer.getStyleClass().add("game-board-layer");
        rootLayer.setAlignment(Pos.CENTER);
        rootLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(board, Pos.CENTER);
        installBoardFitBinding();
        refresh();
        return rootLayer;
    }

    public void refresh() {
        if (board == null) {
            return;
        }

        if (model.getPlayers().isEmpty()) {
            boardShellBuilt = false;
            board.getChildren().clear();
            Label label = new Label("Loading game...");
            label.getStyleClass().add("muted");
            board.getChildren().add(label);
            return;
        }

        ensureBoardShell();
        instructionLabel.setText(actionInstruction());

        int currentRound = model.getLastFinishedRound();
        if (currentRound > 0 && currentRound > lastSeenRound) {
            lastSeenRound = currentRound;
        }

        refreshCenterTrack();
        playerDockSlot.getChildren().setAll(playerDockView.render());
        Platform.runLater(this::updateBoardScale);

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
        java.util.Set<Integer> displayed = new java.util.HashSet<>();
        displayed.addAll(displayedUpperCharIds);
        displayed.addAll(displayedUpperBuildIds);
        displayed.addAll(displayedLowerCharIds);
        displayed.addAll(displayedLowerBuildIds);

        return hasNewId(model.getUpperRowCharacterIds(), displayed)
            || hasNewId(model.getUpperRowBuildingIds(), displayed)
            || hasNewId(model.getLowerRowCharacterIds(), displayed)
            || hasNewId(model.getLowerRowBuildingIds(), displayed);
    }

    private boolean hasNewId(List<Integer> currentIds, java.util.Set<Integer> displayed) {
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
            || !Objects.equals(model.getGameState(), displayedGameState);
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
        displayedActionPlayer = model.getCurrentActionPlayer();
        displayedGameState    = model.getGameState();
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
        displayedActionPlayer = model.getCurrentActionPlayer();
        displayedGameState    = model.getGameState();
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
            Platform.runLater(this::updateBoardScale);
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

    private void installBoardFitBinding() {
        if (fitBindingInstalled) {
            return;
        }
        fitBindingInstalled = true;
        rootLayer.widthProperty().addListener((observable, oldValue, newValue) -> updateBoardScale());
        rootLayer.heightProperty().addListener((observable, oldValue, newValue) -> updateBoardScale());
        board.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> updateBoardScale());
        Platform.runLater(this::updateBoardScale);
    }

    private void updateBoardScale() {
        if (rootLayer == null || board == null) {
            return;
        }
        double boardWidth = board.getLayoutBounds().getWidth();
        double boardHeight = board.getLayoutBounds().getHeight();
        double availableWidth = rootLayer.getWidth() - 22;
        double availableHeight = rootLayer.getHeight() - 16;
        if (boardWidth <= 0 || boardHeight <= 0 || availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        double scale = Math.min(availableWidth / boardWidth, availableHeight / boardHeight);
        scale = Math.max(0.72, Math.min(1.06, scale));
        board.setScaleX(scale);
        board.setScaleY(scale);
    }

    private void ensureBoardShell() {
        if (boardShellBuilt) {
            return;
        }

        instructionLabel = instructionBanner();
        upperMarketBand = emptyMarketBand();
        centerTrack = emptyCenterTrack();
        lowerMarketBand = emptyMarketBand();
        playerDockSlot = new HBox();
        playerDockSlot.setAlignment(Pos.CENTER);

        board.getChildren().setAll(
                gameTitle(),
                instructionLabel,
                upperMarketBand,
                centerTrack,
                lowerMarketBand,
                playerDockSlot
        );
        boardShellBuilt = true;
    }

    private Node gameTitle() {
        StackPane headerStack = new StackPane();
        headerStack.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("MESOS");
        title.getStyleClass().add("game-title");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Button rulesBtn = new Button("Regolamento");
        rulesBtn.getStyleClass().add("secondary-button");
        rulesBtn.setPrefHeight(30);
        org.kordamp.ikonli.javafx.FontIcon rulesIcon = org.kordamp.ikonli.javafx.FontIcon.of(MaterialDesignB.BOOK_OPEN, 14);
        rulesIcon.setIconColor(javafx.scene.paint.Color.web("#552435"));
        rulesBtn.setGraphic(rulesIcon);
        rulesBtn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        rulesBtn.setGraphicTextGap(6);
        rulesBtn.setOnAction(event -> GuiApplication.openRulesPdf());

        StackPane.setAlignment(title, Pos.CENTER);
        StackPane.setAlignment(rulesBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(rulesBtn, new Insets(0, 10, 0, 0));

        headerStack.getChildren().addAll(title, rulesBtn);
        return headerStack;
    }

    private Label instructionBanner() {
        Label banner = new Label(actionInstruction());
        banner.getStyleClass().add("board-instruction");
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setAlignment(Pos.CENTER);
        return banner;
    }

    private HBox emptyMarketBand() {
        HBox band = new HBox(10);
        band.getStyleClass().add("market-band");
        band.setAlignment(Pos.CENTER);
        return band;
    }

    private void refreshMarketBand(HBox band, VBox characters, VBox buildings) {
        band.getChildren().setAll(characters, buildings);
        HBox.setHgrow(characters, Priority.ALWAYS);
        HBox.setHgrow(buildings, Priority.ALWAYS);
    }

    private HBox emptyCenterTrack() {
        HBox track = new HBox(4);
        track.getStyleClass().add("center-track");
        track.setAlignment(Pos.CENTER);
        track.setFillHeight(true);
        return track;
    }

    private void refreshCenterTrack() {
        List<Node> trackNodes = new ArrayList<>();
        Node disconnectedTotems = disconnectedTotemsPanel();
        if (disconnectedTotems != null) {
            trackNodes.add(disconnectedTotems);
        }
        trackNodes.add(turnOrderView.render());
        trackNodes.add(offerTilesView.render());
        trackNodes.add(deckView.render());
        centerTrack.getChildren().setAll(trackNodes);
    }

    private Node disconnectedTotemsPanel() {
        List<PlayerView> disconnectedPlayers = model.getPlayers().values().stream()
                .filter(player -> model.isPlayerDisconnected(player.getNickname()))
                .toList();
        if (disconnectedPlayers.isEmpty()) {
            return null;
        }

        VBox panel = new VBox(5);
        panel.getStyleClass().add("disconnected-totems-panel");
        panel.setAlignment(Pos.CENTER);

        Label title = new Label("Fuori");
        title.getStyleClass().add("disconnected-totems-title");
        panel.getChildren().add(title);

        for (PlayerView player : disconnectedPlayers) {
            HBox row = new HBox(5);
            row.getStyleClass().add("disconnected-totem-row");
            row.setAlignment(Pos.CENTER_LEFT);

            StackPane marker = totemMarker(player.getTotemColor(), player.getNickname(), 34);
            Label name = new Label(player.getNickname());
            name.getStyleClass().add("disconnected-totem-name");
            row.getChildren().addAll(marker, name);
            panel.getChildren().add(row);
        }

        return panel;
    }

    private void showCardPanel(int cardId, boolean canTake, Runnable takeAction) {
        VBox panel = inlinePanel(canTake ? "Confirm card" : "Card details");
        panel.getStyleClass().add("card-focus-panel");
        Label prompt = new Label(canTake
                ? "Do you want to take this card?"
                : "Card selected");
        prompt.getStyleClass().add("inline-panel-subtitle");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button close = inlineButton("Close", false);
        close.setOnAction(event -> hideInlinePanel());
        actions.getChildren().add(close);
        if (canTake) {
            Button take = inlineButton("Take", true);
            take.setOnAction(event -> {
                hideInlinePanel();
                if (takeAction != null) {
                    takeAction.run();
                }
            });
            actions.getChildren().add(take);
        }

        panel.getChildren().addAll(prompt, cardDetailContent(cardId), actions);
        showInlinePanel(panel, Pos.CENTER);
    }

    private Node cardDetailContent(int cardId) {
        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("card-detail-content");

        Node image = cardImages.create(cardId, DETAIL_CARD_WIDTH, DETAIL_CARD_HEIGHT);
        VBox details = new VBox(8);
        details.getStyleClass().add("card-detail-lines");
        for (String line : cardDescriptions.describeCard(
                cardId,
                model.getLastResolvedEventCardId(),
                model.getLastResolvedEventDescription())) {
            Label label = new Label(line);
            label.setWrapText(true);
            label.getStyleClass().add("card-detail-line");
            details.getChildren().add(label);
        }

        content.getChildren().addAll(image, details);
        return content;
    }

    private VBox inlinePanel(String titleText) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("inline-panel");
        Label title = new Label(titleText);
        title.getStyleClass().add("inline-panel-title");
        panel.getChildren().add(title);
        return panel;
    }

    private Button inlineButton(String text, boolean primary) {
        Button button = new Button(text);
        button.getStyleClass().add(primary ? "inline-button-primary" : "inline-button");
        return button;
    }

    private void showInlinePanel(Node panel, Pos alignment) {
        if (rootLayer == null) {
            return;
        }
        hideInlinePanel();

        StackPane overlay = new StackPane(panel);
        overlay.getStyleClass().add("inline-overlay");
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

        FadeTransition fade = new FadeTransition(Duration.millis(160), panel);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(180), panel);
        slide.setToX(0);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void hideInlinePanel() {
        if (rootLayer == null) {
            return;
        }
        rootLayer.getChildren().removeIf(node -> node.getStyleClass().contains("inline-overlay"));
    }

    private StackPane totemMarker(TotemColor color, String owner, double fitHeight) {
        return totemMarkers.create(color, owner, fitHeight);
    }

    private String currentTurnNickname() {
        if ("TotemPlacementState".equals(model.getGameState())) {
            return model.getCurrentTotemPlacementPlayer();
        }

        if ("ActionResolutionState".equals(model.getGameState())) {
            return model.getCurrentActionResolutionPlayer();
        }

        return null;
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String actionInstruction() {
        String current = currentTurnNickname();
        boolean myTurn = current != null && current.equals(model.getMyNickname());

        if ("TotemPlacementState".equals(model.getGameState())) {
            if (myTurn) {
                return "Choose an Offer Tile to place your totem on";
            }

            return current == null
                    ? "Waiting for totem placement"
                    : "Waiting for " + current + " to place their totem";
        }

        if ("ActionResolutionState".equals(model.getGameState())) {
            if (myTurn) {
                return "Choose a card from the row indicated by your tile";
            }

            return current == null
                    ? "Resolving actions"
                    : current + "'s turn";
        }

        return "Game board";
    }

}
