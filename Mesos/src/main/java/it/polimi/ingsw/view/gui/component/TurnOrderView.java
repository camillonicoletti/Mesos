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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

final class TurnOrderView {
    private static final double TILE_WIDTH = 100;
    private static final double TILE_HEIGHT = 164;
    private static final double DETAIL_TILE_WIDTH = 180;
    private static final double DETAIL_TILE_HEIGHT = 290;
    private static final double TOTEM_HEIGHT = 34;

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

        addTileImage(tile, playerCount(), TILE_WIDTH, TILE_HEIGHT, "Turno");
        addTotems(tile);

        tile.setPickOnBounds(true);
        tile.setOnMouseClicked(event -> showDetails());
        GuiAnimations.addHoverScale(tile);

        return tile;
    }

    private void showDetails() {
        int players = playerCount();
        VBox panel = panelFactory.apply("Tessera Ordine di Turno");
        panel.getStyleClass().add("turn-order-focus-panel");

        HBox content = new HBox(16);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("turn-order-detail-content");

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("turn-order-detail-image");
        addTileImage(imageBox, players, DETAIL_TILE_WIDTH, DETAIL_TILE_HEIGHT,
                "Ordine " + players + " giocatori");

        VBox details = new VBox(8);
        details.getStyleClass().add("card-detail-lines");
        for (String line : ruleLines(players)) {
            Label label = new Label(line);
            label.setWrapText(true);
            label.getStyleClass().add("card-detail-line");
            details.getChildren().add(label);
        }

        content.getChildren().addAll(imageBox, details);

        Button close = buttonFactory.apply("Chiudi", false);
        close.setOnAction(event -> hideInlinePanel.run());
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(content, actions);
        showInlinePanel.accept(panel, Pos.CENTER);
    }

    private void addTileImage(StackPane target, int players, double width, double height, String fallbackText) {
        String path = "assets/board/turn_order_" + players + ".png";
        try (InputStream stream = assetCatalog.openResource(path).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitWidth(width);
                image.setFitHeight(height);
                target.getChildren().add(image);
                return;
            }
        } catch (Exception ignored) {
            // Fallback text is added below.
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
            case 2 -> new double[]{-34, 0};
            case 3 -> new double[]{-47, -11, 18};
            case 4 -> new double[]{-45, -15, 14, 42};
            default -> new double[]{-57, -28, 1, 29, 58};
        };
    }

    private List<String> ruleLines(int players) {
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
}
