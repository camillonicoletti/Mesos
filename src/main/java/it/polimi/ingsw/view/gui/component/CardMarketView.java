package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

// Builds the rows of cards on the table that players can look at and take.
// Makes the character row, the building row and the small card used in animations.
final class CardMarketView {
    private static final double CARD_WIDTH = 96;
    private static final double CARD_HEIGHT = 135;

    private final ClientController controller;
    private final ClientModel model;
    private final CardImageFactory cardImages;
    private final CardSelectionPresenter cardSelectionPresenter;

    CardMarketView(ClientController controller,
                   ClientModel model,
                   CardImageFactory cardImages,
                   CardSelectionPresenter cardSelectionPresenter) {
        this.controller = controller;
        this.model = model;
        this.cardImages = cardImages;
        this.cardSelectionPresenter = cardSelectionPresenter;
    }

    VBox characterRow(boolean upper) {
        return cardRow(modelCards(upper, false), upper, false);
    }

    VBox buildingRow(boolean upper) {
        return cardRow(modelCards(upper, true), upper, true);
    }

    Node animationCard(int cardId, double width, double height) {
        StackPane card = new StackPane();
        card.getStyleClass().addAll("card", "card-flight");
        card.setMinSize(width, height);
        card.setPrefSize(width, height);
        card.setMaxSize(width, height);
        card.getChildren().add(cardImages.create(cardId, width, height));
        return card;
    }

    private VBox cardRow(List<Integer> ids, boolean upper, boolean building) {
        VBox box = new VBox(4);
        box.getStyleClass().add("card-row");

        HBox cards = new HBox(8);
        cards.setAlignment(Pos.CENTER);

        for (Integer id : ids) {
            Node card = cardNode(id, upper, building);
            GuiAnimations.addHoverScale(card);
            cards.getChildren().add(card);
        }

        box.getChildren().add(cards);
        return box;
    }

    private Node cardNode(int cardId, boolean upper, boolean building) {
        StackPane card = new StackPane();
        card.getStyleClass().add("card");
        if (canTakeNow(cardId, upper, building)) {
            card.getStyleClass().add("card-pickable");
        }
        card.setUserData(new MarketCardRef(cardId, upper, building));
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setOnMouseClicked(event -> {
            boolean canTake = canTakeNow(cardId, upper, building);
            cardSelectionPresenter.showCard(cardId, canTake, () -> pickCard(cardId, upper, building));
        });
        card.getChildren().add(cardImages.create(cardId, CARD_WIDTH, CARD_HEIGHT));
        return card;
    }

    private void pickCard(int cardId, boolean upper, boolean building) {
        if (!canTakeNow(cardId, upper, building)) {
            return;
        }

        if (building) {
            controller.pickBuildingCard(String.valueOf(cardId), upper);
        } else {
            controller.pickCharacterCard(String.valueOf(cardId), upper);
        }
    }

    private boolean canTakeNow(int cardId, boolean upper, boolean building) {
        boolean isEvent = !building && model.isEventCard(cardId);
        return !isEvent
                && modelCards(upper, building).contains(cardId)
                && model.isMyTurn()
                && model.canCurrentActionPlayerPickFromRow(upper);
    }

    private List<Integer> modelCards(boolean upper, boolean building) {
        if (building) {
            return upper ? model.getUpperRowBuildingIds() : model.getLowerRowBuildingIds();
        }
        return upper ? model.getUpperRowCharacterIds() : model.getLowerRowCharacterIds();
    }

    @FunctionalInterface
    interface CardSelectionPresenter {
        void showCard(int cardId, boolean canTake, Runnable takeAction);
    }

    record MarketCardRef(int cardId, boolean upper, boolean building) {}
}
