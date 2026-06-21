package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.TotemSlotFoodBonusEffect;
import it.polimi.ingsw.model.enums.Era;
import java.util.Collections;

import java.io.Serializable;
import java.util.*;


/**
 * Represents one round of the match.
 * It keeps the visible rows of cards and the offer choices made by players.
 * It also manages row refill, era changes and event resolution order.
 */
public class Round implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int number;
    private final Game game;
    private final Era era;
    private List<EventCard> lowerRowEventCards;
    private List<EventCard> upperRowEventCards;
    private List<CharacterCard> lowerRowTribute;
    private List<CharacterCard> upperRowTribute;
    private List<BuildingCard> lowerRowBuildings;
    private List<BuildingCard> upperRowBuildings;
    private Map<Player, OfferTile> playerChoices = new LinkedHashMap<>();


    public Round(int number, Game game, Era era,
                 List<EventCard> lowerRowEventCards, List<EventCard> upperRowEventCards,
                 List<CharacterCard> lowerRowTribute, List<CharacterCard> upperRowTribute,
                 List<BuildingCard> lowerRowBuildings, List<BuildingCard> upperRowBuildings) {
        this.number = number;
        this.game = game;
        this.era = era;
        this.lowerRowEventCards = (lowerRowEventCards != null) ? lowerRowEventCards : new ArrayList<>();
        this.upperRowEventCards = (upperRowEventCards != null) ? upperRowEventCards : new ArrayList<>();
        this.lowerRowTribute = (lowerRowTribute != null) ? lowerRowTribute : new ArrayList<>();
        this.upperRowTribute = (upperRowTribute != null) ? upperRowTribute : new ArrayList<>();
        this.lowerRowBuildings = (lowerRowBuildings != null) ? lowerRowBuildings : new ArrayList<>();
        this.upperRowBuildings = (upperRowBuildings != null) ? upperRowBuildings : new ArrayList<>();
    }

    public int getNumber() {
        return number;
    }

    public Game getGame() {
        return game;
    }

    public Era getEra() {
        return era;
    }

    List<CharacterCard> mutableLowerRowTribute() {
        return lowerRowTribute;
    }

    List<CharacterCard> mutableUpperRowTribute() {
        return upperRowTribute;
    }

    List<BuildingCard> mutableLowerRowBuildings() {
        return lowerRowBuildings;
    }

    List<BuildingCard> mutableUpperRowBuildings() {
        return upperRowBuildings;
    }

    public List<EventCard> getLowerRowEvents() {
        return Collections.unmodifiableList(lowerRowEventCards);
    }

    public List<EventCard> getUpperRowEvents() {
        return Collections.unmodifiableList(upperRowEventCards);
    }

    public List<CharacterCard> getLowerRowTribute() {
        return Collections.unmodifiableList(lowerRowTribute);
    }

    public List<CharacterCard> getUpperRowTribute() {
        return Collections.unmodifiableList(upperRowTribute);
    }

    public List<BuildingCard> getUpperRowBuildings() {
        return Collections.unmodifiableList(upperRowBuildings);
    }

    public List<BuildingCard> getLowerRowBuildings() {
        return Collections.unmodifiableList(lowerRowBuildings);
    }

    /**
     * Creates the next round by keeping the current visible rows.
     * This lets the controller continue from the same board situation.
     */
    public Round createNextRound(int nextRoundNumber) {
        return new Round(
                nextRoundNumber,
                game,
                game.getCurrentEra(),
                new ArrayList<>(lowerRowEventCards),
                new ArrayList<>(upperRowEventCards),
                new ArrayList<>(lowerRowTribute),
                new ArrayList<>(upperRowTribute),
                new ArrayList<>(lowerRowBuildings),
                new ArrayList<>(upperRowBuildings)
        );
    }

    /**
     * Fills the first tribute rows at the start of the game.
     * Event cards and character cards are separated while drawing.
     */
    public void setupInitialRows() {
        int lowerTarget = game.getPlayers().size() + 1;
        int upperTarget = game.getPlayers().size() + 4;

        // Fill the lower row first.
        while (getLowerTributeRowSize() < lowerTarget) {
            Card drawnCard = game.drawTribeCard();
            if (drawnCard == null) {
                throw new IllegalStateException("Tribute deck is empty during initial setup");
            }

            addTribeCardToRows(drawnCard, upperRowEventCards, lowerRowTribute, "tribute deck");
        }

        // Then fill the upper row.
        while (getUpperTributeRowSize() < upperTarget) {
            Card drawnCard = game.drawTribeCard();
            if (drawnCard == null) {
                throw new IllegalStateException("Tribute deck is empty during initial setup");
            }

            addTribeCardToRows(drawnCard, upperRowEventCards, upperRowTribute, "tribute deck");
        }
    }

    /**
     * Places the first visible buildings in the upper row.
     */
    public void setupInitialBuildings(List<BuildingCard> buildings) {
        upperRowBuildings = new ArrayList<>(buildings);
    }



    /**
     * Places a player's totem on an offer tile and remembers the choice.
     * The saved choice is later used during action resolution.
     */
    public boolean placeTotemOffer(Player player, OfferTile chosenTile) {
        int numPlayers = game.getPlayers().size();
        if (!chosenTile.isAvailableFor(numPlayers))
            return false;
        if (chosenTile.isOccupied())
            return false;
        removeTotemFromTurnOrder(player.getTotem());
        chosenTile.placeTotem(player.getTotem());
        // Save the choice for the action resolution phase.
        playerChoices.put(player, chosenTile);
        return true;
    }

    /**
     * Removes a player's current offer choice, if they had one.
     * Used when a player disconnects before their action is resolved.
     */
    public boolean clearOfferChoiceForPlayer(Player player) {
        if (player == null) {
            return false;
        }

        OfferTile chosenTile = playerChoices.remove(player);
        if (chosenTile == null) {
            return false;
        }

        if (chosenTile.getTotemSlot() == player.getTotem()) {
            chosenTile.removeTotem();
        }
        return true;
    }

    /**
     * Puts a disconnected player's totem back into the lowest free turn-order slot.
     * This keeps the board consistent while the player is not active.
     */
    public boolean returnTotemToLowestFreeTurnOrderSlot(Player player) {
        if (player == null || player.getTotem() == null) {
            return false;
        }

        Board board = game.getBoard();
        TurnOrderTile turnOrderTile = board.getTurnOrderTile();
        for (TurnSlot slot : turnOrderTile.getSlots()) {
            if (slot.getOccupiedBy() == player.getTotem()) {
                return false;
            }
        }

        removeTotemFromOfferTiles(player.getTotem());

        List<TurnSlot> slots = new ArrayList<>(turnOrderTile.getSlots());
        slots.sort(Comparator.comparingInt(TurnSlot::getPosition).reversed());
        for (TurnSlot slot : slots) {
            if (!slot.isOccupied()) {
                slot.placeTotem(player.getTotem());
                return true;
            }
        }
        return false;
    }

    /**
     * Places a player back into the turn order and applies the slot reward.
     * Food and point changes are reported through game listeners.
     */
    public void placeTotemOrderForPlayer(Player player) {
        Board board = game.getBoard();
        TurnOrderTile turnOrderTile = board.getTurnOrderTile();

        TurnSlot targetSlot = null;
        for (TurnSlot slot : turnOrderTile.getSlots()) {
            if (!slot.isOccupied()) {
                targetSlot = slot;
                break;
            }
        }
        if (targetSlot == null) return;

        removeTotemFromOfferTiles(player.getTotem());
        targetSlot.placeTotem(player.getTotem());

        int foodBefore = player.getFood();
        int ppBefore   = player.getPrestigePoints();

        targetSlot.getEffect().apply(player);

        // This building bonus is applied only on effects that explicitly allow it.
        if (targetSlot.getEffect().triggersTotemFoodBonus()) {
            for (BuildingCard building : player.getTribe().getBuildingCards()) {
                applyTotemSlotFoodBonus(player, building);
            }
        }

        int foodDelta = player.getFood() - foodBefore;
        int ppDelta   = player.getPrestigePoints() - ppBefore;
        if (foodDelta != 0) {
            game.notifyListeners(l -> l.onFoodChanged(player.getNickname(), player.getFood(), foodDelta));
        }
        if (ppDelta != 0) {
            game.notifyListeners(l -> l.onPointsChanged(player.getNickname(), player.getPrestigePoints(), ppDelta));
        }
        game.notifyListeners(l -> l.onGameSnapshotChanged());
    }

    private void removeTotemFromTurnOrder(Totem totem) {
        if (totem == null) {
            return;
        }
        TurnOrderTile turnOrderTile = game.getBoard().getTurnOrderTile();
        for (TurnSlot slot : turnOrderTile.getSlots()) {
            if (slot.getOccupiedBy() == totem) {
                slot.removeTotem();
                return;
            }
        }
    }

    private void removeTotemFromOfferTiles(Totem totem) {
        if (totem == null) {
            return;
        }
        for (OfferTile tile : game.getBoard().getOfferTiles()) {
            if (tile.getTotemSlot() == totem) {
                tile.removeTotem();
                return;
            }
        }
    }
    /**
     * Returns events in the exact order in which they must be resolved.
     * Lower-row events always resolve; upper-row events are added only when requested.
     */
    public List<EventCard> getOrderedEventsForResolution(boolean includeUpperRow) {
        List<EventCard> eventsToResolve = new ArrayList<>();

        // Lower-row events always resolve.
        eventsToResolve.addAll(lowerRowEventCards);

        // Upper-row events resolve only when requested.
        if (includeUpperRow) {
            eventsToResolve.addAll(upperRowEventCards);
        }

        // Sustenance events must be kept for the end.
        List<EventCard> normalEvents = new ArrayList<>();
        List<EventCard> sustenanceEvents = new ArrayList<>();

        for (EventCard e : eventsToResolve) {
            if (isSustenanceEvent(e)) {
                sustenanceEvents.add(e);
            } else {
                normalEvents.add(e);
            }
        }

        // Sort by era to keep the order stable.
        normalEvents.sort(Comparator.comparing(EventCard::getEra));
        sustenanceEvents.sort(Comparator.comparing(EventCard::getEra));

        // Normal events first, sustenance events last.
        List<EventCard> ordered = new ArrayList<>();
        ordered.addAll(normalEvents);
        ordered.addAll(sustenanceEvents);

        return ordered;
    }

    /**
     * Returns every card currently visible in the upper row.
     */
    public List<Card> getCurrentBoardCardsUp() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(upperRowTribute);
        cards.addAll(upperRowEventCards);
        cards.addAll(upperRowBuildings);
        return cards;
    }

    /**
     * Returns every card currently visible in the lower row.
     */
    public List<Card> getCurrentBoardCardsLow() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(lowerRowTribute);
        cards.addAll(lowerRowEventCards);
        cards.addAll(lowerRowBuildings);
        return cards;
    }

    /**
     * Removes all tribute and event cards from the lower row.
     */
    public void discardLowerRow() {
        lowerRowEventCards.clear();
        lowerRowTribute.clear();
    }

    /**
     * Moves upper tribute and event cards down to the lower row.
     */
    public void shiftUpperToLowerRow() {
        lowerRowEventCards = new ArrayList<>(upperRowEventCards);
        lowerRowTribute = new ArrayList<>(upperRowTribute);

        upperRowEventCards.clear();
        upperRowTribute.clear();
    }

    public int getUpperTributeRowSize() {
        return upperRowTribute.size() + upperRowEventCards.size();
    }

    public int getLowerTributeRowSize() {
        return lowerRowTribute.size() + lowerRowEventCards.size();
    }

    /**
     * Refills the upper tribute row from the deck.
     * If the deck is empty, the row simply remains smaller.
     */
    public void refillUpperRow() {
        int targetSize = game.getPlayers().size() + 4;

        while (getUpperTributeRowSize() < targetSize) {
            Card drawnCard = game.drawTribeCard();
            if (drawnCard == null) {
                return;
            }

            addTribeCardToRows(drawnCard, upperRowEventCards, upperRowTribute, "tribute deck");
        }
    }

    /**
     * Checks if a newly revealed card should move the game to the next era.
     * A card from the next era appearing in the upper row triggers the transition.
     */
    public void checkEraTransition() {
        Era before = game.getCurrentEra();

        for (CharacterCard card : upperRowTribute) {
            if (before == Era.I && card.getEra() == Era.II)
            { game.endEra(); notifyEra(before); return; }
            if (before == Era.II && card.getEra() == Era.III)
            { game.endEra(); notifyEra(before); return; }
        }
        for (EventCard eventCard : upperRowEventCards) {
            if (before == Era.I && eventCard.getEra() == Era.II)
            { game.endEra(); notifyEra(before); return; }
            if (before == Era.II && eventCard.getEra() == Era.III)
            { game.endEra(); notifyEra(before); return; }
        }
    }
    private void notifyEra(Era before) {
        Era after = game.getCurrentEra();
        if (after != before) game.notifyListeners(l -> l.onEraChanged(after));
    }

    /**
     * Moves upper buildings to the lower building row.
     */
    public void moveUpperBuildingsToLower() {
        lowerRowBuildings = upperRowBuildings;
        upperRowBuildings = new ArrayList<>();
    }
    /**
     * Removes the old lower-row buildings.
     */
    public void discardLowerBuildings() {
        lowerRowBuildings.clear();
    }
    /**
     * Reveals the buildings for the new era.
     */
    public void revealBuildingsForEra(List<BuildingCard> buildings) {
        upperRowBuildings = new ArrayList<>(buildings);
    }
    public Board getBoard() {
        return game.getBoard();
    }

    public Map<Player, OfferTile> getPlayerChoices() {
        return playerChoices;
    }

    private void addTribeCardToRows(Card card,
                                    List<EventCard> eventRow,
                                    List<CharacterCard> tributeRow,
                                    String sourceName) {
        card.accept(new CardVisitor<Void>() {
            @Override public Void visitEvent(EventCard eventCard) {
                eventRow.add(eventCard);
                return null;
            }

            @Override public Void visitCharacter(CharacterCard characterCard) {
                tributeRow.add(characterCard);
                return null;
            }

            @Override public Void visitCard(Card card) {
                throw new IllegalStateException("Unexpected card type in "
                        + sourceName + ": " + card.getClass().getSimpleName());
            }
        });
    }

    private boolean isSustenanceEvent(EventCard eventCard) {
        return Boolean.TRUE.equals(eventCard.accept(new CardVisitor<>() {
            @Override public Boolean visitSustenanceEvent(SustenanceEventCard card) {
                return true;
            }

            @Override public Boolean visitCard(Card card) {
                return false;
            }
        }));
    }

    private void applyTotemSlotFoodBonus(Player player, BuildingCard building) {
        building.acceptEffect(new EffectVisitor<Void>() {
            @Override public Void visitTotemSlotFoodBonus(TotemSlotFoodBonusEffect effect) {
                effect.apply(player);
                return null;
            }

            @Override public Void visitEffect(Effect effect) {
                return null;
            }
        });
    }
}
