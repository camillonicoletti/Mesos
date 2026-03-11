package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.PlayerCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.GameStatus;

import java.util.List;

public class Game {

    private List<Object> players;
    private int currentRoundNumber;
    private Round currentRound;
    private Era currentEra;
    private int remainingCard;
    private Object date;
    private Object board;
    private List<PlayerCard> tributeDeck;
    private List<BuildingCard> buildingDeck;
    private GameStatus status;

    public Game(List<Object> players, int currentRoundNumber, Round currentRound,
                Era currentEra, int remainingCard, Object date,
                Object board, List<PlayerCard> tributeDeck, List<BuildingCard> buildingDeck, GameStatus status) {

        this.players = players;
        this.currentRoundNumber = currentRoundNumber;
        this.currentRound = currentRound;
        this.currentEra = currentEra;
        this.remainingCard = remainingCard;
        this.date = date;
        this.board = board;
        this.tributeDeck = tributeDeck;
        this.buildingDeck = buildingDeck;
        this.status = status;
    }

    public void start() {
        // TODO: start game logic
    }

    public void end() {
        // TODO: end game logic
    }

    public boolean isGameOver() {
        // TODO: check game end conditions
        return false;
    }

    public void nextEra() {
        // TODO: move to next era
    }

    public void endEra() {
        // TODO: era cleanup logic
    }

    public void nextRound() {
        // TODO: advance round
    }

    public void endRound() {
        // TODO: end round logic
    }

    public Round getCurrentRound() {
        return currentRound;
    }

    public List<Object> getPlayers() {
        return players;
    }

    public Era getCurrentEra() {
        return currentEra;
    }

    public int getRemainingTributeDeck() {
        return remainingCard;
    }

    public int getRemainingBuildingDeck() {
        return remainingCard;
    }
}