package it.polimi.ingsw.network.client;

import java.util.Arrays;

public enum GamePhase {
    LOBBY("LobbyState", "LOBBY", "Lobby", true, false, false),
    READY_CHECK("ReadyCheckState", "READY CHECK", "Ready check", true, false, false),
    START_GAME("StartGameState", "START", "Start", false, true, false),
    TOTEM_PLACEMENT("TotemPlacementState", "TOTEM PLACEMENT", "Totem placement", false, true, true),
    ACTION_RESOLUTION("ActionResolutionState", "ACTION RESOLUTION", "Action resolution", false, true, true),
    EVENT_RESOLUTION("EventResolutionState", "EVENT RESOLUTION", "Event resolution", false, false, false),
    END_ROUND("EndRoundState", "END OF ROUND", "End of round", false, false, false),
    SUSPENDED("SuspendedState", "PAUSED", "Paused", false, true, false),
    END_GAME("EndGameState", "GAME OVER", "Game over", false, false, false),
    UNKNOWN(null, null, null, false, false, false);

    private final String stateName;
    private final String displayLabel;
    private final String titleLabel;
    private final boolean preGame;
    private final boolean boardVisible;
    private final boolean turnManaged;

    GamePhase(String stateName, String displayLabel, String titleLabel,
              boolean preGame, boolean boardVisible, boolean turnManaged) {
        this.stateName = stateName;
        this.displayLabel = displayLabel;
        this.titleLabel = titleLabel;
        this.preGame = preGame;
        this.boardVisible = boardVisible;
        this.turnManaged = turnManaged;
    }

    public String stateName() {
        return stateName;
    }

    public String displayLabel() {
        return displayLabel;
    }

    public String titleLabel() {
        return titleLabel;
    }

    public boolean isPreGame() {
        return preGame;
    }

    public boolean hasBoardVisible() {
        return boardVisible;
    }

    public boolean managesCurrentTurn() {
        return turnManaged;
    }

    public boolean isTotemPlacement() {
        return this == TOTEM_PLACEMENT;
    }

    public boolean isActionResolution() {
        return this == ACTION_RESOLUTION;
    }

    public boolean isEventResolution() {
        return this == EVENT_RESOLUTION;
    }

    public boolean isEndRound() {
        return this == END_ROUND;
    }

    public boolean isEndGame() {
        return this == END_GAME;
    }

    public static GamePhase fromStateName(String stateName) {
        if (stateName == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(phase -> stateName.equals(phase.stateName))
                .findFirst()
                .orElse(UNKNOWN);
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
}
