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
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
        layout.getStyleClass().add("lobby-root");

        VBox center = new VBox(22);
        center.setPadding(new Insets(40, 36, 20, 36));
        center.setAlignment(Pos.CENTER);
        center.getStyleClass().add("lobby-stage");

        Label title = new Label("Mesos");
        title.getStyleClass().add("lobby-title");
        Label subtitle = new Label("Lobby  " + model.getLobbyPlayers().size() + "/" + model.getExpectedPlayers());
        subtitle.getStyleClass().add("lobby-subtitle");

        FlowPane players = new FlowPane(18, 18);
        players.setAlignment(Pos.CENTER);
        players.getStyleClass().add("lobby-players");
        for (String nickname : model.getLobbyPlayers()) {
            players.getChildren().add(playerLobbyCard(nickname));
        }

        center.getChildren().addAll(title, subtitle, players);

        HBox bottom = new HBox(18);
        bottom.setPadding(new Insets(0, 34, 28, 34));
        bottom.setAlignment(Pos.CENTER);
        bottom.getStyleClass().add("lobby-bottom");
        bottom.getChildren().addAll(lobbyStatusBlock(), lobbyControls());

        layout.setCenter(center);
        layout.setBottom(bottom);
        return layout;
    }

    private HBox lobbyControls() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("lobby-controls");

        Button ready = primaryButton("Pronto", MaterialDesignC.CHECK_CIRCLE);
        Button unready = secondaryButton("Non pronto", MaterialDesignC.CLOSE_CIRCLE_OUTLINE);
        ready.setOnAction(event -> controller.send(new ReadyMessage(true)));
        unready.setOnAction(event -> controller.send(new ReadyMessage(false)));
        box.getChildren().addAll(ready, unready);

        if (model.isLobbyLeader()) {
            Spinner<Integer> players = new Spinner<>();
            players.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    2, 5, Math.max(2, model.getExpectedPlayers() > 0 ? model.getExpectedPlayers() : 2)));
            Button apply = secondaryButton("Imposta giocatori", MaterialDesignA.ACCOUNT_MULTIPLE_CHECK);
            apply.setOnAction(event -> controller.send(new SetPlayersCountMessage(players.getValue())));
            HBox leaderControls = new HBox(8, players, apply);
            leaderControls.setAlignment(Pos.CENTER);
            leaderControls.getStyleClass().add("lobby-leader-controls");
            box.getChildren().add(leaderControls);
        }

        box.getChildren().add(disconnectButton());
        return box;
    }

    private HBox lobbyStatusBlock() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("lobby-status");
        box.getChildren().add(infoRow(MaterialDesignA.ACCOUNT_CIRCLE, nullSafe(model.getMyNickname())));
        box.getChildren().add(infoRow(MaterialDesignA.ACCOUNT_MULTIPLE, model.getExpectedPlayers() + " giocatori"));

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
        VBox card = new VBox(8);
        card.getStyleClass().add("lobby-player-card");
        boolean leader = model.getLobbyPlayers().indexOf(nickname) == 0;
        boolean readyPlayer = model.getReadyPlayers().contains(nickname);
        if (readyPlayer) {
            card.getStyleClass().add("lobby-player-ready");
        }

        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER);
        TotemColor color = model.getLobbyPlayerColors().get(nickname);
        head.getChildren().add(totemDot(color));

        Label name = new Label(nickname);
        name.getStyleClass().add("lobby-player-name");
        if (leader) {
            name.setGraphic(GuiIcons.icon(MaterialDesignC.CROWN, 16, "#d7a93d"));
            name.setContentDisplay(ContentDisplay.LEFT);
        }
        head.getChildren().add(name);

        Label ready = new Label(readyPlayer ? "Pronto" : "In attesa");
        ready.setGraphic(GuiIcons.icon(readyPlayer
                ? MaterialDesignC.CHECK_CIRCLE
                : MaterialDesignC.CLOCK_OUTLINE, 14));
        ready.setContentDisplay(ContentDisplay.LEFT);
        ready.getStyleClass().add(readyPlayer ? "ok" : "muted");
        card.getChildren().addAll(head, ready);
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

    private Region totemDot(TotemColor color) {
        Region dot = new Region();
        dot.getStyleClass().add("lobby-color-dot");
        dot.setStyle("-fx-background-color: " + totemColorHex(color) + ";");
        return dot;
    }

    private String totemColorHex(TotemColor color) {
        if (color == null) {
            return "#f1ddc0";
        }
        return switch (color) {
            case RED -> "#b94a3a";
            case BLUE -> "#3c6f9e";
            case PURPLE -> "#77529a";
            case YELLOW -> "#d7a93d";
            case WHITE -> "#f3ebdc";
        };
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
