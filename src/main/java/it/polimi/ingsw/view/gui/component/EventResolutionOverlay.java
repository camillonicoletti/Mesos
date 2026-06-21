package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.GamePhase;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;
import it.polimi.ingsw.view.gui.util.TotemPalette;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Full-screen overlay shown while an event is being resolved.
// Shows the big event card and how many points/food each player gained or lost.
final class EventResolutionOverlay {
    private static final String OVERLAY_STYLE = "event-resolution-overlay";

    private final ClientModel model;
    private final CardImageFactory cardImages;
    private final TotemMarkerFactory totemMarkers;

    EventResolutionOverlay(ClientModel model, CardImageFactory cardImages, TotemMarkerFactory totemMarkers) {
        this.model = model;
        this.cardImages = cardImages;
        this.totemMarkers = totemMarkers;
    }


    // Shows the overlay if an event is being resolved, otherwise removes it.
    void refresh(StackPane rootLayer) {
        if (rootLayer == null) {
            return;
        }
        hide(rootLayer);

        if (!GamePhase.fromStateName(model.getGameState()).isEventResolution()) {
            return;
        }

        StackPane overlay = new StackPane(panel(rootLayer));
        overlay.getStyleClass().add(OVERLAY_STYLE);
        overlay.setMouseTransparent(true);
        overlay.setPadding(new Insets(40));
        StackPane.setAlignment(overlay.getChildren().get(0), Pos.CENTER);
        rootLayer.getChildren().add(overlay);
    }

    // Removes the overlay from the screen.
    void hide(StackPane rootLayer) {
        if (rootLayer == null) {
            return;
        }
        rootLayer.getChildren().removeIf(node -> node.getStyleClass().contains(OVERLAY_STYLE));
    }

