package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Optional;

final class DeckView {
    private static final double CARD_WIDTH = 96;
    private static final double CARD_HEIGHT = 135;

    private final ClientModel model;
    private final GuiAssetCatalog assetCatalog;
    private StackPane cardBackNode;

    DeckView(ClientModel model, GuiAssetCatalog assetCatalog) {
        this.model = model;
        this.assetCatalog = assetCatalog;
    }

    VBox render() {
        VBox panel = new VBox(4);
        panel.getStyleClass().add("deck-view");
        panel.setAlignment(Pos.CENTER);
        panel.getChildren().add(GuiViews.sectionTitle("Tribe Deck"));

        StackPane cardBack = new StackPane();
        cardBack.getStyleClass().add("deck-placeholder");
        cardBack.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        addDeckBack(cardBack);
        this.cardBackNode = cardBack;

        Label count = new Label(deckCountText());
        count.getStyleClass().add("deck-count");

        panel.getChildren().addAll(cardBack, count);
        return panel;
    }

    StackPane getCardBackNode() {
        return cardBackNode;
    }

    private void addDeckBack(StackPane cardBack) {
        String backPath = "assets/cards/backs/" + eraBackKey(model.getCurrentEra()) + ".png";
        Optional<Image> cached = assetCatalog.image(backPath);
        if (cached.isPresent()) {
            ImageView img = new ImageView(cached.get());
            img.setFitWidth(CARD_WIDTH);
            img.setFitHeight(CARD_HEIGHT);
            img.setPreserveRatio(true);
            cardBack.getChildren().add(img);
            return;
        }

        Label placeholder = new Label(model.getCurrentEra() == null ? "TRIBE" : "ERA " + model.getCurrentEra().name());
        placeholder.getStyleClass().add("deck-placeholder-label");
        cardBack.getChildren().add(placeholder);
    }

    private String deckCountText() {
        int remaining = model.getRemainingTribeDeckCards();
        String countText = remaining < 0 ? "?" : String.valueOf(remaining);
        return countText + " cards";
    }

    private String eraBackKey(Era era) {
        String suffix = era == null ? "i" : era.name().toLowerCase(Locale.ROOT);
        return "back_character_era_" + suffix;
    }
}
