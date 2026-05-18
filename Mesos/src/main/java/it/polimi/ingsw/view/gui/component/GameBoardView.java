package it.polimi.ingsw.view.gui.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.view.gui.asset.CardAsset;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class GameBoardView {
    private static final double CARD_WIDTH = 96;
    private static final double CARD_HEIGHT = 135;
    private static final double TILE_WIDTH = 100;
    private static final double TILE_HEIGHT = 164;
    private static final double MINI_CARD_WIDTH = 44;
    private static final double MINI_CARD_HEIGHT = 62;
    private static final double DETAIL_CARD_WIDTH = 330;
    private static final double DETAIL_CARD_HEIGHT = 466;
    private static final int FOOD_DISCOUNT_PER_GATHERER = 3;
    private static final String ICON_HUNTER = "assets/icons/summary/hunter.png";
    private static final String ICON_BUILDER = "assets/icons/summary/builder.png";
    private static final String ICON_BUILDER_DISCOUNT = "assets/icons/summary/builder_discount.png";
    private static final String ICON_GATHERER = "assets/icons/summary/gatherer.png";
    private static final String ICON_SUSTENANCE_DISCOUNT = "assets/icons/summary/sustenance_discount.png";
    private static final String ICON_ARTIST = "assets/icons/summary/artist.png";
    private static final String ICON_INVENTOR = "assets/icons/summary/inventor.png";
    private static final String ICON_SHAMAN = "assets/icons/summary/shaman.png";
    private static final String ICON_STAR = "assets/icons/summary/star.png";

    private static final List<Character> TILE_ORDER = List.of('A', 'B', 'C', 'D', 'E', 'F', 'G');

    private static final Map<Character, Integer> TILE_MIN_PLAYERS = Map.of(
            'A', 5,
            'B', 2,
            'C', 2,
            'D', 3,
            'E', 2,
            'F', 2,
            'G', 4
    );

    private final ClientController controller;
    private final ClientModel model;
    private final GuiAssetCatalog assetCatalog;
    private final CardDescriptionService cardDescriptions;
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
        playerDockSlot.getChildren().setAll(playerDock());
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
        trackNodes.add(turnOrderPanel());
        trackNodes.add(totemPlacementPanel());
        trackNodes.add(deckPanel());
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

    private Node turnOrderPanel() {
        StackPane tile = new StackPane();
        tile.getStyleClass().addAll("turn-order-tile", "turn-order-tile-selectable");
        String path = "assets/board/turn_order_" + turnOrderTilePlayers() + ".png";

        try (InputStream stream = assetCatalog.openResource(path).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitWidth(TILE_WIDTH);
                image.setFitHeight(TILE_HEIGHT);
                tile.getChildren().add(image);
            }
        } catch (Exception ignored) {
            // Fallback text is added below.
        }

        if (tile.getChildren().isEmpty()) {
            Label fallback = new Label("Turno");
            fallback.getStyleClass().add("turn-name");
            tile.getChildren().add(fallback);
        }

        addTurnOrderTotems(tile);
        tile.setPickOnBounds(true);
        tile.setOnMouseClicked(event -> showTurnOrderPanel());
        GuiAnimations.addHoverScale(tile);

        return tile;
    }

    private void showTurnOrderPanel() {
        int players = turnOrderTilePlayers();
        VBox panel = inlinePanel("Tessera Ordine di Turno");
        panel.getStyleClass().add("turn-order-focus-panel");

        HBox content = new HBox(16);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("turn-order-detail-content");

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("turn-order-detail-image");
        String path = "assets/board/turn_order_" + players + ".png";
        try (InputStream stream = assetCatalog.openResource(path).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitWidth(180);
                image.setFitHeight(290);
                imageBox.getChildren().add(image);
            }
        } catch (Exception ignored) {
            // Fallback text is added below.
        }
        if (imageBox.getChildren().isEmpty()) {
            Label fallback = new Label("Ordine " + players + " giocatori");
            fallback.getStyleClass().add("turn-name");
            imageBox.getChildren().add(fallback);
        }

        VBox details = new VBox(8);
        details.getStyleClass().add("card-detail-lines");
        for (String line : turnOrderRuleLines(players)) {
            Label label = new Label(line);
            label.setWrapText(true);
            label.getStyleClass().add("card-detail-line");
            details.getChildren().add(label);
        }

        content.getChildren().addAll(imageBox, details);

        Button close = inlineButton("Chiudi", false);
        close.setOnAction(event -> hideInlinePanel());
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(content, actions);
        showInlinePanel(panel, Pos.CENTER);
    }

    private List<String> turnOrderRuleLines(int players) {
        List<String> lines = new ArrayList<>();
        lines.add("Ordine: nel round successivo i totem su questa tessera vengono letti dall'alto verso il basso.");
        lines.add("Fine azione: dopo aver risolto una tessera offerta, il totem torna nel primo spazio libero piu in alto.");

        List<String> slotEffects = switch (players) {
            case 2 -> List.of("+1 cibo", "paga 1 cibo; se non puoi, perdi 2 PP");
            case 3 -> List.of("+2 cibo", "nessun effetto", "paga 1 cibo; se non puoi, perdi 2 PP");
            case 4 -> List.of("+2 cibo", "+1 cibo", "nessun effetto", "paga 1 cibo; se non puoi, perdi 2 PP");
            default -> List.of("+3 cibo", "+1 cibo", "nessun effetto", "nessun effetto",
                    "paga 1 cibo; se non puoi, perdi 2 PP");
        };

        for (int index = 0; index < slotEffects.size(); index++) {
            lines.add("Spazio " + (index + 1) + ": " + slotEffects.get(index) + ".");
        }
        lines.add("Il bonus o il pagamento dello spazio si applica subito quando il totem rientra sulla tessera.");
        return lines;
    }

    private VBox totemPlacementPanel() {
        VBox panel = new VBox(4);
        panel.getStyleClass().add("offer-strip");

        HBox pane = new HBox(4);
        pane.setAlignment(Pos.CENTER);

        for (char tile : activeOfferTiles()) {
            String owner = model.getTotemOnTiles().get(tile);
            Button button = offerTileButton(tile, owner);

            char selectedTile = tile;

            button.setDisable(false);
            button.setOnAction(event -> {
                if (!model.isMyTotemPlacementTurn() || owner != null) {
                    return;
                }
                controller.send(new GameActionMessage(new PlaceTotemAction(selectedTile)));
            });

            GuiAnimations.addHoverScale(button);
            pane.getChildren().add(button);
        }

        panel.getChildren().add(pane);

        return panel;
    }

    private VBox deckPanel() {
        VBox panel = new VBox(4);
        panel.getStyleClass().add("deck-view");
        panel.setAlignment(Pos.CENTER);
        panel.getChildren().add(sectionTitle("Mazzo Tribù"));

        StackPane cardBack = new StackPane();
        cardBack.getStyleClass().add("deck-placeholder");
        cardBack.setPrefSize(CARD_WIDTH, CARD_HEIGHT);

        String eraKey = eraBackKey(model.getCurrentEra());
        String backPath = "assets/cards/backs/" + eraKey + ".png";

        boolean backLoaded = false;
        try (InputStream stream = assetCatalog.openResource(backPath).orElse(null)) {
            if (stream != null) {
                ImageView img = new ImageView(new Image(stream));
                img.setFitWidth(CARD_WIDTH);
                img.setFitHeight(CARD_HEIGHT);
                img.setPreserveRatio(true);
                cardBack.getChildren().add(img);
                backLoaded = true;
            }
        } catch (Exception ignored) {}

        if (!backLoaded) {
            String eraLabel = model.getCurrentEra() == null ? "TRIBÙ" : "ERA " + model.getCurrentEra().name();
            Label placeholder = new Label(eraLabel);
            placeholder.getStyleClass().add("deck-placeholder-label");
            cardBack.getChildren().add(placeholder);
        }

        int remaining = model.getRemainingTribeDeckCards();
        String countText = remaining < 0 ? "?" : String.valueOf(remaining);
        Label count = new Label(countText + " carte");
        count.getStyleClass().add("deck-count");

        panel.getChildren().addAll(cardBack, count);
        return panel;
    }

    private Button offerTileButton(char tile, String owner) {
        String path = "assets/board/offer_" + tile + ".png";

        StackPane content = new StackPane();
        content.setAlignment(Pos.CENTER);

        try (InputStream stream = assetCatalog.openResource(path).orElse(null)) {
            if (stream != null) {
                Image image = new Image(stream);
                ImageView view = new ImageView(image);

                view.setPreserveRatio(true);
                view.setFitWidth(TILE_WIDTH);
                view.setFitHeight(TILE_HEIGHT);

                content.getChildren().add(view);
            }
        } catch (Exception ignored) {
            // Fallback text is added below.
        }

        if (content.getChildren().isEmpty()) {
            Label letter = new Label(String.valueOf(tile));
            letter.getStyleClass().add("offer-letter");
            content.getChildren().add(letter);
        }

        if (owner != null && !model.isPlayerDisconnected(owner)) {
            PlayerView player = model.getPlayers().get(owner);
            TotemColor color = player == null ? null : player.getTotemColor();

            StackPane badge = totemMarker(color, owner, 42);
            badge.setTranslateX(0);
            badge.setTranslateY(-40);

            content.getChildren().add(badge);
            StackPane.setAlignment(badge, Pos.CENTER);
        }

        Button button = new Button();
        button.getStyleClass().add(owner == null ? "offer-tile" : "offer-tile-occupied");
        button.setGraphic(content);
        button.setPrefSize(TILE_WIDTH + 8, TILE_HEIGHT + 8);

        return button;
    }

    private void addTurnOrderTotems(StackPane tile) {
        List<String> order = model.getTurnOrder();
        if (order.isEmpty()) {
            return;
        }

        double[] offsets = turnOrderTotemOffsets(turnOrderTilePlayers());
        Map<String, Integer> positions = model.getTurnOrderPositions();
        for (int index = 0; index < order.size(); index++) {
            String nickname = order.get(index);
            PlayerView player = model.getPlayers().get(nickname);
            if (player == null) {
                continue;
            }
            if (model.isPlayerDisconnected(nickname)) {
                continue;
            }

            int slotIndex = positions.getOrDefault(nickname, index + 1) - 1;
            if (slotIndex < 0 || slotIndex >= offsets.length) {
                continue;
            }

            StackPane marker = totemMarker(player.getTotemColor(), player.getNickname(), 34);
            marker.setTranslateY(offsets[slotIndex]);
            tile.getChildren().add(marker);
            StackPane.setAlignment(marker, Pos.CENTER);
        }
    }

    private double[] turnOrderTotemOffsets(int players) {
        return switch (players) {
            case 2 -> new double[]{-34, 0};
            case 3 -> new double[]{-47, -11, 18};
            case 4 -> new double[]{-45, -15, 14, 42};
            default -> new double[]{-57, -28, 1, 29, 58};
        };
    }

    private VBox playerDock() {
        VBox dock = new VBox(6);
        dock.getStyleClass().add("player-dock");
        dock.setAlignment(Pos.CENTER);

        HBox status = new HBox(12);
        status.getStyleClass().add("table-status");
        status.getChildren().addAll(
                smallStatus("Era", nullSafe(model.getCurrentEra())),
                smallStatus("Round", String.valueOf(model.getRoundNumber())),
                smallStatus("Turno", nullSafe(currentTurnNickname()))
        );

        Button exit = new Button("Esci");
        exit.getStyleClass().add("dock-exit");
        exit.setOnAction(event -> {
            controller.disconnect();
            Platform.exit();
        });

        HBox statusRow = new HBox(14, status, spacer(), exit);
        statusRow.getStyleClass().add("player-dock-status-row");
        statusRow.setAlignment(Pos.CENTER);

        dock.getChildren().addAll(statusRow, playerSummaryRows());

        return dock;
    }

    private VBox playerSummaryRows() {
        VBox rows = new VBox(7);
        rows.getStyleClass().add("player-summary-rows");
        rows.setAlignment(Pos.CENTER);

        List<PlayerView> players = new ArrayList<>(model.getPlayers().values());
        int perRow = players.size() <= 3 ? Math.max(1, players.size()) : 3;
        for (int start = 0; start < players.size(); start += perRow) {
            HBox row = new HBox(9);
            row.setAlignment(Pos.CENTER);
            row.getStyleClass().add("player-summary-strip");
            int end = Math.min(players.size(), start + perRow);
            for (int index = start; index < end; index++) {
                row.getChildren().add(playerSummary(players.get(index), players.size()));
            }
            rows.getChildren().add(row);
        }
        return rows;
    }

    private Node currentTribePanel() {
        PlayerView player = model.getPlayers().get(model.getMyNickname());
        if (player == null) {
            player = model.getPlayers().get(currentTurnNickname());
        }
        if (player == null) {
            Region empty = new Region();
            empty.setMinHeight(0);
            return empty;
        }

        VBox panel = new VBox(6);
        panel.getStyleClass().add("current-tribe-panel");
        panel.setAlignment(Pos.CENTER);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("Tribu di " + player.getNickname());
        title.getStyleClass().add("tribe-title");
        Label resources = new Label("Cibo " + player.getFood() + "   PP " + player.getPrestigePoints());
        resources.getStyleClass().add("tribe-resources");
        header.getChildren().addAll(totemDot(player.getTotemColor()), title, resources);

        Node summaries = tribeStatsGrid(player, false);

        FlowPane cards = tribeCards(player, MINI_CARD_WIDTH, MINI_CARD_HEIGHT);
        cards.getStyleClass().add("current-tribe-cards");

        panel.getChildren().addAll(header, summaries, cards);
        return panel;
    }

    private Node playerSummary(PlayerView player, int playerCount) {
        boolean disconnected = model.isPlayerDisconnected(player.getNickname());
        boolean current = player.getNickname().equals(currentTurnNickname());
        boolean me = player.getNickname().equals(model.getMyNickname());

        VBox card = new VBox(3);
        card.getStyleClass().add("player-summary");
        card.getStyleClass().add(playerCount <= 3 ? "player-summary-roomy" : "player-summary-many");

        if (current) {
            card.getStyleClass().add("player-summary-current");
        }

        if (me) {
            card.getStyleClass().add("player-summary-me");
        }

        if (disconnected) {
            card.getStyleClass().add("player-summary-disconnected");
        }
        card.setOnMouseClicked(event -> showTribePanel(player));
        GuiAnimations.addHoverScale(card);

        HBox head = new HBox(7);
        head.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label((me ? "Tu · " : "") + player.getNickname());
        name.getStyleClass().add("player-summary-name");

        head.getChildren().addAll(totemDot(player.getTotemColor()), name);

        Label resources = new Label("Cibo " + player.getFood() + "   PP " + player.getPrestigePoints()
                + "   Edifici " + player.getBuildingCardIds().size());
        resources.getStyleClass().add("player-summary-stat");

        Node stats = tribeStatsGrid(player, true);

        card.getChildren().addAll(head, resources, stats);
        Node buildings = buildingSummaryStrip(player);
        if (buildings != null) {
            card.getChildren().add(buildings);
        }

        return card;
    }

    private Node buildingSummaryStrip(PlayerView player) {
        List<Integer> buildingIds = player.getBuildingCardIds();
        if (buildingIds.isEmpty()) {
            return null;
        }

        HBox strip = new HBox(5);
        strip.getStyleClass().add("player-building-strip");
        strip.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Edifici");
        title.getStyleClass().add("player-building-title");
        strip.getChildren().add(title);

        int visibleCount = Math.min(5, buildingIds.size());
        int hiddenCount = buildingIds.size() - visibleCount;
        List<Integer> visibleBuildings = buildingIds.subList(buildingIds.size() - visibleCount, buildingIds.size());
        for (Integer buildingId : visibleBuildings) {
            strip.getChildren().add(stackedMiniCardNode(buildingId, 30, 42));
        }

        if (hiddenCount > 0) {
            Label more = new Label("+" + hiddenCount);
            more.getStyleClass().add("player-summary-more");
            strip.getChildren().add(more);
        }

        return strip;
    }

    private FlowPane tribeCards(PlayerView player, double width, double height) {
        FlowPane cards = new FlowPane(6, 6);
        cards.setAlignment(Pos.CENTER);
        for (Integer id : player.getCharacterCardIds()) {
            cards.getChildren().add(miniCardNode(id, width, height));
        }
        for (Integer id : player.getBuildingCardIds()) {
            cards.getChildren().add(miniCardNode(id, width, height));
        }
        if (cards.getChildren().isEmpty()) {
            Label empty = new Label("Nessuna carta nella tribu");
            empty.getStyleClass().add("muted");
            cards.getChildren().add(empty);
        }
        return cards;
    }

    private Node miniCardNode(int cardId, double width, double height) {
        StackPane card = new StackPane();
        card.getStyleClass().add("mini-card");
        card.setPrefSize(width, height);
        card.getChildren().add(cardImageNode(cardId, width, height));
        card.setOnMouseClicked(event -> showCardPanel(cardId, false, null));
        GuiAnimations.addHoverScale(card);
        return card;
    }

    private Label summaryPill(String label, String value) {
        Label pill = new Label(label + ": " + value);
        pill.getStyleClass().add("tribe-summary-pill");
        return pill;
    }

    private Node tribeStatsGrid(PlayerView player, boolean compact) {
        TribeSummaryStats stats = tribeSummaryStats(player);

        if (compact) {
            HBox grid = new HBox(6);
            grid.setAlignment(Pos.CENTER);
            grid.getStyleClass().addAll("tribe-stat-grid", "tribe-stat-grid-compact");
            addTribeStatCells(grid, player, stats, true);
            return grid;
        }

        FlowPane grid = new FlowPane(7, 7);
        grid.setAlignment(Pos.CENTER);
        grid.getStyleClass().add("tribe-stat-grid");
        addTribeStatCells(grid, player, stats, false);
        return grid;
    }

    private void addTribeStatCells(javafx.scene.layout.Pane grid, PlayerView player,
                                   TribeSummaryStats stats, boolean compact) {
        grid.getChildren().add(tribeStatCell("gatherer",
                List.of(ICON_GATHERER, ICON_SUSTENANCE_DISCOUNT),
                String.valueOf(stats.gatherers()),
                "-" + stats.gathererFoodDiscount(),
                compact,
                player));
        grid.getChildren().add(tribeStatCell("hunter",
                List.of(ICON_HUNTER),
                String.valueOf(stats.hunters()),
                "",
                compact,
                player));
        grid.getChildren().add(tribeStatCell("builder",
                List.of(ICON_BUILDER, ICON_BUILDER_DISCOUNT),
                String.valueOf(stats.builders()),
                "-" + stats.builderFoodDiscount(),
                compact,
                player));
        grid.getChildren().add(tribeStatCell("shaman",
                List.of(ICON_SHAMAN, ICON_STAR),
                String.valueOf(stats.shamans()),
                String.valueOf(stats.shamanStars()),
                compact,
                player));
        grid.getChildren().add(tribeStatCell("artist",
                List.of(ICON_ARTIST),
                String.valueOf(stats.artists()),
                "",
                compact,
                player));
        grid.getChildren().add(tribeStatCell("inventor",
                List.of(ICON_INVENTOR),
                String.valueOf(stats.inventors()),
                String.valueOf(stats.distinctInventorIcons()),
                compact,
                player));
    }

    private Node tribeStatCell(String type, List<String> iconPaths, String primary, String secondary,
                               boolean compact, PlayerView player) {
        VBox cell = new VBox(3);
        cell.getStyleClass().addAll("tribe-stat-cell", "tribe-stat-" + type);
        if (iconPaths.size() > 1) {
            cell.getStyleClass().add("tribe-stat-cell-wide");
        }
        cell.setAlignment(Pos.CENTER);

        HBox top = new HBox(3);
        top.getStyleClass().add("tribe-stat-icons");
        top.setAlignment(Pos.CENTER);
        for (String iconPath : iconPaths) {
            double iconSize = compact ? (iconPaths.size() > 1 ? 27 : 31) : (iconPaths.size() > 1 ? 64 : 68);
            top.getChildren().add(statIcon(iconPath, iconSize));
        }

        HBox values = new HBox(5);
        values.setAlignment(Pos.CENTER);
        Label main = new Label(primary);
        main.getStyleClass().add("tribe-stat-main");
        values.getChildren().add(main);
        if (secondary != null && !secondary.isBlank()) {
            Label sub = new Label(secondary);
            sub.getStyleClass().add("tribe-stat-sub");
            values.getChildren().add(sub);
        }

        cell.getChildren().addAll(top, values);
        if (compact) {
            cell.getChildren().add(characterCardsForType(player, type));
        }
        return cell;
    }

    private Node characterCardsForType(PlayerView player, String type) {
        Pane cards = new Pane();
        cards.getStyleClass().add("tribe-stat-card-strip");
        double stripWidth = 34;
        double stripHeight = 110;
        cards.setMinSize(stripWidth, stripHeight);
        cards.setPrefSize(stripWidth, stripHeight);
        cards.setMaxSize(stripWidth, stripHeight);
        cards.setClip(new Rectangle(stripWidth, stripHeight));

        List<Integer> matching = player.getCharacterCardIds().stream()
                .filter(cardId -> assetCatalog.findCard(cardId)
                        .flatMap(CardAsset::getType)
                        .map(type::equals)
                        .orElse(false))
                .toList();

        if (matching.isEmpty()) {
            Region empty = new Region();
            empty.getStyleClass().add("tribe-stat-empty-card-slot");
            empty.setLayoutX(3);
            empty.setLayoutY(32);
            cards.getChildren().add(empty);
            return cards;
        }

        int visibleCount = Math.min(10, matching.size());
        int hiddenCount = matching.size() - visibleCount;
        List<Integer> visibleCards = matching.subList(matching.size() - visibleCount, matching.size());

        double cardWidth = 32;
        double cardHeight = 45;
        double reservedBadgeHeight = hiddenCount > 0 ? 14 : 0;
        double availableStackHeight = stripHeight - reservedBadgeHeight;
        double step = visibleCards.size() == 1
                ? 0
                : Math.min(12, (availableStackHeight - cardHeight) / (visibleCards.size() - 1));
        double stackHeight = cardHeight + Math.max(0, visibleCards.size() - 1) * step;
        double startX = (stripWidth - cardWidth) / 2;
        double startY = Math.max(0, availableStackHeight - stackHeight);

        for (int index = 0; index < visibleCards.size(); index++) {
            Node card = stackedMiniCardNode(visibleCards.get(index), cardWidth, cardHeight);
            card.setLayoutX(startX);
            card.setLayoutY(startY + index * step);
            card.setViewOrder(visibleCards.size() - index);
            cards.getChildren().add(card);
        }

        if (hiddenCount > 0) {
            Label more = new Label("+" + hiddenCount);
            more.getStyleClass().add("tribe-stat-card-more");
            more.setLayoutX(7);
            more.setLayoutY(stripHeight - reservedBadgeHeight);
            cards.getChildren().add(more);
        }
        return cards;
    }

    private Node stackedMiniCardNode(int cardId, double width, double height) {
        StackPane card = new StackPane();
        card.getStyleClass().addAll("mini-card", "mini-card-stacked");
        card.setMinSize(width, height);
        card.setPrefSize(width, height);
        card.setMaxSize(width, height);
        card.getChildren().add(cardImageNode(cardId, width, height));
        card.setOnMouseClicked(event -> showCardPanel(cardId, false, null));
        return card;
    }

    private Node statIcon(String path, double size) {
        try (InputStream stream = assetCatalog.openResource(path).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setFitWidth(size);
                image.setFitHeight(size);
                image.setPreserveRatio(true);
                image.getStyleClass().add("tribe-stat-icon");
                return image;
            }
        } catch (Exception ignored) {
            // Fallback dot below.
        }

        Region fallback = new Region();
        fallback.getStyleClass().add("tribe-stat-icon-fallback");
        return fallback;
    }

    private TribeSummaryStats tribeSummaryStats(PlayerView player) {
        int gatherers = 0;
        int hunters = 0;
        int builders = 0;
        int builderFoodDiscount = 0;
        int shamans = 0;
        int shamanStars = 0;
        int artists = 0;
        int inventors = 0;
        Set<String> inventionIcons = new HashSet<>();

        for (Integer cardId : player.getCharacterCardIds()) {
            Optional<CardAsset> asset = assetCatalog.findCard(cardId);
            if (asset.isEmpty()) {
                continue;
            }

            String type = asset.get().getType().orElse("");
            JsonObject details = assetCatalog.findCardDetails(cardId).orElse(null);

            switch (type) {
                case "gatherer" -> gatherers++;
                case "hunter" -> hunters++;
                case "builder" -> {
                    builders++;
                    if (details != null) {
                        builderFoodDiscount += intDetail(details, "foodDiscount");
                    }
                }
                case "shaman" -> {
                    shamans++;
                    if (details != null) {
                        shamanStars += intDetail(details, "stars");
                    }
                }
                case "artist" -> artists++;
                case "inventor" -> {
                    inventors++;
                    if (details != null) {
                        String icon = stringDetail(details, "inventionIcon");
                        if (!icon.isBlank()) {
                            inventionIcons.add(icon);
                        }
                    }
                }
                default -> {
                    // Ignore non-character cards in this six-slot character summary.
                }
            }
        }

        return new TribeSummaryStats(
                gatherers,
                gatherers * FOOD_DISCOUNT_PER_GATHERER,
                hunters,
                builders,
                builderFoodDiscount,
                shamans,
                shamanStars,
                artists,
                inventors,
                inventionIcons.size()
        );
    }

    private HBox smallStatus(String label, String value) {
        Label key = new Label(label);
        key.getStyleClass().add("table-status-key");

        Label val = new Label(value);
        val.getStyleClass().add("table-status-value");

        HBox row = new HBox(8, key, val);
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
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

    private int turnOrderTilePlayers() {
        int players = model.getPlayers().isEmpty()
                ? model.getExpectedPlayers()
                : model.getPlayers().size();
        players = Math.max(players, model.getTurnOrder().size());
        return Math.max(2, Math.min(5, players));
    }

    private Node cardNode(int cardId, boolean canTake, Runnable action) {
        StackPane card = new StackPane();
        card.getStyleClass().add("card");
        if (canTake) {
            card.getStyleClass().add("card-pickable");
        }
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setOnMouseClicked(event -> showCardPanel(cardId, canTake, action));
        card.getChildren().add(cardImageNode(cardId, CARD_WIDTH, CARD_HEIGHT));
        return card;
    }

    private Node cardImageNode(int cardId, double width, double height) {
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        StackPane card = new StackPane();
        card.setPrefSize(width, height);
        if (asset.isPresent() && asset.get().isFrontAvailable()) {
            try (InputStream stream = assetCatalog.openResource(asset.get().getFrontPath()).orElse(null)) {
                if (stream != null) {
                    ImageView image = new ImageView(new Image(stream));

                    image.setFitWidth(width);
                    image.setFitHeight(height);
                    image.setPreserveRatio(true);

                    card.getChildren().add(image);

                    return card;
                }
            } catch (Exception ignored) {
                // Fall through to a placeholder if the optional local image is unreadable.
            }
        }

        Label placeholder = new Label(asset.map(assetCatalog::placeholderLabelFor).orElse("CARD #" + cardId));
        placeholder.setWrapText(true);
        placeholder.setAlignment(Pos.CENTER);

        card.getChildren().add(placeholder);

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

        Node image = cardImageNode(cardId, DETAIL_CARD_WIDTH, DETAIL_CARD_HEIGHT);
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

    private void showTribePanel(PlayerView player) {
        VBox panel = inlinePanel("Tribu di " + player.getNickname());
        panel.getStyleClass().add("tribe-focus-panel");
        VBox content = new VBox(12);
        content.getStyleClass().add("tribe-panel-content");
        FlowPane largeCards = tribeCards(player, 108, 152);
        largeCards.getStyleClass().add("tribe-panel-cards");
        content.getChildren().addAll(
                tribePanelHeader(player),
                tribeStatsGrid(player, false),
                summaryPill("Personaggi", countSummary(player.getCharacterCardIds())),
                summaryPill("Edifici", countSummary(player.getBuildingCardIds())),
                largeCards
        );
        content.setMaxWidth(1080);

        Button close = inlineButton("Chiudi", false);
        close.setOnAction(event -> hideInlinePanel());
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(content, actions);
        showInlinePanel(panel, Pos.CENTER);
    }

    private Node tribePanelHeader(PlayerView player) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(player.getNickname());
        name.getStyleClass().add("tribe-title");
        Label resources = new Label("Cibo " + player.getFood() + "   PP " + player.getPrestigePoints());
        resources.getStyleClass().add("tribe-resources");
        header.getChildren().addAll(totemDot(player.getTotemColor()), name, resources);
        return header;
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

    private String countSummary(List<Integer> cardIds) {
        if (cardIds.isEmpty()) {
            return "0";
        }

        Map<String, Long> counts = cardIds.stream()
                .map(cardDescriptions::summaryLabelFor)
                .collect(Collectors.groupingBy(label -> label, LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(" · "));
    }

    private StackPane totemMarker(TotemColor color, String owner) {
        return totemMarker(color, owner, 52);
    }

    private StackPane totemMarker(TotemColor color, String owner, double fitHeight) {
        StackPane marker = new StackPane();
        marker.getStyleClass().add("tile-totem-marker");
        double width = Math.max(18, fitHeight * 0.72);
        marker.setMinSize(width, fitHeight);
        marker.setPrefSize(width, fitHeight);
        marker.setMaxSize(width, fitHeight);

        try (InputStream stream = assetCatalog.openResource(totemAssetPath(color)).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitHeight(fitHeight);
                marker.getChildren().add(image);
                return marker;
            }
        } catch (Exception ignored) {
            // Fallback marker is added below.
        }

        Region body = new Region();
        body.getStyleClass().add("tile-totem-body");
        double fallbackSize = Math.max(16, fitHeight * 0.64);
        body.setMinSize(fallbackSize, fallbackSize);
        body.setPrefSize(fallbackSize, fallbackSize);
        body.setMaxSize(fallbackSize, fallbackSize);
        body.setStyle("-fx-background-color: " + totemColorHex(color) + ";");
        marker.getChildren().add(body);
        return marker;
    }

    private String totemAssetPath(TotemColor color) {
        return switch (color == null ? TotemColor.WHITE : color) {
            case RED -> "assets/tokens/totem_red.png";
            case YELLOW -> "assets/tokens/totem_yellow.png";
            case BLUE -> "assets/tokens/totem_blue.png";
            case PURPLE -> "assets/tokens/totem_purple.png";
            case WHITE -> "assets/tokens/totem_white.png";
        };
    }

    private String stringDetail(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return "";
        }
        return value.getAsString();
    }

    private int intDetail(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return 0;
        }
        return value.getAsInt();
    }

    private boolean canPickFromRow(boolean upper) {
        return model.canCurrentActionPlayerPickFromRow(upper);
    }

    private List<Character> activeOfferTiles() {
        int players = Math.max(2, model.getPlayers().isEmpty()
                ? model.getExpectedPlayers()
                : model.getPlayers().size());

        return TILE_ORDER.stream()
                .filter(tile -> TILE_MIN_PLAYERS.get(tile) <= players)
                .collect(Collectors.toList());
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

    private Region totemDot(TotemColor color) {
        Region dot = new Region();
        dot.getStyleClass().add("totem-dot");
        dot.setStyle("-fx-background-color: " + totemColorHex(color) + ";");

        return dot;
    }

    private String eraBackKey(it.polimi.ingsw.model.enums.Era era) {
        if (era == null) return "back_character_era_i";
        return switch (era) {
            case I   -> "back_character_era_i";
            case II  -> "back_character_era_ii";
            case III -> "back_character_era_iii";
        };
    }

    private String totemColorHex(TotemColor color) {
        if (color == null) {
            return "#b8ad98";
        }

        return switch (color) {
            case RED -> "#b94a3a";
            case BLUE -> "#3c6f9e";
            case PURPLE -> "#77529a";
            case YELLOW -> "#d7a93d";
            case WHITE -> "#f3ebdc";
        };
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");

        return label;
    }

    private record TribeSummaryStats(
            int gatherers,
            int gathererFoodDiscount,
            int hunters,
            int builders,
            int builderFoodDiscount,
            int shamans,
            int shamanStars,
            int artists,
            int inventors,
            int distinctInventorIcons
    ) {
    }

}
