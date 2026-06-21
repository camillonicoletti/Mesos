package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serial;
import java.util.Map;

/**
 * Final result of the match, including data needed for the victory screen.
 */
public class GameEndedMessage implements ServerMessage {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String winnerNickname;
    private final Map<String, Integer> finalScores;
    // Server-authoritative food used for tiebreaker display.
    private final Map<String, Integer> finalFood;

    public GameEndedMessage(String winnerNickname, Map<String, Integer> finalScores, Map<String, Integer> finalFood) {
        this.winnerNickname = winnerNickname;
        this.finalScores = Map.copyOf(finalScores);
        this.finalFood = Map.copyOf(finalFood);
    }

    public String getWinnerNickname() {
        return winnerNickname;
    }

    public Map<String, Integer> getFinalScores() {
        return finalScores;
    }

    public Map<String, Integer> getFinalFood() {
        return finalFood;
    }
}
