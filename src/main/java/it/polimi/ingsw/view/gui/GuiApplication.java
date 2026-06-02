package it.polimi.ingsw.view.gui;

import atlantafx.base.theme.PrimerDark;
import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.GamePhase;
import it.polimi.ingsw.view.gui.asset.GuiAssetCatalog;
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
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public final class GuiApplication extends Application {
    private static final Duration TOAST_VISIBLE_DURATION = Duration.seconds(10);
    private static final Duration TOAST_ANIMATION_DURATION = Duration.millis(180);

    private static LaunchContext launchContext;
    private static GuiApplication instance;

    private StackPane rulesOverlay;
    private int currentRulesPage = 1;

    public static void openRulesPdf() {
        if (instance == null) {
            return;
        }
        Platform.runLater(() -> instance.showRulesModal());
    }

    public void showRulesModal() {
        if (rulesOverlay != null) {
            sceneRoot.getChildren().remove(rulesOverlay);
        }

        currentRulesPage = 1;
        rulesOverlay = createRulesOverlay();
        sceneRoot.getChildren().add(rulesOverlay);

        Node card = rulesOverlay.getChildren().get(0);
        
        // Initial state for animation
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
        
        // Responsive modal height bound to 86% of application height
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
        var closeIcon = org.kordamp.ikonli.javafx.FontIcon.of(MaterialDesignC.CLOSE, 16);
        closeIcon.setIconColor(javafx.scene.paint.Color.web("#fff2cf"));
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(event -> hideRulesModal());
        
        header.getChildren().addAll(title, headerSpacer, closeBtn);

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-background-color: #1e1e1e; -fx-background-radius: 8; -fx-border-radius: 8;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox pagesBox = new VBox(22);
        pagesBox.setPadding(new Insets(16));
        pagesBox.setAlignment(Pos.TOP_CENTER);
        pagesBox.setStyle("-fx-background-color: #1e1e1e;");

        for (int p = 1; p <= 8; p++) {
            try {
                String path = "gui/assets/rules/page-" + p + ".png";
                java.net.URL url = GuiApplication.class.getClassLoader().getResource(path);
                if (url != null) {
                    javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
                    javafx.scene.image.Image img = new javafx.scene.image.Image(url.toExternalForm(), true);
                    imgView.setImage(img);
                    imgView.setPreserveRatio(true);
                    
                    // Perfect responsive width binding
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
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                hideRulesModal();
            }
        });

        return overlay;
    }


    private ClientController controller;
    private ClientModel model;
    private GuiAssetCatalog assetCatalog;
    private StackPane sceneRoot;
    private BorderPane root;
    private StackPane suspensionOverlay;
    private Label suspensionTimerLabel;
    private Label suspensionConnectedLabel;
    private Label suspensionDisconnectedLabel;
    private VBox toastHost;
    private Label timerLabel;
    private Label endGameSubtitleLabel;
    private String pendingNickname;
    private Timeline timer;
    private boolean renderScheduled;
    private long shownConnectionEventVersion;
    private long shownErrorVersion;
    private Screen currentScreen;
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

    static void launchWith(ClientController controller, ClientModel model) {
        launchContext = new LaunchContext(controller, model);
        Application.launch(GuiApplication.class);
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        if (launchContext == null) {
            throw new IllegalStateException("GUI launched without client context");
        }

        controller = launchContext.controller();
        model = launchContext.model();
        controller.setDispatcher(Platform::runLater);
        assetCatalog = GuiAssetCatalog.loadDefault();
        GuiAudio.preloadActionClick();
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

        // The controller drives this view through the View callbacks (see Gui),
        // so we no longer observe the model directly.

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
        GuiAudio.shutdown();
    }

    /**
     * Coalesces model-change notifications into a single render per JavaFX
     * pulse. A burst of {@code notifyChanged()} calls (e.g. the many messages
     * that land at game start or a round transition) would otherwise trigger
     * one full board refresh each; here they collapse into one. The dispatcher
     * runs every model write on the FX thread, so this flag is single-threaded.
     */
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

    private void render() {
        if (model.isLoginSuccess() && model.getMyNickname() == null && pendingNickname != null) {
            // The controller no longer re-notifies us on this write, so don't bail
            // out here: set the nickname and keep rendering this same pass.
            model.setMyNickname(pendingNickname);
        }

        Screen nextScreen = resolveScreen();
        if (nextScreen != currentScreen) {
            switchScreen(nextScreen);
            currentScreen = nextScreen;
        }
        refreshCurrentScreen();
        refreshTimer();
        showPendingToasts();
    }

    private Screen resolveScreen() {
        if (!model.isLoginSuccess()) {
            return Screen.LOGIN;
        }
        if (model.isGameEnded()) {
            return Screen.END_GAME;
        }
        if (model.isInPreGame()) {
            return Screen.LOBBY;
        }
        return Screen.GAME;
    }

    private void switchScreen(Screen screen) {
        root.setTop(null);
        root.setLeft(null);
        root.setRight(null);
        root.setBottom(null);
        timerLabel = null;
        syncAudioFor(screen);

        switch (screen) {
            case LOGIN -> {
                if (loginView == null) {
                    loginView = new LoginView(controller, model, pendingNickname,
                            nickname -> pendingNickname = nickname);
                    Node loginPanel = loginView.render();
                    loginShell = new StackPane(loginPanel);
                    loginShell.setPadding(new Insets(40));
                    StackPane.setAlignment(loginPanel, Pos.CENTER);
                    loginShell.getStyleClass().add("login-root");
                }
                root.setCenter(loginShell);
            }
            case LOBBY -> {
                if (lobbyView == null) {
                    lobbyView = new LobbyView(controller, model, assetCatalog);
                    lobbyShell = new BorderPane();
                    lobbyShell.getStyleClass().add("lobby-shell");
                    lobbyShell.setCenter(lobbyView.render());
                }
                root.setCenter(lobbyShell);
            }
            case GAME -> {
                if (gameBoardView == null) {
                    gameBoardView = new GameBoardView(controller, model, assetCatalog);
                    gameShell = new BorderPane();
                    gameShell.getStyleClass().add("game-shell");
                    gameShell.setCenter(gameBoardView.render());
                }
                root.setCenter(gameShell);
            }
            case END_GAME -> {
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
                root.setCenter(endGameStack);
            }
        }
    }

    private void syncAudioFor(Screen screen) {
        switch (screen) {
            case LOBBY, END_GAME -> GuiAudio.playMenuMusic();
            case LOGIN, GAME -> GuiAudio.stopMenuMusic();
        }
    }

    private void refreshCurrentScreen() {
        if (currentScreen == null) {
            return;
        }
        switch (currentScreen) {
            case LOGIN -> loginView.refresh();
            case LOBBY -> lobbyView.refresh();
            case GAME -> gameBoardView.refresh();
            case END_GAME -> {
                String winNick = model.getWinnerNickname();
                endGameSubtitleLabel.setText(winNick != null
                        ? "Winner: " + winNick
                        : "Shared victory");
                ensureEndGameLeaderboardRequested();
                endGameView.refresh();
            }
        }
    }

    private BorderPane endGameShell() {
        BorderPane layout = new BorderPane();
        endGameSubtitleLabel = new Label();
        return layout;
    }

    private void showMyLeaderboardRank(int rank, int playersCount) {
        leaderboardRank = rank;
        leaderboardRankPlayersCount = playersCount;
        // Remember the bucket so ensureEndGameLeaderboardRequested() doesn't fetch
        // it again: the controller already auto-requests the full leaderboard
        // right after delivering this rank.
        requestedLeaderboardPlayersCount = playersCount;
        syncLeaderboardToEndGame();
    }

    // ── View callbacks (forwarded by Gui, driven by ClientController) ─────────

    /** The live application, or {@code null} before {@link #start} has run. */
    static GuiApplication current() {
        return instance;
    }

    /** Login/reconnect was answered: adopt the chosen nickname and re-render. */
    void onLoginResult() {
        if (model.isLoginSuccess() && model.getMyNickname() == null && pendingNickname != null) {
            model.setMyNickname(pendingNickname);
        }
        scheduleRender();
    }

    /** Any board/lobby/lifecycle state changed: coalesced full re-render. */
    void onBoardEvent() {
        scheduleRender();
    }

    /** A leaderboard response arrived (delegates to the existing painter). */
    void onLeaderboard(int playersCount, List<LeaderboardEntry> entries) {
        showLeaderboard(playersCount, entries);
    }

    /** This client's final rank arrived (delegates to the existing painter). */
    void onMyRank(int rank, int playersCount) {
        showMyLeaderboardRank(rank, playersCount);
    }

    private void showLeaderboard(int playersCount, List<LeaderboardEntry> entries) {
        leaderboardEntriesPlayersCount = playersCount;
        leaderboardEntries = entries == null ? List.of() : new ArrayList<>(entries);
        syncLeaderboardToEndGame();
    }

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

    private StackPane centeredShell(Node node) {
        StackPane wrapper = new StackPane(node);
        wrapper.setPadding(new Insets(24));
        return wrapper;
    }

    private void refreshTimer() {
        refreshSuspensionOverlay();
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

    /**
     * Builds the full-screen overlay shown when the server reports the match is suspended
     * (only one player remains connected, FA4 — disconnection resilience). The overlay is
     * purely visual: the server stays authoritative on the suspension timeout and on the
     * forced-winner decision; this view only mirrors {@code ClientModel} state.
     */
    private StackPane createSuspensionOverlay() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("suspension-overlay-root");
        overlay.setVisible(false);
        overlay.setManaged(false);
        // pickOnBounds = true so the overlay swallows clicks on the underlying board while suspended.
        overlay.setPickOnBounds(true);

        Image bg = loadSuspensionBackground();
        if (bg != null) {
            // Apply via inline style so CSS rules don't override BackgroundImage; cover so it fills the screen.
            overlay.setStyle("-fx-background-image: url('"
                    + getClass().getResource("/assets/suspension/suspended_match_bg.png").toExternalForm()
                    + "'); -fx-background-size: cover; -fx-background-position: center;");
        } else {
            // No image available: fall back to the CSS gradient defined for this class.
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

        // Voluntary quit from the suspension screen: sends an explicit QuitMessage so the
        // server terminates the match for everyone instead of keeping it suspended on the
        // assumption this player might come back. Server-side handling in LobbyManager#handleQuit.
        // Then close the window: the local model still carries the stale suspension flag and
        // its countdown is no longer meaningful for someone who chose to leave.
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

    /**
     * Attempts to load the dedicated suspended-match background. The asset folder
     * ({@code src/main/resources/assets/}) is git-ignored, so the file may legitimately
     * be missing on some machines; in that case we silently fall back to the gradient
     * defined in {@code .suspension-overlay-root}.
     */
    private Image loadSuspensionBackground() {
        try {
            var url = getClass().getResource("/assets/suspension/suspended_match_bg.png");
            if (url == null) {
                return null;
            }
            Image image = new Image(url.toExternalForm(), false);
            return image.isError() ? null : image;
        } catch (RuntimeException ex) {
            // Any loader failure (corrupt file, unsupported format) falls back to the CSS gradient.
            return null;
        }
    }

    private void refreshSuspensionOverlay() {
        if (suspensionOverlay == null) {
            return;
        }
        // Hide overlay once the game has actually ended (e.g. forced-winner timeout fired):
        // the EndGameView is the authoritative end screen, the suspension overlay must step aside.
        // Also hide it after a voluntary quit (!isConnected): the local model still carries the
        // pre-quit suspension flag, but the countdown is no longer meaningful to a player who left.
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
        // Last 10 seconds: switch to the critical style (red glow) for visual urgency.
        if (remaining >= 0L && remaining <= 10_000L) {
            if (!suspensionTimerLabel.getStyleClass().contains("suspension-timer-critical")) {
                suspensionTimerLabel.getStyleClass().add("suspension-timer-critical");
            }
        } else {
            suspensionTimerLabel.getStyleClass().remove("suspension-timer-critical");
        }

        java.util.List<String> connected = new java.util.ArrayList<>();
        java.util.List<String> disconnected = new java.util.ArrayList<>();
        for (String nick : model.getPlayers().keySet()) {
            if (model.isPlayerDisconnected(nick)) {
                disconnected.add(nick);
            } else {
                connected.add(nick);
            }
        }
        java.util.Collections.sort(connected);
        java.util.Collections.sort(disconnected);

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
        return GamePhase.titleLabelFor(model.getGameState(), "Synchronizing");
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

    private enum Screen {
        LOGIN,
        LOBBY,
        GAME,
        END_GAME
    }
}
