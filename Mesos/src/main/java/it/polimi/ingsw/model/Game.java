package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CardVisitor;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.EndGameEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.ArtistCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.InventorCard;
import it.polimi.ingsw.model.board.TurnSlot;
import java.util.Collections;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import it.polimi.ingsw.controller.listener.GameListener;
import java.util.function.Consumer;

public class Game implements Serializable {
    private static final long serialVersionUID = 1L;
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
    private final Map<Player, Integer> finalScores = new HashMap<>();
    private transient Consumer<Consumer<GameListener>> notifier;
    private boolean finalScoresCalculated = false;
    private boolean gameEnded = false;

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
                Board board, List<Card> tributeDeck, List<BuildingCard> era1BuildingDeck, List<BuildingCard> era2BuildingDeck, List<BuildingCard> era3BuildingDeck) {

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
    }

    public void start() {

        currentRoundNumber = 1;
        currentEra = Era.I;

        // Create the board FIRST — setupInitialRows() and setupInitialBuildings() may need it
        this.board = BoardFactory.createBoard(players.size());

        for (Player p : players) {
            p.setGame(this);
        }

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
        gameEnded = true;
        calculateFinalScores();
    }

    public boolean isGameOver() {
        return gameEnded;
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
            return;
        }

        if (currentRoundNumber >= MAX_ROUNDS) {
            end();
            return;
        }

        currentRoundNumber++;
        currentRound = currentRound.createNextRound(currentRoundNumber);
    }

    /**
     * Calculates the current score for a single player without caching it.
     */
    private int calculateScoreForPlayer(Player player) {
        int finalScore = player.getPrestigePoints();

        finalScore += calculateInventorPoints(player);
        finalScore += calculateArtistPoints(player);
        finalScore += calculateBuilderPoints(player);

        for (BuildingCard building : player.getTribe().getBuildingCards()) {
            finalScore += building.getPointsEndOfMatch();
        }

        for (BuildingCard building : player.getTribe().getBuildingCards()) {
            finalScore += endGamePointsFrom(player, building);
        }

        return finalScore;
    }

    private int endGamePointsFrom(Player player, BuildingCard building) {
        return building.acceptEffect(new EffectVisitor<>() {
            @Override public Integer visitEndGame(EndGameEffect effect) {
                return effect.calculatePoints(player);
            }

            @Override public Integer visitEffect(Effect effect) {
                return 0;
            }
        });
    }

    public void calculateFinalScores() {
        if (finalScoresCalculated) {
            return;
        }

        finalScores.clear();

        for (Player player : players) {
            finalScores.put(player, calculateScoreForPlayer(player));
        }

        finalScoresCalculated = true;
    }


    private int calculateBuilderPoints(Player player) {
        int points = 0;
        CardVisitor<Integer> builderPoints = new CardVisitor<>() {
            @Override public Integer visitBuilder(BuilderCard builder) {
                return builder.getPointsEndOfMatch();
            }

            @Override public Integer visitCard(Card card) {
                return 0;
            }
        };
        for (CharacterCard card : player.getTribe().getCharacterCards()) {
            points += card.accept(builderPoints);
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

        List<CharacterCard> mutableRow;

        if (currentRound.getUpperRowTribute().contains(chosenCard)
                && row.equals(currentRound.getUpperRowTribute())) {
            mutableRow = currentRound.mutableUpperRowTribute();
        } else if (currentRound.getLowerRowTribute().contains(chosenCard)
                && row.equals(currentRound.getLowerRowTribute())) {
            mutableRow = currentRound.mutableLowerRowTribute();
        } else {
            throw new IllegalArgumentException("Selected row does not belong to the current round");
        }

        int foodBefore = player.getFood();
        int ppBefore   = player.getPrestigePoints();

        mutableRow.remove(chosenCard);
        player.addCharacterCard(chosenCard);

        applyCharacterImmediateEffects(player, chosenCard);

        int foodDelta = player.getFood() - foodBefore;
        int ppDelta   = player.getPrestigePoints() - ppBefore;
        if (foodDelta != 0) {
            notifyListeners(l -> l.onFoodChanged(player.getNickname(), player.getFood(), foodDelta));
        }
        if (ppDelta != 0) {
            notifyListeners(l -> l.onPointsChanged(player.getNickname(), player.getPrestigePoints(), ppDelta));
        }
    }

    private void applyCharacterImmediateEffects(Player player, CharacterCard card) {
        card.accept(new CardVisitor<Void>() {
            @Override public Void visitHunter(HunterCard hunter) {
                if (hunter.hasFoodIcon()) {
                    int huntersInTribe = player.getTribe().countCharactersByType(HunterCard.class);
                    player.addFood(huntersInTribe);
                }
                return null;
            }

            @Override public Void visitCard(Card card) {
                return null;
            }
        });
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

        List<BuildingCard> mutableRow;

        if (currentRound.getUpperRowBuildings().contains(chosenCard)
                && row.equals(currentRound.getUpperRowBuildings())) {
            mutableRow = currentRound.mutableUpperRowBuildings();
        } else if (currentRound.getLowerRowBuildings().contains(chosenCard)
                && row.equals(currentRound.getLowerRowBuildings())) {
            mutableRow = currentRound.mutableLowerRowBuildings();
        } else {
            throw new IllegalArgumentException("Selected row does not belong to the current round");
        }

        player.spendFood(finalCost);
        if (finalCost > 0) {
            notifyListeners(l -> l.onFoodChanged(player.getNickname(), player.getFood(), -finalCost));
        }
        mutableRow.remove(chosenCard);
        player.addBuildingCard(chosenCard);
    }
    public boolean isLastRound() {
        return currentRoundNumber == MAX_ROUNDS;
    }
    private int getBuildingDiscount(Player player) {
        return player.getTribe().getBuildingDiscount();
    }

    public boolean canAffordBuilding(Player player, BuildingCard card) {
        int finalCost = Math.max(0, card.getFoodCost() - getBuildingDiscount(player));
        return player.canAfford(finalCost);
    }


    public void startNewEra(){}

    public Round getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Round round) {
        this.currentRound = round;
    }


    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
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
        return Collections.unmodifiableList(tributeDeck);
    }

    public Board getBoard() {
        return board;
    }

    /**
     * Returns the player's score.
     * Before the game ends, this method returns a live score preview without caching it.
     * After the game ends, it returns the cached final score.
     *
     * @param player the player whose score is requested
     * @return the player's score
     */
    public int getFinalScore(Player player) {
        if (!gameEnded) {
            return calculateScoreForPlayer(player);
        }

        if (!finalScoresCalculated) {
            calculateFinalScores();
        }

        return finalScores.getOrDefault(player, player.getPrestigePoints());
    }

    public Map<Player, Integer> getFinalScores() {
        if (!gameEnded) {
            Map<Player, Integer> liveScores = new HashMap<>();

            for (Player player : players) {
                liveScores.put(player, calculateScoreForPlayer(player));
            }

            return liveScores;
        }

        if (!finalScoresCalculated) {
            calculateFinalScores();
        }

        return new HashMap<>(finalScores);
    }
    /**
     * Returns all co-winners applying the official tiebreaker chain:
     * 1. Most PP; 2. Most food among PP-tied players; 3. Shared victory.
     */
    public List<Player> getWinners() {
        if (players.isEmpty()) return List.of();

        int maxScore = players.stream().mapToInt(this::getFinalScore).max().orElse(0);

        List<Player> topScorers = players.stream()
                .filter(p -> getFinalScore(p) == maxScore)
                .collect(Collectors.toList());

        if (topScorers.size() == 1) return topScorers;

        int maxFood = topScorers.stream().mapToInt(Player::getFood).max().orElse(0);

        return topScorers.stream()
                .filter(p -> p.getFood() == maxFood)
                .collect(Collectors.toList());
    }

    /** Returns the single winner, or null when multiple players share the victory. */
    public Player getWinner() {
        List<Player> winners = getWinners();
        return winners.size() == 1 ? winners.get(0) : null;
    }
    // LISTENER
    public void setNotifier(Consumer<Consumer<GameListener>> notifier) {
        this.notifier = notifier;
    }

    public void notifyListeners(Consumer<GameListener> event) {
        if (notifier != null) notifier.accept(event);
    }
}
