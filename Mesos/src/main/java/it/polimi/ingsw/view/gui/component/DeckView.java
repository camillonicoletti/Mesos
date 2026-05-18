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

import java.io.InputStream;

final class DeckView {
    private static final double CARD_WIDTH = 96;
    private static final double CARD_HEIGHT = 135;

    private final ClientModel model;
    private final GuiAssetCatalog assetCatalog;

    DeckView(ClientModel model, GuiAssetCatalog assetCatalog) {
        this.model = model;
        this.assetCatalog = assetCatalog;
    }

    VBox render() {
        VBox panel = new VBox(4);
        panel.getStyleClass().add("deck-view");
        panel.setAlignment(Pos.CENTER);
        panel.getChildren().add(sectionTitle("Mazzo Tribù"));

        StackPane cardBack = new StackPane();
        cardBack.getStyleClass().add("deck-placeholder");
        cardBack.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        addDeckBack(cardBack);

        Label count = new Label(deckCountText());
        count.getStyleClass().add("deck-count");

        panel.getChildren().addAll(cardBack, count);
        return panel;
    }

    private void addDeckBack(StackPane cardBack) {
        String backPath = "assets/cards/backs/" + eraBackKey(model.getCurrentEra()) + ".png";
        try (InputStream stream = assetCatalog.openResource(backPath).orElse(null)) {
            if (stream != null) {
                ImageView img = new ImageView(new Image(stream));
                img.setFitWidth(CARD_WIDTH);
                img.setFitHeight(CARD_HEIGHT);
                img.setPreserveRatio(true);
                cardBack.getChildren().add(img);
                return;
            }
        } catch (Exception ignored) {
            // Fallback label is added below.
        }

        Label placeholder = new Label(model.getCurrentEra() == null ? "TRIBÙ" : "ERA " + model.getCurrentEra().name());
        placeholder.getStyleClass().add("deck-placeholder-label");
        cardBack.getChildren().add(placeholder);
    }

    private String deckCountText() {
        int remaining = model.getRemainingTribeDeckCards();
        String countText = remaining < 0 ? "?" : String.valueOf(remaining);
        return countText + " carte";
    }

    private String eraBackKey(Era era) {
        if (era == null) {
            return "back_character_era_i";
        }
        return switch (era) {
            case I -> "back_character_era_i";
            case II -> "back_character_era_ii";
            case III -> "back_character_era_iii";
        };
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }
}
