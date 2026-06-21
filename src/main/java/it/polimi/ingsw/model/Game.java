package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CardVisitor;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.EndGameEffect;
import it.polimi.ingsw.model.enums.Era;
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

/**
 * Represents a full match.
 * It owns players, board, current round, current era, decks and final scores.
 * The controller changes the game through this class, so this is the main model object.
 */
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

    /**
     * Creates a game with its players, decks and optional saved state.
     * It is used both for a new match and when a saved match is restored.
     */
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

    /**
     * Starts the match by creating the board, the first round and the first visible cards.
     * It also assigns the first random turn order and starting food.
     */
    public void start() {

        currentRoundNumber = 1;
        currentEra = Era.I;

        // The board must exist before rows and buildings are prepared.
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
            currentRound = new Round(currentRoundNumber, this, Era.I, null, null, null, null, null, null);
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
        // Players later in turn order receive more starting food as compensation.
        return switch (positionIndex) {
            case 0 -> 2;
            case 1, 2 -> 3;
            case 3, 4 -> 4;
            default -> throw new IllegalArgumentException("Invalid turn order position: " + positionIndex);
        };
    }

    /**
     * Ends the match and stores the final scores.
     * Calling it twice is safe because final scoring is guarded.
     */
    public void end() {
        gameEnded = true;
        calculateFinalScores();
    }

    public boolean isGameOver() {
        return gameEnded;
    }

    /** Moves the match to the next era and refuses to go past Era III. */
    public void nextEra() {
        switch (currentEra) {
            case I -> currentEra = Era.II;
            case II -> currentEra = Era.III;
            case III -> throw new IllegalStateException("cannot advance beyond Era III");
            default -> throw new IllegalStateException("Unknown era: " + currentEra);
        }
    }

    /**
     * Updates rows and buildings when the game changes era.
     * Upper buildings move down, old buildings may be discarded, and new era buildings are revealed.
     */
    public void endEra() {
        if (currentEra == Era.III) {
            return;
        }

        // At the II to III transition, lower-row buildings are discarded instead of carried over.
        if (currentEra == Era.II) {
            currentRound.discardLowerBuildings();
        }

        currentRound.moveUpperBuildingsToLower();

        nextEra();

        List<BuildingCard> newEraBuildings = getBuildingsForEra(currentEra);
        currentRound.revealBuildingsForEra(newEraBuildings);
        newEraBuildings.clear();
    }

    /**
     * Opens the next round, or ends the game after the last round.
     * Once the game is over, it does nothing.
     */
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
     * Calculates one player's score from points, cards and end-game effects.
     * This can be used as a live preview before the score is frozen.
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

    /**
     * Calculates and saves the final score for every player.
     * The result is cached so end-game effects are not applied more than once.
     */
    public void calculateFinalScores() {
        // Final scoring must run only once.
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
    /** Returns the hidden building deck for the requested era. */
    public List<BuildingCard> getBuildingsForEra(Era era) {
        return switch (era) {
            case I -> era1BuildingDeck;
            case II -> era2BuildingDeck;
            case III -> era3BuildingDeck;
        };
    }

    /** Draws the next tribute card, or returns null if the deck is empty. */
    public Card drawTribeCard() {
        if (tributeDeck == null || tributeDeck.isEmpty()) {
            return null;
        }

        return tributeDeck.remove(0);
    }

    /**
     * Gives a selected character card to a player and applies its instant effects.
     * It also notifies listeners when food or prestige points change.
     */
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
                    // The new Hunter already counts as part of the tribe.
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

    /**
     * Gives a selected building card to a player and makes them pay its cost.
     * Builder discounts are applied before checking if the player can afford it.
     */
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
    /** Returns true during the final round; upper-row events resolve only then. */
    public boolean isLastRound() {
        return currentRoundNumber == MAX_ROUNDS;
    }
    private int getBuildingDiscount(Player player) {
        return player.getTribe().getBuildingDiscount();
    }

    /** Checks if a player can pay for a building after discounts. */
    public boolean canAffordBuilding(Player player, BuildingCard card) {
        int finalCost = Math.max(0, card.getFoodCost() - getBuildingDiscount(player));
        return player.canAfford(finalCost);
    }

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
     * Returns the player's score: live during the match, saved after the end.
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

    /**
     * Returns all player scores.
     * Before the end it recomputes a live preview; after the end it returns cached values.
     */
    public Map<Player, Integer> getFinalScores() {
        if (!gameEnded) {
            Map<Player, Integer> liveScores = new HashMap<>();

            for (Player player : players) {
                liveScores.put(player, calculateScoreForPlayer(player));
            }

            return liveScores;
        }

        calculateFinalScores();
        return new HashMap<>(finalScores);
    }
    /**
     * Finds the winner, using food as the tiebreaker when scores are equal.
     * If players are still tied after food, they share the victory.
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

    /** Returns one winner, or null if the victory is shared. */
    public Player getWinner() {
        List<Player> winners = getWinners();
        return winners.size() == 1 ? winners.get(0) : null;
    }
    // LISTENER
    /**
     * Connects the model to the listeners managed by the controller.
     * This is transient, so it must be set again after loading a saved game.
     */
    public void setNotifier(Consumer<Consumer<GameListener>> notifier) {
        this.notifier = notifier;
    }

    /** Sends a model change to the listeners, if they are connected. */
    public void notifyListeners(Consumer<GameListener> event) {
        if (notifier != null) notifier.accept(event);
    }
}
