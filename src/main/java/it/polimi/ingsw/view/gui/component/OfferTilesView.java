package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Shows the offer tiles (A-G) where players place their totems to pick an action.
final class OfferTilesView {
    private static final double TILE_WIDTH = 100;
    private static final double TILE_HEIGHT = 164;
    private static final double TOTEM_HEIGHT = 42;
    private static final double TOTEM_SLOT_Y = -47;
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
    private final TotemMarkerFactory totemMarkers;

    OfferTilesView(ClientController controller,
                   ClientModel model,
                   GuiAssetCatalog assetCatalog,
                   TotemMarkerFactory totemMarkers) {
        this.controller = controller;
        this.model = model;
        this.assetCatalog = assetCatalog;
        this.totemMarkers = totemMarkers;
    }

    VBox render() {
        VBox panel = new VBox(4);
        panel.getStyleClass().add("offer-strip");

        HBox pane = new HBox(4);
        pane.setAlignment(Pos.CENTER);

        for (char tile : activeTiles()) {
            String owner = model.getTotemOnTiles().get(tile);
            Button button = offerTileButton(tile, owner);
            button.setOnAction(event -> placeTotem(tile, owner));
            GuiAnimations.addHoverScale(button);
            pane.getChildren().add(button);
        }

        panel.getChildren().add(pane);
        return panel;
    }

    private Button offerTileButton(char tile, String owner) {
        StackPane content = new StackPane();
        content.setAlignment(Pos.CENTER);
        addTileImage(content, tile);
        addPlacedTotem(content, owner);

        Button button = new Button();
        button.getStyleClass().add(owner == null ? "offer-tile" : "offer-tile-occupied");
        button.setGraphic(content);
        button.setPrefSize(TILE_WIDTH + 8, TILE_HEIGHT + 8);
        button.setDisable(owner != null);
        return button;
    }

    private void addTileImage(StackPane content, char tile) {
        String path = "assets/board/offer_" + tile + ".png";
        Optional<Image> cached = assetCatalog.image(path);
        if (cached.isPresent()) {
            ImageView view = new ImageView(cached.get());
            view.setPreserveRatio(true);
            view.setFitWidth(TILE_WIDTH);
            view.setFitHeight(TILE_HEIGHT);
            content.getChildren().add(view);
            return;
        }

        Label letter = new Label(String.valueOf(tile));
        letter.getStyleClass().add("offer-letter");
        content.getChildren().add(letter);
    }

    private void addPlacedTotem(StackPane content, String owner) {
        if (owner == null) {
            return;
        }

        PlayerView player = model.getPlayers().get(owner);
        TotemColor color = player == null ? null : player.getTotemColor();
        StackPane badge = totemMarkers.create(color, owner, TOTEM_HEIGHT);
        TotemMarkerFactory.tagBoardTotem(badge, owner);
        if (model.isPlayerDisconnected(owner)) {
            badge.getStyleClass().add("tile-totem-disconnected");
        }
        badge.setTranslateX(0);
        badge.setTranslateY(TOTEM_SLOT_Y);

        content.getChildren().add(badge);
        StackPane.setAlignment(badge, Pos.CENTER);
    }

    private void placeTotem(char tile, String owner) {
        if (!model.isMyTotemPlacementTurn() || owner != null) {
            return;
        }

        controller.placeTotem(tile);
    }

    private List<Character> activeTiles() {
        int players = Math.max(2, model.getPlayers().isEmpty()
                ? model.getExpectedPlayers()
                : model.getPlayers().size());

        return TILE_ORDER.stream()
                .filter(tile -> TILE_MIN_PLAYERS.get(tile) <= players)
                .collect(Collectors.toList());
    }
}
