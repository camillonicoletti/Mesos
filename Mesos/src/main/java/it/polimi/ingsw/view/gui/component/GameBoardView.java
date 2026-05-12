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
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class GameBoardView {
    private static final double CARD_WIDTH = 112;
    private static final double CARD_HEIGHT = 158;
    private static final double TILE_WIDTH = 116;
    private static final double TILE_HEIGHT = 190;
    private static final double MINI_CARD_WIDTH = 48;
    private static final double MINI_CARD_HEIGHT = 68;
    private static final double DETAIL_CARD_WIDTH = 380;
    private static final double DETAIL_CARD_HEIGHT = 535;
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
    private StackPane rootLayer;
    private VBox board;
    private boolean boardShellBuilt;
    private Label instructionLabel;
    private HBox upperMarketBand;
    private HBox centerTrack;
    private HBox lowerMarketBand;
    private HBox playerDockSlot;

    public GameBoardView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.controller = controller;
        this.model = model;
        this.assetCatalog = assetCatalog;
    }

    public Node render() {
        if (rootLayer != null) {
            refresh();
            return rootLayer;
        }

        board = new VBox(20);
        board.getStyleClass().add("game-board");
        board.setPadding(new Insets(22, 26, 28, 26));
        rootLayer = new StackPane(board);
        rootLayer.getStyleClass().add("game-board-layer");
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
                instructionLabel,
                upperMarketBand,
                centerTrack,
                lowerMarketBand,
                playerDockSlot
        );
        boardShellBuilt = true;
    }

    private Label instructionBanner() {
        Label banner = new Label(actionInstruction());
        banner.getStyleClass().add("board-instruction");
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setAlignment(Pos.CENTER);
        return banner;
    }

    private HBox emptyMarketBand() {
        HBox band = new HBox(16);
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
        HBox track = new HBox(10);
        track.getStyleClass().add("center-track");
        track.setAlignment(Pos.CENTER);
        track.setFillHeight(true);
        return track;
    }

    private void refreshCenterTrack() {
        centerTrack.getChildren().setAll(turnOrderPanel(), totemPlacementPanel(), deckPanel());
    }

    private Node turnOrderPanel() {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("turn-order-tile");
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

        return tile;
    }

    private VBox totemPlacementPanel() {
        VBox panel = new VBox(8);
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
        VBox panel = new VBox(8);
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

        if (owner != null) {
            PlayerView player = model.getPlayers().get(owner);
            TotemColor color = player == null ? null : player.getTotemColor();

            StackPane badge = totemMarker(color, owner, 42);
            badge.setTranslateX(4);
            badge.setTranslateY(-44);

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
        int count = Math.min(order.size(), offsets.length);
        for (int index = 0; index < count; index++) {
            PlayerView player = model.getPlayers().get(order.get(index));
            if (player == null) {
                continue;
            }

            StackPane marker = totemMarker(player.getTotemColor(), player.getNickname(), 30);
            marker.setTranslateY(offsets[index]);
            tile.getChildren().add(marker);
            StackPane.setAlignment(marker, Pos.CENTER);
        }
    }

    private double[] turnOrderTotemOffsets(int players) {
        return switch (players) {
            case 2 -> new double[]{-34, -2};
            case 3 -> new double[]{-36, -3, 29};
            case 4 -> new double[]{-51, -22, 8, 39};
            default -> new double[]{-59, -29, 1, 31, 60};
        };
    }

    private HBox playerDock() {
        HBox dock = new HBox(14);
        dock.getStyleClass().add("player-dock");
        dock.setAlignment(Pos.CENTER);

        VBox status = new VBox(4);
        status.getStyleClass().add("table-status");
        status.getChildren().addAll(
                smallStatus("Era", nullSafe(model.getCurrentEra())),
                smallStatus("Round", String.valueOf(model.getRoundNumber())),
                smallStatus("Turno", nullSafe(currentTurnNickname()))
        );

        FlowPane players = new FlowPane(10, 10);
        players.setAlignment(Pos.CENTER);
        players.getStyleClass().add("player-summary-flow");

        for (PlayerView player : model.getPlayers().values()) {
            players.getChildren().add(playerSummary(player));
        }

        Button exit = new Button("Esci");
        exit.getStyleClass().add("dock-exit");
        exit.setOnAction(event -> {
            controller.disconnect();
            Platform.exit();
        });

        dock.getChildren().addAll(status, players, exit);

        HBox.setHgrow(players, Priority.ALWAYS);

        return dock;
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

        FlowPane summaries = tribeStatsGrid(player, false);

        FlowPane cards = tribeCards(player, MINI_CARD_WIDTH, MINI_CARD_HEIGHT);
        cards.getStyleClass().add("current-tribe-cards");

        panel.getChildren().addAll(header, summaries, cards);
        return panel;
    }

    private Node playerSummary(PlayerView player) {
        boolean disconnected = model.isPlayerDisconnected(player.getNickname());
        boolean current = player.getNickname().equals(currentTurnNickname());
        boolean me = player.getNickname().equals(model.getMyNickname());

        VBox card = new VBox(5);
        card.getStyleClass().add("player-summary");

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

        FlowPane stats = tribeStatsGrid(player, true);
        FlowPane cards = tribeCards(player, 38, 54);
        cards.getStyleClass().add("player-summary-cards");

        card.getChildren().addAll(head, resources, stats, cards);

        return card;
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

    private FlowPane tribeStatsGrid(PlayerView player, boolean compact) {
        TribeSummaryStats stats = tribeSummaryStats(player);

        FlowPane grid = new FlowPane(compact ? 5 : 7, compact ? 5 : 7);
        grid.setAlignment(Pos.CENTER);
        grid.getStyleClass().add("tribe-stat-grid");
        if (compact) {
            grid.getStyleClass().add("tribe-stat-grid-compact");
        }

        grid.getChildren().add(tribeStatCell("gatherer",
                List.of(ICON_GATHERER, ICON_SUSTENANCE_DISCOUNT),
                String.valueOf(stats.gatherers()),
                "-" + stats.gathererFoodDiscount()));
        grid.getChildren().add(tribeStatCell("hunter",
                List.of(ICON_HUNTER),
                String.valueOf(stats.hunters()),
                ""));
        grid.getChildren().add(tribeStatCell("builder",
                List.of(ICON_BUILDER, ICON_BUILDER_DISCOUNT),
                String.valueOf(stats.builders()),
                "-" + stats.builderFoodDiscount()));
        grid.getChildren().add(tribeStatCell("shaman",
                List.of(ICON_SHAMAN, ICON_STAR),
                String.valueOf(stats.shamans()),
                String.valueOf(stats.shamanStars())));
        grid.getChildren().add(tribeStatCell("artist",
                List.of(ICON_ARTIST),
                String.valueOf(stats.artists()),
                ""));
        grid.getChildren().add(tribeStatCell("inventor",
                List.of(ICON_INVENTOR),
                String.valueOf(stats.inventors()),
                String.valueOf(stats.distinctInventorIcons())));

        return grid;
    }

    private Node tribeStatCell(String type, List<String> iconPaths, String primary, String secondary) {
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
            top.getChildren().add(statIcon(iconPath));
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
        return cell;
    }

    private Node statIcon(String path) {
        try (InputStream stream = assetCatalog.openResource(path).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setFitWidth(72);
                image.setFitHeight(72);
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

    private VBox cardRow(String title, List<Integer> ids, boolean upper, boolean building) {
        VBox box = new VBox(8);
        box.getStyleClass().add("card-row");
        HBox cards = new HBox(12);
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
        for (String line : cardRuleLines(cardId)) {
            Label label = new Label(line);
            label.setWrapText(true);
            label.getStyleClass().add("card-detail-line");
            details.getChildren().add(label);
        }

        content.getChildren().addAll(image, details);
        return content;
    }

    private List<String> cardRuleLines(int cardId) {
        List<String> lines = new ArrayList<>();
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        Optional<JsonObject> details = assetCatalog.findCardDetails(cardId);

        asset.ifPresent(card -> {
            StringBuilder header = new StringBuilder(labelForKind(card.getKind()));
            card.getType().ifPresent(type -> header.append(" - ").append(labelForType(type)));
            card.getEra().ifPresent(era -> header.append(" | Era ").append(era));
            lines.add(header.toString());
        });

        if (asset.isPresent() && details.isPresent()) {
            CardAsset card = asset.get();
            JsonObject data = details.get();
            switch (card.getKind()) {
                case "character" -> lines.add(characterRuleText(data));
                case "building" -> {
                    lines.add(buildingRuleText(data));
                    lines.add("Costo: " + intDetail(data, "foodCost") + " cibo. Punti a fine partita: "
                            + intDetail(data, "pointsEndOfMatch") + ".");
                }
                case "event" -> lines.add(eventRuleText(data));
                default -> lines.addAll(cardDetailLines(cardId));
            }
        } else {
            lines.addAll(cardDetailLines(cardId));
        }

        if (cardId == model.getLastResolvedEventCardId()
                && model.getLastResolvedEventDescription() != null) {
            lines.add("Ultimo effetto risolto: " + model.getLastResolvedEventDescription());
        }

        return lines;
    }

    private String characterRuleText(JsonObject data) {
        String type = stringDetail(data, "type");
        return switch (type) {
            case "hunter" -> boolDetail(data, "foodIcon")
                    ? "Cacciatore: conta negli eventi di caccia e mostra anche un'icona cibo."
                    : "Cacciatore: conta negli eventi di caccia.";
            case "builder" -> "Costruttore: riduce di " + intDetail(data, "foodDiscount")
                    + " il costo degli edifici e vale " + intDetail(data, "pointsEndOfMatch")
                    + " PP a fine partita.";
            case "gatherer" -> "Raccoglitore: conta per gli effetti e gli edifici legati ai raccoglitori.";
            case "artist" -> "Artista: conta nelle pitture rupestri e negli effetti legati agli artisti.";
            case "inventor" -> "Inventore: porta l'icona invenzione " + stringDetail(data, "inventionIcon")
                    + " per set, coppie ed effetti degli inventori.";
            case "shaman" -> "Sciamano: aggiunge " + intDetail(data, "stars")
                    + " stelle nei rituali sciamanici.";
            default -> "Carta personaggio.";
        };
    }

    private String buildingRuleText(JsonObject data) {
        JsonElement effect = data.get("effect");
        String effectName = effectName(effect);
        return switch (effectName) {
            case "FullSetFood" -> "Edificio: quando completi set di personaggi diversi, ottieni cibo bonus.";
            case "SustenanceFoodDiscount" -> "Edificio: durante il sostentamento riduce il cibo richiesto per "
                    + labelForType(effectTarget(effect)) + ".";
            case "ShamanicRitualImmunity" -> "Edificio: ti protegge dagli effetti negativi del rituale sciamanico.";
            case "TotemSlotFoodBonus" -> "Edificio: fornisce cibo bonus in base allo slot/tessera totem scelta.";
            case "InventorPairFood" -> "Edificio: da cibo per le coppie di inventori nella tua tribu.";
            case "ShamanicRitualDoublePP" -> "Edificio: raddoppia i PP guadagnati nei rituali sciamanici.";
            case "Shaman3ExtraStars" -> "Edificio: aumenta le stelle dei tuoi sciamani.";
            case "Hunt1Food1PP" -> "Edificio: durante la caccia aggiunge 1 cibo e 1 PP.";
            case "EndBuilderPP" -> "Edificio: a fine partita da PP in base ai costruttori.";
            case "CavePaintingsFood" -> "Edificio: durante le pitture rupestri fornisce cibo bonus.";
            case "ExtraCardUpperRow" -> "Edificio: permette di ottenere una carta extra dalla fila alta.";
            case "EndPP" -> "Edificio: assegna PP finali secondo il suo effetto.";
            case "EndCharacterCountPP" -> "Edificio: a fine partita da " + effectInt(effect, "pointsPerCard")
                    + " PP per ogni " + labelForType(effectTarget(effect)) + ".";
            default -> "Edificio: " + formatDetailValue(effect) + ".";
        };
    }

    private String eventRuleText(JsonObject data) {
        String suffix = boolDetail(data, "closeEvent") ? " Questo e un evento finale." : "";
        return switch (stringDetail(data, "type")) {
            case "hunt" -> "Caccia: ogni cacciatore fornisce " + intDetail(data, "foodPerHunter")
                    + " cibo e " + intDetail(data, "ppPerHunter") + " PP." + suffix;
            case "sustenance" -> "Sostentamento: ogni personaggio non nutrito causa -"
                    + intDetail(data, "ppPenaltyPerUnfed") + " PP." + suffix;
            case "shamanicRitual" -> "Rituale sciamanico: chi ha piu stelle guadagna "
                    + intDetail(data, "ppGain") + " PP, chi ne ha meno perde "
                    + intDetail(data, "ppLoss") + " PP." + suffix;
            case "cavePaintings" -> "Pitture rupestri: servono almeno " + intDetail(data, "minArtists")
                    + " artisti; se non li hai perdi " + intDetail(data, "ppPenalty")
                    + " PP, altrimenti ottieni " + intDetail(data, "ppPerArtist")
                    + " PP per artista." + suffix;
            default -> "Evento." + suffix;
        };
    }

    private List<String> cardDetailLines(int cardId) {
        List<String> lines = new ArrayList<>();
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        lines.add("ID: " + cardId);
        asset.ifPresent(card -> {
            lines.add("Categoria: " + labelForKind(card.getKind()));
            card.getType().ifPresent(type -> lines.add("Tipo: " + labelForType(type)));
            card.getEra().ifPresent(era -> lines.add("Era: " + era));
        });

        assetCatalog.findCardDetails(cardId).ifPresent(details -> {
            List<String> extra = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : details.entrySet()) {
                String key = entry.getKey();
                if ("id".equals(key) || "type".equals(key) || "era".equals(key)) {
                    continue;
                }
                extra.add(labelForDetailKey(key) + ": " + formatDetailValue(entry.getValue()));
            }
            if (!extra.isEmpty()) {
                lines.addAll(extra);
            }
        });

        if (cardId == model.getLastResolvedEventCardId()
                && model.getLastResolvedEventDescription() != null) {
            lines.add("Ultimo effetto risolto: " + model.getLastResolvedEventDescription());
        }

        return lines;
    }

    private void showTribePanel(PlayerView player) {
        VBox panel = inlinePanel("Tribu di " + player.getNickname());
        VBox content = new VBox(12);
        content.getStyleClass().add("tribe-panel-content");
        content.getChildren().addAll(
                tribePanelHeader(player),
                tribeStatsGrid(player, false),
                summaryPill("Personaggi", countSummary(player.getCharacterCardIds())),
                summaryPill("Edifici", countSummary(player.getBuildingCardIds())),
                tribeCards(player, 78, 110)
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(720);
        scroll.setPrefViewportHeight(460);
        scroll.getStyleClass().add("tribe-panel-scroll");

        Button close = inlineButton("Chiudi", false);
        close.setOnAction(event -> hideInlinePanel());
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(scroll, actions);
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
                .map(this::cardSummaryType)
                .collect(Collectors.groupingBy(label -> label, LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(" · "));
    }

    private String cardSummaryType(int cardId) {
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        if (asset.isEmpty()) {
            return "Carta";
        }
        CardAsset card = asset.get();
        if ("building".equals(card.getKind())) {
            return assetCatalog.findCardDetails(cardId)
                    .map(details -> details.get("effect"))
                    .map(this::formatDetailValue)
                    .map(this::labelForType)
                    .orElse("Edificio");
        }
        return card.getType().map(this::labelForType).orElse(labelForKind(card.getKind()));
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

    private boolean boolDetail(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    private String effectName(JsonElement effect) {
        if (effect == null || effect.isJsonNull()) {
            return "";
        }
        if (effect.isJsonObject()) {
            JsonElement name = effect.getAsJsonObject().get("name");
            return name == null || name.isJsonNull() ? "" : name.getAsString();
        }
        return effect.getAsString();
    }

    private String effectTarget(JsonElement effect) {
        if (effect == null || !effect.isJsonObject()) {
            return "";
        }
        JsonElement target = effect.getAsJsonObject().get("targetType");
        return target == null || target.isJsonNull() ? "" : target.getAsString();
    }

    private int effectInt(JsonElement effect, String key) {
        if (effect == null || !effect.isJsonObject()) {
            return 0;
        }
        JsonElement value = effect.getAsJsonObject().get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    private String labelForKind(String kind) {
        return switch (kind) {
            case "character" -> "Personaggio";
            case "building" -> "Edificio";
            case "event" -> "Evento";
            default -> capitalize(kind);
        };
    }

    private String labelForType(String type) {
        return switch (type) {
            case "hunter" -> "Cacciatori";
            case "builder" -> "Costruttori";
            case "gatherer" -> "Raccoglitori";
            case "artist" -> "Artisti";
            case "inventor" -> "Inventori";
            case "shaman" -> "Sciamani";
            case "hunt" -> "Caccia";
            case "sustenance" -> "Sostentamento";
            case "shamanicRitual" -> "Rituale sciamanico";
            case "cavePaintings" -> "Pitture rupestri";
            case "FullSetFood" -> "Cibo da set completo";
            case "SustenanceFoodDiscount" -> "Sconto sostentamento";
            case "ShamanicRitualImmunity" -> "Immunita rituale sciamanico";
            case "TotemSlotFoodBonus" -> "Bonus cibo slot totem";
            case "InventorPairFood" -> "Cibo da coppie inventori";
            case "ShamanicRitualDoublePP" -> "Doppio rituale sciamanico";
            case "Shaman3ExtraStars" -> "Stelle sciamano extra";
            case "Hunt1Food1PP" -> "Caccia: cibo e PP";
            case "EndBuilderPP" -> "PP finali costruttori";
            case "CavePaintingsFood" -> "Cibo pitture rupestri";
            case "ExtraCardUpperRow" -> "Carta extra fila alta";
            case "EndPP" -> "PP finali";
            case "EndCharacterCountPP" -> "PP finali per personaggio";
            default -> capitalize(type);
        };
    }

    private String labelForDetailKey(String key) {
        return switch (key) {
            case "minPlayers" -> "Giocatori minimi";
            case "foodIcon" -> "Icona cibo";
            case "pointsEndOfMatch" -> "Punti fine partita";
            case "foodDiscount" -> "Sconto cibo";
            case "inventionIcon" -> "Icona invenzione";
            case "stars" -> "Stelle";
            case "foodCost" -> "Costo cibo";
            case "effect" -> "Effetto";
            case "closeEvent" -> "Evento finale";
            case "foodPerHunter" -> "Cibo per cacciatore";
            case "ppPerHunter" -> "PP per cacciatore";
            case "ppPenaltyPerUnfed" -> "Penalita PP per non nutrito";
            case "ppGain" -> "PP guadagnati";
            case "ppLoss" -> "PP persi";
            case "minArtists" -> "Artisti minimi";
            case "ppPenalty" -> "Penalita PP";
            case "ppPerArtist" -> "PP per artista";
            case "targetType" -> "Tipo bersaglio";
            case "pointsPerCard" -> "Punti per carta";
            case "name" -> "Nome";
            default -> capitalize(key);
        };
    }

    private String formatDetailValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "-";
        }
        if (value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isBoolean()) {
                return value.getAsBoolean() ? "si" : "no";
            }
            if (value.getAsJsonPrimitive().isString()) {
                return labelForType(value.getAsString());
            }
            return value.getAsString();
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            String name = object.has("name") ? labelForType(object.get("name").getAsString()) : "Effetto";
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if ("name".equals(entry.getKey())) {
                    continue;
                }
                parts.add(labelForDetailKey(entry.getKey()) + " " + formatDetailValue(entry.getValue()));
            }
            return parts.isEmpty() ? name : name + " (" + String.join(", ", parts) + ")";
        }
        return value.toString();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private char getMyCurrentTile() {
        String me = model.getMyNickname();

        if (me == null) {
            return 0;
        }

        for (Map.Entry<Character, String> entry : model.getTotemOnTiles().entrySet()) {
            if (me.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return 0;
    }

    private boolean canPickFromRow(boolean upper) {
        if (!model.isMyTurn()) {
            return false;
        }

        char tile = getMyCurrentTile();

        return switch (tile) {
            case 'B', 'D' -> !upper;
            case 'C', 'F' -> upper;
            case 'E', 'G' -> true;
            default -> false;
        };
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
