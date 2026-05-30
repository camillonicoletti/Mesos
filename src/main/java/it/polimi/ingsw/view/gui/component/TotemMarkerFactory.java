package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.io.InputStream;

final class TotemMarkerFactory {
    private static final String BOARD_TOTEM_PREFIX = "board-totem:";

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

    static void tagBoardTotem(Node node, String owner) {
        if (node != null && owner != null) {
            node.setUserData(BOARD_TOTEM_PREFIX + owner);
        }
    }

    static String boardTotemOwner(Node node) {
        if (node == null || !(node.getUserData() instanceof String tag)) {
            return null;
        }
        return tag.startsWith(BOARD_TOTEM_PREFIX)
                ? tag.substring(BOARD_TOTEM_PREFIX.length())
                : null;
    }

    private StackPane baseMarker(String styleClass, String owner, double width, double height) {
        StackPane marker = new StackPane();
        marker.getStyleClass().add(styleClass);
        marker.setAccessibleText(owner == null ? "Totem" : "Totem di " + owner);
        setFixedSize(marker, width, height);
        return marker;
    }

    private boolean addTotemImage(StackPane marker, TotemColor color, double fitHeight) {
        try (InputStream stream = assetCatalog.openResource(assetPath(color)).orElse(null)) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitHeight(fitHeight);
                marker.getChildren().add(image);
                return true;
            }
        } catch (Exception ignored) {
            // Fallback marker is added by the caller.
        }

        return false;
    }

    private String assetPath(TotemColor color) {
        return switch (color == null ? TotemColor.WHITE : color) {
            case RED -> "assets/tokens/totem_red.png";
            case YELLOW -> "assets/tokens/totem_yellow.png";
            case BLUE -> "assets/tokens/totem_blue.png";
            case PURPLE -> "assets/tokens/totem_purple.png";
            case WHITE -> "assets/tokens/totem_white.png";
        };
    }

    private void setFixedSize(Region region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }
}
