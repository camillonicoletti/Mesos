package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnSlot;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.enums.EventType;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.SlotType;
import it.polimi.ingsw.model.Totem;

import java.util.*;


public class Round {

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

    public List<EventCard> getLowerRowEvents() {
        return lowerRowEventCards;
    }

    public List<EventCard> getUpperRowEvents() {
        return upperRowEventCards;
    }

    public List<CharacterCard> getLowerRowTribute() {
        return lowerRowTribute;
    }

    public List<CharacterCard> getUpperRowTribute() {
        return upperRowTribute;
    }

    public List<BuildingCard> getUpperRowBuildings(){return upperRowBuildings; }

    public List<BuildingCard> getLowerRowBuildings(){return lowerRowBuildings;}

    public void execute() {

        // playerChoices maps each player to the OfferTile they selected during TOTEM_PLACEMENT,
        // allowing execute() to resolve the correct action for each player;
        // chosenCards is empty until the controller is implemented —
        // the controller will pass the actual cards chosen by the player
        phase = RoundPhase.ACTION_RESOLUTION;
        for (Map.Entry<Player, OfferTile> entry : getOfferResolutionOrder()) {
            resolveOfferAction(entry.getKey(), entry.getValue(), new ArrayList<>());
        }

        phase = RoundPhase.TURN_ORDER;
        placeTotemOrder();
        phase = RoundPhase.EVENT_RESOLUTION;
        resolveEvent();
        phase = RoundPhase.ROUND_CLEANUP;
        discardLowerRow();
        shiftUpperToLowerRow();
        refillUpperRow();
        checkEraTransition();
        // remove all totems from offer tiles for the next round
        game.getBoard().resetOfferTiles();
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

        // 1. riempi la fila inferiore
        while (getLowerTributeRowSize() < lowerTarget) {
            Card drawnCard = game.drawTribeCard();

            if (drawnCard instanceof EventCard eventCard) {
                upperRowEventCards.add(eventCard);
            } else if (drawnCard instanceof CharacterCard characterCard) {
                lowerRowTribute.add(characterCard);
            }
        }

        // 2. riempi la fila superiore
        while (getUpperTributeRowSize() < upperTarget) {
            Card drawnCard = game.drawTribeCard();

            if (drawnCard instanceof EventCard eventCard) {
                upperRowEventCards.add(eventCard);
            } else if (drawnCard instanceof CharacterCard characterCard) {
                upperRowTribute.add(characterCard);
            }
        }
    }

    public void setupInitialBuildings(List<BuildingCard> buildings) {
        upperRowBuildings = new ArrayList<>(buildings);
    }

    private void drawCardToUpperRow(List<Card> deck) {
        Card drawn = deck.remove(0);

        if (drawn instanceof EventCard eventCard) {
            upperRowEventCards.add(eventCard);
        } else if (drawn instanceof CharacterCard characterCard) {
            upperRowTribute.add(characterCard);
        } else {
            throw new IllegalStateException("Unexpected card type: " + drawn.getClass().getSimpleName());
        }
    }

    private void drawCardToLowerRow(List<Card> deck) {
        Card drawn = deck.remove(0);

        if (drawn instanceof EventCard eventCard) {
            lowerRowEventCards.add(eventCard);
        } else if (drawn instanceof CharacterCard characterCard) {
            lowerRowTribute.add(characterCard);
        } else {
            throw new IllegalStateException("Unexpected card type: " + drawn.getClass().getSimpleName());
        }
    }


    public boolean placeTotemOffer(Player player, OfferTile chosenTile) {
        int numPlayers = game.getPlayers().size();
        if (!chosenTile.isAvailableFor(numPlayers))
            return false;
        if (chosenTile.isOccupied())
            return false;
        chosenTile.placeTotem(player.getTotem());
        // Saves the player's choice so that execute() can resolve actions for each player
        // using their chosen OfferTile during ACTION_RESOLUTION phase
        playerChoices.put(player, chosenTile);
        return true;
    }



    public void resolveOfferAction(Player player, OfferTile chosenTile, List<Card> chosenCards) {
        for (ActionSlot slot : chosenTile.getSlots()) {
            SlotType type = slot.getType();

            if (type == SlotType.FOOD) {
                // give the player the food indicated on the slot
                player.addFood(slot.getAmount());

            } else if (type == SlotType.UPPER_ROW || type == SlotType.LOWER_ROW) {
                List<CharacterCard> tributeRow = (type == SlotType.UPPER_ROW) ? upperRowTribute : lowerRowTribute;
                List<BuildingCard> buildingRow = (type == SlotType.UPPER_ROW) ? upperRowBuildings : lowerRowBuildings;

                int amount = slot.getAmount();
                int taken = 0;

                // iterate over the cards chosen by the player, stopping once the slot limit is reached
                for (Card chosen : chosenCards) {
                    if (taken >= amount) break;


                    if (chosen instanceof CharacterCard cc && tributeRow.contains(cc)) {
                        // character card: free, just add to tribe
                        tributeRow.remove(cc);
                        player.addCharacterCard(cc);
                        taken++;

                    } else if (chosen instanceof BuildingCard bc && buildingRow.contains(bc)) {
                        // building card: pay food cost minus builder discount
                        int actualCost = Math.max(0, bc.getFoodCost() - player.getTribe().getBuildingDiscount());
                        if (player.canAfford(actualCost)) {
                            player.spendFood(actualCost);
                            buildingRow.remove(bc);
                            player.addBuildingCard(bc);
                            //bc.getEffect().apply(player); // apply immediate building effect
                            taken++;
                        }
                        // if cannot afford, skip silently — controller should validate before calling
                    }
                }
            }
        }
    }



