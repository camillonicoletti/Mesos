package it.polimi.ingsw.network.protocol.clientbound;
import it.polimi.ingsw.model.enums.Era;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GameSnapshot implements Serializable {
    private final List<PlayerSnapshot> players;
    private final List<Integer> upperRowCharacterAndEventIds;
    private final List<Integer> lowerRowCharacterAndEventIds;
    private final List<Integer> upperRowBuildingIds;
    private final List<Integer> lowerRowBuildingIds;
    private final Era currentEra;
    private final int roundNumber;
    private final Map<Character, String> totemOnTiles;
    private final List<String> turnOrder;

    public GameSnapshot(List<PlayerSnapshot> players,
                        List<Integer> upperRowCharacterAndEventIds,
                        List<Integer> lowerRowCharacterAndEventIds,
                        List<Integer> upperRowBuildingIds,
                        List<Integer> lowerRowBuildingIds,
                        Era currentEra, int roundNumber,
                        Map<Character, String> totemOnTiles,
                        List<String> turnOrder) {
        this.players = players;
        this.upperRowCharacterAndEventIds = upperRowCharacterAndEventIds;
        this.lowerRowCharacterAndEventIds = lowerRowCharacterAndEventIds;
        this.upperRowBuildingIds = upperRowBuildingIds;
        this.lowerRowBuildingIds = lowerRowBuildingIds;
        this.currentEra = currentEra;
        this.roundNumber = roundNumber;
        this.totemOnTiles = totemOnTiles;
        this.turnOrder = turnOrder;
    }

    public List<PlayerSnapshot> getPlayers() { return players; }
    public List<Integer> getUpperRowCharacterAndEventIds() { return upperRowCharacterAndEventIds; }
    public List<Integer> getLowerRowCharacterAndEventIds() { return lowerRowCharacterAndEventIds; }
    public List<Integer> getUpperRowBuildingIds() { return upperRowBuildingIds; }
    public List<Integer> getLowerRowBuildingIds() { return lowerRowBuildingIds; }
    public Era getCurrentEra() { return currentEra; }
    public int getRoundNumber() { return roundNumber; }
    public Map<Character, String> getTotemOnTiles() { return totemOnTiles; }
    public List<String> getTurnOrder() { return turnOrder; }
}