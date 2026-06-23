package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.GamePhase;
import it.polimi.ingsw.view.View;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tui implements View {

    private static final long TRIBE_VISIBLE_MILLIS = 10_000L;
    private static final long BUILDING_EFFECT_VISIBLE_MILLIS = 8_000L;

    private Thread inputThread;
    private final ClientController controller;
    private final ClientModel model;
    private final CommandParser parser;
    private final Scanner scanner;
    private final ExecutorService outboundExecutor;
    private final Object modelLock = new Object();
    private final Renderer renderer;
    private volatile String activePrompt;
    private volatile long tribeVisibleUntilMillis = -1L;
    private volatile long buildingEffectVisibleUntilMillis = -1L;
    private volatile int visibleTribeNumber;
    private volatile int visibleBuildingEffectId;
    // Leaderboard
    private volatile Integer endGameMyRank = null;
    private volatile int endGameMyRankPlayersCount = 0;
    private volatile List<LeaderboardEntry> endGameLeaderboard = null;
    private volatile int endGameLeaderboardPlayersCount = 0;

    // Wire up controller, model, command parser, stdin scanner and the renderer.
    public Tui(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model      = model;
        this.parser     = new CommandParser(controller);
        this.scanner    = new Scanner(System.in);
        this.outboundExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "tui-client-outbound");
            thread.setDaemon(true);
            return thread;
        });
        this.controller.setOutboundDispatcher(outboundExecutor::execute);
        this.renderer   = new Renderer(model);
    }

    /**
     * Single rendering path, invoked by the {@link View} callbacks the
     * controller fires after each server message. Re-renders the board,
     * repaints the end-game artifacts, restores the active prompt and wakes any
     * thread blocked in one of the wait-loops below.
     */
    // Repaint the whole screen and wake up any thread waiting on the model.
    private void refresh() {
        renderer.render(model);
        paintEndGameLeaderboardArtifacts();
        if (activePrompt != null) {
            System.out.print(activePrompt);
        }
        synchronized (modelLock) {
            modelLock.notifyAll();
        }
    }

    // ── View output callbacks (driven by ClientController) ───────────────────

    @Override public void onLoginResult()            { refresh(); } // server answered our login/reconnect, repaint
    @Override public void onLobbyChanged()           { refresh(); } // someone joined/left or lobby settings changed
    @Override public void onGameStarted()            { refresh(); } // match just kicked off
    @Override public void onGameStateChanged()       { refresh(); } // we moved to a new game phase
    @Override public void onBoardChanged()           { refresh(); } // board/cards/totems changed
    @Override public void onEventResolved()          { refresh(); } // an event card got resolved
    @Override public void onGameEnded()              { refresh(); } // game over
    @Override public void onError()                  { refresh(); } // server sent an error to show
    @Override public void onConnectionLost()         { refresh(); } // we dropped the connection
    @Override public void onGameSuspended()          { refresh(); } // game paused (someone disconnected)
    @Override public void onGameResumed()            { refresh(); } // game un-paused
    @Override public void onPlayerConnectionChanged(){ refresh(); } // a player connected/disconnected

    // Entry point: login, wait for first sync, start input + timer threads, then block until the game ends.
    @Override
    public void run() {
        try {
            renderer.render(model);
            loginLoop();
            if (!isClientActive()) {
                return;
            }

            waitForInitialStateSync();
            if (!isClientActive()) {
                return;
            }

            inputThread = new Thread(this::interactiveInputLoop);
            inputThread.setDaemon(true);
            inputThread.start();

            Thread timerThread = new Thread(this::suspensionTimerRenderLoop);
            timerThread.setDaemon(true);
            timerThread.start();

            waitForGameEnd();
            endGame();
        } finally {
            outboundExecutor.shutdownNow();
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────

    // Keep asking for nickname (+ reconnect choice or color) until login succeeds or we drop.
    private void loginLoop() {
        while (!model.isLoginSuccess() && model.isConnected()) {
            String nickname = readLineWithPrompt("Nickname: ").trim();
            if (nickname.isBlank()) continue;
            String previousError = model.getLastError();

            if (askReconnect()) {
                controller.reconnect(nickname);
            } else {
                TotemColor color = askTotemColor();
                if (color == null) continue;
                controller.login(nickname, color);
            }
            waitForLoginOutcome(previousError);

            if (model.isLoginSuccess()) {
                model.setMyNickname(nickname);
            } else if (model.getLastError() != null) {
                System.out.println("[ERROR] " + model.getLastError());
            }
        }
    }

    // Ask y/n whether they want to reconnect to an existing match.
    private boolean askReconnect() {
        while (true) {
            String answer = readLineWithPrompt(
                    "Do you want to reconnect to an existing match? [y/n]: ").trim().toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) return true;
            if (answer.equals("n") || answer.equals("no"))  return false;
            System.out.println("[ERROR] Please answer 'y' or 'n'.");
        }
    }

    // Ask for a totem color, re-prompting until they pick a free valid one (quit -> null).
    private TotemColor askTotemColor() {
        while (true) {
            java.util.List<TotemColor> available = model.getAvailableTotemColors();

            if (available.isEmpty()) {
                System.out.println("[ERROR] No totem color is available right now.");
                return null;
            }

            String input = readLineWithPrompt("Totem Color: ").trim().toUpperCase();
            if (input.equalsIgnoreCase("quit")) return null;

            TotemColor parsed;
            try {
                parsed = TotemColor.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] Invalid color. Type 'quit' to cancel.");
                continue;
            }

            if (!available.contains(parsed)) {
                System.out.println("[ERROR] Color " + parsed + " is already taken.");
                continue;
            }
            return parsed;
        }
    }
    // ── Main input loop ──────────────────────────────────────────────────────

    // Main in-game loop: read a line and route it to the right handler/command.
    private void interactiveInputLoop() {
        while (isClientActive() && !model.isGameEnded()) {
            if (model.isReconnecting()) {
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
                continue;
            }
            if (model.isGameSuspended()
                    || model.getLastConnectionEvent() != null
                    || model.getLastTurnEvent() != null) {
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
                continue;
            }

            String line;
            try {
                line = readLineWithPrompt("> ").trim();
            } catch (Exception e) {
                return;
            }
            if (line.isBlank()) {
                if (renderer.isTribeVisible()) {
                    closeTribe();
                } else if (renderer.isBuildingEffectVisible()) {
                    closeBuildingEffect();
                }
                continue;
            }

            String state = model.getGameState();
            if (isSeeEffectCommand(line)) {
                handleSeeEffectCommand(line, state);
                continue;
            }
            if (isShowTribeCommand(line)) {
                handleShowTribeCommand(line, state);
                continue;
            }

            GamePhase phase = GamePhase.fromStateName(state);
            if (GamePhase.isPreGameState(state)) {
                handlePreGameCommand(line);
                continue;
            }

            if (phase.isTotemPlacement()
                    && line.toLowerCase().startsWith("place")
                    && !model.isMyTotemPlacementTurn()) {
                String currentPlayer = model.getCurrentTotemPlacementPlayer();
                if (currentPlayer != null) {
                    System.out.println("[ERROR] It's not your turn: it's " + currentPlayer + "'s turn.");
                } else {
                    System.out.println("[ERROR] No player available for placement at this moment.");
                }
                continue;
            }

            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            if (parser.parse(line, () -> { })) {
                controller.disconnect();
                return;
            }
        }
    }

    // Background thread: while the game is paused, re-render every second so the countdown ticks.
    private void suspensionTimerRenderLoop() {
        while (isClientActive() && !model.isGameEnded()) {
            if (model.isGameSuspended()) {
                renderer.render(model);
                if (activePrompt != null) {
                    System.out.print(activePrompt);
                }
            }

            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // ── Pre-game commands (lobby + ready check) ──────────────────────────────

    // Handle lobby / ready-check commands (ready, unready, players <n>, ...).
    private void handlePreGameCommand(String line) {
        if (line.equalsIgnoreCase("help")) {
            printPreGameHelp();
            return;
        }

        if (model.isLobbyLeader()
                && model.getExpectedPlayers() < CommandParser.MIN_PLAYERS
                && line.matches("\\d+")) {
            parser.parse("players " + line, () -> { });
            return;
        }

        if (line.toLowerCase().startsWith("players") && !model.isLobbyLeader()) {
            System.out.println("[ERROR] Only the lobby leader can choose the number of participants.");
            return;
        }

        if (parser.parse(line, this::printPreGameHelp)) {
            controller.disconnect();
            return;
        }
    }

    // Print which pre-game commands are available (the leader sees a couple extra).
    private void printPreGameHelp() {
        if (model.isLobbyLeader()) {
            System.out.println("Available commands before the game starts: ready, unready, players <"
                    + CommandParser.MIN_PLAYERS + "-" + CommandParser.MAX_PLAYERS + ">, quit");
            if (model.getExpectedPlayers() < CommandParser.MIN_PLAYERS) {
                System.out.println("As lobby leader you can also type a number between "
                        + CommandParser.MIN_PLAYERS + " and "
                        + CommandParser.MAX_PLAYERS + " directly.");
            }
        } else {
            System.out.println("Available commands before the game starts: ready, unready, quit");
        }
    }

    // ── End ──────────────────────────────────────────────────────────────────

    // Final cleanup: wait for the input thread to finish, then disconnect.
    private void endGame() {
        if (!isClientActive()) {
            System.out.println("\nConnection lost.");
            return;
        }
        System.out.println("\nGame ended. Press ENTER to quit.");

        if (inputThread != null) {
            try {
                inputThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        controller.disconnect();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    // Block up to 10s waiting for any model update (generic helper).
    private void waitForModel() {
        synchronized (modelLock) {
            try { modelLock.wait(10_000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // Block until the server answers our login/reconnect (success, or a new error shows up).
    private void waitForLoginOutcome(String previousError) {
        synchronized (modelLock) {
            while (model.isConnected()
                    && !model.isLoginSuccess()
                    && java.util.Objects.equals(previousError, model.getLastError())) {
                try {
                    modelLock.wait(10_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    // Block until the game ends or we lose the connection.
    private void waitForGameEnd() {
        synchronized (modelLock) {
            while (isClientActive() && !model.isGameEnded()) {
                try { modelLock.wait(1_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    // Block until the first state/lobby snapshot lands after login.
    private void waitForInitialStateSync() {
        synchronized (modelLock) {
            while (isClientActive()
                    && model.getGameState() == null
                    && model.getLobbyPlayers().isEmpty()) {
                try { modelLock.wait(1_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    // True if we're still connected or in the middle of reconnecting.
    private boolean isClientActive() {
        return model.isConnected() || model.isReconnecting();
    }

    // Print a prompt and read one line from stdin (remembers the prompt so it can be repainted).
    private String readLineWithPrompt(String prompt) {
        activePrompt = prompt;
        if (!prompt.isEmpty()) {
            System.out.print(prompt);
        }
        try {
            return scanner.nextLine();
        } finally {
            activePrompt = null;
        }
    }

    // Dump the in-game command cheat sheet.
    private void printHelp() {
        System.out.println("  place <letter>         — place totem");
        System.out.println("  pick <id> upper|lower  — take character card");
        System.out.println("  build <id> upper|lower — take building card");
        System.out.println("  see effect <id>        — show a building effect locally");
        System.out.println("  show tribe <n>         — show one other tribe locally");
        System.out.println("  quit                   — disconnect");
        System.out.println("  help                   — show available commands");
    }

    // True if the line looks like "see effect <id>".
    private boolean isSeeEffectCommand(String line) {
        return line != null && line.trim().toLowerCase().matches("see\\s+effect\\s+\\d+");
    }

    // Validate and show a building's effect locally for a few seconds.
    private void handleSeeEffectCommand(String line, String state) {
        if (!isBoardVisibleState(state)) {
            System.out.println("[ERROR] see effect is available only while the board is visible.");
            return;
        }

        int buildingId = parseSeeEffectId(line);
        if (!CardCatalog.isBuilding(buildingId)) {
            System.out.println("[ERROR] " + buildingId + " is not a building card.");
            return;
        }
        if (!model.getUpperRowBuildingIds().contains(buildingId)
                && !model.getLowerRowBuildingIds().contains(buildingId)) {
            System.out.println("[ERROR] Building " + buildingId + " is not visible on the board.");
            return;
        }

        long deadline = System.currentTimeMillis() + BUILDING_EFFECT_VISIBLE_MILLIS;
        buildingEffectVisibleUntilMillis = deadline;
        visibleBuildingEffectId = buildingId;
        renderer.setVisibleBuildingEffectId(buildingId);
        renderer.render(model);
        scheduleBuildingEffectClose(deadline, buildingId);
    }

    // True if the board is on screen in this game state.
    private boolean isBoardVisibleState(String state) {
        return GamePhase.fromStateName(state).hasBoardVisible();
    }

    // Pull the building id out of "see effect <id>".
    private int parseSeeEffectId(String line) {
        String[] parts = line.trim().split("\\s+");
        return Integer.parseInt(parts[2]);
    }

    // Spawn a daemon that auto-closes the building-effect popup once its time runs out.
    private void scheduleBuildingEffectClose(long deadline, int buildingId) {
        Thread closer = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (!renderer.isBuildingEffectVisible()
                        || buildingEffectVisibleUntilMillis != deadline
                        || visibleBuildingEffectId != buildingId) {
                    return;
                }

                if (System.currentTimeMillis() >= deadline) {
                    closeBuildingEffect();
                    return;
                }
            }
        });
        closer.setDaemon(true);
        closer.start();
    }

    // True if the line looks like "show tribe <n>".
    private boolean isShowTribeCommand(String line) {
        return line != null && line.trim().toLowerCase().matches("show\\s+tribe\\s+\\d+");
    }

    // Validate and show another player's tribe locally for a few seconds.
    private void handleShowTribeCommand(String line, String state) {
        GamePhase phase = GamePhase.fromStateName(state);
        if (GamePhase.isPreGameState(state)) {
            System.out.println("[ERROR] show tribe is available only during the match.");
            return;
        }
        if (phase.isEventResolution()) {
            System.out.println("[ERROR] show tribe is not available during event resolution.");
            return;
        }
        if (phase.isEndGame()) {
            System.out.println("[ERROR] The game is already over.");
            return;
        }

        int tribeNumber = parseShowTribeNumber(line);
        int maxTribeNumber = otherTribeCount();
        if (tribeNumber < 1 || tribeNumber > maxTribeNumber) {
            System.out.println("[ERROR] Unknown tribe number. Use show tribe <1-" + maxTribeNumber + ">.");
            return;
        }

        long deadline = System.currentTimeMillis() + TRIBE_VISIBLE_MILLIS;
        tribeVisibleUntilMillis = deadline;
        visibleTribeNumber = tribeNumber;
        renderer.setVisibleTribeNumber(tribeNumber, countdownSeconds(deadline));
        renderer.render(model);
        scheduleTribeClose(deadline, tribeNumber);
    }

    // Pull the tribe number out of "show tribe <n>".
    private int parseShowTribeNumber(String line) {
        String[] parts = line.trim().split("\\s+");
        return Integer.parseInt(parts[2]);
    }

    // Count how many tribes belong to players other than me.
    private int otherTribeCount() {
        String myNick = model.getMyNickname();
        int count = 0;
        for (String nickname : model.getPlayers().keySet()) {
            if (!nickname.equals(myNick)) {
                count++;
            }
        }
        return count;
    }

    // Spawn a daemon that ticks the tribe countdown and closes the popup when time's up.
    private void scheduleTribeClose(long deadline, int tribeNumber) {
        Thread closer = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (!renderer.isTribeVisible()
                        || tribeVisibleUntilMillis != deadline
                        || visibleTribeNumber != tribeNumber) {
                    return;
                }

                int remaining = countdownSeconds(deadline);
                if (System.currentTimeMillis() >= deadline) {
                    closeTribe();
                    return;
                }

                renderer.setVisibleTribeNumber(tribeNumber, remaining);
                renderer.render(model);
                if (activePrompt != null) {
                    System.out.print(activePrompt);
                }
            }
        });
        closer.setDaemon(true);
        closer.start();
    }

    // Seconds left until the deadline, rounded up.
    private int countdownSeconds(long deadline) {
        long remainingMillis = Math.max(0L, deadline - System.currentTimeMillis());
        return (int) Math.ceil(remainingMillis / 1000.0);
    }

    // Hide the tribe popup and repaint.
    private void closeTribe() {
        tribeVisibleUntilMillis = -1L;
        visibleTribeNumber = 0;
        renderer.setVisibleTribeNumber(0, 0);
        renderer.render(model);
        if (activePrompt != null) {
            System.out.print(activePrompt);
        }
    }

    // Hide the building-effect popup and repaint.
    private void closeBuildingEffect() {
        buildingEffectVisibleUntilMillis = -1L;
        visibleBuildingEffectId = 0;
        renderer.setVisibleBuildingEffectId(0);
        renderer.render(model);
        if (activePrompt != null) {
            System.out.print(activePrompt);
        }
    }

    // ── Leaderboard rendering ───────────────────────────────────────────────

    private static final DateTimeFormatter LEADERBOARD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    // Server told us our final rank: stash it and print the banner.
    @Override
    public void showMyRank(int rank, int playersCount) {
        endGameMyRank = rank;
        endGameMyRankPlayersCount = playersCount;
        paintMyRank(rank, playersCount);
        if (activePrompt != null) {
            System.out.print(activePrompt);
        }
        // The controller auto-fetches the full leaderboard right after this call,
        // which arrives via showLeaderboard() and is painted below the rank banner.
    }

    // Server sent the full leaderboard: stash it and print the table.
    @Override
    public void showLeaderboard(int playersCount, List<LeaderboardEntry> entries) {
        endGameLeaderboard = entries;
        endGameLeaderboardPlayersCount = playersCount;
        paintLeaderboard(playersCount, entries);
        if (activePrompt != null) {
            System.out.print(activePrompt);
        }
    }

    /**
     * Re-prints the rank banner and leaderboard table after each render cycle,
     * so they survive the screen wipe done by Renderer.render().
     */
    // Re-print rank + leaderboard after a render wipe so they don't disappear.
    private void paintEndGameLeaderboardArtifacts() {
        Integer rank = endGameMyRank;
        if (rank != null) {
            paintMyRank(rank, endGameMyRankPlayersCount);
        }
        List<LeaderboardEntry> entries = endGameLeaderboard;
        if (entries != null) {
            paintLeaderboard(endGameLeaderboardPlayersCount, entries);
        }
    }

    // Print the "you finished #N" line.
    private void paintMyRank(int rank, int playersCount) {
        System.out.println();
        System.out.printf("You finished #%d in the %d-player leaderboard.%n",
                rank, playersCount);
    }

    // Print the leaderboard table (or a placeholder if it's empty).
    private void paintLeaderboard(int playersCount, List<LeaderboardEntry> entries) {
        StringBuilder out = new StringBuilder();
        out.append('\n');
        out.append("══════════════ LEADERBOARD (").append(playersCount).append(" players) ══════════════\n");

        if (entries == null || entries.isEmpty()) {
            out.append("  (no matches recorded yet)\n");
        } else {
            out.append(String.format("  %-6s %-20s %-7s %s%n", "RANK", "NICKNAME", "SCORE", "DATE"));
            out.append("  ──────────────────────────────────────────────────────────\n");
            for (LeaderboardEntry e : entries) {
                out.append(String.format("  #%-5d %-20s %-7d %s%n",
                        e.rank(),
                        truncate(e.nickname(), 20),
                        e.finalScore(),
                        LEADERBOARD_DATE_FORMAT.format(e.playedAt())));
            }
        }
        out.append("══════════════════════════════════════════════════════════════");

        System.out.println(out);
    }

    // Cut a string down to max length, adding an ellipsis if it was too long.
    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
