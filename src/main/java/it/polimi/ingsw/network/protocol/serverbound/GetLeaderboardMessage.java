package it.polimi.ingsw.network.protocol.serverbound;

import java.io.Serial;

public record GetLeaderboardMessage(
        int playersCount,
        int limit
) implements ClientMessage {
    @Serial
    private static final long serialVersionUID = 1L;
}