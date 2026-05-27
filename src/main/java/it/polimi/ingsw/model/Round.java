package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.*;
import it.polimi.ingsw.model.card.*;
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.TotemSlotFoodBonusEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import java.util.Collections;

import java.io.Serializable;
import java.util.*;


/**
 * Per-round state of a Mesos match: the lower/upper rows of tribute, event and
 * building cards, the round number and phase, and the per-player offer-tile
 * choices made during totem placement. Provides the operations that mutate
 * these rows (setup, refill, era transition, discard) and the deterministic
 * event-resolution ordering consumed by {@link Game}.
 */
public class Round implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int number;
    private final Game game;
    private RoundPhase phase;
    private final Era era;
    private List<EventCard> lowerRowEventCards;
    private List<EventCard> upperRowEventCards;
    private List<CharacterCard> lowerRowTribute;
    private List<CharacterCard> upperRowTribute;
    private List<BuildingCard> lowerRowBuildings;
    private List<BuildingCard> upperRowBuildings;
    private Map<Player, OfferTile> playerChoices = new LinkedHashMap<>();


    public Round(int number, Game game, RoundPhase phase, Era era,
                 List<EventCard> lowerRowEventCards, List<EventCard> upperRowEventCards,
                 List<CharacterCard> lowerRowTribute, List<CharacterCard> upperRowTribute,
                 List<BuildingCard> lowerRowBuildings, List<BuildingCard> upperRowBuildings) {
        this.number = number;
        this.game = game;
        this.phase = phase;
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

    public RoundPhase getPhase() {
        return phase;
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

    List<EventCard> mutableLowerRowEvents() {
        return lowerRowEventCards;
    }

    List<EventCard> mutableUpperRowEvents() {
        return upperRowEventCards;
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
    public Round createNextRound(int nextRoundNumber) {
        return new Round(
                nextRoundNumber,
                game,
                RoundPhase.TOTEM_PLACEMENT,
                game.getCurrentEra(),
                new ArrayList<>(lowerRowEventCards),
                new ArrayList<>(upperRowEventCards),
                new ArrayList<>(lowerRowTribute),
                new ArrayList<>(upperRowTribute),
                new ArrayList<>(lowerRowBuildings),
                new ArrayList<>(upperRowBuildings)
        );
    }

    public void setupInitialRows() {
        int lowerTarget = game.getPlayers().size() + 1;
        int upperTarget = game.getPlayers().size() + 4;

        // 1. Fill the lower row up to target size
        while (getLowerTributeRowSize() < lowerTarget) {
            Card drawnCard = game.drawTribeCard();
            if (drawnCard == null) {
                throw new IllegalStateException("Tribute deck is empty during initial setup");
            }

            addTribeCardToRows(drawnCard, upperRowEventCards, lowerRowTribute, "tribute deck");
        }

        // 2. Fill the upper row up to target size
        while (getUpperTributeRowSize() < upperTarget) {
            Card drawnCard = game.drawTribeCard();

            addTribeCardToRows(drawnCard, upperRowEventCards, upperRowTribute, "tribute deck");
        }
    }

    public void setupInitialBuildings(List<BuildingCard> buildings) {
        upperRowBuildings = new ArrayList<>(buildings);
    }

    private void drawCardToUpperRow(List<Card> deck) {
        Card drawn = deck.remove(0);
        addTribeCardToRows(drawn, upperRowEventCards, upperRowTribute, "tribute deck");
    }

    private void drawCardToLowerRow(List<Card> deck) {
        Card drawn = deck.remove(0);
        addTribeCardToRows(drawn, lowerRowEventCards, lowerRowTribute, "tribute deck");
    }


    public boolean placeTotemOffer(Player player, OfferTile chosenTile) {
        int numPlayers = game.getPlayers().size();
        if (!chosenTile.isAvailableFor(numPlayers))
            return false;
        if (chosenTile.isOccupied())
            return false;
        removeTotemFromTurnOrder(player.getTotem());
        chosenTile.placeTotem(player.getTotem());
        // Saves the player's choice so that execute() can resolve actions for each player
        // using their chosen OfferTile during ACTION_RESOLUTION phase
        playerChoices.put(player, chosenTile);
        return true;
    }

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

        // TotemSlotFoodBonusEffect applies only on food-reward slots.
        if (targetSlot.getEffect() instanceof GainFoodSlotEffect) {
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



    public List<EventCard> getOrderedEventsForResolution(boolean includeUpperRow) {
        List<EventCard> eventsToResolve = new ArrayList<>();

        // 1. Collect lower-row events
        eventsToResolve.addAll(lowerRowEventCards);

        // 2. On the last round, also include upper-row events
        if (includeUpperRow) {
            eventsToResolve.addAll(upperRowEventCards);
        }

        // 3. Sustenance events are always resolved last
        List<EventCard> normalEvents = new ArrayList<>();
        List<EventCard> sustenanceEvents = new ArrayList<>();

        for (EventCard e : eventsToResolve) {
            if (isSustenanceEvent(e)) {
                sustenanceEvents.add(e);
            } else {
                normalEvents.add(e);
            }
        }

        // 4. Sort each group by Era for deterministic resolution order
        normalEvents.sort(Comparator.comparing(EventCard::getEra));
        sustenanceEvents.sort(Comparator.comparing(EventCard::getEra));

        // 5. Reassemble: normal events first, sustenance last
        List<EventCard> ordered = new ArrayList<>();
        ordered.addAll(normalEvents);
        ordered.addAll(sustenanceEvents);

        return ordered;
    }

    public void resolveEvent() {
        boolean includeUpperRow = game.isLastRound();
        List<EventCard> eventCards = getOrderedEventsForResolution(includeUpperRow);

        for (EventCard eventCard : eventCards) {
            eventCard.resolve(game);
        }
    }

    public List<Card> getCurrentBoardCardsUp() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(upperRowTribute);
        cards.addAll(upperRowEventCards);
        cards.addAll(upperRowBuildings);
        return cards;
    }

    public List<Card> getCurrentBoardCardsLow() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(lowerRowTribute);
        cards.addAll(lowerRowEventCards);
        cards.addAll(lowerRowBuildings);
        return cards;
    }

    public void discardLowerRow() {
        lowerRowEventCards.clear();
        lowerRowTribute.clear();
    }

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

    // An era ends when a card from the next era appears in the upper row after refill.
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

    public void moveUpperBuildingsToLower() {
        lowerRowBuildings = upperRowBuildings;
        upperRowBuildings = new ArrayList<>();
    }
    public void discardLowerBuildings() {
        lowerRowBuildings.clear();
    }
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
