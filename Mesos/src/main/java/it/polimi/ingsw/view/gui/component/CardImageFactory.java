package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;
import java.util.Optional;

final class CardImageFactory implements PlayerDockView.CardImageProvider {
    private final GuiAssetCatalog assetCatalog;

    CardImageFactory(GuiAssetCatalog assetCatalog) {
        this.assetCatalog = assetCatalog;
    }

    @Override
    public Node create(int cardId, double width, double height) {
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
}
