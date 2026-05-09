package it.polimi.ingsw.network.protocol.clientbound;

import java.util.Map;

public class GameEndedMessage implements ServerMessage {
    private final String winnerNickname;
    private final Map<String, Integer> finalScores;

    public GameEndedMessage(String winnerNickname, Map<String, Integer> finalScores) {
        this.winnerNickname = winnerNickname;
        this.finalScores = Map.copyOf(finalScores);
    }

    public String getWinnerNickname() {
        return winnerNickname;
    }

    public Map<String, Integer> getFinalScores() {
        return finalScores;
    }
}