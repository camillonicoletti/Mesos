package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.Optional;

// Builds the picture of a card from its id.
// If the image is missing, it shows a simple text placeholder instead.
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
            Optional<Image> cached = assetCatalog.image(asset.get().getFrontPath());
            if (cached.isPresent()) {
                ImageView image = new ImageView(cached.get());
                image.setFitWidth(width);
                image.setFitHeight(height);
                image.setPreserveRatio(true);
                card.getChildren().add(image);
                return card;
            }
        }

        Label placeholder = new Label(asset.map(assetCatalog::placeholderLabelFor).orElse("CARD #" + cardId));
        placeholder.setWrapText(true);
        placeholder.setAlignment(Pos.CENTER);
        card.getChildren().add(placeholder);

        return card;
    }
}
