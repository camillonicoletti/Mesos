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
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class GameBoardView {
    private static final List<Character> TILE_ORDER = List.of('A', 'B', 'C', 'D', 'E', 'F', 'G');
    private static final Map<Character, Integer> TILE_MIN_PLAYERS = Map.of(
            'A', 5, 'B', 2, 'C', 2, 'D', 3, 'E', 2, 'F', 2, 'G', 4
    );
    private static final Map<Character, String> TILE_EFFECTS = new LinkedHashMap<>() {{
        put('A', "+3 cibo");
        put('B', "1 carta sotto");
        put('C', "1 carta sopra");
        put('D', "2 carte sotto");
        put('E', "1 sopra + 1 sotto");
        put('F', "2 carte sopra");
        put('G', "2 sopra + 1 sotto");
    }};

    private final ClientController controller;
    private final ClientModel model;
    private final GuiAssetCatalog assetCatalog;

    public GameBoardView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.controller = controller;
        this.model = model;
        this.assetCatalog = assetCatalog;
    }

    public Node render() {
        VBox board = new VBox(18);
        board.getStyleClass().add("game-board");
        board.setPadding(new Insets(22));
        board.getChildren().addAll(
                cardRow("Personaggi / Eventi - fila alta", model.getUpperRowCharacterIds(), true, false),
                cardRow("Edifici - fila alta", model.getUpperRowBuildingIds(), true, true),
                centerTrackPanel(),
                cardRow("Personaggi / Eventi - fila bassa", model.getLowerRowCharacterIds(), false, false),
                cardRow("Edifici - fila bassa", model.getLowerRowBuildingIds(), false, true)
        );
        return board;
    }

    private HBox centerTrackPanel() {
        HBox track = new HBox(18);
        track.getStyleClass().add("center-track");
        track.setAlignment(Pos.CENTER);
        track.getChildren().addAll(turnOrderPanel(), totemPlacementPanel());
        return track;
    }

    private VBox turnOrderPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("turn-panel");
        panel.getChildren().add(sectionTitle("Tessera turno"));

        List<String> order = visibleTurnOrder();
        String current = currentTurnNickname();

        if (order.isEmpty()) {
            Label empty = new Label("In attesa dei piazzamenti");
            empty.getStyleClass().add("muted");
            panel.getChildren().add(empty);
            return panel;
        }

        for (int i = 0; i < order.size(); i++) {
            String nickname = order.get(i);
            PlayerView player = model.getPlayers().get(nickname);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add(nickname.equals(current) ? "turn-slot-active" : "turn-slot");

            Label position = new Label(String.valueOf(i + 1));
            position.getStyleClass().add("turn-position");

            Region totem = totemDot(player == null ? null : player.getTotemColor());
            Label name = new Label(nickname);
            name.getStyleClass().add("turn-name");
            Label effect = new Label(turnEffectLabel(i + 1));
            effect.getStyleClass().add("turn-effect");

            row.getChildren().addAll(position, totem, name, spacer(), effect);
            if (nickname.equals(current)) {
                GuiAnimations.pulse(row);
            }
            panel.getChildren().add(row);
        }
        return panel;
    }

    private VBox totemPlacementPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("offer-panel");
        panel.getChildren().add(sectionTitle("Tessere offerta"));

        FlowPane pane = new FlowPane(12, 12);
        pane.setAlignment(Pos.CENTER_LEFT);
        for (char tile : activeOfferTiles()) {
            String owner = model.getTotemOnTiles().get(tile);
            Button button = offerTileButton(tile, owner);
            char selectedTile = tile;
            button.setDisable(!model.isMyTotemPlacementTurn() || owner != null);
            button.setOnAction(event -> controller.send(new GameActionMessage(new PlaceTotemAction(selectedTile))));
            GuiAnimations.addHoverScale(button);
            pane.getChildren().add(button);
        }

        panel.getChildren().add(pane);
        return panel;
    }

    private Button offerTileButton(char tile, String owner) {
        String path = "gui/assets/board/offer_" + tile + ".png";
        VBox content = new VBox(6);
        content.setAlignment(Pos.CENTER);

        try (InputStream stream = assetCatalog.openResource(path).orElse(null)) {
            if (stream != null) {
                Image image = new Image(stream);
                ImageView view = new ImageView(image);
                view.setPreserveRatio(true);
                view.setFitWidth(120);
                view.setFitHeight(80);
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

        Label effect = new Label(TILE_EFFECTS.getOrDefault(tile, ""));
        effect.setGraphic(GuiIcons.tileIcon(tile));
        effect.setContentDisplay(ContentDisplay.LEFT);
        effect.getStyleClass().add("offer-effect");
        content.getChildren().add(effect);

        if (owner != null) {
            PlayerView player = model.getPlayers().get(owner);
            HBox ownerBox = new HBox(6, totemDot(player == null ? null : player.getTotemColor()), new Label(owner));
            ownerBox.setAlignment(Pos.CENTER);
            ownerBox.getStyleClass().add("offer-owner");
            content.getChildren().add(ownerBox);
        }

        Button button = new Button();
        button.getStyleClass().add(owner == null ? "offer-tile" : "offer-tile-occupied");
        button.setGraphic(content);
        button.setPrefSize(154, 146);
        return button;
    }

    private VBox cardRow(String title, List<Integer> ids, boolean upper, boolean building) {
        VBox box = new VBox(8);
        box.getStyleClass().add("card-row");
        box.getChildren().add(sectionTitle(title));
        FlowPane cards = new FlowPane(12, 12);
        cards.setAlignment(Pos.CENTER_LEFT);
        for (Integer id : ids) {
            Node card = cardNode(id, () -> {
                if (!model.isMyTurn()) {
                    return;
                }
                if (building) {
                    controller.send(new GameActionMessage(new PickBuildingAction(String.valueOf(id), upper)));
                } else {
                    controller.send(new GameActionMessage(new PickCardAction(String.valueOf(id), upper)));
                }
            });
            card.setDisable(!model.isMyTurn());
            GuiAnimations.addHoverScale(card);
            cards.getChildren().add(card);
        }
        box.getChildren().add(cards);
        return box;
    }

    private Node cardNode(int cardId, Runnable action) {
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        StackPane card = new StackPane();
        card.getStyleClass().add("card");
        card.setPrefSize(124, 176);
        card.setOnMouseClicked(event -> action.run());

        if (asset.isPresent() && asset.get().isFrontAvailable()) {
            try (InputStream stream = assetCatalog.openResource(asset.get().getFrontPath()).orElse(null)) {
                if (stream != null) {
                    ImageView image = new ImageView(new Image(stream));
                    image.setFitWidth(124);
                    image.setFitHeight(176);
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

    private List<Character> activeOfferTiles() {
        int players = Math.max(2, model.getPlayers().isEmpty()
                ? model.getExpectedPlayers()
                : model.getPlayers().size());
        return TILE_ORDER.stream()
                .filter(tile -> TILE_MIN_PLAYERS.get(tile) <= players)
                .collect(Collectors.toList());
    }

    private List<String> visibleTurnOrder() {
        if ("ActionResolutionState".equals(model.getGameState())) {
            List<String> ordered = actionResolutionOrder();
            return ordered.isEmpty() ? model.getTurnOrder() : ordered;
        }

        List<String> placedPlayers = List.copyOf(model.getTotemOnTiles().values());
        return model.getTurnOrder().stream()
                .filter(name -> !placedPlayers.contains(name))
                .filter(name -> !model.isPlayerDisconnected(name))
                .collect(Collectors.toList());
    }

    private List<String> actionResolutionOrder() {
        return TILE_ORDER.stream()
                .map(tile -> model.getTotemOnTiles().get(tile))
                .filter(nickname -> nickname != null && !model.isPlayerDisconnected(nickname))
                .distinct()
                .collect(Collectors.toList());
    }

    private String turnEffectLabel(int position) {
        int numPlayers = Math.max(2, Math.max(model.getPlayers().size(), model.getTurnOrder().size()));
        if (numPlayers == 2) {
            return position == 1 ? "+2 cibo" : "-";
        }
        if (numPlayers == 3) {
            return position == 1 ? "+2 cibo" : position == 3 ? "-1 cibo +3 PP" : "-";
        }
        return switch (position) {
            case 1 -> "+2 cibo";
            case 3 -> "-1 cibo +3 PP";
            case 4 -> "-";
            case 5 -> "-2 cibo +6 PP";
            default -> "-";
        };
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

    private Region totemDot(TotemColor color) {
        Region dot = new Region();
        dot.getStyleClass().add("totem-dot");
        dot.setStyle("-fx-background-color: " + totemColorHex(color) + ";");
        return dot;
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

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
