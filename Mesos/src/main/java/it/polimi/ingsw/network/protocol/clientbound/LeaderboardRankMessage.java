package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serial;

public record LeaderboardRankMessage(
        String nickname,
        int rank,
        int playersCount
) implements ServerMessage {
    @Serial
    private static final long serialVersionUID = 1L;
}