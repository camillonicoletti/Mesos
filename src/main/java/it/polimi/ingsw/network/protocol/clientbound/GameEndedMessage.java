package it.polimi.ingsw.network.protocol.clientbound;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;

public class GameEndedMessage implements ServerMessage {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String winnerNickname;
    private final Map<String, Integer> finalScores;
    // Server-authoritative food at game end; used by the client for tiebreaker display
    // because PlayerView.getFood() may lag behind (not all events trigger a snapshot).
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