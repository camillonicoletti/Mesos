package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.GameStatus;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.Totem;


import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Game {

    private static final int MAX_ROUNDS = 10;

    private final List<Player> players;
    private int currentRoundNumber;
    private Round currentRound;
    private Era currentEra;
    private LocalDateTime date;
    private Board board;
    private List<Card> tributeDeck;
    private List<BuildingCard> era1BuildingDeck;
    private List<BuildingCard> era2BuildingDeck;
    private List<BuildingCard> era3BuildingDeck;
    private GameStatus status;
    private int getBuildingCountForEra(Era era) {
        int playersCount = players.size();

        return switch (playersCount) {
            case 2 -> switch (era) {
                case I -> 1;
                case II -> 2;
                case III -> 3;
            };
            case 3 -> switch (era) {
                case I -> 2;
                case II -> 2;
                case III -> 4;
            };
            case 4 -> switch (era) {
                case I -> 2;
                case II -> 3;
                case III -> 4;
            };
            case 5 -> switch (era) {
                case I -> 2;
                case II -> 3;
                case III -> 5;
            };
            default -> throw new IllegalStateException("Unsupported number of players");
        };
    }

    public Game(List<Player> players, int currentRoundNumber, Round currentRound,
                Era currentEra, LocalDateTime date,
                Board board, List<Card> tributeDeck, List<BuildingCard> era1BuildingDeck, List<BuildingCard> era2BuildingDeck, List<BuildingCard> era3BuildingDeck, GameStatus status) {

        this.players = players;
        this.currentRoundNumber = currentRoundNumber;
        this.currentRound = currentRound;
        this.currentEra = currentEra;
        this.date = date;
        this.board = board;
        this.tributeDeck = tributeDeck;
        this.era1BuildingDeck = era1BuildingDeck != null ? era1BuildingDeck : new ArrayList<>();
        this.era2BuildingDeck = era2BuildingDeck != null ? era2BuildingDeck : new ArrayList<>();
        this.era3BuildingDeck = era3BuildingDeck != null ? era3BuildingDeck : new ArrayList<>();
        this.status = status;
    }

    public void start() {
        if (status != GameStatus.WAITING) {
            throw new IllegalStateException("Game cannot start from status: " + status);
        }

        currentRoundNumber = 1;
        currentEra = Era.I;
        status = GameStatus.IN_PROGRESS;

        if (currentRound == null) {
            currentRound = new Round(currentRoundNumber, this, RoundPhase.TOTEM_PLACEMENT, Era.I, null, null, null, null, null, null);
        }

        currentRound.setupInitialRows();
        currentRound.setupInitialBuildings(era1BuildingDeck);
        era1BuildingDeck.clear();

        // Assign a random initial turn order at game start
        List<Totem> totems = new ArrayList<>();
        for (Player p : players) {
            totems.add(p.getTotem());
        }
        board.getTurnOrderTile().assignRandomTurnOrder(totems);
    }


    public void end() {
        status = GameStatus.FINISHED;
    }

    public boolean isGameOver() {
        return status == GameStatus.FINISHED || currentRoundNumber >= MAX_ROUNDS;
    }

    public void nextEra() {
        switch (currentEra) {
            case I -> currentEra = Era.II;
            case II -> currentEra = Era.III;
            case III -> throw new IllegalStateException("cannot adavnce beyond Era III");
            default -> throw new IllegalStateException("Uknown era: " + currentEra);
        }
    }

    public void endEra() {
        if (currentEra == Era.III) {
            return;
        }

        if (currentEra == Era.II) {
            currentRound.discardLowerBuildings();
        }

        currentRound.moveUpperBuildingsToLower();

        nextEra();

        List<BuildingCard> newEraBuildings = getBuildingsForEra(currentEra);
        currentRound.revealBuildingsForEra(newEraBuildings);
        newEraBuildings.clear();
    }

    public void nextRound() {
        if (isGameOver()) {
            end();
            return;
        }

        if (currentRoundNumber >= MAX_ROUNDS) {
            end();
            return;
        }

        currentRoundNumber++;
        currentRound = currentRound.createNextRound(currentRoundNumber);
    }

    public void endRound() {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot end round if game is not running");
        }

        currentRound.execute();

        if (isGameOver()) {
            end();
        } else {
            nextRound();
        }
    }

    public List<BuildingCard> getBuildingsForEra(Era era) {
        return switch (era) {
            case I -> era1BuildingDeck;
            case II -> era2BuildingDeck;
            case III -> era3BuildingDeck;
        };
    }
    public void startNewEra(){}

    public Round getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Round round) {
        this.currentRound = round;
    }


    public List<Player> getPlayers() {
        return players;
    }

    public Era getCurrentEra() {
        return currentEra;
    }

    public int getRemainingTributeDeck() {
        return tributeDeck.size();
    }

    public int getRemainingBuildingDeck() {
        return era1BuildingDeck.size()
                + era2BuildingDeck.size()
                + era3BuildingDeck.size();
    }

    public List<Card> getTributeDeck() {
        return tributeDeck;
    }

    public Board getBoard() {
        return board;
    }
}