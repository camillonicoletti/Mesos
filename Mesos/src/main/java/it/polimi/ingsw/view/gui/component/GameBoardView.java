package it.polimi.ingsw.view.gui.component;

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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class GameBoardView {
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
                marketBand("Fila bassa",
                        cardRow("Personaggi / Eventi", model.getLowerRowCharacterIds(), false, false),
                        cardRow("Edifici", model.getLowerRowBuildingIds(), false, true)),
                playerDock()
        );

        return board;
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

        track.getChildren().addAll(turnOrderPanel(), deckPanel(), totemPlacementPanel());

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
                image.setFitWidth(148);
                image.setFitHeight(218);
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

        HBox pane = new HBox(2);
        pane.setAlignment(Pos.CENTER);

        for (char tile : activeOfferTiles()) {
            String owner = model.getTotemOnTiles().get(tile);
            Button button = offerTileButton(tile, owner);

            char selectedTile = tile;

            button.setDisable(!model.isMyTotemPlacementTurn() || owner != null);
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
        cardBack.setPrefSize(84, 118);

        String eraKey = eraBackKey(model.getCurrentEra());
        String backPath = "gui/assets/cards/backs/" + eraKey + ".png";

        boolean backLoaded = false;
        try (InputStream stream = assetCatalog.openResource(backPath).orElse(null)) {
            if (stream != null) {
                ImageView img = new ImageView(new Image(stream));
                img.setFitWidth(84);
                img.setFitHeight(118);
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
                view.setFitWidth(215);
                view.setFitHeight(138);

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

            StackPane badge = new StackPane();

            Region circle = new Region();
            circle.setPrefSize(36, 36);
            circle.setMinSize(36, 36);
            circle.setStyle("-fx-background-color: " + totemColorHex(color) + ";"
                    + "-fx-background-radius: 18;");

            Label initial = new Label(owner.isEmpty() ? "?" : String.valueOf(owner.charAt(0)).toUpperCase());
            initial.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");

            badge.getChildren().addAll(circle, initial);

            content.getChildren().add(badge);
            StackPane.setAlignment(badge, Pos.TOP_CENTER);
        }

        Button button = new Button();
        button.getStyleClass().add(owner == null ? "offer-tile" : "offer-tile-occupied");
        button.setGraphic(content);
        button.setPrefSize(228, 154);

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

            Node card = cardNode(id, () -> {
                if (isEvent || !model.isMyTurn() || !canPickFromRow(upper)) {
                    return;
                }

                if (building) {
                    controller.send(new GameActionMessage(new PickBuildingAction(String.valueOf(id), upper)));
                } else {
                    controller.send(new GameActionMessage(new PickCardAction(String.valueOf(id), upper)));
                }
            });

            card.setDisable(isEvent);

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

    private Node cardNode(int cardId, Runnable action) {
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);

        StackPane card = new StackPane();
        card.getStyleClass().add("card");
        card.setPrefSize(112, 158);
        card.setOnMouseClicked(event -> action.run());

        if (asset.isPresent() && asset.get().isFrontAvailable()) {
            try (InputStream stream = assetCatalog.openResource(asset.get().getFrontPath()).orElse(null)) {
                if (stream != null) {
                    ImageView image = new ImageView(new Image(stream));

                    image.setFitWidth(112);
                    image.setFitHeight(158);
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
