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

        if (isInPreGame()) {
            runPreGameFlow();
            if (!model.isConnected()) {
                return;
            }
        }

        Thread inputThread = new Thread(this::gameInputLoop);
        inputThread.setDaemon(true);
        inputThread.start();

        waitForGameEnd();
        endGame();
    }

    // ── Login ────────────────────────────────────────────────────────────────

    private void loginLoop() {
        while (!model.isLoginSuccess() && model.isConnected()) {
            String nickname = readLineWithPrompt("Nickname: ").trim();
            if (nickname.isBlank()) continue;

            if (askReconnect()) {
                controller.send(new ReconnectMessage(nickname));
            } else {
                TotemColor color = askTotemColor();
                if (color == null) continue;
                controller.send(new LoginMessage(nickname, color));
            }
            waitForModel();

            if (model.isLoginSuccess()) {
                model.setMyNickname(nickname);
            } else if (model.getLastError() != null) {
                System.out.println("[ERRORE] " + model.getLastError());
            }
        }
    }

    private boolean askReconnect() {
        String answer = readLineWithPrompt("Vuoi riconnetterti a una partita esistente? [y/N]: ").trim();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes") || answer.equalsIgnoreCase("s");
    }

    private TotemColor askTotemColor() {
        System.out.println("Scegli un colore tra: RED / BLUE / PURPLE / YELLOW / WHITE");
        try {
            return TotemColor.valueOf(readLineWithPrompt("Colore: ").trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("[ERRORE] Colore non valido.");
            return null;
        }
    }

    // ── Numero giocatori (solo primo) ────────────────────────────────────────

    private boolean isFirstPlayer() {
        return model.getExpectedPlayers() < 2
                && model.getMyNickname() != null
                && !model.getLobbyPlayers().isEmpty()
                && model.getMyNickname().equals(model.getLobbyPlayers().get(0));
    }

    private void askPlayerCount() {
        System.out.println("Sei il primo giocatore — sei tu a decidere quanti giocatori parteciperanno.");
        while (model.getExpectedPlayers() < 2) {
            String input = readLineWithPrompt("Numero di giocatori (min 2, max 5): ").trim();
            try {
                int n = Integer.parseInt(input);
                if (n < 2 || n > 5) {
                    System.out.println("[ERRORE] Il numero deve essere tra 2 e 5.");
                    continue;
                }
                controller.send(new SetPlayersCountMessage(n));
                waitForModel();
            } catch (NumberFormatException e) {
                System.out.println("[ERRORE] Inserisci un numero valido.");
            }
        }
        System.out.println("Perfetto! In attesa degli altri " + (model.getExpectedPlayers() - 1) + " giocatori...");
    }

    private void runPreGameFlow() {
        waitForLobbyUpdate();
        while (model.isConnected() && isInPreGame()) {
            String state = model.getGameState();

            if ("LobbyState".equals(state)) {
                if (model.getExpectedPlayers() < 2) {
                    if (isFirstPlayer()) {
                        askPlayerCount();
                    } else {
                        waitForModel();
                    }
                } else {
                    waitForReadyCheck();
                }
            } else if ("ReadyCheckState".equals(state)) {
                readyLoop();
            } else {
                waitForModel();
            }
        }
    }

    // ── Attesa lobby piena ───────────────────────────────────────────────────

    private void waitForReadyCheck() {
        synchronized (modelLock) {
            while (model.isConnected() && !"ReadyCheckState".equals(model.getGameState())) {
                try { modelLock.wait(1_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    // ── Ready ────────────────────────────────────────────────────────────────

    private void readyLoop() {
        System.out.println("La lobby è al completo!");
        System.out.println("Digita  ready  per confermare la tua presenza.");

        boolean localReady = false;
        while (model.isConnected() && !isGameStarted()) {
            if (!localReady) {
                String line = readLineWithPrompt("> ").trim();

                if (line.equalsIgnoreCase("ready")) {
                    controller.send(new ReadyMessage(true));
                    localReady = true;
                    System.out.println("Hai confermato! In attesa degli altri giocatori...");
                } else if (line.equalsIgnoreCase("unready")) {
                    System.out.println("Non hai ancora confermato.");
                } else {
                    System.out.println("Digita  ready  per confermare.");
                }
            } else {
                // ho già confermato: aspetto in silenzio gli altri
                synchronized (modelLock) {
                    try { modelLock.wait(1_000); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
            }
        }
    }
    // ── Game ─────────────────────────────────────────────────────────────────

    private void gameInputLoop() {
        while (model.isConnected() && !isGameEnded()) {
            if (model.isGameSuspended()) {
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
                continue;
            }

            System.out.print("> ");
            String line;
            try {
                line = readLineWithPrompt("> ").trim();
            } catch (Exception e) {
                return;
            }
            if (line.isBlank()) continue;

            if ("TotemPlacementState".equals(model.getGameState())
                    && line.toLowerCase().startsWith("place")
                    && !model.isMyTotemPlacementTurn()) {
                String currentPlayer = model.getCurrentTotemPlacementPlayer();
                if (currentPlayer != null) {
                    System.out.println("[ERRORE] Non è il tuo turno: deve piazzare " + currentPlayer + ".");
                } else {
                    System.out.println("[ERRORE] Nessun giocatore disponibile per il piazzamento in questo momento.");
                }
                continue;
            }

            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            ClientMessage msg = parser.parse(line);
            if (msg != null) controller.send(msg);
        }
    }

    // ── End ──────────────────────────────────────────────────────────────────

    private void endGame() {
        if (!model.isConnected()) {
            System.out.println("\nConnessione chiusa.");
            return;
        }
        System.out.println("\nPartita terminata. Premi INVIO per uscire.");
        readLineWithPrompt("");
        controller.send(new DisconnectMessage());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void waitForModel() {
        synchronized (modelLock) {
            try { modelLock.wait(10_000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private void waitForLobbyUpdate() {
        synchronized (modelLock) {
            while (model.isConnected() && model.getLobbyPlayers().isEmpty()) {
                try { modelLock.wait(5_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void waitForGameEnd() {
        synchronized (modelLock) {
            while (model.isConnected() && !isGameEnded()) {
                try { modelLock.wait(1_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private boolean isGameStarted() {
        String s = model.getGameState();
        return s != null
                && !s.equals("LobbyState")
                && !s.equals("ReadyCheckState");
    }

    private boolean isInPreGame() {
        String state = model.getGameState();
        return state == null || "LobbyState".equals(state) || "ReadyCheckState".equals(state);
    }

    private boolean isGameEnded() {
        return "EndGameState".equals(model.getGameState());
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
        System.out.println("  place <lettera>        — posiziona il totem");
        System.out.println("  pick <id> upper|lower  — prendi carta personaggio");
        System.out.println("  build <id> upper|lower — prendi edificio");
        System.out.println("  quit                   — disconnettiti");
        System.out.println("  help                   — questo messaggio");
    }
}