    // Builds the whole panel: the big card on the left and the texts/table on the right.
    private Node panel(StackPane rootLayer) {
        HBox panel = new HBox(28);
        panel.getStyleClass().add("event-resolution-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(Region.USE_PREF_SIZE);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.prefWidthProperty().bind(rootLayer.widthProperty().subtract(120));
        panel.prefHeightProperty().bind(rootLayer.heightProperty().subtract(120));

        VBox leftColumn = new VBox(10);
        leftColumn.getStyleClass().add("event-resolution-left");
        leftColumn.setAlignment(Pos.CENTER);
        leftColumn.getChildren().add(eventCardPreviewLarge());
        HBox.setHgrow(leftColumn, Priority.SOMETIMES);

        VBox rightColumn = new VBox(14);
        rightColumn.getStyleClass().add("event-resolution-right");
        rightColumn.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        Label eyebrow = new Label("RESOLUTION EVENT");
        eyebrow.getStyleClass().add("event-resolution-eyebrow");

        Label title = new Label(GuiViews.nullSafe(model.getLastResolvedEventTitle()).toUpperCase());
        title.getStyleClass().add("event-resolution-title");
        title.setWrapText(true);

        Region divider = new Region();
        divider.getStyleClass().add("event-resolution-divider");

        Label description = new Label(model.getLastResolvedEventDescription() == null
                ? "Resolving the next event..."
                : model.getLastResolvedEventDescription());
        description.getStyleClass().add("event-resolution-description");
        description.setWrapText(true);

        rightColumn.getChildren().addAll(eyebrow, title, divider, description, eventEffectsTable());
        panel.getChildren().addAll(leftColumn, rightColumn);
        return panel;
    }

    // Builds the big event card image (or a text placeholder if there is no card).
    private Node eventCardPreviewLarge() {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("event-resolution-card-frame");
        frame.setMinSize(320, 450);
        int cardId = model.getLastResolvedEventCardId();
        if (cardId > 0) {
            Node image = cardImages.create(cardId, 320, 450);
            frame.getChildren().add(image);
        } else {
            Label placeholder = new Label("Event card");
            placeholder.getStyleClass().add("event-resolution-card-placeholder");
            placeholder.setAlignment(Pos.CENTER);
            GuiViews.setFixedSize(placeholder, 320, 450);
            frame.getChildren().add(placeholder);
        }
        return frame;
    }

    // Builds the table with one row per player, sorted from best to worst result.
    private Node eventEffectsTable() {
        VBox table = new VBox(8);
        table.getStyleClass().add("event-resolution-table");

        HBox header = new HBox(
                eventHeaderCell("PLAYER", 240),
                eventHeaderCell("PP", 120),
                eventHeaderCell("FOOD", 120)
        );
        header.getStyleClass().add("event-resolution-table-header");
        table.getChildren().add(header);

        List<EventResolvedMessage.PlayerEffect> effects =
                new ArrayList<>(model.getLastResolvedEventEffects());
        effects.sort(Comparator
                .comparingInt(EventResolvedMessage.PlayerEffect::getPointsDelta).reversed()
                .thenComparing(Comparator.comparingInt(EventResolvedMessage.PlayerEffect::getFoodDelta).reversed())
                .thenComparing(EventResolvedMessage.PlayerEffect::getNickname));

        if (effects.isEmpty()) {
            Label waiting = new Label("Waiting for event results");
            waiting.getStyleClass().add("event-resolution-empty");
            table.getChildren().add(waiting);
            return table;
        }

        int rank = 1;
        for (EventResolvedMessage.PlayerEffect effect : effects) {
            table.getChildren().add(eventEffectRow(rank, effect));
            rank++;
        }
        return table;
    }

    // Builds one title cell of the table header (PLAYER, PP, FOOD).
    private Label eventHeaderCell(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("event-resolution-header-cell");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        if (!"PLAYER".equals(text)) {
            label.setAlignment(Pos.CENTER);
        }
        return label;
    }

    // Builds one table row: rank, totem, name and the player's PP and food changes.
    private Node eventEffectRow(int rank, EventResolvedMessage.PlayerEffect effect) {
        HBox row = new HBox(10);
        row.getStyleClass().add("event-resolution-row");
        row.setAlignment(Pos.CENTER_LEFT);

        HBox playerCell = new HBox(10);
        playerCell.setAlignment(Pos.CENTER_LEFT);
        playerCell.setMinWidth(232);
        playerCell.setPrefWidth(232);

        Label rankLabel = new Label("#" + rank);
        rankLabel.getStyleClass().add("event-resolution-rank");

        TotemColor color = totemOf(effect.getNickname());
        StackPane totem = totemMarkers.create(color, effect.getNickname(), 28);

        Label name = new Label(effect.getNickname().toUpperCase());
        name.getStyleClass().add("event-resolution-player");
        name.setStyle("-fx-text-fill: " + TotemPalette.textHex(color) + ";");

        playerCell.getChildren().addAll(rankLabel, totem, name);

        row.getChildren().addAll(
                playerCell,
                deltaCell(effect.getPointsDelta(), effect.getNewPoints()),
                deltaCell(effect.getFoodDelta(), effect.getNewFood())
        );
        return row;
    }

    // Builds one number cell: the change (+/-) on top and the new total below.
    private Node deltaCell(int delta, int total) {
        VBox cell = new VBox(2);
        cell.getStyleClass().add("event-resolution-delta-cell");
        cell.setMinWidth(120);
        cell.setPrefWidth(120);
        cell.setAlignment(Pos.CENTER);

        Label deltaLabel = new Label(formatDelta(delta));
        deltaLabel.getStyleClass().add("event-resolution-delta");
        if (delta > 0) {
            deltaLabel.getStyleClass().add("event-resolution-delta-positive");
        } else if (delta < 0) {
            deltaLabel.getStyleClass().add("event-resolution-delta-negative");
        } else {
            deltaLabel.getStyleClass().add("event-resolution-delta-neutral");
        }

        Label totalLabel = new Label("tot " + total);
        totalLabel.getStyleClass().add("event-resolution-total");
        cell.getChildren().addAll(deltaLabel, totalLabel);
        return cell;
    }

    // Turns a number into text, adding a "+" in front when it is positive.
    private String formatDelta(int delta) {
        if (delta > 0) {
            return "+" + delta;
        }
        return String.valueOf(delta);
    }

    // Finds the totem color of a player from their nickname.
    private TotemColor totemOf(String nickname) {
        PlayerView player = model.getPlayers().get(nickname);
        return player == null ? null : player.getTotemColor();
    }

}
