package it.polimi.ingsw.client.tui;

import it.polimi.ingsw.controller.action.*;
import it.polimi.ingsw.network.protocol.serverbound.*;

public class CommandParser {

    /**
     * Comandi supportati:
     *   ready
     *   players <n>
     *   place <tileId>         es. place A
     *   pick <cardId> upper|lower
     *   build <cardId> upper|lower
     *   quit
     */
    public ClientMessage parse(String input) {
        if (input == null || input.isBlank()) return null;

        String[] parts = input.trim().split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "ready":
                return new ReadyMessage();

            case "players":
                if (parts.length < 2) {
                    System.out.println("Uso: players <numero>");
                    return null;
                }
                try {
                    int n = Integer.parseInt(parts[1]);
                    return new SetPlayersCountMessage(n);
                } catch (NumberFormatException e) {
                    System.out.println("Numero non valido.");
                    return null;
                }

            case "place":
                if (parts.length < 2 || parts[1].length() != 1) {
                    System.out.println("Uso: place <lettera>   es. place A");
                    return null;
                }
                return new GameActionMessage(new PlaceTotemAction(parts[1].toUpperCase().charAt(0)));

            case "pick":
                if (parts.length < 3) {
                    System.out.println("Uso: pick <cardId> upper|lower");
                    return null;
                }
                return new GameActionMessage(new PickCardAction(
                        parts[1], parts[2].equalsIgnoreCase("upper")));

            case "build":
                if (parts.length < 3) {
                    System.out.println("Uso: build <cardId> upper|lower");
                    return null;
                }
                return new GameActionMessage(new PickBuildingAction(
                        parts[1], parts[2].equalsIgnoreCase("upper")));

            case "quit":
                return new DisconnectMessage();

            default:
                System.out.println("Comando non riconosciuto: " + cmd);
                return null;
        }
    }
}