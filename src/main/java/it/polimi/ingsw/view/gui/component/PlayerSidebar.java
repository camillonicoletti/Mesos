package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.GamePhase;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

public final class PlayerSidebar {
    private final ClientController controller;
    private final ClientModel model;

    public PlayerSidebar(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model = model;
    }

    public Node render() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(18));
        box.setPrefWidth(280);
        box.getStyleClass().add("side-panel");
        box.getChildren().addAll(statusBlock(), new Separator(), playersPanel(), new Separator(), disconnectButton());
        return box;
    }

    private VBox statusBlock() {
        VBox box = new VBox(8);
        box.getChildren().add(sectionTitle("Status"));
        box.getChildren().add(infoRow(MaterialDesignA.ACCOUNT_CIRCLE, "Nickname: " + nullSafe(model.getMyNickname())));
        box.getChildren().add(infoRow(MaterialDesignP.PINE_TREE, "Era: " + nullSafe(model.getCurrentEra())));
        box.getChildren().add(infoRow(MaterialDesignC.CALENDAR_CLOCK, "Round: " + model.getRoundNumber()));
        box.getChildren().add(infoRow(MaterialDesignP.PLAY_CIRCLE, "Turn: " + currentTurnText()));

        Label connection = new Label(model.isConnected() ? "Connected" : "Disconnected");
        connection.setGraphic(GuiIcons.icon(model.isConnected() ? MaterialDesignC.CONNECTION : MaterialDesignC.CLOSE_NETWORK, 15));
        connection.setContentDisplay(ContentDisplay.LEFT);
        connection.getStyleClass().add(model.isConnected() ? "ok" : "error");
        box.getChildren().add(connection);

        if (model.getLastConnectionEvent() != null) {
            Label event = new Label(model.getLastConnectionEvent());
            event.getStyleClass().add("notice");
            box.getChildren().add(event);
        }
        if (model.getLastTurnEvent() != null) {
            Label event = new Label(model.getLastTurnEvent());
            event.getStyleClass().add("notice");
            box.getChildren().add(event);
        }
        if (model.getLastError() != null) {
            box.getChildren().add(errorLabel());
        }
        return box;
    }

    private VBox playersPanel() {
        VBox box = new VBox(10);
        box.getChildren().add(sectionTitle("Players"));
        for (PlayerView player : model.getPlayers().values()) {
            boolean disconnected = model.isPlayerDisconnected(player.getNickname());
            VBox card = new VBox(3);
            card.getStyleClass().add(disconnected ? "player-card-disconnected" : "player-card");

            Label name = new Label(player.getNickname());
            name.setGraphic(GuiIcons.icon(disconnected ? MaterialDesignA.ACCOUNT_OFF : MaterialDesignA.ACCOUNT, 15));
            name.setContentDisplay(ContentDisplay.LEFT);
            name.getStyleClass().add(disconnected ? "disconnected" : "player-row");

            Label resources = new Label("Food: " + player.getFood() + "   PP: " + player.getPrestigePoints());
            resources.getStyleClass().add("muted");

            Label characters = new Label("Characters: " + player.getCharacterCardIds().size());
            characters.setGraphic(GuiIcons.icon(MaterialDesignA.ACCOUNT_MULTIPLE, 13));
            characters.setContentDisplay(ContentDisplay.LEFT);
            characters.getStyleClass().add("muted");

            Label buildings = new Label("Buildings: " + player.getBuildingCardIds().size());
            buildings.setGraphic(GuiIcons.icon(MaterialDesignP.PINE_TREE, 13));
            buildings.setContentDisplay(ContentDisplay.LEFT);
            buildings.getStyleClass().add("muted");

            card.getChildren().addAll(name, resources, characters, buildings);
            box.getChildren().add(card);
        }
        return box;
    }

    private Button disconnectButton() {
        Button quit = new Button("Quit");
        quit.getStyleClass().add("secondary-button");
        GuiIcons.decorateButton(quit, MaterialDesignL.LOGOUT);
        quit.setOnAction(event -> {
            controller.disconnect();
            Platform.exit();
        });
        return quit;
    }

    private HBox infoRow(Ikon ikon, String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        HBox row = new HBox(8, GuiIcons.icon(ikon, 15), label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("info-row");
        return row;
    }

    private String currentTurnText() {
        String turn = currentTurnNickname();
        return turn == null ? "-" : turn;
    }

    private String currentTurnNickname() {
        GamePhase phase = GamePhase.fromStateName(model.getGameState());
        if (phase.isTotemPlacement()) {
            return model.getCurrentTotemPlacementPlayer();
        }
        if (phase.isActionResolution()) {
            return model.getCurrentActionResolutionPlayer();
        }
        return null;
    }

    private Label errorLabel() {
        Label label = new Label(model.getLastError() == null ? "" : model.getLastError());
        label.getStyleClass().add("error");
        label.setWrapText(true);
        return label;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
