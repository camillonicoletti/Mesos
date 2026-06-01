package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.GuiApplication;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public final class GameBoardView {
    private final ClientModel model;
    private final CardDescriptionService cardDescriptions;
    private final CardImageFactory cardImages;
    private final TotemMarkerFactory totemMarkers;
    private final CardMarketView cardMarketView;
    private final TurnOrderView turnOrderView;
    private final OfferTilesView offerTilesView;
    private final DeckView deckView;
    private final PlayerDockView playerDockView;
    private final EventResolutionOverlay eventResolutionOverlay;
    private final OverlayController overlay;
    private final CardFocusView cardFocusView;
    private final TotemMoveAnimator totemAnimator;
    private final RowController rowController;
    private StackPane rootLayer;
    private VBox board;
    private boolean boardShellBuilt;
    private Label instructionLabel;
    private HBox upperMarketBand;
    private HBox centerTrack;
    private HBox lowerMarketBand;
    private HBox playerDockSlot;
    private BoardScaler boardScaler;

    public GameBoardView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.model = model;
        this.cardDescriptions = new CardDescriptionService(assetCatalog);
        this.cardImages = new CardImageFactory(assetCatalog);
        this.totemMarkers = new TotemMarkerFactory(assetCatalog);
        this.overlay = new OverlayController();
        this.cardFocusView = new CardFocusView(model, cardDescriptions, cardImages, overlay);
        this.cardMarketView = new CardMarketView(controller, model, cardImages, cardFocusView::show);
        this.turnOrderView = new TurnOrderView(model, assetCatalog, totemMarkers,
                overlay::inlinePanel,
                overlay::inlineButton,
                overlay::showInlinePanel,
                overlay::hideInlinePanel);
        this.offerTilesView = new OfferTilesView(controller, model, assetCatalog, totemMarkers);
        this.deckView = new DeckView(model, assetCatalog);
        this.playerDockView = new PlayerDockView(controller, model, assetCatalog, cardDescriptions, totemMarkers,
                cardImages,
                cardId -> cardFocusView.show(cardId, false, null),
                overlay::inlinePanel,
                overlay::inlineButton,
                overlay::showInlinePanel,
                overlay::hideInlinePanel);
        this.eventResolutionOverlay = new EventResolutionOverlay(model, cardImages, totemMarkers);
        this.totemAnimator = new TotemMoveAnimator(model);
        this.rowController = new RowController(model, cardMarketView, deckView);
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
        boardScaler = new BoardScaler(rootLayer, board);
        overlay.attach(rootLayer);
        boardScaler.install();
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
            eventResolutionOverlay.refresh(rootLayer);
            return;
        }

        ensureBoardShell();
        instructionLabel.setText(actionInstruction());

        TotemMoveAnimator.Pending totemRefresh = totemAnimator.begin();
        refreshCenterTrack();
        playerDockSlot.getChildren().setAll(playerDockView.render());
        boardScaler.update();
        rootLayer.applyCss();
        rootLayer.layout();
        totemAnimator.finish(totemRefresh);
        Platform.runLater(boardScaler::update);
        eventResolutionOverlay.refresh(rootLayer);

        rowController.update();
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
        totemAnimator.attach(rootLayer, board, centerTrack);
        rowController.attach(rootLayer, board, upperMarketBand, lowerMarketBand, boardScaler);
        boardShellBuilt = true;
    }

    private Node gameTitle() {
        StackPane headerStack = new StackPane();
        headerStack.setMaxWidth(Double.MAX_VALUE);

        Node title = MesosLogo.create(190, "game-title-logo");

        Button rulesBtn = new Button("Regolamento");
        rulesBtn.getStyleClass().addAll("secondary-button", "game-rules-button");
        rulesBtn.setPrefHeight(34);
        GuiIcons.decorateButton(rulesBtn, MaterialDesignB.BOOK_OPEN);
        GuiAnimations.addHoverScale(rulesBtn);
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
