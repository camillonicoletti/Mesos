package it.polimi.ingsw.view;

import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;

import java.util.List;


public interface View {

    /**
     * Starts the view. Blocks for the lifetime of the client session.
     */
    void run();

    // ── Output: lobby / lifecycle ────────────────────────────────────────────

    /** The server answered a login/reconnect attempt (LoginAckMessage). */
    void onLoginResult();

    /** The lobby roster, ready flags or expected-player count changed. */
    void onLobbyChanged();

    /** The match just started; the first full snapshot is available. */
    void onGameStarted();

    // ── Output: in-game state ────────────────────────────────────────────────

    /** The game state machine moved to a new state (StateChangedMessage). */
    void onGameStateChanged();

    /**
     * Generic board state changed: snapshot/sync, era, food, points, cards
     * taken, current player, totem placed, round ended, extra-pick budget.
     * These all map to "re-render the board" in both views.
     */
    void onBoardChanged();

    /** An era event was resolved (EventResolvedMessage). */
    void onEventResolved();

    /** The match ended; final scores and winner are available. */
    void onGameEnded();

    // ── Output: connection / errors ──────────────────────────────────────────

    /** The server rejected the last request (ErrorMessage). */
    void onError();

    /** The connection to the server was lost or closed. */
    void onConnectionLost();

    /** The match was suspended (too few connected players). */
    void onGameSuspended();

    /** The match resumed after a suspension. */
    void onGameResumed();

    /** A player disconnected from or reconnected to the running match. */
    void onPlayerConnectionChanged();

    // ── Output: leaderboard ──────────────────────────────────────────────────

    /**
     * Renders the top-N leaderboard for a given player count.
     * Called when a LeaderboardMessage arrives from the server (usually as a
     * response to a GetLeaderboardMessage sent by this client).
     *
     * @param playersCount player-count bucket these entries belong to
     * @param entries      ordered list, rank 1 first; may be empty
     */
    void showLeaderboard(int playersCount, List<LeaderboardEntry> entries);

    /**
     * Renders the local player's final rank for the match just played.
     * Called when a LeaderboardRankMessage arrives from the server at the
     * end of a match.
     *
     * @param rank         1-based position in the player-count leaderboard
     * @param playersCount the bucket this rank refers to
     */
    void showMyRank(int rank, int playersCount);
}
