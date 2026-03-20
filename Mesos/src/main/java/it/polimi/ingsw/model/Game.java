package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.GameStatus;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.InventorCard;
import it.polimi.ingsw.model.board.TurnSlot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


public class Game {

    private static final int MAX_ROUNDS = 10;

    private final List<Player> players;
    private int currentRoundNumber;
    private Round currentRound;
    private Era currentEra;
    private final LocalDateTime date;
    private final Board board;
    private List<Card> tributeDeck;
    private List<BuildingCard> era1BuildingDeck;
    private List<BuildingCard> era2BuildingDeck;
    private List<BuildingCard> era3BuildingDeck;
    private GameStatus status;
    private final Map<Player, Integer> finalScores = new HashMap<>();
    private boolean finalScoresCalculated = false;
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

        // Create the board with the right offer tiles and turn order tile for this number of players
        this.board = BoardFactory.createBoard(players.size());


        List<Totem> totems = new ArrayList<>();
        for (Player p : players) {
            totems.add(p.getTotem());
        }
        board.getTurnOrderTile().assignRandomTurnOrder(totems);
        distributeInitialFood();

    }

    private void distributeInitialFood() {
        Map<Totem, Player> totemOwner = new HashMap<>();
        for (Player player : players) {
            totemOwner.put(player.getTotem(), player);
        }

        int position = 0;
        for (TurnSlot slot : board.getTurnOrderTile().getSlots()) {
            if (!slot.isOccupied()) {
                continue;
            }

            Player owner = totemOwner.get(slot.getOccupiedBy());
            if (owner == null) {
                continue;
            }

            owner.addFood(getInitialFoodForPosition(position));
            position++;
        }
    }

    private int getInitialFoodForPosition(int positionIndex) {
        return switch (positionIndex) {
            case 0 -> 2;
            case 1, 2 -> 3;
            case 3, 4 -> 4;
            default -> throw new IllegalArgumentException("Invalid turn order position: " + positionIndex);
        };
    }

    public void end() {
        calculateFinalScores();
        status = GameStatus.FINISHED;
    }

    public boolean isGameOver() {
        return status == GameStatus.FINISHED || currentRoundNumber > MAX_ROUNDS;
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

    public void calculateFinalScores() {
        if (finalScoresCalculated) {
            return;
        }

        finalScores.clear();

        for (Player player : players) {
            int finalScore = player.getPrestigePoints();

            // 1) punti dovuti ai costruttori
            finalScore += calculateBuilderPoints(player);

            // 2) punti dovuti agli inventori
            finalScore += calculateInventorPoints(player);

            // 3) 10 punti per ogni coppia di artisti
            finalScore += calculateArtistPoints(player);

            // 4) punti dovuti agli edifici
            finalScore += calculateBuildingPoints(player);

            finalScores.put(player, finalScore);
        }

        finalScoresCalculated = true;
    }

    private int calculateBuilderPoints(Player player) {
        int builders = player.getTribe().countCharactersByType(BuilderCard.class);
        return builders;
    }

    private int calculateInventorPoints(Player player) {
        int inventors = player.getTribe().countCharactersByType(InventorCard.class);
        int differentIcons = player.getTribe().countDistinctInventorIcons();

        return inventors * differentIcons;
    }

    private int calculateArtistPoints(Player player) {
        int artists = player.getTribe().countCharactersByType(ArtistCard.class);
        int pairs = artists / 2;
        return pairs * 10;
    }

    private int calculateBuildingPoints(Player player) {
        int points = 0;

        for (BuildingCard building : player.getTribe().getBuildingCards()) {
            points += building.getPointsEndOfMatch();
        }

        return points;
    }


    public List<BuildingCard> getBuildingsForEra(Era era) {
        return switch (era) {
            case I -> era1BuildingDeck;
            case II -> era2BuildingDeck;
            case III -> era3BuildingDeck;
        };
    }

    public Card drawTribeCard() {
        if (tributeDeck == null || tributeDeck.isEmpty()) {
            return null;
        }

        return tributeDeck.remove(0);
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

    public int getFinalScore(Player player) {
        if (!finalScoresCalculated) {
            calculateFinalScores();
        }

        return finalScores.getOrDefault(player, player.getPrestigePoints());
    }

    public Map<Player, Integer> getFinalScores() {
        if (!finalScoresCalculated) {
            calculateFinalScores();
        }

        return new HashMap<>(finalScores);
    }

    public Player getWinner() {
        if (!finalScoresCalculated) {
            calculateFinalScores();
        }

        return players.stream()
                .max(Comparator.comparingInt(this::getFinalScore))
                .orElse(null);
    }
}