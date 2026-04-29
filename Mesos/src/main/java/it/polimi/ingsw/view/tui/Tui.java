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

    public Tui(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model      = model;
        this.parser     = new CommandParser();
        this.scanner    = new Scanner(System.in);
        this.renderer   = new Renderer(model);

        model.addListener(() -> {
            renderer.render(model);
            synchronized (modelLock) {
                modelLock.notifyAll();
            }
        });
    }

    @Override
    public void run() {
        renderer.render(model);
        loginLoop();
        waitForLobbyUpdate();

        if (isFirstPlayer()) {
            askPlayerCount();
        }

        waitForReadyCheck();
        readyLoop();

        Thread inputThread = new Thread(this::gameInputLoop);
        inputThread.setDaemon(true);
        inputThread.start();

        waitForGameEnd();
        endGame();
    }

    // ── Login ────────────────────────────────────────────────────────────────

    private void loginLoop() {
        while (!model.isLoginSuccess()) {
            System.out.print("Nickname: ");
            String nickname = scanner.nextLine().trim();
            if (nickname.isBlank()) continue;

            TotemColor color = askTotemColor();
            if (color == null) continue;

            controller.send(new LoginMessage(nickname, color));
            waitForModel();

            if (model.isLoginSuccess()) {
                model.setMyNickname(nickname);   // ← salva il proprio nickname
            } else if (model.getLastError() != null) {
                System.out.println("[ERRORE] " + model.getLastError());
            }
        }
    }

    private TotemColor askTotemColor() {
        System.out.println("Scegli un colore tra: RED / BLUE / PURPLE / YELLOW / WHITE");
        System.out.print("Colore: ");
        try {
            return TotemColor.valueOf(scanner.nextLine().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("[ERRORE] Colore non valido.");
            return null;
        }
    }

    // ── Numero giocatori (solo primo) ────────────────────────────────────────

    private boolean isFirstPlayer() {
        return model.getLobbyPlayers().size() == 1
                && model.getExpectedPlayers() < 2;
    }

    private void askPlayerCount() {
        System.out.println("Sei il primo giocatore — sei tu a decidere quanti giocatori parteciperanno.");
        while (model.getExpectedPlayers() < 2) {
            System.out.print("Numero di giocatori (min 2, max 5): ");
            String input = scanner.nextLine().trim();
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

    // ── Attesa lobby piena ───────────────────────────────────────────────────

    private void waitForReadyCheck() {
        synchronized (modelLock) {
            while (!"ReadyCheckState".equals(model.getGameState())) {
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
        while (!isGameStarted()) {
            if (!localReady) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();

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
        while (!isGameEnded()) {
            if (model.isGameSuspended()) {
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
                continue;
            }

            System.out.print("> ");
            String line;
            try {
                line = scanner.nextLine().trim();
            } catch (Exception e) {
                return;
            }
            if (line.isBlank()) continue;

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
        System.out.println("\nPartita terminata. Premi INVIO per uscire.");
        scanner.nextLine();
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
            while (model.getLobbyPlayers().isEmpty()) {
                try { modelLock.wait(5_000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void waitForGameEnd() {
        synchronized (modelLock) {
            while (!isGameEnded()) {
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

    private boolean isGameEnded() {
        return "EndGameState".equals(model.getGameState());
    }

    private void printHelp() {
        System.out.println("  place <lettera>        — posiziona il totem");
        System.out.println("  pick <id> upper|lower  — prendi carta personaggio");
        System.out.println("  build <id> upper|lower — prendi edificio");
        System.out.println("  quit                   — disconnettiti");
        System.out.println("  help                   — questo messaggio");
    }
}