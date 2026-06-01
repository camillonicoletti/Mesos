package it.polimi.ingsw.view.gui.component;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.util.GuiAnimations;
import it.polimi.ingsw.view.gui.util.GuiAudio;
import it.polimi.ingsw.view.gui.util.GuiIcons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
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
import javafx.scene.layout.VBox;
import it.polimi.ingsw.view.gui.GuiApplication;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;

import java.io.InputStream;

public final class LobbyView {
    private static final String LOBBY_LOGO_PATH = "assets/icons/icon_mesos.png";
    private static final Rectangle2D LOBBY_LOGO_VIEWPORT = new Rectangle2D(100, 230, 1240, 450);

    private final ClientController controller;
    private final ClientModel model;
    private final TotemMarkerFactory totemMarkers;
    private BorderPane layout;
    private Label subtitle;
    private FlowPane players;
    private HBox bottom;

    public LobbyView(ClientController controller, ClientModel model, GuiAssetCatalog assetCatalog) {
        this.controller = controller;
        this.model = model;
        this.totemMarkers = new TotemMarkerFactory(assetCatalog);
    }

    public Node render() {
        if (layout != null) {
            refresh();
            return layout;
        }

        layout = new BorderPane();
        layout.getStyleClass().add("lobby-root");

        VBox center = new VBox(4);
        center.setPadding(new Insets(24, 36, 20, 36));
        center.setAlignment(Pos.CENTER);
        center.getStyleClass().add("lobby-stage");

        Node title = lobbyLogo();
        VBox.setMargin(title, new Insets(0, 0, 8, 0));
        subtitle = new Label();
        subtitle.getStyleClass().add("lobby-subtitle");

        players = new FlowPane(10, 6);
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

        subtitle.setText("Lobby  " + model.getLobbyPlayers().size() + "/" + expectedPlayersText());

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
                ? secondaryButton("Not ready", MaterialDesignC.CLOSE_CIRCLE_OUTLINE)
                : primaryButton("Ready", MaterialDesignC.CHECK_CIRCLE);
        readyToggle.getStyleClass().addAll("lobby-action-button", ready ? "ready-toggle-on" : "ready-toggle-off");
        GuiAnimations.addHoverScale(readyToggle);
        readyToggle.setOnAction(event -> controller.send(new ReadyMessage(!ready)));
        box.getChildren().add(readyToggle);

        Button rulesBtn = secondaryButton("Regolamento", MaterialDesignB.BOOK_OPEN);
        rulesBtn.getStyleClass().addAll("game-rules-button", "lobby-action-button");
        GuiAnimations.addHoverScale(rulesBtn);
        rulesBtn.setOnAction(event -> GuiApplication.openRulesPdf());
        box.getChildren().add(rulesBtn);

        box.getChildren().add(audioToggleButton());

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

        Label label = new Label("Number of players");
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
            GuiAnimations.addHoverScale(choice);
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
        box.getChildren().add(infoRow(MaterialDesignA.ACCOUNT_MULTIPLE, expectedPlayersText() + " players"));

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
        if (model.getLastError() != null) {
            box.getChildren().add(errorLabel());
        }
        return box;
    }

    private Node playerLobbyCard(String nickname) {
        VBox card = new VBox(5);
        card.getStyleClass().add("lobby-player-card");
        boolean leader = model.getLobbyPlayers().indexOf(nickname) == 0;
        boolean readyPlayer = model.getReadyPlayers().contains(nickname);

        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER);
        TotemColor color = model.getLobbyPlayerColors().get(nickname);
        head.getChildren().add(totemMarkers.createLobbyMarker(color));

        Label name = new Label(nickname);
        name.getStyleClass().add("lobby-player-name");
        if (leader) {
            name.setGraphic(GuiIcons.icon(MaterialDesignC.CROWN, 13, "#d7a93d"));
            name.setContentDisplay(ContentDisplay.LEFT);
        }
        head.getChildren().add(name);

        HBox readyStatus = new HBox(6);
        readyStatus.setAlignment(Pos.CENTER);
        readyStatus.getStyleClass().add("lobby-ready-status");
        readyStatus.getChildren().add(readyDot(readyPlayer));

        Label ready = new Label(readyPlayer ? "Ready" : "Waiting");
        ready.getStyleClass().add(readyPlayer ? "ok" : "muted");
        readyStatus.getChildren().add(ready);

        card.getChildren().addAll(head, readyStatus);
        return card;
    }

    private Button disconnectButton() {
        Button quit = secondaryButton("Quit", MaterialDesignL.LOGOUT);
        quit.getStyleClass().addAll("lobby-action-button", "lobby-quit-button");
        GuiAnimations.addHoverScale(quit);
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

    private Region readyDot(boolean ready) {
        Region dot = new Region();
        dot.getStyleClass().addAll("lobby-ready-dot", ready ? "lobby-ready-on" : "lobby-ready-off");
        return dot;
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

    private Button audioToggleButton() {
        Button button = secondaryButton("Audio", audioIcon());
        button.getStyleClass().addAll("game-rules-button", "lobby-action-button");
        button.setAccessibleText("Toggle audio");
        GuiAnimations.addHoverScale(button);
        button.setOnAction(event -> {
            boolean enabled = GuiAudio.toggleEnabled();
            if (enabled) {
                GuiAudio.playMenuMusic();
            }
            GuiIcons.decorateButton(button, audioIcon());
        });
        return button;
    }

    private MaterialDesignV audioIcon() {
        return GuiAudio.isEnabled() ? MaterialDesignV.VOLUME_HIGH : MaterialDesignV.VOLUME_OFF;
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

    private String expectedPlayersText() {
        int expected = model.getExpectedPlayers();
        return expected < 2 ? "?" : String.valueOf(expected);
    }

    private Node lobbyLogo() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(LOBBY_LOGO_PATH)) {
            if (stream != null) {
                ImageView logo = new ImageView(new Image(stream));
                logo.setViewport(LOBBY_LOGO_VIEWPORT);
                logo.setPreserveRatio(true);
                logo.setSmooth(true);
                logo.setFitWidth(430);
                logo.getStyleClass().add("lobby-title-logo");
                return logo;
            }
        } catch (Exception ignored) {
            // Fall back to the generated title below.
        }

        return MesosLogo.create(430, "lobby-title-logo");
    }
}
