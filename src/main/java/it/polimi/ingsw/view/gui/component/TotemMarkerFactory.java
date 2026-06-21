package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.TotemPalette;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Optional;

// Builds the little totem markers (the colored player pieces) shown on the board.
final class TotemMarkerFactory {
    private static final Object BOARD_TOTEM_OWNER_KEY = new Object();

    private final GuiAssetCatalog assetCatalog;

    TotemMarkerFactory(GuiAssetCatalog assetCatalog) {
        this.assetCatalog = assetCatalog;
    }

    StackPane create(TotemColor color, String owner) {
        return create(color, owner, 52);
    }

    StackPane create(TotemColor color, String owner, double fitHeight) {
        StackPane marker = baseMarker("tile-totem-marker", owner, Math.max(18, fitHeight * 0.72), fitHeight);
        if (addTotemImage(marker, color, fitHeight)) {
            return marker;
        }

        Region body = new Region();
        body.getStyleClass().add("tile-totem-body");
        double fallbackSize = Math.max(16, fitHeight * 0.64);
        setFixedSize(body, fallbackSize, fallbackSize);
        body.setStyle("-fx-background-color: " + colorHex(color) + ";");
        marker.getChildren().add(body);
        return marker;
    }

    StackPane createLobbyMarker(TotemColor color) {
        StackPane marker = baseMarker("lobby-totem-marker", null, 36, 50);
        if (addTotemImage(marker, color, 50)) {
            return marker;
        }

        Region fallback = new Region();
        fallback.getStyleClass().add("lobby-color-dot");
        fallback.setStyle("-fx-background-color: " + colorHex(color) + ";");
        marker.getChildren().add(fallback);
        return marker;
    }

    String colorHex(TotemColor color) {
        return TotemPalette.fillHex(color);
    }

    static void tagBoardTotem(Node node, String owner) {
        if (node != null && owner != null) {
            node.getProperties().put(BOARD_TOTEM_OWNER_KEY, owner);
        }
    }

    static String boardTotemOwner(Node node) {
        if (node == null) {
            return null;
        }
        Object owner = node.getProperties().get(BOARD_TOTEM_OWNER_KEY);
        return owner == null ? null : owner.toString();
    }

    private StackPane baseMarker(String styleClass, String owner, double width, double height) {
        StackPane marker = new StackPane();
        marker.getStyleClass().add(styleClass);
        marker.setAccessibleText(owner == null ? "Totem" : "Totem di " + owner);
        setFixedSize(marker, width, height);
        return marker;
    }

    private boolean addTotemImage(StackPane marker, TotemColor color, double fitHeight) {
        Optional<Image> cached = assetCatalog.image(assetPath(color));
        if (cached.isPresent()) {
            ImageView image = new ImageView(cached.get());
            image.setPreserveRatio(true);
            image.setFitHeight(fitHeight);
            marker.getChildren().add(image);
            return true;
        }
        return false;
    }

    private String assetPath(TotemColor color) {
        return TotemPalette.assetPath(color);
    }

    private void setFixedSize(Region region, double width, double height) {
        GuiViews.setFixedSize(region, width, height);
    }
}
