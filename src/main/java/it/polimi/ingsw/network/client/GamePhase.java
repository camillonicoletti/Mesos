package it.polimi.ingsw.network.client;

/**
 * Client-side view of the match lifecycle.
 *
 * <p>Modelled with the <em>State</em> pattern: each phase is its own class that
 * answers the lifecycle questions polymorphically, so callers never branch on a
 * type code. The string {@code stateName} is the server state class name carried
 * by the protocol; {@link #fromStateName(String)} resolves it to the matching
 * singleton, falling back to {@link #UNKNOWN} for anything unrecognised.</p>
 */
public abstract sealed class GamePhase {

    public static final GamePhase LOBBY = new Lobby();
    public static final GamePhase READY_CHECK = new ReadyCheck();
    public static final GamePhase START_GAME = new StartGame();
    public static final GamePhase TOTEM_PLACEMENT = new TotemPlacement();
    public static final GamePhase ACTION_RESOLUTION = new ActionResolution();
    public static final GamePhase EVENT_RESOLUTION = new EventResolution();
    public static final GamePhase END_ROUND = new EndRound();
    public static final GamePhase SUSPENDED = new Suspended();
    public static final GamePhase END_GAME = new EndGame();
    public static final GamePhase UNKNOWN = new Unknown();

    /** Every resolvable phase, in lifecycle order (UNKNOWN is the fallback only). */
    private static final GamePhase[] KNOWN = {
            LOBBY, READY_CHECK, START_GAME, TOTEM_PLACEMENT, ACTION_RESOLUTION,
            EVENT_RESOLUTION, END_ROUND, SUSPENDED, END_GAME
    };

    /** Name of the server state class this phase mirrors, {@code null} for {@link #UNKNOWN}. */
    public abstract String stateName();

    /** Short banner label, {@code null} for {@link #UNKNOWN}. */
    public abstract String displayLabel();

    /** Title-bar label, {@code null} for {@link #UNKNOWN}. */
    public abstract String titleLabel();

    public boolean isPreGame() {
        return false;
    }

    public boolean hasBoardVisible() {
        return false;
    }

    public boolean managesCurrentTurn() {
        return false;
    }

    public boolean isTotemPlacement() {
        return false;
    }

    public boolean isActionResolution() {
        return false;
    }

    public boolean isEventResolution() {
        return false;
    }

    public boolean isEndRound() {
        return false;
    }

    public boolean isEndGame() {
        return false;
    }

    public static GamePhase fromStateName(String stateName) {
        if (stateName == null) {
            return UNKNOWN;
        }
        for (GamePhase phase : KNOWN) {
            if (stateName.equals(phase.stateName())) {
                return phase;
            }
        }
        return UNKNOWN;
    }

    public static boolean isPreGameState(String stateName) {
        return stateName == null || fromStateName(stateName).isPreGame();
    }

    public static boolean hasGameStarted(String stateName) {
        return stateName != null && !fromStateName(stateName).isPreGame();
    }

    public static String displayLabelFor(String stateName) {
        GamePhase phase = fromStateName(stateName);
        return phase == UNKNOWN ? stateName : phase.displayLabel();
    }

    public static String titleLabelFor(String stateName, String nullLabel) {
        if (stateName == null) {
            return nullLabel;
        }
        GamePhase phase = fromStateName(stateName);
        return phase == UNKNOWN ? stateName : phase.titleLabel();
    }

    public static final class Lobby extends GamePhase {
        private Lobby() {}
        @Override public String stateName() { return "LobbyState"; }
        @Override public String displayLabel() { return "LOBBY"; }
        @Override public String titleLabel() { return "Lobby"; }
        @Override public boolean isPreGame() { return true; }
    }

    public static final class ReadyCheck extends GamePhase {
        private ReadyCheck() {}
        @Override public String stateName() { return "ReadyCheckState"; }
        @Override public String displayLabel() { return "READY CHECK"; }
        @Override public String titleLabel() { return "Ready check"; }
        @Override public boolean isPreGame() { return true; }
    }

    public static final class StartGame extends GamePhase {
        private StartGame() {}
        @Override public String stateName() { return "StartGameState"; }
        @Override public String displayLabel() { return "START"; }
        @Override public String titleLabel() { return "Start"; }
        @Override public boolean hasBoardVisible() { return true; }
    }

    public static final class TotemPlacement extends GamePhase {
        private TotemPlacement() {}
        @Override public String stateName() { return "TotemPlacementState"; }
        @Override public String displayLabel() { return "TOTEM PLACEMENT"; }
        @Override public String titleLabel() { return "Totem placement"; }
        @Override public boolean hasBoardVisible() { return true; }
        @Override public boolean managesCurrentTurn() { return true; }
        @Override public boolean isTotemPlacement() { return true; }
    }

    public static final class ActionResolution extends GamePhase {
        private ActionResolution() {}
        @Override public String stateName() { return "ActionResolutionState"; }
        @Override public String displayLabel() { return "ACTION RESOLUTION"; }
        @Override public String titleLabel() { return "Action resolution"; }
        @Override public boolean hasBoardVisible() { return true; }
        @Override public boolean managesCurrentTurn() { return true; }
        @Override public boolean isActionResolution() { return true; }
    }

    public static final class EventResolution extends GamePhase {
        private EventResolution() {}
        @Override public String stateName() { return "EventResolutionState"; }
        @Override public String displayLabel() { return "EVENT RESOLUTION"; }
        @Override public String titleLabel() { return "Event resolution"; }
        @Override public boolean isEventResolution() { return true; }
    }

    public static final class EndRound extends GamePhase {
        private EndRound() {}
        @Override public String stateName() { return "EndRoundState"; }
        @Override public String displayLabel() { return "END OF ROUND"; }
        @Override public String titleLabel() { return "End of round"; }
        @Override public boolean isEndRound() { return true; }
    }

    public static final class Suspended extends GamePhase {
        private Suspended() {}
        @Override public String stateName() { return "SuspendedState"; }
        @Override public String displayLabel() { return "PAUSED"; }
        @Override public String titleLabel() { return "Paused"; }
        @Override public boolean hasBoardVisible() { return true; }
    }

    public static final class EndGame extends GamePhase {
        private EndGame() {}
        @Override public String stateName() { return "EndGameState"; }
        @Override public String displayLabel() { return "GAME OVER"; }
        @Override public String titleLabel() { return "Game over"; }
        @Override public boolean isEndGame() { return true; }
    }

    public static final class Unknown extends GamePhase {
        private Unknown() {}
        @Override public String stateName() { return null; }
        @Override public String displayLabel() { return null; }
        @Override public String titleLabel() { return null; }
    }
}
