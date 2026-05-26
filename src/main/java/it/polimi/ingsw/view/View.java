package it.polimi.ingsw.view;

import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;

import java.util.List;

/**
 * Client-side view contract.
 *
 * <p>Implementations (TUI, GUI) render the application state to the user.
 * Most state is pushed through {@link it.polimi.ingsw.network.client.ClientModel}
 * listeners, but some discrete events are delivered directly through the
 * methods declared here, so each implementation can choose its own
 * presentation without bloating the shared client model with view-only data.
 */
public interface View {

    /**
     * Starts the view. Blocks for the lifetime of the client session.
     */
    void run();

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