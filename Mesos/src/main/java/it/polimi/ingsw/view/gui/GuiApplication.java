package it.polimi.ingsw.view.gui;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReadyMessage;
import it.polimi.ingsw.network.protocol.serverbound.ReconnectMessage;
import it.polimi.ingsw.network.protocol.serverbound.SetPlayersCountMessage;
import it.polimi.ingsw.view.gui.asset.CardAsset;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GuiApplication extends Application {
    private static LaunchContext launchContext;

    private ClientController controller;
    private ClientModel model;
    private GuiAssetCatalog assetCatalog;
    private BorderPane root;
    private Label timerLabel;
    private String pendingNickname;
    private Timeline timer;

    static void launchWith(ClientController controller, ClientModel model) {
        launchContext = new LaunchContext(controller, model);
        Application.launch(GuiApplication.class);
    }

    @Override
    public void start(Stage stage) {
        if (launchContext == null) {
            throw new IllegalStateException("GUI launched without client context");
        }

        controller = launchContext.controller();
        model = launchContext.model();
        assetCatalog = GuiAssetCatalog.loadDefault();
        root = new BorderPane();
        root.getStyleClass().add("app");

        model.addListener(() -> Platform.runLater(this::render));

        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(cssResource());
        stage.setTitle("Mesos");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> controller.disconnect());
        stage.show();

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshTimer()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        render();
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void render() {
        if (model.isLoginSuccess() && model.getMyNickname() == null && pendingNickname != null) {
            model.setMyNickname(pendingNickname);
            return;
        }

        if (!model.isLoginSuccess()) {
            renderLogin();
        } else if (model.isGameEnded()) {
            renderEndGame();
        } else if (model.isInPreGame()) {
            renderLobby();
        } else {
            renderGame();
        }
        refreshTimer();
    }

    private void renderLogin() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("login-panel");
        panel.setMaxWidth(420);
        panel.setAlignment(Pos.CENTER_LEFT);

        Label title = title("Mesos");
        Label subtitle = new Label("Entra nella partita o riconnettiti a una partita gia avviata.");
        subtitle.getStyleClass().add("muted");

        TextField nickname = new TextField(pendingNickname != null ? pendingNickname : "");
        nickname.setPromptText("Nickname");
        nickname.setMaxWidth(Double.MAX_VALUE);

        ComboBox<TotemColor> color = new ComboBox<>();
        color.getItems().addAll(TotemColor.values());
        color.setMaxWidth(Double.MAX_VALUE);
        color.setPromptText("Colore totem");

        CheckBox reconnect = new CheckBox("Riconnettiti");
        reconnect.selectedProperty().addListener((obs, oldValue, selected) -> color.setDisable(selected));

        Button enter = primaryButton("Entra");
        enter.setMaxWidth(Double.MAX_VALUE);
        enter.setOnAction(event -> {
            String name = nickname.getText().trim();
            if (name.isBlank()) {
                return;
            }
            pendingNickname = name;
            if (reconnect.isSelected()) {
                controller.send(new ReconnectMessage(name));
            } else if (color.getValue() != null) {
                controller.send(new LoginMessage(name, color.getValue()));
            }
        });

        Label error = errorLabel();
        panel.getChildren().addAll(title, subtitle, nickname, color, reconnect, enter, error);
        centerOnly(panel);
    }

    private void renderLobby() {
        BorderPane layout = shell("Lobby", "Preparazione partita");

        VBox left = sidePanel();
        left.getChildren().addAll(statusBlock(), new Separator(), lobbyControls());

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
        root.setCenter(layout);
    }

    private void renderGame() {
        BorderPane layout = shell("Partita", stateLabel());
        VBox left = sidePanel();
        left.getChildren().addAll(statusBlock(), new Separator(), playersPanel(), new Separator(), disconnectButton());

        VBox board = new VBox(16);
        board.setPadding(new Insets(20));
        board.getChildren().addAll(turnOrderPanel(), totemPlacementPanel(), cardRowsPanel());

        ScrollPane scroll = new ScrollPane(board);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll");
        layout.setLeft(left);
        layout.setCenter(scroll);
        root.setCenter(layout);
    }

    private void renderEndGame() {
        BorderPane layout = shell("Fine partita", "Vincitore: " + nullSafe(model.getWinnerNickname()));
        VBox content = new VBox(14);
        content.setPadding(new Insets(28));
        content.getChildren().add(sectionTitle("Classifica finale"));

        model.getFinalScores().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> content.getChildren().add(new Label(entry.getKey() + " - " + entry.getValue() + " PP")));

        content.getChildren().add(disconnectButton());
        layout.setCenter(content);
        root.setCenter(layout);
    }

    private BorderPane shell(String title, String subtitle) {
        BorderPane layout = new BorderPane();
        VBox top = new VBox(4);
        top.setPadding(new Insets(18, 22, 12, 22));
        top.getStyleClass().add("topbar");
        Label h1 = title(title);
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("muted");
        timerLabel = new Label();
        timerLabel.getStyleClass().add("timer");
        HBox line = new HBox(16, sub, spacer(), timerLabel);
        line.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(h1, line);
        layout.setTop(top);
        return layout;
    }

    private VBox lobbyControls() {
        VBox box = new VBox(12);
        box.getChildren().add(sectionTitle("Comandi"));

        HBox readyRow = new HBox(8);
        Button ready = primaryButton("Pronto");
        Button unready = secondaryButton("Non pronto");
        ready.setOnAction(event -> controller.send(new ReadyMessage(true)));
        unready.setOnAction(event -> controller.send(new ReadyMessage(false)));
        readyRow.getChildren().addAll(ready, unready);
        box.getChildren().add(readyRow);

        if (model.isLobbyLeader()) {
            Spinner<Integer> players = new Spinner<>();
            players.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    2, 5, Math.max(2, model.getExpectedPlayers() > 0 ? model.getExpectedPlayers() : 2)));
            Button apply = secondaryButton("Imposta giocatori");
            apply.setOnAction(event -> controller.send(new SetPlayersCountMessage(players.getValue())));
            box.getChildren().add(new HBox(8, players, apply));
        }

        box.getChildren().add(disconnectButton());
        return box;
    }

    private VBox statusBlock() {
        VBox box = new VBox(8);
        box.getChildren().add(sectionTitle("Stato"));
        box.getChildren().add(new Label("Nickname: " + nullSafe(model.getMyNickname())));
        box.getChildren().add(new Label("Era: " + nullSafe(model.getCurrentEra())));
        box.getChildren().add(new Label("Round: " + model.getRoundNumber()));
        box.getChildren().add(new Label("Turno: " + currentTurnText()));

        Label connection = new Label(connectionText());
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
        box.getChildren().add(sectionTitle("Giocatori"));
        for (PlayerView player : model.getPlayers().values()) {
            Label label = new Label(player.getNickname() + "  cibo " + player.getFood()
                    + "  PP " + player.getPrestigePoints());
            label.getStyleClass().add(model.isPlayerDisconnected(player.getNickname()) ? "disconnected" : "player-row");
            box.getChildren().add(label);
        }
        return box;
    }

    private HBox turnOrderPanel() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(sectionTitle("Ordine turno"));
        for (String nickname : model.getTurnOrder()) {
            Label chip = new Label(nickname);
            chip.getStyleClass().add(nickname.equals(currentTurnNickname()) ? "chip-active" : "chip");
            row.getChildren().add(chip);
        }
        return row;
    }

    private FlowPane totemPlacementPanel() {
        FlowPane pane = new FlowPane(10, 10);
        pane.getChildren().add(sectionTitle("Tessere offerta"));
        for (char tile = 'A'; tile <= 'G'; tile++) {
            String owner = model.getTotemOnTiles().get(tile);
            Button button = secondaryButton(tile + (owner == null ? "" : " - " + owner));
            char selectedTile = tile;
            button.setDisable(!model.isMyTotemPlacementTurn() || owner != null);
            button.setOnAction(event -> controller.send(new GameActionMessage(new PlaceTotemAction(selectedTile))));
            pane.getChildren().add(button);
        }
        return pane;
    }

    private VBox cardRowsPanel() {
        VBox rows = new VBox(18);
        rows.getChildren().add(cardRow("Personaggi / Eventi sopra", model.getUpperRowCharacterIds(), true, false));
        rows.getChildren().add(cardRow("Personaggi / Eventi sotto", model.getLowerRowCharacterIds(), false, false));
        rows.getChildren().add(cardRow("Edifici sopra", model.getUpperRowBuildingIds(), true, true));
        rows.getChildren().add(cardRow("Edifici sotto", model.getLowerRowBuildingIds(), false, true));
        return rows;
    }

    private VBox cardRow(String title, List<Integer> ids, boolean upper, boolean building) {
        VBox box = new VBox(8);
        box.getChildren().add(sectionTitle(title));
        FlowPane cards = new FlowPane(12, 12);
        for (Integer id : ids) {
            Node card = cardNode(id, () -> {
                if (!model.isMyTurn()) {
                    return;
                }
                if (building) {
                    controller.send(new GameActionMessage(new PickBuildingAction(String.valueOf(id), upper)));
                } else {
                    controller.send(new GameActionMessage(new PickCardAction(String.valueOf(id), upper)));
                }
            });
            card.setDisable(!model.isMyTurn());
            cards.getChildren().add(card);
        }
        box.getChildren().add(cards);
        return box;
    }

    private Node cardNode(int cardId, Runnable action) {
        Optional<CardAsset> asset = assetCatalog.findCard(cardId);
        StackPane card = new StackPane();
        card.getStyleClass().add("card");
        card.setPrefSize(116, 164);
        card.setOnMouseClicked(event -> action.run());

        if (asset.isPresent() && asset.get().isFrontAvailable()) {
            try (InputStream stream = assetCatalog.openResource(asset.get().getFrontPath()).orElse(null)) {
                if (stream != null) {
                    ImageView image = new ImageView(new Image(stream));
                    image.setFitWidth(116);
                    image.setFitHeight(164);
                    image.setPreserveRatio(true);
                    card.getChildren().add(image);
                    return card;
                }
            } catch (Exception ignored) {
                // Fall through to a placeholder if the optional local image is unreadable.
            }
        }

        Label placeholder = new Label(asset.map(assetCatalog::placeholderLabelFor).orElse("CARD #" + cardId));
        placeholder.setWrapText(true);
        placeholder.setAlignment(Pos.CENTER);
        card.getChildren().add(placeholder);
        return card;
    }

    private Node playerLobbyCard(String nickname) {
        VBox card = new VBox(6);
        card.getStyleClass().add("player-card");
        Label name = new Label((model.getLobbyPlayers().indexOf(nickname) == 0 ? "[leader] " : "") + nickname);
        Label ready = new Label(model.getReadyPlayers().contains(nickname) ? "Pronto" : "In attesa");
        TotemColor color = model.getLobbyPlayerColors().get(nickname);
        Label colorLabel = new Label(color == null ? "Colore non assegnato" : color.name());
        ready.getStyleClass().add(model.getReadyPlayers().contains(nickname) ? "ok" : "muted");
        card.getChildren().addAll(name, colorLabel, ready);
        return card;
    }

    private Button disconnectButton() {
        Button quit = secondaryButton("Esci");
        quit.setOnAction(event -> {
            controller.disconnect();
            Platform.exit();
        });
        return quit;
    }

    private void centerOnly(Node node) {
        StackPane wrapper = new StackPane(node);
        wrapper.setPadding(new Insets(24));
        root.setCenter(wrapper);
        root.setTop(null);
        root.setLeft(null);
    }

    private void refreshTimer() {
        if (timerLabel == null) {
            return;
        }
        if (model.isGameSuspended()) {
            long remaining = model.getSuspensionRemainingMillis();
            timerLabel.setText("Partita sospesa - timeout " + formatMillis(remaining));
            timerLabel.getStyleClass().setAll("timer", "timer-alert");
        } else {
            timerLabel.setText("Connessione attiva");
            timerLabel.getStyleClass().setAll("timer");
        }
    }

    private String currentTurnText() {
        String turn = currentTurnNickname();
        return turn == null ? "-" : turn;
    }

    private String currentTurnNickname() {
        if ("TotemPlacementState".equals(model.getGameState())) {
            return model.getCurrentTotemPlacementPlayer();
        }
        if ("ActionResolutionState".equals(model.getGameState())) {
            return model.getCurrentActionResolutionPlayer();
        }
        return null;
    }

    private String stateLabel() {
        String state = model.getGameState();
        if ("TotemPlacementState".equals(state)) {
            return "Piazzamento totem";
        }
        if ("ActionResolutionState".equals(state)) {
            return "Risoluzione azioni";
        }
        return state == null ? "Sincronizzazione" : state;
    }

    private String connectionText() {
        return model.isConnected() ? "Connesso" : "Disconnesso";
    }

    private Label errorLabel() {
        Label label = new Label(model.getLastError() == null ? "" : model.getLastError());
        label.getStyleClass().add("error");
        label.setWrapText(true);
        return label;
    }

    private VBox sidePanel() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(18));
        box.setPrefWidth(280);
        box.getStyleClass().add("side-panel");
        return box;
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("title");
        return label;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        return button;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String formatMillis(long millis) {
        if (millis < 0) {
            return "--:--";
        }
        long totalSeconds = millis / 1000L;
        return String.format("%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    private String cssResource() {
        return GuiApplication.class.getClassLoader()
                .getResource("gui/gui.css")
                .toExternalForm();
    }

    private record LaunchContext(ClientController controller, ClientModel model) {}
}
