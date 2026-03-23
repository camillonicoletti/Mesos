package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.effect.EndBuilderPPEffect;
import it.polimi.ingsw.model.card.effect.EndCharacterCountPPEffect;
import it.polimi.ingsw.model.card.effect.EndFullSetPPEffect;
import it.polimi.ingsw.model.card.effect.EndGameEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.GameStatus;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HunterCard;
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
    private Board board;
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

        // Create the board FIRST — setupInitialRows() and setupInitialBuildings() may need it
        this.board = BoardFactory.createBoard(players.size());

        List<Totem> totems = new ArrayList<>();
        for (Player p : players) {
            totems.add(p.getTotem());
        }
        board.getTurnOrderTile().assignRandomTurnOrder(totems);

        if (currentRound == null) {
            currentRound = new Round(currentRoundNumber, this, RoundPhase.TOTEM_PLACEMENT, Era.I, null, null, null, null, null, null);
        }

        currentRound.setupInitialRows();
        currentRound.setupInitialBuildings(era1BuildingDeck);
        era1BuildingDeck.clear();
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
        return status == GameStatus.FINISHED;
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
        if (status == GameStatus.FINISHED) {
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

        if (isLastRound()) {
            end();
        } else {
            nextRound();
        }
    }

    /**
     * Calculates the final score for each player.
     * Building effects that trigger at end of game implement EndGameEffect,
     * which exposes calculatePoints() — this returns the points as a value
     * instead of calling apply() directly on the player, avoiding double counting
     * with the prestige points already accumulated during the game.
     */
    public void calculateFinalScores() {
        if (finalScoresCalculated) {
            return;
        }

        finalScores.clear();

        for (Player player : players) {
            int finalScore = player.getPrestigePoints();

            // inventor points — inventors * distinct icons
            finalScore += calculateInventorPoints(player);

            // artist points — 10 PP per pair of artists
            finalScore += calculateArtistPoints(player);

            // base builder points — always counted
            //TODO: check Builder Point, then modify test
            finalScore += calculateBuilderPoints(player);

            // end-of-game building effects: all BuildingCards implementing EndGameEffect
            // use calculatePoints() to avoid modifying player state and double counting
            for (BuildingCard building : player.getTribe().getBuildingCards()) {
                if (building.getEffect() instanceof EndGameEffect endEffect) {
                    finalScore += endEffect.calculatePoints(player);
                }
            }

            finalScores.put(player, finalScore);
        }

        finalScoresCalculated = true;
    }


    private int calculateBuilderPoints(Player player) {
        int points = 0;
        for (CharacterCard card : player.getTribe().getCharacterCards()) {
            if (card instanceof BuilderCard builder) {
                points += builder.getPointsEndOfMatch();
            }
        }
        return points;
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

    public void takeCharacterCard(Player player, List<CharacterCard> row, CharacterCard chosenCard) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (row == null) {
            throw new IllegalArgumentException("Row cannot be null");
        }
        if (chosenCard == null) {
            throw new IllegalArgumentException("Chosen card cannot be null");
        }
        if (!row.contains(chosenCard)) {
            throw new IllegalArgumentException("Chosen character card is not in the selected row");
        }

        row.remove(chosenCard);
        player.addCharacterCard(chosenCard);

        applyCharacterImmediateEffects(player, chosenCard);
    }

    private void applyCharacterImmediateEffects(Player player, CharacterCard card) {
        if (card instanceof HunterCard hunter && hunter.hasFoodIcon()) {
            int huntersInTribe = player.getTribe().countCharactersByType(HunterCard.class);
            player.addFood(huntersInTribe);
        }
    }

    public void takeBuildingCard(Player player, List<BuildingCard> row, BuildingCard chosenCard) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (row == null) {
            throw new IllegalArgumentException("Row cannot be null");
        }
        if (chosenCard == null) {
            throw new IllegalArgumentException("Chosen card cannot be null");
        }
        if (!row.contains(chosenCard)) {
            throw new IllegalArgumentException("Chosen building card is not in the selected row");
        }

        int baseCost = chosenCard.getFoodCost();
        int discount = getBuildingDiscount(player);
        int finalCost = Math.max(0, baseCost - discount);

        if (player.getFood() < finalCost) {
            throw new IllegalStateException("Player does not have enough food to take this building");
        }

        player.spendFood(finalCost);
        row.remove(chosenCard);
        player.addBuildingCard(chosenCard);
    }

    public boolean isLastRound() {
        return currentRoundNumber == MAX_ROUNDS;
    }

    private int getBuildingDiscount(Player player) {
        return player.getTribe().getBuildingDiscount();
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