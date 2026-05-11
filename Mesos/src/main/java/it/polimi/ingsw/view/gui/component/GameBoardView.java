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
import javafx.geometry.Rectangle2D;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class GameBoardView {
    private static final double CARD_WIDTH = 112;
    private static final double CARD_HEIGHT = 158;
    private static final double TILE_WIDTH = 116;
    private static final double TILE_HEIGHT = 190;
    private static final double MINI_CARD_WIDTH = 48;
    private static final double MINI_CARD_HEIGHT = 68;
    private static final double DETAIL_CARD_WIDTH = 245;
    private static final double DETAIL_CARD_HEIGHT = 345;
    private static final String TOTEM_SHEET = "gui/assets/tokens/pedine_specs_1.png";

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

    public GameBoardView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.controller = controller;
        this.model = model;
        this.assetCatalog = assetCatalog;
    }

    public Node render() {
        if (model.getPlayers().isEmpty()) {
            VBox waiting = new VBox();
            waiting.getStyleClass().add("game-board");
            waiting.setPadding(new Insets(22));

            Label label = new Label("Loading game...");
            label.getStyleClass().add("muted");

            waiting.getChildren().add(label);
            return waiting;
        }

        VBox board = new VBox(20);
        board.getStyleClass().add("game-board");
        board.setPadding(new Insets(22, 26, 28, 26));

        board.getChildren().addAll(
                instructionBanner(),
                marketBand("Fila alta",
                        cardRow("Personaggi / Eventi", model.getUpperRowCharacterIds(), true, false),
                        cardRow("Edifici", model.getUpperRowBuildingIds(), true, true)),
                centerTrackPanel(),
                currentTribePanel(),
                marketBand("Fila bassa",
                        cardRow("Personaggi / Eventi", model.getLowerRowCharacterIds(), false, false),
                        cardRow("Edifici", model.getLowerRowBuildingIds(), false, true)),
                playerDock()
        );

        rootLayer = new StackPane(board);
        rootLayer.getStyleClass().add("game-board-layer");
        return rootLayer;
    }

    private Label instructionBanner() {
        Label banner = new Label(actionInstruction());
        banner.getStyleClass().add("board-instruction");
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setAlignment(Pos.CENTER);
        return banner;
    }

    private HBox marketBand(String title, VBox characters, VBox buildings) {
        HBox band = new HBox(16);
        band.getStyleClass().add("market-band");
        band.setAlignment(Pos.CENTER);

        band.getChildren().addAll(characters, buildings);

        HBox.setHgrow(characters, Priority.ALWAYS);
        HBox.setHgrow(buildings, Priority.ALWAYS);

        return band;
    }

    private HBox centerTrackPanel() {
        HBox track = new HBox(10);
        track.getStyleClass().add("center-track");
        track.setAlignment(Pos.CENTER);
        track.setFillHeight(true);

        track.getChildren().addAll(turnOrderPanel(), totemPlacementPanel(), deckPanel());

        return track;
    }

    private Node turnOrderPanel() {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("turn-order-tile");
        String path = "gui/assets/board/turn_order_" + turnOrderTilePlayers() + ".png";

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
        String backPath = "gui/assets/cards/backs/" + eraKey + ".png";

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
        String path = "gui/assets/board/offer_" + tile + ".png";

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

            StackPane badge = totemMarker(color, owner);
            badge.setTranslateY(-10);

            content.getChildren().add(badge);
            StackPane.setAlignment(badge, Pos.TOP_CENTER);
        }

        Button button = new Button();
        button.getStyleClass().add(owner == null ? "offer-tile" : "offer-tile-occupied");
        button.setGraphic(content);
        button.setPrefSize(TILE_WIDTH + 8, TILE_HEIGHT + 8);

        return button;
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
        PlayerView player = model.getPlayers().get(currentTurnNickname());
        if (player == null) {
            player = model.getPlayers().get(model.getMyNickname());
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

        FlowPane summaries = new FlowPane(8, 6);
        summaries.setAlignment(Pos.CENTER);
        summaries.getChildren().add(summaryPill("Personaggi", countSummary(player.getCharacterCardIds())));
        summaries.getChildren().add(summaryPill("Edifici", countSummary(player.getBuildingCardIds())));

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

        Label resources = new Label("Cibo " + player.getFood() + "   PP " + player.getPrestigePoints());
        resources.getStyleClass().add("player-summary-stat");

        Label cards = new Label("Personaggi " + player.getCharacterCardIds().size()
                + "   Edifici " + player.getBuildingCardIds().size());
        cards.getStyleClass().add("player-summary-stat");

        card.getChildren().addAll(head, resources, cards);

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
        showInlinePanel(panel, Pos.CENTER_RIGHT);
    }

    private Node cardDetailContent(int cardId) {
        HBox content = new HBox(18);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("card-detail-content");

        Node image = cardImageNode(cardId, DETAIL_CARD_WIDTH, DETAIL_CARD_HEIGHT);
        VBox details = new VBox(8);
        details.getStyleClass().add("card-detail-lines");
        for (String line : cardDetailLines(cardId)) {
            Label label = new Label(line);
            label.setWrapText(true);
            label.getStyleClass().add("card-detail-line");
            details.getChildren().add(label);
        }

        content.getChildren().addAll(image, details);
        return content;
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
        StackPane marker = new StackPane();
        marker.getStyleClass().add("tile-totem-marker");

        try (InputStream stream = assetCatalog.openResource(TOTEM_SHEET).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setViewport(totemViewport(color));
                image.setPreserveRatio(true);
                image.setFitHeight(44);
                marker.getChildren().add(image);
                return marker;
            }
        } catch (Exception ignored) {
            // Fallback marker is added below.
        }

        Region body = new Region();
        body.getStyleClass().add("tile-totem-body");
        body.setStyle("-fx-background-color: " + totemColorHex(color) + ";");
        marker.getChildren().add(body);
        return marker;
    }

    private Rectangle2D totemViewport(TotemColor color) {
        return switch (color == null ? TotemColor.WHITE : color) {
            case RED -> new Rectangle2D(315, 670, 92, 160);
            case YELLOW -> new Rectangle2D(482, 670, 92, 160);
            case BLUE -> new Rectangle2D(642, 670, 94, 160);
            case PURPLE -> new Rectangle2D(806, 670, 94, 160);
            case WHITE -> new Rectangle2D(970, 670, 92, 160);
        };
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

}
