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
        suspended = connectedPlayersCount <= 1;

        // The forced-win countdown is decoupled from the suspended flag on purpose:
        // it only makes sense with EXACTLY one connected player. With zero connected
        // (e.g. right after a restore from persistence, before anyone has reconnected)
        // nobody can win the timeout, so the clock must not run yet. Anchor the deadline
        // the moment the count first reaches one, and clear it whenever it isn't one —
        // this also invalidates any stale absolute timestamp restored from disk (the
        // secondary case where the savestate was written while already suspended).
        boolean countdownShouldRun = connectedPlayersCount == 1;
        boolean countdownRunning = suspensionStartedAtMillis >= 0L;
        if (countdownShouldRun && !countdownRunning) {
            suspensionStartedAtMillis = nowMillis;
        } else if (!countdownShouldRun) {
            suspensionStartedAtMillis = -1L;
        }
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
