package it.polimi.ingsw.network.client.tui;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.serverbound.*;

import java.util.Scanner;

public class Tui {

    private final ClientController controller;
    private final ClientModel model;
    private final CommandParser parser;
    private final Scanner scanner;
    private final Object responseLock = new Object();

    public Tui(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model      = model;
        this.parser     = new CommandParser();
        this.scanner    = new Scanner(System.in);

        Renderer renderer = new Renderer(model);
        model.addListener(() -> {
            renderer.render(model);
            synchronized (responseLock) {
                responseLock.notifyAll();
            }
        });
    }

    public void run() {
        loginLoop();
        lobbyLoop();
        gameLoop();
        endGame();
    }

    // -------------------------------------------------------
    // FASE 1 — Login
    // -------------------------------------------------------

    private void loginLoop() {
        while (!model.isLoginSuccess()) {
            System.out.print("Nickname: ");
            String nickname = scanner.nextLine().trim();

            TotemColor color = askTotemColor();
            if (color == null) continue;

            controller.send(new LoginMessage(nickname, color));
            waitForModelChange();

            if (!model.isLoginSuccess() && model.getLastError() != null) {
                System.out.println("[ERRORE] " + model.getLastError());
            }
        }
        System.out.println("Login effettuato. In attesa della lobby...");
    }

    private TotemColor askTotemColor() {
        System.out.print("Colore totem (RED / BLUE / PURPLE / YELLOW / WHITE): ");
        String input = scanner.nextLine().trim().toUpperCase();
        try {
            return TotemColor.valueOf(input);
        } catch (IllegalArgumentException e) {
            System.out.println("Colore non valido. Scegli tra: RED, BLUE, PURPLE, YELLOW, WHITE");
            return null;
        }
    }

    // -------------------------------------------------------
    // FASE 2 — Lobby
    // -------------------------------------------------------

    private void lobbyLoop() {
        System.out.println("Digita 'players <n>' per impostare il numero di giocatori (solo il primo).");
        System.out.println("Digita 'ready' quando sei pronto.");

        while (!isGameStarted()) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;

            ClientMessage msg = parser.parse(line);
            if (msg != null) {
                controller.send(msg);
                waitForModelChange();
            }
        }
    }

    // -------------------------------------------------------
    // FASE 3 — Partita
    // -------------------------------------------------------

    private void gameLoop() {
        System.out.println("\nLa partita è iniziata!");
        printHelp();

        while (!isGameEnded()) {
            if (model.isGameSuspended()) {
                String disconnected = model.getLastDisconnectedPlayer();
                if (disconnected != null) {
                    System.out.println("[PAUSA] " + disconnected + " si è disconnesso. In attesa...");
                } else {
                    System.out.println("[PAUSA] Partita sospesa. In attesa degli altri giocatori...");
                }
                waitForModelChange();
                continue;
            }

            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;

            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            ClientMessage msg = parser.parse(line);
            if (msg != null) controller.send(msg);
        }
    }

    // -------------------------------------------------------
    // FASE 4 — Fine partita
    // -------------------------------------------------------

    private void endGame() {
        System.out.println("\nPartita terminata. Premi INVIO per uscire.");
        scanner.nextLine();
        controller.send(new DisconnectMessage());
    }

    // -------------------------------------------------------
    // HELPER
    // -------------------------------------------------------

    private void waitForModelChange() {
        synchronized (responseLock) {
            try {
                responseLock.wait(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isGameStarted() {
        String state = model.getGameState();
        return state != null
                && !state.equals("LobbyState")
                && !state.equals("ReadyCheckState");
    }

    private boolean isGameEnded() {
        return "EndGameState".equals(model.getGameState());
    }

    private void printHelp() {
        System.out.println("Comandi disponibili:");
        System.out.println("  place <lettera>        — posiziona il totem  (es. place A)");
        System.out.println("  pick <id> upper|lower  — prendi carta personaggio");
        System.out.println("  build <id> upper|lower — prendi edificio");
        System.out.println("  quit                   — disconnettiti");
        System.out.println("  help                   — mostra questo messaggio");
    }
}