package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.View;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.serverbound.*;

import java.util.Scanner;

public class Tui implements View {

    private final ClientController controller;
    private final ClientModel model;
    private final CommandParser parser;
    private final Scanner scanner;
    private final Object modelLock = new Object();
    private final Renderer renderer;
    private volatile String activePrompt;

    public Tui(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model      = model;
        this.parser     = new CommandParser();
        this.scanner    = new Scanner(System.in);
        this.renderer   = new Renderer(model);

        model.addListener(() -> {
            renderer.render(model);
            if (activePrompt != null) {
                System.out.print(activePrompt);
            }
            synchronized (modelLock) {
                modelLock.notifyAll();
            }
        });
    }

    @Override
    public void run() {
        renderer.render(model);
        loginLoop();
        if (!model.isConnected()) {
            return;
        }

        waitForInitialStateSync();
        if (!model.isConnected()) {
            return;
        }

        Thread inputThread = new Thread(this::interactiveInputLoop);
        inputThread.setDaemon(true);
        inputThread.start();

        Thread timerThread = new Thread(this::suspensionTimerRenderLoop);
        timerThread.setDaemon(true);
        timerThread.start();

        waitForGameEnd();
        endGame();
    }

    // ── Login ────────────────────────────────────────────────────────────────

    private void loginLoop() {
        while (!model.isLoginSuccess() && model.isConnected()) {
            String nickname = readLineWithPrompt("Nickname: ").trim();
            if (nickname.isBlank()) continue;
            String previousError = model.getLastError();

            if (askReconnect()) {
                controller.send(new ReconnectMessage(nickname));
            } else {
                TotemColor color = askTotemColor();
                if (color == null) continue;
                controller.send(new LoginMessage(nickname, color));
            }
            waitForLoginOutcome(previousError);

            if (model.isLoginSuccess()) {
                model.setMyNickname(nickname);
            } else if (model.getLastError() != null) {
                System.out.println("[ERROR] " + model.getLastError());
            }
        }
    }

    private boolean askReconnect() {
        String answer = readLineWithPrompt("Do you want to reconnect to an existing match? [y/N]: ").trim();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    private TotemColor askTotemColor() {
        while (true) {
            String input = readLineWithPrompt("Totem Color: ").trim().toUpperCase();
            if (input.equalsIgnoreCase("quit")) return null;
            try {
                return TotemColor.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] Invalid color. Choose between: "
                        + java.util.Arrays.toString(TotemColor.values())
                        + " (or 'quit' to cancel).");
            }
        }
    }

    // ── Main input loop ──────────────────────────────────────────────────────

    private void interactiveInputLoop() {
        while (model.isConnected() && !model.isGameEnded()) {
            if (model.isGameSuspended()) {
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
                continue;
            }

            String line;
            try {
                line = readLineWithPrompt("> ").trim();
            } catch (Exception e) {
                return;
            }
            if (line.isBlank()) continue;

            String state = model.getGameState();
            if (state == null || "LobbyState".equals(state) || "ReadyCheckState".equals(state)) {
                handlePreGameCommand(line);
                continue;
            }

            if ("TotemPlacementState".equals(state)
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

            ClientMessage msg = parser.parse(line);
            if (msg instanceof DisconnectMessage) {
                controller.disconnect();
                return;
            }
            if (msg != null) controller.send(msg);
        }
    }

    private void suspensionTimerRenderLoop() {
        while (model.isConnected() && !model.isGameEnded()) {
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

    private void handlePreGameCommand(String line) {
        if (line.equalsIgnoreCase("help")) {
            printPreGameHelp();
            return;
        }

        // Lobby leader shortcut: typing just a number sets the player count
        if (model.isLobbyLeader()
                && model.getExpectedPlayers() < CommandParser.MIN_PLAYERS
                && line.matches("\\d+")) {
            ClientMessage shortcut = parser.parse("players " + line);
            if (shortcut != null) controller.send(shortcut);
            return;
        }

        // Reject "players" command from non-leaders before delegating to the parser
        if (line.toLowerCase().startsWith("players") && !model.isLobbyLeader()) {
            System.out.println("[ERROR] Only the lobby leader can choose the number of participants.");
            return;
        }

        ClientMessage msg = parser.parse(line);
        if (msg instanceof DisconnectMessage) {
            controller.disconnect();
            return;
        }
        if (msg != null) {
            controller.send(msg);
            return;
        }

        // Parser already printed an "Unknown command" hint; show context-aware help
        printPreGameHelp();
    }

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

    private void endGame() {
        if (!model.isConnected()) {
            System.out.println("\nConnection lost.");
            return;
        }
        System.out.println("\nGame ended. Press ENTER to quit.");
        readLineWithPrompt("");
        controller.disconnect();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void waitForModel() {
        synchronized (modelLock) {
            try { modelLock.wait(10_000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

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

    private void waitForGameEnd() {
        synchronized (modelLock) {
            while (model.isConnected() && !model.isGameEnded()) {
                try { modelLock.wait(1_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void waitForInitialStateSync() {
        synchronized (modelLock) {
            while (model.isConnected()
                    && model.getGameState() == null
                    && model.getLobbyPlayers().isEmpty()) {
                try { modelLock.wait(1_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

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

    private void printHelp() {
        System.out.println("  place <letter>         — place totem");
        System.out.println("  pick <id> upper|lower  — take character card");
        System.out.println("  build <id> upper|lower — take building card");
        System.out.println("  quit                   — disconnect");
        System.out.println("  help                   — show available commands");
    }
}