    public void placeTotemOrder() {
        Board board = game.getBoard();
        board.getTurnOrderTile().assignTurnOrder(board);

        // Build a map Totem → Player to find the owner of each occupied slot
        Map<Totem, Player> totemOwner = new HashMap<>();
        for (Player p : game.getPlayers()) {
            totemOwner.put(p.getTotem(), p);
        }

        // Apply the effect of each occupied TurnSlot to its owner
        for (TurnSlot slot : board.getTurnOrderTile().getSlots()) {
            if (!slot.isOccupied()) continue;
            Player owner = totemOwner.get(slot.getOccupiedBy());
            if (owner == null) continue;

            switch (slot.getEffect()) {
                case GAIN_FOOD -> owner.addFood(slot.getEffectValue());
                case PAY_FOOD  -> owner.applyTurnPenalty(slot.getEffectValue(), slot.getPenaltyPoints());
                case NONE      -> {}
            }
        }
    }

    private List<Map.Entry<Player, OfferTile>> getOfferResolutionOrder() {
        Board board = game.getBoard();

        Map<Totem, Player> totemOwner = new HashMap<>();
        for (Player player : game.getPlayers()) {
            totemOwner.put(player.getTotem(), player);
        }

        List<Map.Entry<Player, OfferTile>> orderedChoices = new ArrayList<>();

        List<OfferTile> orderedTiles = new ArrayList<>(board.getOfferTiles());
        orderedTiles.sort(Comparator.comparing(OfferTile::getLetter));

        for (OfferTile tile : orderedTiles) {
            if (!tile.isOccupied()) {
                continue;
            }

            Player owner = totemOwner.get(tile.getTotemSlot());
            if (owner != null) {
                orderedChoices.add(Map.entry(owner, tile));
            }
        }

        return orderedChoices;
    }

    private List<EventCard> getOrderedEventsForResolution(boolean includeUpperRow) {
        List<EventCard> eventsToResolve = new ArrayList<>();

        // 1. prendo eventi lower
        eventsToResolve.addAll(lowerRowEventCards);

        // 2. eventualmente aggiungo upper
        if (includeUpperRow) {
            eventsToResolve.addAll(upperRowEventCards);
        }

        // 3. separo sustenance dagli altri
        List<EventCard> normalEvents = new ArrayList<>();
        List<EventCard> sustenanceEvents = new ArrayList<>();

        for (EventCard e : eventsToResolve) {
            if (e.getEventType() == EventType.SUSTENANCE) {
                sustenanceEvents.add(e);
            } else {
                normalEvents.add(e);
            }
        }

        // 4. ordino per era
        normalEvents.sort(Comparator.comparing(EventCard::getEra));
        sustenanceEvents.sort(Comparator.comparing(EventCard::getEra));

        // 5. ricompongo: prima normali, poi sustenance
        List<EventCard> ordered = new ArrayList<>();
        ordered.addAll(normalEvents);
        ordered.addAll(sustenanceEvents);

        return ordered;
    }

    public void resolveEvent() {
        boolean includeUpperRow = game.isGameOver();
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
        List<Card> tributeDeck = game.getTributeDeck();
        int targetSize = game.getPlayers().size() + 4;

        while (getUpperTributeRowSize() < targetSize && !tributeDeck.isEmpty()) {
            Card drawnCard = tributeDeck.remove(0);

            if (drawnCard instanceof EventCard eventCard) {
                upperRowEventCards.add(eventCard);
            } else if (drawnCard instanceof CharacterCard characterCard) {
                upperRowTribute.add(characterCard);
            } else {
                throw new IllegalStateException("Unexpected card type in tribute deck: " + drawnCard.getClass().getSimpleName());
            }
        }
    }

    public void checkEraTransition() {
        Era currentEra = game.getCurrentEra();

        for (CharacterCard card : upperRowTribute) {
            if (currentEra == Era.I && card.getEra() == Era.II) {
                game.endEra();
                return;
            }
            if (currentEra == Era.II && card.getEra() == Era.III) {
                game.endEra();
                return;
            }
        }

        for (EventCard eventCard : upperRowEventCards) {
            if (currentEra == Era.I && eventCard.getEra() == Era.II) {
                game.endEra();
                return;
            }
            if (currentEra == Era.II && eventCard.getEra() == Era.III) {
                game.endEra();
                return;
            }
        }
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
}
