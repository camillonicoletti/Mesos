package it.polimi.ingsw.network.protocol.clientbound;

import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;

import java.io.Serial;
import java.util.List;

/**
 * Leaderboard page requested by the client from the lobby or menu.
 */
public record LeaderboardMessage(
        int playersCount,
        List<LeaderboardEntry> entries
) implements ServerMessage {
    @Serial
    private static final long serialVersionUID = 1L;
}
