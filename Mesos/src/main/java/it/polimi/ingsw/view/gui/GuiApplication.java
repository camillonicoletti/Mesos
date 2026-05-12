package it.polimi.ingsw.view.gui;

import atlantafx.base.theme.PrimerDark;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.component.EndGameView;
import it.polimi.ingsw.view.gui.component.GameBoardView;
import it.polimi.ingsw.view.gui.component.LoginView;
import it.polimi.ingsw.view.gui.component.LobbyView;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class GuiApplication extends Application {
    private static final Duration TOAST_VISIBLE_DURATION = Duration.seconds(10);
    private static final Duration TOAST_ANIMATION_DURATION = Duration.millis(180);

    private static LaunchContext launchContext;
    

    private ClientController controller;
    private ClientModel model;
    private GuiAssetCatalog assetCatalog;
    private StackPane sceneRoot;
    private BorderPane root;
    private VBox suspensionOverlay;
    private Label suspensionTimerLabel;
    private VBox toastHost;
    private Label timerLabel;
    private String pendingNickname;
    private Timeline timer;
    private long shownConnectionEventVersion;
    private long shownErrorVersion;

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
        controller.setDispatcher(Platform::runLater);
        assetCatalog = GuiAssetCatalog.loadDefault();
        root = new BorderPane();
        root.getStyleClass().add("app");
        suspensionOverlay = createSuspensionOverlay();
        toastHost = new VBox(10);
        toastHost.getStyleClass().add("toast-host");
        toastHost.setPickOnBounds(false);
        sceneRoot = new StackPane(root, suspensionOverlay, toastHost);
        StackPane.setAlignment(suspensionOverlay, Pos.CENTER);
        StackPane.setAlignment(toastHost, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toastHost, new Insets(0, 22, 22, 0));
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        model.addListener(this::render);

        Scene scene = new Scene(sceneRoot, 1280, 820);
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
        showPendingToasts();
    }

    private void renderLogin() {
        centerOnly(new LoginView(controller, model, pendingNickname, nickname -> pendingNickname = nickname).render());
    }

    private void renderLobby() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("lobby-shell");
        timerLabel = null;
        layout.setCenter(new LobbyView(controller, model).render());
        root.setCenter(layout);
    }

    private void renderGame() {
        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("game-shell");
        timerLabel = null;

        ScrollPane scroll = new ScrollPane(new GameBoardView(controller, model, assetCatalog).render());
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("scroll");
        layout.setCenter(scroll);
        root.setCenter(layout);
    }

    private void renderEndGame() {
        BorderPane layout = shell("Fine partita", "Vincitore: " + nullSafe(model.getWinnerNickname()));
        layout.setCenter(new EndGameView(controller, model).render());
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

    private void centerOnly(Node node) {
        StackPane wrapper = new StackPane(node);
        wrapper.setPadding(new Insets(24));
        root.setCenter(wrapper);
        root.setTop(null);
        root.setLeft(null);
    }

    private void refreshTimer() {
        refreshSuspensionOverlay();
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

    private VBox createSuspensionOverlay() {
        VBox overlay = new VBox(10);
        overlay.getStyleClass().add("suspension-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setMaxWidth(520);
        overlay.setVisible(false);
        overlay.setManaged(false);

        Label title = new Label("Partita sospesa");
        title.getStyleClass().add("suspension-title");

        Label message = new Label("E rimasto un solo giocatore connesso. La partita riprende quando rientra almeno un altro giocatore.");
        message.getStyleClass().add("suspension-message");
        message.setWrapText(true);
        message.setMaxWidth(460);
        message.setAlignment(Pos.CENTER);

        suspensionTimerLabel = new Label();
        suspensionTimerLabel.getStyleClass().add("suspension-timer");

        overlay.getChildren().addAll(title, message, suspensionTimerLabel);
        return overlay;
    }

    private void refreshSuspensionOverlay() {
        if (suspensionOverlay == null) {
            return;
        }
        boolean suspended = model.isGameSuspended();
        suspensionOverlay.setVisible(suspended);
        if (!suspended) {
            return;
        }

        long remaining = model.getSuspensionRemainingMillis();
        String countdown = remaining >= 0L ? formatMillis(remaining) : "--:--";
        suspensionTimerLabel.setText("Vittoria automatica tra " + countdown);
    }

    private void showPendingToasts() {
        long connectionVersion = model.getLastConnectionEventVersion();
        String connectionEvent = model.getLastConnectionEvent();
        if (connectionVersion > shownConnectionEventVersion && connectionEvent != null) {
            shownConnectionEventVersion = connectionVersion;
            showToast(connectionEvent, false);
        }

        long errorVersion = model.getLastErrorVersion();
        String error = model.getLastError();
        if (errorVersion > shownErrorVersion && error != null && !error.isBlank()) {
            shownErrorVersion = errorVersion;
            showToast(error, true);
        }
    }

    private void showToast(String message, boolean error) {
        Label toast = new Label(message);
        toast.setWrapText(true);
        toast.setMaxWidth(360);
        toast.getStyleClass().addAll("gui-toast", error ? "gui-toast-error" : "gui-toast-info");
        toast.setOpacity(0);
        toast.setTranslateX(24);

        toastHost.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(TOAST_ANIMATION_DURATION, toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(TOAST_ANIMATION_DURATION, toast);
        slideIn.setFromX(24);
        slideIn.setToX(0);

        PauseTransition wait = new PauseTransition(TOAST_VISIBLE_DURATION);

        FadeTransition fadeOut = new FadeTransition(TOAST_ANIMATION_DURATION, toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        TranslateTransition slideOut = new TranslateTransition(TOAST_ANIMATION_DURATION, toast);
        slideOut.setFromX(0);
        slideOut.setToX(24);

        SequentialTransition animation = new SequentialTransition(
                new ParallelTransition(fadeIn, slideIn),
                wait,
                new ParallelTransition(fadeOut, slideOut)
        );
        animation.setOnFinished(event -> toastHost.getChildren().remove(toast));
        animation.play();
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

    private Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("title");
        return label;
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
