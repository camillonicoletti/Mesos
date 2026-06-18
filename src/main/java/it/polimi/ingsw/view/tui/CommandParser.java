package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.network.client.ClientController;

public class CommandParser {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    private final ClientController controller;

    public CommandParser(ClientController controller) {
        this.controller = controller;
    }

    /**
     * Parses and runs one command line via the shared {@link ClientController}.
     * Returns {@code true} when the user asked to quit; {@code onUnknown} runs
     * when the command is not recognised.
     */
    public boolean parse(String input, Runnable onUnknown) {
        if (input == null || input.isBlank()) {
            return false;
        }

        String[] parts = input.trim().split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "ready":
                if (parts.length != 1) {
                    System.out.println("Usage: ready");
                    return false;
                }
                controller.setReady(true);
                return false;

            case "unready":
                if (parts.length != 1) {
                    System.out.println("Usage: unready");
                    return false;
                }
                controller.setReady(false);
                return false;

            case "players":
                if (parts.length != 2) {
                    System.out.println("Usage: players <" + MIN_PLAYERS + "-" + MAX_PLAYERS + ">");
                    return false;
                }

                try {
                    int n = Integer.parseInt(parts[1]);
                    if (n < MIN_PLAYERS || n > MAX_PLAYERS) {
                        System.out.println("[ERROR] Player count must be between "
                                + MIN_PLAYERS + " and " + MAX_PLAYERS + ".");
                        return false;
                    }
                    controller.setPlayersCount(n);
                } catch (NumberFormatException e) {
                    System.out.println("[ERROR] Invalid number.");
                }
                return false;

            case "place":
                if (parts.length != 2 || parts[1].length() != 1) {
                    System.out.println("Usage: place <letter>   e.g. place A");
                    return false;
                }
                controller.placeTotem(parts[1].toUpperCase().charAt(0));
                return false;

            case "pick":
                if (parts.length != 3) {
                    System.out.println("Usage: pick <cardId> upper|lower");
                    return false;
                }
                if (!isValidRow(parts[2])) {
                    System.out.println("[ERROR] Row must be 'upper' or 'lower'.");
                    return false;
                }
                controller.pickCharacterCard(parts[1], isUpperRow(parts[2]));
                return false;

            case "build":
                if (parts.length != 3) {
                    System.out.println("Usage: build <cardId> upper|lower");
                    return false;
                }
                if (!isValidRow(parts[2])) {
                    System.out.println("[ERROR] Row must be 'upper' or 'lower'.");
                    return false;
                }
                controller.pickBuildingCard(parts[1], isUpperRow(parts[2]));
                return false;

            case "skip":
                if (parts.length != 1) {
                    System.out.println("Usage: skip");
                    return false;
                }
                controller.skipRound();
                return false;

            case "skipextra":
                if (parts.length != 1) {
                    System.out.println("Usage: skipextra");
                    return false;
                }
                controller.skipExtraPick();
                return false;

            case "endgame":
                if (parts.length != 1) {
                    System.out.println("Usage: endgame");
                    return false;
                }
                controller.skipToEnd();
                return false;

            case "quit":
                if (parts.length != 1) {
                    System.out.println("Usage: quit");
                    return false;
                }
                return true;

            default:
                System.out.println("Unknown command: " + cmd);
                onUnknown.run();
                return false;
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
