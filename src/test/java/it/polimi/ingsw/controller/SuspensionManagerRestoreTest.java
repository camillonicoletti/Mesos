package it.polimi.ingsw.controller;

import it.polimi.ingsw.model.Game;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Targeted tests for the forced-win countdown anchoring in SuspensionManager,
 * focused on the persistence-restore scenario (resilience + persistence interaction).
 *
 * SuspensionManager.updateStatus treats two concepts separately: the match
 * being suspended (at most 1 connected player, paused, overlay shown) and
 * the forced-win countdown actually running (only with exactly one
 * connected player). The countdown must not run while zero players are
 * connected — the window right after a restore from disk, before anyone has
 * reconnected — and any stale absolute timestamp restored from a savestate
 * that was written while already suspended must be invalidated.
 *
 * All timing is injected through the explicit nowMillis parameters so
 * the tests are fully deterministic and never touch the wall clock.
 */
class SuspensionManagerRestoreTest {

    private static final long TIMEOUT_MILLIS = 60_000L;

    /**
     * Builds a minimal in-progress game. updateStatus only inspects
     * Game.isGameOver() (false by default), so the empty player list and
     * null collaborators are irrelevant to what we exercise here.
     */
    private static Game inProgressGame() {
        return new Game(new ArrayList<>(), 1, null, null, LocalDateTime.now(),
                null, null, null, null, null);
    }

    /**
     * Case 1 — restore with 0 connected players (all marked disconnected by
     * markAllPlayersDisconnected). The match is suspended (overlay will
     * show "waiting"), but the forced-win countdown must NOT be running yet:
     * with nobody connected there is no candidate to win the timeout.
     */
    @Test
    void restoreWithZeroConnectedSuspendsButDoesNotStartCountdown() {
        SuspensionManager manager = new SuspensionManager(TIMEOUT_MILLIS);
        Game game = inProgressGame();

        long restoreTime = 1_000L;
        manager.updateStatus(game, 0, restoreTime);

        assertTrue(manager.isSuspended(), "≤1 connected must mark the match suspended");
        assertEquals(-1L, manager.getSuspensionRemainingMillis(restoreTime),
                "with 0 connected the countdown must be inactive (remaining == -1)");
    }

    /**
     * Case 2 — after the restore-wait (0 connected), the first player reconnects
     * (count → 1). The countdown must start FRESH from that moment, i.e. the full
     * timeout, regardless of how long the server sat at 0 connected.
     */
    @Test
    void firstReconnectAfterRestoreStartsFreshFullCountdown() {
        SuspensionManager manager = new SuspensionManager(TIMEOUT_MILLIS);
        Game game = inProgressGame();

        // Server restored, then sat for 30s with nobody connected.
        manager.updateStatus(game, 0, 1_000L);
        long reconnectTime = 31_000L;

        manager.updateStatus(game, 1, reconnectTime);

        assertTrue(manager.isSuspended());
        assertEquals(TIMEOUT_MILLIS, manager.getSuspensionRemainingMillis(reconnectTime),
                "countdown must restart from the full timeout at the moment count reaches 1");
        // And it counts down normally from there.
        assertEquals(TIMEOUT_MILLIS - 5_000L,
                manager.getSuspensionRemainingMillis(reconnectTime + 5_000L),
                "countdown must decrease in real time once anchored");
    }

    /**
     * Case 3 — secondary bug: the savestate was written while already suspended,
     * so a stale absolute suspensionStartedAtMillis is restored. We
     * reproduce a "stale" anchor via the public API (anchor at t=1000 with 1
     * connected) and then drop to 0 connected at a much later time, as happens on
     * restore. The stale timestamp must be invalidated, leaving the countdown
     * inactive — never a remaining computed against the pre-crash timestamp.
     */
    @Test
    void staleTimestampInvalidatedWhenCountDropsToZero() {
        SuspensionManager manager = new SuspensionManager(TIMEOUT_MILLIS);
        Game game = inProgressGame();

        // Anchor a countdown long ago (stands in for the timestamp persisted on disk).
        manager.updateStatus(game, 1, 1_000L);
        assertEquals(TIMEOUT_MILLIS, manager.getSuspensionRemainingMillis(1_000L));

        // Restore path: everyone marked disconnected → 0 connected, much later.
        long restoreTime = 500_000L;
        manager.updateStatus(game, 0, restoreTime);

        assertTrue(manager.isSuspended());
        assertEquals(-1L, manager.getSuspensionRemainingMillis(restoreTime),
                "the stale pre-crash timestamp must be invalidated, not used to compute remaining");
    }

    /**
     * Case 4 — normal in-game flow (no persistence): a 2-player match where one
     * player disconnects (count 2 → 1). The countdown must start normally at that
     * moment, from the full timeout — confirming the persistence-restore handling
     * above does not change the standard suspension path.
     */
    @Test
    void normalTwoToOneDisconnectionStartsCountdown() {
        SuspensionManager manager = new SuspensionManager(TIMEOUT_MILLIS);
        Game game = inProgressGame();

        // Both connected: not suspended, no countdown.
        manager.updateStatus(game, 2, 5_000L);
        assertFalse(manager.isSuspended());
        assertEquals(-1L, manager.getSuspensionRemainingMillis(5_000L));

        // One disconnects: suspend and start the countdown now.
        long disconnectTime = 6_000L;
        manager.updateStatus(game, 1, disconnectTime);

        assertTrue(manager.isSuspended());
        assertEquals(TIMEOUT_MILLIS, manager.getSuspensionRemainingMillis(disconnectTime),
                "a normal 2→1 disconnection must start a full fresh countdown");

        // The other reconnects (1 → 2): resume, countdown cleared.
        manager.updateStatus(game, 2, disconnectTime + 10_000L);
        assertFalse(manager.isSuspended());
        assertEquals(-1L, manager.getSuspensionRemainingMillis(disconnectTime + 10_000L),
                "climbing back to ≥2 connected must clear the countdown");
    }
}
