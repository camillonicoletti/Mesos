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
    private final List<String> disconnectedPlayers;

    public GameSnapshot(List<PlayerSnapshot> players,
                        List<Integer> upperRowCharacterAndEventIds,
                        List<Integer> lowerRowCharacterAndEventIds,
                        List<Integer> upperRowBuildingIds,
                        List<Integer> lowerRowBuildingIds,
                        Era currentEra,
                        int roundNumber,
                        Map<Character, String> totemOnTiles,
                        List<String> turnOrder) {
        this(players,
                upperRowCharacterAndEventIds,
                lowerRowCharacterAndEventIds,
                upperRowBuildingIds,
                lowerRowBuildingIds,
                currentEra,
                roundNumber,
                totemOnTiles,
                turnOrder,
                List.of());
    }

    public GameSnapshot(List<PlayerSnapshot> players,
                        List<Integer> upperRowCharacterAndEventIds,
                        List<Integer> lowerRowCharacterAndEventIds,
                        List<Integer> upperRowBuildingIds,
                        List<Integer> lowerRowBuildingIds,
                        Era currentEra,
                        int roundNumber,
                        Map<Character, String> totemOnTiles,
                        List<String> turnOrder,
                        List<String> disconnectedPlayers) {
        this.players = List.copyOf(players);
        this.upperRowCharacterAndEventIds = List.copyOf(upperRowCharacterAndEventIds);
        this.lowerRowCharacterAndEventIds = List.copyOf(lowerRowCharacterAndEventIds);
        this.upperRowBuildingIds = List.copyOf(upperRowBuildingIds);
        this.lowerRowBuildingIds = List.copyOf(lowerRowBuildingIds);
        this.currentEra = currentEra;
        this.roundNumber = roundNumber;
        this.totemOnTiles = Map.copyOf(totemOnTiles);
        this.turnOrder = List.copyOf(turnOrder);
        this.disconnectedPlayers = List.copyOf(disconnectedPlayers);
    }

    public List<PlayerSnapshot> getPlayers() {
        return players;
    }

    public List<Integer> getUpperRowCharacterAndEventIds() {
        return upperRowCharacterAndEventIds;
    }

    public List<Integer> getLowerRowCharacterAndEventIds() {
        return lowerRowCharacterAndEventIds;
    }

    public List<Integer> getUpperRowBuildingIds() {
        return upperRowBuildingIds;
    }

    public List<Integer> getLowerRowBuildingIds() {
        return lowerRowBuildingIds;
    }

    public Era getCurrentEra() {
        return currentEra;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Map<Character, String> getTotemOnTiles() {
        return totemOnTiles;
    }

    public List<String> getTurnOrder() {
        return turnOrder;
    }

    public List<String> getDisconnectedPlayers() {
        return disconnectedPlayers;
    }
}