package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.controller.action.*;
import it.polimi.ingsw.network.protocol.serverbound.*;

public class CommandParser {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    /**
     * Supported commands:
     * <pre>
     *   ready
     *   unready
     *   players {@code <n>}           (2..5)
     *   place {@code <tileId>}        e.g. place A
     *   pick {@code <cardId>} upper|lower
     *   build {@code <cardId>} upper|lower
     *   quit
     * </pre>
     */
    public ClientMessage parse(String input) {
        if (input == null || input.isBlank()) return null;

        String[] parts = input.trim().split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "ready":
                if (parts.length != 1) {
                    System.out.println("Usage: ready");
                    return null;
                }
                return new ReadyMessage(true);

            case "unready":
                if (parts.length != 1) {
                    System.out.println("Usage: unready");
                    return null;
                }
                return new ReadyMessage(false);

            case "players":
                if (parts.length != 2) {
                    System.out.println("Usage: players <" + MIN_PLAYERS + "-" + MAX_PLAYERS + ">");
                    return null;
                }

                try {
                    int n = Integer.parseInt(parts[1]);
                    if (n < MIN_PLAYERS || n > MAX_PLAYERS) {
                        System.out.println("[ERROR] Player count must be between "
                                + MIN_PLAYERS + " and " + MAX_PLAYERS + ".");
                        return null;
                    }
                    return new SetPlayersCountMessage(n);
                } catch (NumberFormatException e) {
                    System.out.println("[ERROR] Invalid number.");
                    return null;
                }

            case "place":
                if (parts.length != 2 || parts[1].length() != 1) {
                    System.out.println("Usage: place <letter>   e.g. place A");
                    return null;
                }
                return new GameActionMessage(new PlaceTotemAction(parts[1].toUpperCase().charAt(0)));

            case "pick":
                if (parts.length != 3) {
                    System.out.println("Usage: pick <cardId> upper|lower");
                    return null;
                }

                if (!isValidRow(parts[2])) {
                    System.out.println("[ERROR] Row must be 'upper' or 'lower'.");
                    return null;
                }

                return new GameActionMessage(new PickCardAction(
                        parts[1],
                        isUpperRow(parts[2])
                ));

            case "build":
                if (parts.length != 3) {
                    System.out.println("Usage: build <cardId> upper|lower");
                    return null;
                }

                if (!isValidRow(parts[2])) {
                    System.out.println("[ERROR] Row must be 'upper' or 'lower'.");
                    return null;
                }

                return new GameActionMessage(new PickBuildingAction(
                        parts[1],
                        isUpperRow(parts[2])
                ));

            case "skip":
                if (parts.length != 1) {
                    System.out.println("Usage: skip");
                    return null;
                }
                return new GameActionMessage(new DebugSkipRoundAction());

            case "skipextra":
                if (parts.length != 1) {
                    System.out.println("Usage: skipextra");
                    return null;
                }
                return new GameActionMessage(new SkipExtraPickAction());

            case "endgame":
                if (parts.length != 1) {
                    System.out.println("Usage: endgame");
                    return null;
                }
                return new GameActionMessage(new DebugSkipToEndAction());

            case "quit":
                if (parts.length != 1) {
                    System.out.println("Usage: quit");
                    return null;
                }
                return new DisconnectMessage();

            default:
                System.out.println("Unknown command: " + cmd);
                return null;
        }
    }

    /**
     * Returns true only if the row argument is explicitly supported.
     */
    private boolean isValidRow(String row) {
        return row.equalsIgnoreCase("upper") || row.equalsIgnoreCase("lower");
    }

    /**
     * Converts a valid row argument into the boolean used by game actions.
     */
    private boolean isUpperRow(String row) {
        return row.equalsIgnoreCase("upper");
    }
}