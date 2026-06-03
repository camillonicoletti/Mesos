package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

final class TurnOrderView {
    private static final double TILE_WIDTH = 100;
    private static final double TILE_HEIGHT = 164;
    private static final double DETAIL_TILE_WIDTH = 180;
    private static final double DETAIL_TILE_HEIGHT = 290;
    private static final double TOTEM_HEIGHT = 42;

    private final ClientModel model;
    private final GuiAssetCatalog assetCatalog;
    private final TotemMarkerFactory totemMarkers;
    private final Function<String, VBox> panelFactory;
    private final BiFunction<String, Boolean, Button> buttonFactory;
    private final BiConsumer<Node, Pos> showInlinePanel;
    private final Runnable hideInlinePanel;

    TurnOrderView(ClientModel model,
                  GuiAssetCatalog assetCatalog,
                  TotemMarkerFactory totemMarkers,
                  Function<String, VBox> panelFactory,
                  BiFunction<String, Boolean, Button> buttonFactory,
                  BiConsumer<Node, Pos> showInlinePanel,
                  Runnable hideInlinePanel) {
        this.model = model;
        this.assetCatalog = assetCatalog;
        this.totemMarkers = totemMarkers;
        this.panelFactory = panelFactory;
        this.buttonFactory = buttonFactory;
        this.showInlinePanel = showInlinePanel;
        this.hideInlinePanel = hideInlinePanel;
    }

    Node render() {
        StackPane tile = new StackPane();
        tile.getStyleClass().addAll("turn-order-tile", "turn-order-tile-selectable");

        addTileImage(tile, playerCount(), TILE_WIDTH, TILE_HEIGHT, "Turn");
        addTotems(tile);

        tile.setPickOnBounds(true);
        tile.setOnMouseClicked(event -> showDetails());
        GuiAnimations.addHoverScale(tile);

        return tile;
    }

    private void showDetails() {
        int players = playerCount();
        HBox panel = new HBox(36);
        panel.getStyleClass().addAll("card-focus-panel", "turn-order-focus-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane tileFrame = new StackPane();
        tileFrame.getStyleClass().add("card-focus-frame");
        addTileImage(tileFrame, players, DETAIL_TILE_WIDTH, DETAIL_TILE_HEIGHT,
                "Order — " + players + " players");

        VBox leftCol = new VBox(tileFrame);
        leftCol.getStyleClass().add("card-focus-left");
        leftCol.setAlignment(Pos.CENTER);

        VBox rightCol = new VBox(18);
        rightCol.getStyleClass().add("card-focus-right");
        rightCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.setMaxWidth(560);
        rightCol.setPrefWidth(560);

        Label eyebrow = new Label("BOARD TILE");
        eyebrow.getStyleClass().add("card-focus-eyebrow");

        Label title = new Label("TURN ORDER TILE");
        title.getStyleClass().add("card-focus-title");
        title.setWrapText(true);

        Region divider = new Region();
        divider.getStyleClass().add("card-focus-divider");

        VBox details = new VBox(8);
        details.getStyleClass().add("card-focus-body");
        for (String line : ruleLines(players)) {
            Label label = new Label(line);
            label.setWrapText(true);
            label.getStyleClass().add("card-focus-line");
            details.getChildren().add(label);
        }

        Button close = buttonFactory.apply("Close", false);
        close.getStyleClass().addAll("card-focus-action-button", "card-focus-close-button");
        close.setMinSize(156, 52);
        close.setOnAction(event -> hideInlinePanel.run());
        HBox actions = new HBox(close);
        actions.getStyleClass().add("card-focus-actions");
        actions.setAlignment(Pos.CENTER);

        rightCol.getChildren().addAll(eyebrow, title, divider, details, actions);
        panel.getChildren().addAll(leftCol, rightCol);
        showInlinePanel.accept(panel, Pos.CENTER);
    }

    private void addTileImage(StackPane target, int players, double width, double height, String fallbackText) {
        String path = "assets/board/turn_order_" + players + ".png";
        Optional<Image> cached = assetCatalog.image(path);
        if (cached.isPresent()) {
            ImageView image = new ImageView(cached.get());
            image.setPreserveRatio(true);
            image.setFitWidth(width);
            image.setFitHeight(height);
            target.getChildren().add(image);
            return;
        }

        Label fallback = new Label(fallbackText);
        fallback.getStyleClass().add("turn-name");
        target.getChildren().add(fallback);
    }

    private void addTotems(StackPane tile) {
        List<String> order = model.getTurnOrder();
        if (order.isEmpty()) {
            return;
        }

        double[] offsets = totemOffsets(playerCount());
        Map<String, Integer> positions = model.getTurnOrderPositions();
        for (int index = 0; index < order.size(); index++) {
            String nickname = order.get(index);
            PlayerView player = model.getPlayers().get(nickname);
            if (player == null || model.isPlayerDisconnected(nickname)) {
                continue;
            }

            int slotIndex = positions.getOrDefault(nickname, index + 1) - 1;
            if (slotIndex < 0 || slotIndex >= offsets.length) {
                continue;
            }

            StackPane marker = totemMarkers.create(player.getTotemColor(), player.getNickname(), TOTEM_HEIGHT);
            TotemMarkerFactory.tagBoardTotem(marker, nickname);
            marker.setTranslateY(offsets[slotIndex]);
            tile.getChildren().add(marker);
            StackPane.setAlignment(marker, Pos.CENTER);
        }
    }

    private int playerCount() {
        int players = model.getPlayers().isEmpty()
                ? model.getExpectedPlayers()
                : model.getPlayers().size();
        players = Math.max(players, model.getTurnOrder().size());
        return Math.max(2, Math.min(5, players));
    }

    private double[] totemOffsets(int players) {
        return switch (players) {
            case 2 -> new double[]{-39, -13};
            case 3 -> new double[]{-46, -20, 6};
            case 4 -> new double[]{-52, -26, 0, 25};
            default -> new double[]{-62, -36, -10, 15, 41};
        };
    }

    private List<String> ruleLines(int players) {
        List<String> lines = new ArrayList<>();
        lines.add("Order: in the next round, totems on this tile are read from top to bottom.");
        lines.add("End of action: after resolving an offer tile, the totem returns to the highest free slot.");

        List<String> slotEffects = switch (players) {
            case 2 -> List.of("+1 food", "pay 1 food; if you can't, lose 2 PP");
            case 3 -> List.of("+2 food", "no effect", "pay 1 food; if you can't, lose 2 PP");
            case 4 -> List.of("+2 food", "+1 food", "no effect", "pay 1 food; if you can't, lose 2 PP");
            default -> List.of("+3 food", "+1 food", "no effect", "no effect",
                    "pay 1 food; if you can't, lose 2 PP");
        };

        for (int index = 0; index < slotEffects.size(); index++) {
            lines.add("Slot " + (index + 1) + ": " + slotEffects.get(index) + ".");
        }
        lines.add("The slot bonus or penalty applies immediately when the totem returns to the tile.");
        return lines;
    }
}
