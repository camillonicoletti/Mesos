package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
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

public final class GameBoardView {
    private static final double CARD_WIDTH = 96;
    private static final double CARD_HEIGHT = 135;
    private static final double DETAIL_CARD_WIDTH = 330;
    private static final double DETAIL_CARD_HEIGHT = 466;

    private final ClientController controller;
    private final ClientModel model;
    private final GuiAssetCatalog assetCatalog;
    private final CardDescriptionService cardDescriptions;
    private final CardImageFactory cardImages;
    private final TotemMarkerFactory totemMarkers;
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

    public GameBoardView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.controller = controller;
        this.model = model;
        this.assetCatalog = assetCatalog;
        this.cardDescriptions = new CardDescriptionService(assetCatalog);
        this.cardImages = new CardImageFactory(assetCatalog);
        this.totemMarkers = new TotemMarkerFactory(assetCatalog);
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
        refreshMarketBand(upperMarketBand,
                cardRow("Personaggi / Eventi", model.getUpperRowCharacterIds(), true, false),
                cardRow("Edifici", model.getUpperRowBuildingIds(), true, true));
        refreshCenterTrack();
        refreshMarketBand(lowerMarketBand,
                cardRow("Personaggi / Eventi", model.getLowerRowCharacterIds(), false, false),
                cardRow("Edifici", model.getLowerRowBuildingIds(), false, true));
        playerDockSlot.getChildren().setAll(playerDockView.render());
        Platform.runLater(this::updateBoardScale);
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

    private Label gameTitle() {
        Label title = new Label("MESOS");
        title.getStyleClass().add("game-title");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        return title;
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

    private VBox cardRow(String title, List<Integer> ids, boolean upper, boolean building) {
        VBox box = new VBox(4);
        box.getStyleClass().add("card-row");
        HBox cards = new HBox(8);
        cards.setAlignment(Pos.CENTER);

        for (Integer id : ids) {
            boolean isEvent = !building && model.isEventCard(id);
            boolean canTake = !isEvent && model.isMyTurn() && canPickFromRow(upper);

            Node card = cardNode(id, canTake, () -> {
                if (!canTake) {
                    return;
                }

                if (building) {
                    controller.send(new GameActionMessage(new PickBuildingAction(String.valueOf(id), upper)));
                } else {
                    controller.send(new GameActionMessage(new PickCardAction(String.valueOf(id), upper)));
                }
            });

            GuiAnimations.addHoverScale(card);
            cards.getChildren().add(card);
        }

        box.getChildren().add(cards);

        return box;
    }

    private Node cardNode(int cardId, boolean canTake, Runnable action) {
        StackPane card = new StackPane();
        card.getStyleClass().add("card");
        if (canTake) {
            card.getStyleClass().add("card-pickable");
        }
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setOnMouseClicked(event -> showCardPanel(cardId, canTake, action));
        card.getChildren().add(cardImages.create(cardId, CARD_WIDTH, CARD_HEIGHT));
        return card;
    }

    private void showCardPanel(int cardId, boolean canTake, Runnable takeAction) {
        VBox panel = inlinePanel(canTake ? "Conferma carta" : "Dettagli carta");
        panel.getStyleClass().add("card-focus-panel");
        Label prompt = new Label(canTake
                ? "Vuoi prendere questa carta?"
                : "Carta selezionata");
        prompt.getStyleClass().add("inline-panel-subtitle");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button close = inlineButton("Chiudi", false);
        close.setOnAction(event -> hideInlinePanel());
        actions.getChildren().add(close);
        if (canTake) {
            Button take = inlineButton("Prendi", true);
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

    private boolean canPickFromRow(boolean upper) {
        return model.canCurrentActionPlayerPickFromRow(upper);
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
                return "Scegli una Tessera Offerta su cui posizionare il tuo totem";
            }

            return current == null
                    ? "In attesa del piazzamento dei totem"
                    : "Attendi il piazzamento di " + current;
        }

        if ("ActionResolutionState".equals(model.getGameState())) {
            if (myTurn) {
                return "Scegli una carta disponibile dalla fila indicata dalla tua tessera";
            }

            return current == null
                    ? "Risoluzione azioni in corso"
                    : "Turno di " + current;
        }

        return "Tabellone di gioco";
    }

}
