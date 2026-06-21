package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientModel;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

// Shows a single card up close: big image, its description and Close/Take buttons.
// Used both to just look at a card and to confirm taking it.
final class CardFocusView {
    private static final double DETAIL_CARD_WIDTH = 330;
    private static final double DETAIL_CARD_HEIGHT = 466;

    private final ClientModel model;
    private final CardDescriptionService cardDescriptions;
    private final CardImageFactory cardImages;
    private final OverlayController overlay;

    CardFocusView(ClientModel model,
                  CardDescriptionService cardDescriptions,
                  CardImageFactory cardImages,
                  OverlayController overlay) {
        this.model = model;
        this.cardDescriptions = cardDescriptions;
        this.cardImages = cardImages;
        this.overlay = overlay;
    }

    void show(int cardId, boolean canTake, Runnable takeAction) {
        HBox panel = new HBox(36);
        panel.getStyleClass().add("card-focus-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        // Left column: large card image inside a golden frame
        StackPane cardFrame = new StackPane();
        cardFrame.getStyleClass().add("card-focus-frame");
        cardFrame.getChildren().add(cardImages.create(cardId, DETAIL_CARD_WIDTH, DETAIL_CARD_HEIGHT));
        VBox leftCol = new VBox(cardFrame);
        leftCol.getStyleClass().add("card-focus-left");
        leftCol.setAlignment(Pos.CENTER);

        // Right column: header, description lines, action buttons
        VBox rightCol = new VBox(18);
        rightCol.getStyleClass().add("card-focus-right");
        rightCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.setMaxWidth(460);
        rightCol.setPrefWidth(460);

        Label eyebrow = new Label(canTake ? "CONFIRM CARD" : "CARD DETAILS");
        eyebrow.getStyleClass().add("card-focus-eyebrow");

        List<String> lines = cardDescriptions.describeCard(
                cardId,
                model.getLastResolvedEventCardId(),
                model.getLastResolvedEventDescription());
        String headline = lines.isEmpty() ? "" : lines.get(0);
        Label title = new Label(headline.toUpperCase());
        title.getStyleClass().add("card-focus-title");
        title.setWrapText(true);

        Region divider = new Region();
        divider.getStyleClass().add("card-focus-divider");

        VBox body = new VBox(8);
        body.getStyleClass().add("card-focus-body");
        for (int i = 1; i < lines.size(); i++) {
            Label label = new Label(lines.get(i));
            label.setWrapText(true);
            label.getStyleClass().add("card-focus-line");
            body.getChildren().add(label);
        }

        Label prompt = new Label(canTake
                ? "Do you want to take this card?"
                : "Card selected");
        prompt.getStyleClass().add("card-focus-prompt");
        prompt.setWrapText(true);

        HBox actions = new HBox(10);
        actions.getStyleClass().add("card-focus-actions");
        actions.setAlignment(Pos.CENTER);
        Button close = overlay.inlineButton("Close", false);
        close.getStyleClass().addAll("card-focus-action-button", "card-focus-close-button");
        close.setMinSize(156, 52);
        close.setOnAction(event -> overlay.hideInlinePanel());
        actions.getChildren().add(close);
        if (canTake) {
            Button take = overlay.inlineButton("Take", true);
            take.getStyleClass().addAll("card-focus-action-button", "card-focus-take-button");
            take.setMinSize(156, 52);
            take.setOnAction(event -> {
                overlay.hideInlinePanel();
                if (takeAction != null) {
                    takeAction.run();
                }
            });
            actions.getChildren().add(take);
        }

        rightCol.getChildren().addAll(eyebrow, title, divider, body, prompt, actions);
        panel.getChildren().addAll(leftCol, rightCol);

        overlay.showInlinePanel(panel, Pos.CENTER);
    }
}
