package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serial;

/**
 * Rank of one player after the server saves a finished game.
 */
public record LeaderboardRankMessage(
        String nickname,
        int rank,
        int playersCount
) implements ServerMessage {
    @Serial
    private static final long serialVersionUID = 1L;
}
