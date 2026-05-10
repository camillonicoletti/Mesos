package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

public final class LobbyView {
    private final ClientController controller;
    private final ClientModel model;

    public LobbyView(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model = model;
    }

    public Node render() {
        BorderPane layout = new BorderPane();

        VBox left = sidePanel();
        left.getChildren().addAll(lobbyStatusBlock(), new Separator(), lobbyControls());

        VBox center = new VBox(18);
        center.setPadding(new Insets(22));
        center.getChildren().add(sectionTitle("Giocatori"));
        FlowPane players = new FlowPane(12, 12);
        for (String nickname : model.getLobbyPlayers()) {
            players.getChildren().add(playerLobbyCard(nickname));
        }
        center.getChildren().add(players);

        layout.setLeft(left);
        layout.setCenter(center);
        return layout;
    }

    private VBox lobbyControls() {
        VBox box = new VBox(12);
        box.getChildren().add(sectionTitle("Comandi"));

        HBox readyRow = new HBox(8);
        Button ready = primaryButton("Pronto", MaterialDesignC.CHECK_CIRCLE);
        Button unready = secondaryButton("Non pronto", MaterialDesignC.CLOSE_CIRCLE_OUTLINE);
        ready.setOnAction(event -> controller.send(new ReadyMessage(true)));
        unready.setOnAction(event -> controller.send(new ReadyMessage(false)));
        readyRow.getChildren().addAll(ready, unready);
        box.getChildren().add(readyRow);

        if (model.isLobbyLeader()) {
            Spinner<Integer> players = new Spinner<>();
            players.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    2, 5, Math.max(2, model.getExpectedPlayers() > 0 ? model.getExpectedPlayers() : 2)));
            Button apply = secondaryButton("Imposta giocatori", MaterialDesignA.ACCOUNT_MULTIPLE_CHECK);
            apply.setOnAction(event -> controller.send(new SetPlayersCountMessage(players.getValue())));
            box.getChildren().add(new HBox(8, players, apply));
        }

        box.getChildren().add(disconnectButton());
        return box;
    }

    private VBox lobbyStatusBlock() {
        VBox box = new VBox(8);
        box.getChildren().add(sectionTitle("Stato"));
        box.getChildren().add(infoRow(MaterialDesignA.ACCOUNT_CIRCLE, "Nickname: " + nullSafe(model.getMyNickname())));
        box.getChildren().add(infoRow(MaterialDesignA.ACCOUNT_MULTIPLE, "Giocatori attesi: " + model.getExpectedPlayers()));

        Label connection = new Label(model.isConnected() ? "Connesso" : "Disconnesso");
        connection.setGraphic(GuiIcons.icon(model.isConnected() ? MaterialDesignC.CONNECTION : MaterialDesignC.CLOSE_NETWORK, 15));
        connection.setContentDisplay(ContentDisplay.LEFT);
        connection.getStyleClass().add(model.isConnected() ? "ok" : "error");
        box.getChildren().add(connection);

        if (model.getLastConnectionEvent() != null) {
            Label event = new Label(model.getLastConnectionEvent());
            event.getStyleClass().add("notice");
            box.getChildren().add(event);
        }
        if (model.getLastError() != null) {
            box.getChildren().add(errorLabel());
        }
        return box;
    }

    private Node playerLobbyCard(String nickname) {
        VBox card = new VBox(6);
        card.getStyleClass().add("player-card");
        boolean leader = model.getLobbyPlayers().indexOf(nickname) == 0;
        Label name = new Label(nickname);
        if (leader) {
            name.setGraphic(GuiIcons.icon(MaterialDesignC.CROWN, 16, "#d7a93d"));
            name.setContentDisplay(ContentDisplay.LEFT);
        }
        Label ready = new Label(model.getReadyPlayers().contains(nickname) ? "Pronto" : "In attesa");
        ready.setGraphic(GuiIcons.icon(model.getReadyPlayers().contains(nickname)
                ? MaterialDesignC.CHECK_CIRCLE
                : MaterialDesignC.CLOCK_OUTLINE, 14));
        ready.setContentDisplay(ContentDisplay.LEFT);
        TotemColor color = model.getLobbyPlayerColors().get(nickname);
        Label colorLabel = new Label(color == null ? "Colore non assegnato" : color.name());
        ready.getStyleClass().add(model.getReadyPlayers().contains(nickname) ? "ok" : "muted");
        card.getChildren().addAll(name, colorLabel, ready);
        return card;
    }

    private Button disconnectButton() {
        Button quit = secondaryButton("Esci", MaterialDesignL.LOGOUT);
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

    private VBox sidePanel() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(18));
        box.setPrefWidth(280);
        box.getStyleClass().add("side-panel");
        return box;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Button primaryButton(String text, Ikon ikon) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        GuiIcons.decorateButton(button, ikon);
        return button;
    }

    private Button secondaryButton(String text, Ikon ikon) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        GuiIcons.decorateButton(button, ikon);
        return button;
    }

    private Label errorLabel() {
        Label label = new Label(model.getLastError() == null ? "" : model.getLastError());
        label.getStyleClass().add("error");
        label.setWrapText(true);
        return label;
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
