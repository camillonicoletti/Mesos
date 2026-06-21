package it.polimi.ingsw.view.gui;

import atlantafx.base.theme.PrimerDark;
import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
import it.polimi.ingsw.view.gui.asset.GuiAssetLoader;
import it.polimi.ingsw.view.gui.component.EndGameView;
import it.polimi.ingsw.view.gui.component.GameBoardView;
import it.polimi.ingsw.view.gui.component.LoginView;
import it.polimi.ingsw.view.gui.component.LobbyView;
import it.polimi.ingsw.view.gui.util.GuiAudio;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GuiApplication extends Application {
    private static final Duration TOAST_VISIBLE_DURATION = Duration.seconds(10);
    private static final Duration TOAST_ANIMATION_DURATION = Duration.millis(180);

    private static LaunchContext launchContext;
    private static GuiApplication instance;

    private ClientController controller;
    private ClientModel model;
    private ExecutorService outboundExecutor;
    private GuiAssetCatalog assetCatalog;
    private StackPane sceneRoot;
    private BorderPane root;
    private StackPane rulesOverlay;
    private StackPane suspensionOverlay;
    private Label suspensionTimerLabel;
    private Label suspensionConnectedLabel;
    private Label suspensionDisconnectedLabel;
    private StackPane serverDisconnectedOverlay;
    private VBox toastHost;
    private Label timerLabel;
    private Label endGameSubtitleLabel;
    private String pendingNickname;
    private Timeline timer;
    private boolean renderScheduled;
    private long shownConnectionEventVersion;
    private long shownErrorVersion;
    private ScreenController currentScreen;
    private final ScreenController loginScreen = new LoginScreen();
    private final ScreenController lobbyScreen = new LobbyScreen();
    private final ScreenController gameScreen = new GameScreen();
    private final ScreenController endGameScreen = new EndGameScreen();
    private LoginView loginView;
    private LobbyView lobbyView;
    private GameBoardView gameBoardView;
    private EndGameView endGameView;
    private StackPane loginShell;
    private BorderPane lobbyShell;
    private BorderPane gameShell;
    private BorderPane endGameShell;
    private StackPane endGameStack;
    private Integer leaderboardRank;
    private int leaderboardRankPlayersCount;
    private int leaderboardEntriesPlayersCount;
    private int requestedLeaderboardPlayersCount;
    private List<LeaderboardEntry> leaderboardEntries;

    // Saves the controller and model, then starts the JavaFX window.
    static void launchWith(ClientController controller, ClientModel model) {
        launchContext = new LaunchContext(controller, model);
        Application.launch(GuiApplication.class);
    }

    // Gives back the currently open window (or null if not started yet).
    static GuiApplication current() {
        return instance;
    }

    // Opens the rules popup (does nothing if the window is not open).
    public static void openRulesPdf() {
        if (instance == null) {
            return;
        }
        Platform.runLater(() -> instance.showRulesModal());
    }

    // Builds the whole window when the app starts and shows the first screen.
    @Override
    public void start(Stage stage) {
        instance = this;
        if (launchContext == null) {
            throw new IllegalStateException("GUI launched without client context");
        }

        controller = launchContext.controller();
        model = launchContext.model();
        outboundExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "gui-client-outbound");
            thread.setDaemon(true);
            return thread;
        });
        controller.setDispatcher(Platform::runLater);
        controller.setOutboundDispatcher(outboundExecutor::execute);
        assetCatalog = GuiAssetCatalog.loadDefault();
        GuiAudio.preloadActionClick();
        root = new BorderPane();
        root.getStyleClass().add("app");
        suspensionOverlay = createSuspensionOverlay();
        serverDisconnectedOverlay = createServerDisconnectedOverlay();
        toastHost = new VBox(10);
        toastHost.getStyleClass().add("toast-host");
        toastHost.setPickOnBounds(false);
        sceneRoot = new StackPane(root, suspensionOverlay, serverDisconnectedOverlay, toastHost);
        StackPane.setAlignment(suspensionOverlay, Pos.CENTER);
        StackPane.setAlignment(serverDisconnectedOverlay, Pos.CENTER);
        StackPane.setAlignment(toastHost, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toastHost, new Insets(0, 22, 22, 0));
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

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

    // Cleans up (stops timer, threads and audio) when the app closes.
    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
        if (outboundExecutor != null) {
            outboundExecutor.shutdownNow();
        }
        GuiAudio.shutdown();
    }

    // Asks for a redraw, but only one at a time to avoid useless extra redraws.
    private void scheduleRender() {
        if (renderScheduled) {
            return;
        }
        renderScheduled = true;
        Platform.runLater(() -> {
            renderScheduled = false;
            render();
        });
    }

    // Picks the right screen for the current state and refreshes it.
    private void render() {
        if (model.isLoginSuccess() && model.getMyNickname() == null && pendingNickname != null) {
            model.setMyNickname(pendingNickname);
        }

        ScreenController nextScreen = resolveScreen();
        if (nextScreen != currentScreen) {
            switchScreen(nextScreen);
            currentScreen = nextScreen;
        }
        refreshCurrentScreen();
        refreshTimer();
        showPendingToasts();
    }

    // Decides which screen to show: login, lobby, game or end game.
    private ScreenController resolveScreen() {
        if (!model.isLoginSuccess()) {
            return loginScreen;
        }
        if (model.isGameEnded()) {
            return endGameScreen;
        }
        if (model.isInPreGame()) {
            return lobbyScreen;
        }
        return gameScreen;
    }

    // Clears the old screen, sets the menu music and shows the new screen.
    private void switchScreen(ScreenController screen) {
        root.setTop(null);
        root.setLeft(null);
        root.setRight(null);
        root.setBottom(null);
        timerLabel = null;

        if (screen.playsMenuMusic()) {
            GuiAudio.playMenuMusic();
        } else {
            GuiAudio.stopMenuMusic();
        }

        root.setCenter(screen.mount());
    }

    // Tells the current screen to update itself with the latest data.
    private void refreshCurrentScreen() {
        if (currentScreen != null) {
            currentScreen.refresh();
        }
    }

    // After login, saves the nickname and asks for a redraw.
    void onLoginResult() {
        if (model.isLoginSuccess() && model.getMyNickname() == null && pendingNickname != null) {
            model.setMyNickname(pendingNickname);
        }
        scheduleRender();
    }

    // Any game change happened: just ask for a redraw.
    void onBoardEvent() {
        scheduleRender();
    }

    // The server sent the leaderboard: store and show it.
    void onLeaderboard(int playersCount, List<LeaderboardEntry> entries) {
        showLeaderboard(playersCount, entries);
    }

    // The server sent my position in the ranking: store and show it.
    void onMyRank(int rank, int playersCount) {
        showMyLeaderboardRank(rank, playersCount);
    }

    // Keeps the leaderboard list and passes it to the end-game screen.
    private void showLeaderboard(int playersCount, List<LeaderboardEntry> entries) {
        leaderboardEntriesPlayersCount = playersCount;
        leaderboardEntries = entries == null ? List.of() : new ArrayList<>(entries);
        syncLeaderboardToEndGame();
    }

    // Keeps my rank and passes it to the end-game screen.
    private void showMyLeaderboardRank(int rank, int playersCount) {
        leaderboardRank = rank;
        leaderboardRankPlayersCount = playersCount;
        requestedLeaderboardPlayersCount = playersCount;
        syncLeaderboardToEndGame();
    }

    // Asks the server for the leaderboard once the final scores are ready.
    private void ensureEndGameLeaderboardRequested() {
        if (endGameView == null || model.getFinalScores().isEmpty()) {
            return;
        }
        int playersCount = model.getFinalScores().size();
        endGameView.setLeaderboardPlayersCount(playersCount);
        if (leaderboardEntries == null && requestedLeaderboardPlayersCount != playersCount) {
            requestedLeaderboardPlayersCount = playersCount;
            controller.requestLeaderboard(playersCount, 10);
        }
    }

    // Copies the saved rank and list into the end-game screen if it exists.
    private void syncLeaderboardToEndGame() {
        if (endGameView == null) {
            return;
        }
        if (leaderboardRank != null) {
            endGameView.setLeaderboardRank(leaderboardRank, leaderboardRankPlayersCount);
        }
        if (leaderboardEntries != null) {
            endGameView.setLeaderboardEntries(leaderboardEntriesPlayersCount, leaderboardEntries);
        }
    }

    // Creates the empty container for the end-game screen.
    private BorderPane endGameShell() {
        BorderPane layout = new BorderPane();
        endGameSubtitleLabel = new Label();
        return layout;
    }

    // Shows the rules popup with a fade-and-zoom-in animation.
    public void showRulesModal() {
        if (rulesOverlay != null) {
            sceneRoot.getChildren().remove(rulesOverlay);
        }

        rulesOverlay = createRulesOverlay();
        sceneRoot.getChildren().add(rulesOverlay);

        Node card = rulesOverlay.getChildren().get(0);

        rulesOverlay.setOpacity(0);
        card.setScaleX(0.94);
        card.setScaleY(0.94);

        FadeTransition fade = new FadeTransition(Duration.millis(240), rulesOverlay);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(240), card);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition pt = new ParallelTransition(fade, scale);
        pt.play();
    }

    // Hides the rules popup with a fade-and-zoom-out animation, then removes it.
    public void hideRulesModal() {
        if (rulesOverlay != null) {
            Node card = rulesOverlay.getChildren().get(0);

            FadeTransition fade = new FadeTransition(Duration.millis(180), rulesOverlay);
            fade.setFromValue(1);
            fade.setToValue(0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(180), card);
            scale.setToX(0.94);
            scale.setToY(0.94);

            ParallelTransition pt = new ParallelTransition(fade, scale);
            pt.setOnFinished(event -> {
                sceneRoot.getChildren().remove(rulesOverlay);
                rulesOverlay = null;
            });
            pt.play();
        }
    }

    // Builds the rules popup: title, close button and the 8 rule pages to scroll.
    private StackPane createRulesOverlay() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("rules-overlay");
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.64);");

        VBox card = new VBox(15);
        card.getStyleClass().add("rules-card");
        card.setStyle(
            "-fx-background-color: #2b2b2b;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: #d7a93d;" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 2;" +
            "-fx-max-width: 760px;" +
            "-fx-padding: 16;"
        );
        card.setAlignment(Pos.CENTER);
        card.maxHeightProperty().bind(overlay.heightProperty().multiply(0.86));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("MESOS RULES");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d7a93d;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.getStyleClass().add("secondary-button");
        closeBtn.setStyle(
                "-fx-background-color: #d94e33;" +
                "-fx-background-radius: 999px;" +
                "-fx-border-color: #fff2cf;" +
                "-fx-border-radius: 999px;" +
                "-fx-border-width: 1px;" +
                "-fx-min-width: 34px;" +
                "-fx-min-height: 34px;" +
                "-fx-pref-width: 34px;" +
                "-fx-pref-height: 34px;" +
                "-fx-padding: 0;"
        );
        FontIcon closeIcon = FontIcon.of(MaterialDesignC.CLOSE, 16);
        closeIcon.setIconColor(Color.web("#fff2cf"));
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(event -> hideRulesModal());

        header.getChildren().addAll(title, headerSpacer, closeBtn);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-background-color: #1e1e1e; -fx-background-radius: 8; -fx-border-radius: 8;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox pagesBox = new VBox(22);
        pagesBox.setPadding(new Insets(16));
        pagesBox.setAlignment(Pos.TOP_CENTER);
        pagesBox.setStyle("-fx-background-color: #1e1e1e;");

        for (int p = 1; p <= 8; p++) {
            try {
                String path = "gui/assets/rules/page-" + p + ".png";
                String externalForm = GuiAssetLoader.url(path).map(URL::toExternalForm).orElse(null);
                if (externalForm != null) {
                    ImageView imgView = new ImageView();
                    Image img = new Image(externalForm, true);
                    imgView.setImage(img);
                    imgView.setPreserveRatio(true);
                    imgView.fitWidthProperty().bind(scrollPane.widthProperty().subtract(48));
                    imgView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 4); -fx-background-radius: 6;");
                    pagesBox.getChildren().add(imgView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        scrollPane.setContent(pagesBox);

        overlay.setOnMouseClicked(event -> {
            if (event.getTarget() == overlay) {
                hideRulesModal();
            }
        });

        card.getChildren().addAll(header, scrollPane);
        overlay.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);

        overlay.setFocusTraversable(true);
        Platform.runLater(() -> overlay.requestFocus());
        overlay.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideRulesModal();
            }
        });

        return overlay;
    }

    // Builds the "match paused" screen with player lists, timer and quit button.
    private StackPane createSuspensionOverlay() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("suspension-overlay-root");
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setPickOnBounds(true);

        Image bg = loadSuspensionBackground();
        if (bg != null) {
            overlay.setStyle("-fx-background-image: url('"
                    + GuiAssetLoader.url("assets/suspension/suspended_match_bg.png").map(URL::toExternalForm).orElse("")
                    + "'); -fx-background-size: cover; -fx-background-position: center;");
        } else {
            overlay.getStyleClass().add("suspension-overlay-fallback");
        }

        Region dim = new Region();
        dim.getStyleClass().add("suspension-dim");

        VBox card = new VBox(14);
        card.getStyleClass().add("suspension-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(560);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Label title = new Label("MATCH PAUSED");
        title.getStyleClass().add("suspension-title");

        Label message = new Label("Waiting for at least one other player to reconnect...");
        message.getStyleClass().add("suspension-message");
        message.setWrapText(true);
        message.setMaxWidth(500);
        message.setAlignment(Pos.CENTER);

        Label hint = new Label("Only one player is still connected to this match.");
        hint.getStyleClass().add("suspension-hint");
        hint.setWrapText(true);
        hint.setMaxWidth(500);
        hint.setAlignment(Pos.CENTER);

        suspensionConnectedLabel = new Label();
        suspensionConnectedLabel.getStyleClass().add("suspension-roster-connected");
        suspensionConnectedLabel.setWrapText(true);
        suspensionConnectedLabel.setMaxWidth(500);
        suspensionConnectedLabel.setAlignment(Pos.CENTER);

        suspensionDisconnectedLabel = new Label();
        suspensionDisconnectedLabel.getStyleClass().add("suspension-roster-disconnected");
        suspensionDisconnectedLabel.setWrapText(true);
        suspensionDisconnectedLabel.setMaxWidth(500);
        suspensionDisconnectedLabel.setAlignment(Pos.CENTER);

        suspensionTimerLabel = new Label();
        suspensionTimerLabel.getStyleClass().add("suspension-timer");

        Button quitButton = new Button("QUIT");
        quitButton.getStyleClass().add("suspension-quit");
        quitButton.setOnAction(event -> {
            controller.quit();
            Platform.exit();
        });

        card.getChildren().addAll(title, message, hint,
                suspensionConnectedLabel, suspensionDisconnectedLabel,
                suspensionTimerLabel, quitButton);

        overlay.getChildren().addAll(dim, card);
        StackPane.setAlignment(card, Pos.CENTER);
        return overlay;
    }

    // Loads the background image for the pause screen (null if missing).
    private Image loadSuspensionBackground() {
        try {
            URL url = GuiAssetLoader.url("assets/suspension/suspended_match_bg.png").orElse(null);
            if (url == null) {
                return null;
            }
            Image image = new Image(url.toExternalForm(), false);
            return image.isError() ? null : image;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // Shows or hides the pause screen and updates its timer and player lists.
    private void refreshSuspensionOverlay() {
        if (suspensionOverlay == null) {
            return;
        }
        boolean suspended = model.isGameSuspended() && !model.isGameEnded() && model.isConnected();
        suspensionOverlay.setVisible(suspended);
        suspensionOverlay.setManaged(suspended);
        if (!suspended) {
            return;
        }

        long remaining = model.getSuspensionRemainingMillis();
        if (remaining >= 0L) {
            suspensionTimerLabel.setText("Automatic win timeout: " + formatMillis(remaining));
        } else {
            suspensionTimerLabel.setText("Automatic win timeout: waiting for the server");
        }
        if (remaining >= 0L && remaining <= 10_000L) {
            if (!suspensionTimerLabel.getStyleClass().contains("suspension-timer-critical")) {
                suspensionTimerLabel.getStyleClass().add("suspension-timer-critical");
            }
        } else {
            suspensionTimerLabel.getStyleClass().remove("suspension-timer-critical");
        }

        List<String> connected = new ArrayList<>();
        List<String> disconnected = new ArrayList<>();
        for (String nick : model.getPlayers().keySet()) {
            if (model.isPlayerDisconnected(nick)) {
                disconnected.add(nick);
            } else {
                connected.add(nick);
            }
        }
        Collections.sort(connected);
        Collections.sort(disconnected);

        if (connected.isEmpty()) {
            suspensionConnectedLabel.setText("Still connected: —");
        } else {
            suspensionConnectedLabel.setText("Still connected: " + String.join(", ", connected));
        }
        if (disconnected.isEmpty()) {
            suspensionDisconnectedLabel.setText("Disconnected: —");
            suspensionDisconnectedLabel.setVisible(false);
            suspensionDisconnectedLabel.setManaged(false);
        } else {
            suspensionDisconnectedLabel.setText("Disconnected: " + String.join(", ", disconnected));
            suspensionDisconnectedLabel.setVisible(true);
            suspensionDisconnectedLabel.setManaged(true);
        }
    }

    // Builds the "server disconnected" screen with a message and quit button.
    private StackPane createServerDisconnectedOverlay() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("suspension-overlay-root");
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setPickOnBounds(true);

        Image bg = loadSuspensionBackground();
        if (bg != null) {
            overlay.setStyle("-fx-background-image: url('"
                    + GuiAssetLoader.url("assets/suspension/suspended_match_bg.png").map(URL::toExternalForm).orElse("")
                    + "'); -fx-background-size: cover; -fx-background-position: center;");
        } else {
            overlay.getStyleClass().add("suspension-overlay-fallback");
        }

        Region dim = new Region();
        dim.getStyleClass().add("suspension-dim");

        VBox card = new VBox(14);
        card.getStyleClass().add("suspension-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(560);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Label title = new Label("SERVER DISCONNECTED");
        title.getStyleClass().add("suspension-title");

        Label message = new Label("Connection to the server was lost. Restart the server and reconnect with the same nickname to resume the match.");
        message.getStyleClass().add("suspension-message");
        message.setWrapText(true);
        message.setMaxWidth(500);
        message.setAlignment(Pos.CENTER);

        Button quitButton = new Button("QUIT");
        quitButton.getStyleClass().add("suspension-quit");
        quitButton.setOnAction(event -> Platform.exit());

        card.getChildren().addAll(title, message, quitButton);
        overlay.getChildren().addAll(dim, card);
        StackPane.setAlignment(card, Pos.CENTER);
        return overlay;
    }

    // Shows or hides the "server disconnected" screen based on the state.
    private void refreshServerDisconnectedOverlay() {
        if (serverDisconnectedOverlay == null) {
            return;
        }
        boolean show = model.isServerDisconnected() && !model.isGameEnded();
        serverDisconnectedOverlay.setVisible(show);
        serverDisconnectedOverlay.setManaged(show);
    }

    // Called every second: updates the overlays and the connection/timer label.
    private void refreshTimer() {
        refreshSuspensionOverlay();
        refreshServerDisconnectedOverlay();
        if (currentScreen == gameScreen && gameBoardView != null) {
            gameBoardView.refreshElapsedTimer();
        }
        if (timerLabel == null) {
            return;
        }
        if (model.isGameSuspended()) {
            long remaining = model.getSuspensionRemainingMillis();
            timerLabel.setText("MATCH PAUSED — " + formatMillis(remaining));
            timerLabel.getStyleClass().setAll("timer", "timer-alert");
        } else {
            timerLabel.setText("Connection active");
            timerLabel.getStyleClass().setAll("timer");
        }
    }

    // Shows a new toast for the latest connection message or error, only once.
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

    // Shows a small popup message that slides in, waits, then slides out.
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

    // Turns milliseconds into a "mm:ss" text for the timers.
    private String formatMillis(long millis) {
        if (millis < 0) {
            return "--:--";
        }
        long totalSeconds = millis / 1000L;
        return String.format("%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    // Finds the GUI stylesheet (CSS) file, or fails if it is missing.
    private String cssResource() {
        return GuiAssetLoader.url("gui/gui.css")
                .orElseThrow(() -> new IllegalStateException("Missing GUI stylesheet"))
                .toExternalForm();
    }

    private record LaunchContext(ClientController controller, ClientModel model) {}

    // Base class for the four screens: each one builds itself and updates itself.
    private abstract class ScreenController {
        // Builds the screen's content and returns it.
        abstract Node mount();

        // Updates the screen with the latest data.
        abstract void refresh();

        // Says if this screen should play the menu music (off by default).
        boolean playsMenuMusic() {
            return false;
        }
    }

    // The login screen.
    private final class LoginScreen extends ScreenController {
        // Builds the login view the first time and returns it.
        @Override
        Node mount() {
            if (loginView == null) {
                loginView = new LoginView(controller, model, pendingNickname,
                        nickname -> pendingNickname = nickname);
                Node loginPanel = loginView.render();
                loginShell = new StackPane(loginPanel);
                loginShell.setPadding(new Insets(40));
                StackPane.setAlignment(loginPanel, Pos.CENTER);
                loginShell.getStyleClass().add("login-root");
            }
            return loginShell;
        }

        // Updates the login view.
        @Override
        void refresh() {
            loginView.refresh();
        }
    }

    // The lobby (waiting room) screen.
    private final class LobbyScreen extends ScreenController {
        // Builds the lobby view the first time and returns it.
        @Override
        Node mount() {
            if (lobbyView == null) {
                lobbyView = new LobbyView(controller, model, assetCatalog);
                lobbyShell = new BorderPane();
                lobbyShell.getStyleClass().add("lobby-shell");
                lobbyShell.setCenter(lobbyView.render());
            }
            return lobbyShell;
        }

        // Updates the lobby view.
        @Override
        void refresh() {
            lobbyView.refresh();
        }

        // The lobby plays the menu music.
        @Override
        boolean playsMenuMusic() {
            return true;
        }
    }

    // The main game board screen.
    private final class GameScreen extends ScreenController {
        // Builds the game board view the first time and returns it.
        @Override
        Node mount() {
            if (gameBoardView == null) {
                gameBoardView = new GameBoardView(controller, model, assetCatalog);
                gameShell = new BorderPane();
                gameShell.getStyleClass().add("game-shell");
                gameShell.setCenter(gameBoardView.render());
            }
            return gameShell;
        }

        // Updates the game board view.
        @Override
        void refresh() {
            gameBoardView.refresh();
        }
    }

    // The end-game (final results) screen.
    private final class EndGameScreen extends ScreenController {
        // Builds the end-game view and asks for the leaderboard the first time.
        @Override
        Node mount() {
            if (endGameView == null) {
                endGameView = new EndGameView(controller, model, assetCatalog);
                endGameShell = endGameShell();
                endGameShell.setCenter(endGameView.render());
                syncLeaderboardToEndGame();
                ensureEndGameLeaderboardRequested();
                Region overlay = new Region();
                overlay.getStyleClass().add("endgame-overlay");
                overlay.setMouseTransparent(true);
                endGameStack = new StackPane(overlay, endGameShell);
                endGameStack.getStyleClass().add("endgame-root");
            }
            return endGameStack;
        }

        // Updates the winner text and refreshes the end-game view.
        @Override
        void refresh() {
            String winNick = model.getWinnerNickname();
            endGameSubtitleLabel.setText(winNick != null
                    ? "Winner: " + winNick
                    : "Shared victory");
            ensureEndGameLeaderboardRequested();
            endGameView.refresh();
        }

        // The end-game screen plays the menu music.
        @Override
        boolean playsMenuMusic() {
            return true;
        }
    }
}
