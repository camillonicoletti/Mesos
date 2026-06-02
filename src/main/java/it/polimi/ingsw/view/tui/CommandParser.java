package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.network.client.ClientController;

public class CommandParser {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;

    /**
     * Outcome of parsing one command line.
     * <ul>
     *   <li>{@code HANDLED} — a valid command was executed (or a usage hint was
     *       printed for a malformed one); the caller has nothing else to do.</li>
     *   <li>{@code QUIT} — the user asked to leave; the caller should disconnect
     *       and stop reading input.</li>
     *   <li>{@code UNKNOWN} — the command was not recognised.</li>
     * </ul>
     */
    public enum Result { HANDLED, QUIT, UNKNOWN }

    private final ClientController controller;

    public CommandParser(ClientController controller) {
        this.controller = controller;
    }

    /**
     * Parses one command line and executes the matching action through the
     * shared {@link ClientController} façade — the same methods the GUI buttons
     * call, so the message construction lives in one place.
     *
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
    public Result parse(String input) {
        if (input == null || input.isBlank()) {
            return Result.UNKNOWN;
        }

        String[] parts = input.trim().split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "ready":
                if (parts.length != 1) {
                    System.out.println("Usage: ready");
                    return Result.HANDLED;
                }
                controller.setReady(true);
                return Result.HANDLED;

            case "unready":
                if (parts.length != 1) {
                    System.out.println("Usage: unready");
                    return Result.HANDLED;
                }
                controller.setReady(false);
                return Result.HANDLED;

            case "players":
                if (parts.length != 2) {
                    System.out.println("Usage: players <" + MIN_PLAYERS + "-" + MAX_PLAYERS + ">");
                    return Result.HANDLED;
                }

                try {
                    int n = Integer.parseInt(parts[1]);
                    if (n < MIN_PLAYERS || n > MAX_PLAYERS) {
                        System.out.println("[ERROR] Player count must be between "
                                + MIN_PLAYERS + " and " + MAX_PLAYERS + ".");
                        return Result.HANDLED;
                    }
                    controller.setPlayersCount(n);
                } catch (NumberFormatException e) {
                    System.out.println("[ERROR] Invalid number.");
                }
                return Result.HANDLED;

            case "place":
                if (parts.length != 2 || parts[1].length() != 1) {
                    System.out.println("Usage: place <letter>   e.g. place A");
                    return Result.HANDLED;
                }
                controller.placeTotem(parts[1].toUpperCase().charAt(0));
                return Result.HANDLED;

            case "pick":
                if (parts.length != 3) {
                    System.out.println("Usage: pick <cardId> upper|lower");
                    return Result.HANDLED;
                }
                if (!isValidRow(parts[2])) {
                    System.out.println("[ERROR] Row must be 'upper' or 'lower'.");
                    return Result.HANDLED;
                }
                controller.pickCharacterCard(parts[1], isUpperRow(parts[2]));
                return Result.HANDLED;

            case "build":
                if (parts.length != 3) {
                    System.out.println("Usage: build <cardId> upper|lower");
                    return Result.HANDLED;
                }
                if (!isValidRow(parts[2])) {
                    System.out.println("[ERROR] Row must be 'upper' or 'lower'.");
                    return Result.HANDLED;
                }
                controller.pickBuildingCard(parts[1], isUpperRow(parts[2]));
                return Result.HANDLED;

            case "skip":
                if (parts.length != 1) {
                    System.out.println("Usage: skip");
                    return Result.HANDLED;
                }
                controller.skipRound();
                return Result.HANDLED;

            case "skipextra":
                if (parts.length != 1) {
                    System.out.println("Usage: skipextra");
                    return Result.HANDLED;
                }
                controller.skipExtraPick();
                return Result.HANDLED;

            case "endgame":
                if (parts.length != 1) {
                    System.out.println("Usage: endgame");
                    return Result.HANDLED;
                }
                controller.skipToEnd();
                return Result.HANDLED;

            case "quit":
                if (parts.length != 1) {
                    System.out.println("Usage: quit");
                    return Result.HANDLED;
                }
                return Result.QUIT;

            default:
                System.out.println("Unknown command: " + cmd);
                return Result.UNKNOWN;
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
