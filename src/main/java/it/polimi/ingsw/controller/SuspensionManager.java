package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.SuspendedState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;

import java.io.Serializable;

/** Tracks suspension timing and forced-winner logic when too few players remain connected. */
public class SuspensionManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean suspended;
    private GameState suspendedStateReturn;
    private long suspensionStartedAtMillis;
    private long suspensionTimeoutMillis;
    private String forcedWinnerNickname;

    public SuspensionManager(long defaultTimeoutMillis) {
        this.suspended = false;
        this.suspendedStateReturn = null;
        this.suspensionStartedAtMillis = -1L;
        this.suspensionTimeoutMillis = defaultTimeoutMillis;
        this.forcedWinnerNickname = null;
    }

    public void resetForGame(Game game) {
        suspended = false;
        suspendedStateReturn = null;
        suspensionStartedAtMillis = -1L;
        forcedWinnerNickname = null;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public GameState getSuspendedStateReturn() {
        return suspendedStateReturn;
    }

    public void updateStatus(Game game, int connectedPlayersCount, long nowMillis) {
        if (game == null || game.isGameOver()) {
            suspended = false;
            suspensionStartedAtMillis = -1L;
            return;
        }

        // Suspend when ≤1 player is connected: the game cannot continue with fewer than 2 active players.
        boolean shouldSuspend = connectedPlayersCount <= 1;
        if (!suspended && shouldSuspend) {
            suspensionStartedAtMillis = nowMillis;
        } else if (suspended && !shouldSuspend) {
            suspensionStartedAtMillis = -1L;
        }

        suspended = shouldSuspend;
    }

    public long getSuspensionTimeoutMillis() {
        return suspensionTimeoutMillis;
    }

    public void setSuspensionTimeoutMillis(long suspensionTimeoutMillis) {
        if (suspensionTimeoutMillis <= 0) {
            throw new IllegalArgumentException("Suspension timeout must be positive");
        }

        this.suspensionTimeoutMillis = suspensionTimeoutMillis;
    }

    public long getSuspensionStartedAtMillis() {
        return suspensionStartedAtMillis;
    }

    public long getSuspensionRemainingMillis(long nowMillis) {
        if (!suspended || suspensionStartedAtMillis < 0L) {
            return -1L;
        }

        long elapsedMillis = Math.max(0L, nowMillis - suspensionStartedAtMillis);
        return Math.max(0L, suspensionTimeoutMillis - elapsedMillis);
    }

    public boolean checkTimeout(Game game, int connectedPlayersCount, Player onlyConnectedPlayer, long nowMillis) {
        if (!suspended || game == null || game.isGameOver()) {
            return false;
        }

        if (suspensionStartedAtMillis < 0L) {
            return false;
        }

        // Timeout only applies when exactly one player is connected; with zero the game is already over.
        if (connectedPlayersCount != 1) {
            return false;
        }

        if (nowMillis - suspensionStartedAtMillis < suspensionTimeoutMillis) {
            return false;
        }

        if (onlyConnectedPlayer == null) {
            return false;
        }

        forcedWinnerNickname = onlyConnectedPlayer.getNickname();
        suspended = false;
        suspendedStateReturn = null;
        suspensionStartedAtMillis = -1L;
        return true;
    }

    public Player consumeForcedWinner(Game game) {
        if (forcedWinnerNickname == null || game == null) {
            return null;
        }

        Player forcedWinner = game.getPlayers().stream()
                .filter(player -> player.getNickname().equals(forcedWinnerNickname))
                .findFirst()
                .orElse(null);
        // Clear so the forced winner can only be consumed once.
        forcedWinnerNickname = null;
        return forcedWinner;
    }

    public SuspendedState enter(Game game, GameState currentState) {
        if (currentState instanceof SuspendedState || game == null || game.isGameOver()) {
            return null;
        }

        suspendedStateReturn = currentState;
        return new SuspendedState(suspendedStateReturn);
    }

    public GameState resume(GameState currentState) {
        if (!(currentState instanceof SuspendedState) || suspendedStateReturn == null) {
            return null;
        }

        GameState stateToResume = suspendedStateReturn;
        suspendedStateReturn = null;
        return stateToResume;
    }

    public void ensureValidTimeout(long defaultTimeoutMillis) {
        if (suspensionTimeoutMillis <= 0L) {
            suspensionTimeoutMillis = defaultTimeoutMillis;
        }
    }
}
