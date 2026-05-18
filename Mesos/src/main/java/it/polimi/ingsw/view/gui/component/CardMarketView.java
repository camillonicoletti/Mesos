package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

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

    private VBox cardRow(List<Integer> ids, boolean upper, boolean building) {
        VBox box = new VBox(4);
        box.getStyleClass().add("card-row");

        HBox cards = new HBox(8);
        cards.setAlignment(Pos.CENTER);

        for (Integer id : ids) {
            boolean isEvent = !building && model.isEventCard(id);
            boolean canTake = !isEvent && model.isMyTurn() && model.canCurrentActionPlayerPickFromRow(upper);
            Node card = cardNode(id, canTake, () -> pickCard(id, upper, building, canTake));
            GuiAnimations.addHoverScale(card);
            cards.getChildren().add(card);
        }

        box.getChildren().add(cards);
        return box;
    }

    private Node cardNode(int cardId, boolean canTake, Runnable action) {
        StackPane card = new StackPane();
        card.getStyleClass().add("card");
        if (canTake) {
            card.getStyleClass().add("card-pickable");
        }
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setOnMouseClicked(event -> cardSelectionPresenter.showCard(cardId, canTake, action));
        card.getChildren().add(cardImages.create(cardId, CARD_WIDTH, CARD_HEIGHT));
        return card;
    }

    private void pickCard(int cardId, boolean upper, boolean building, boolean canTake) {
        if (!canTake) {
            return;
        }

        if (building) {
            controller.send(new GameActionMessage(new PickBuildingAction(String.valueOf(cardId), upper)));
        } else {
            controller.send(new GameActionMessage(new PickCardAction(String.valueOf(cardId), upper)));
        }
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
}
