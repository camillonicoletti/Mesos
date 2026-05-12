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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.io.InputStream;

public final class LobbyView {
    private final ClientController controller;
    private final ClientModel model;
    private BorderPane layout;
    private Label subtitle;
    private FlowPane players;
    private HBox bottom;

    public LobbyView(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model = model;
    }

    public Node render() {
        if (layout != null) {
            refresh();
            return layout;
        }

        layout = new BorderPane();
        layout.getStyleClass().add("lobby-root");

        VBox center = new VBox(22);
        center.setPadding(new Insets(40, 36, 20, 36));
        center.setAlignment(Pos.CENTER);
        center.getStyleClass().add("lobby-stage");

        Label title = new Label("Mesos");
        title.getStyleClass().add("lobby-title");
        subtitle = new Label();
        subtitle.getStyleClass().add("lobby-subtitle");

        players = new FlowPane(18, 18);
        players.setAlignment(Pos.CENTER);
        players.getStyleClass().add("lobby-players");

        center.getChildren().addAll(title, subtitle, players);

        bottom = new HBox(18);
        bottom.setPadding(new Insets(0, 34, 28, 34));
        bottom.setAlignment(Pos.CENTER);
        bottom.getStyleClass().add("lobby-bottom");

        layout.setCenter(center);
        layout.setBottom(bottom);
        refresh();
        return layout;
    }

    public void refresh() {
        if (layout == null) {
            return;
        }

        subtitle.setText("Lobby  " + model.getLobbyPlayers().size() + "/" + model.getExpectedPlayers());

        players.getChildren().clear();
        for (String nickname : model.getLobbyPlayers()) {
            players.getChildren().add(playerLobbyCard(nickname));
        }

        bottom.getChildren().setAll(lobbyStatusBlock(), lobbyControls());
    }

    private HBox lobbyControls() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("lobby-controls");

        boolean ready = model.getMyNickname() != null
                && model.getReadyPlayers().contains(model.getMyNickname());
        Button readyToggle = ready
                ? secondaryButton("Non pronto", MaterialDesignC.CLOSE_CIRCLE_OUTLINE)
                : primaryButton("Pronto", MaterialDesignC.CHECK_CIRCLE);
        readyToggle.getStyleClass().add(ready ? "ready-toggle-on" : "ready-toggle-off");
        readyToggle.setOnAction(event -> controller.send(new ReadyMessage(!ready)));
        box.getChildren().add(readyToggle);

        if (model.isLobbyLeader()) {
            box.getChildren().add(playerCountSelector());
        }

        box.getChildren().add(disconnectButton());
        return box;
    }

    private VBox playerCountSelector() {
        VBox selector = new VBox(6);
        selector.setAlignment(Pos.CENTER);
        selector.getStyleClass().add("player-count-selector");

        Label label = new Label("Numero giocatori");
        label.getStyleClass().add("player-count-label");

        HBox choices = new HBox(7);
        choices.setAlignment(Pos.CENTER);
        choices.getStyleClass().add("player-count-choices");

        int selected = model.getExpectedPlayers();
        int joined = model.getLobbyPlayers().size();
        for (int count = 2; count <= 5; count++) {
            Button choice = new Button(String.valueOf(count));
            choice.getStyleClass().add("player-count-choice");
            if (count == selected) {
                choice.getStyleClass().add("player-count-choice-selected");
            }
            if (count < joined) {
                choice.setDisable(true);
            }
            int chosen = count;
            choice.setOnAction(event -> controller.send(new SetPlayersCountMessage(chosen)));
            choices.getChildren().add(choice);
        }

        selector.getChildren().addAll(label, choices);
        return selector;
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

        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER);
        TotemColor color = model.getLobbyPlayerColors().get(nickname);
        head.getChildren().add(totemMarker(color));

        Label name = new Label(nickname);
        name.getStyleClass().add("lobby-player-name");
        if (leader) {
            name.setGraphic(GuiIcons.icon(MaterialDesignC.CROWN, 16, "#d7a93d"));
            name.setContentDisplay(ContentDisplay.LEFT);
        }
        head.getChildren().add(name);

        HBox readyStatus = new HBox(7);
        readyStatus.setAlignment(Pos.CENTER);
        readyStatus.getStyleClass().add("lobby-ready-status");
        readyStatus.getChildren().add(readyDot(readyPlayer));

        Label ready = new Label(readyPlayer ? "Pronto" : "In attesa");
        ready.getStyleClass().add(readyPlayer ? "ok" : "muted");
        readyStatus.getChildren().add(ready);

        card.getChildren().addAll(head, readyStatus);
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

    private StackPane totemMarker(TotemColor color) {
        StackPane marker = new StackPane();
        marker.getStyleClass().add("lobby-totem-marker");
        marker.setMinSize(28, 38);
        marker.setPrefSize(28, 38);
        marker.setMaxSize(28, 38);

        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(totemAssetPath(color))) {
            if (stream != null) {
                ImageView image = new ImageView(new Image(stream));
                image.setPreserveRatio(true);
                image.setFitHeight(38);
                marker.getChildren().add(image);
                return marker;
            }
        } catch (Exception ignored) {
            // Fallback colored dot below.
        }

        Region fallback = new Region();
        fallback.getStyleClass().add("lobby-color-dot");
        fallback.setStyle("-fx-background-color: " + totemColorHex(color) + ";");
        marker.getChildren().add(fallback);
        return marker;
    }

    private String totemAssetPath(TotemColor color) {
        return switch (color == null ? TotemColor.WHITE : color) {
            case RED -> "assets/tokens/totem_red.png";
            case YELLOW -> "assets/tokens/totem_yellow.png";
            case BLUE -> "assets/tokens/totem_blue.png";
            case PURPLE -> "assets/tokens/totem_purple.png";
            case WHITE -> "assets/tokens/totem_white.png";
        };
    }

    private Region readyDot(boolean ready) {
        Region dot = new Region();
        dot.getStyleClass().addAll("lobby-ready-dot", ready ? "lobby-ready-on" : "lobby-ready-off");
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
