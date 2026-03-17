package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.SlotType;

import java.util.List;
import java.util.ArrayList;

public class Round {

    private final int number;
    private final Game game;
    private RoundPhase phase;
    private final Era era;
    private List<Event> lowerRowEvents;
    private List<Event> upperRowEvents;
    private List<CharacterCard> lowerRowTribute;
    private List<CharacterCard> upperRowTribute;
    private List<BuildingCard> lowerRowBuildings;
    private List<BuildingCard> upperRowBuildings;

    public Round(int number, Game game, RoundPhase phase, Era era,
                 List<Event> lowerRowEvents, List<Event> upperRowEvents,
                 List<CharacterCard> lowerRowTribute, List<CharacterCard> upperRowTribute,
                 List<BuildingCard> lowerRowBuildings, List<BuildingCard> upperRowBuildings) {
        this.number = number;
        this.game = game;
        this.phase = phase;
        this.era = era;
        this.lowerRowEvents = (lowerRowEvents != null) ? lowerRowEvents : new ArrayList<>();
        this.upperRowEvents = (upperRowEvents != null) ? upperRowEvents : new ArrayList<>();
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

    public List<Event> getLowerRowEvents() {
        return lowerRowEvents;
    }

    public List<Event> getUpperRowEvents() {
        return upperRowEvents;
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
        phase = RoundPhase.TOTEM_PLACEMENT;
        placeTotemOffer();

        phase = RoundPhase.ACTION_RESOLUTION;
        resolveOfferAction();

        phase = RoundPhase.TURN_ORDER;
        placeTotemOrder();

        phase = RoundPhase.EVENT_RESOLUTION;
        resolveEvent();

        phase = RoundPhase.ROUND_CLEANUP;
        discardLowerRow();
        shiftUpperToLowerRow();
        refillUpperRow();
        checkEraTransition();

    }

    public Round createNextRound(int nextRoundNumber) {
        return new Round(
                nextRoundNumber,
                game,
                RoundPhase.TOTEM_PLACEMENT,
                game.getCurrentEra(),
                new ArrayList<>(lowerRowEvents),
                new ArrayList<>(upperRowEvents),
                new ArrayList<>(lowerRowTribute),
                new ArrayList<>(upperRowTribute),
                new ArrayList<>(lowerRowBuildings),
                new ArrayList<>(upperRowBuildings)
        );
    }

    private void drawCardToUpperRow(List<Card> deck) {
        Card drawn = deck.remove(0);

        if (drawn instanceof Event event) {
            upperRowEvents.add(event);
        } else if (drawn instanceof CharacterCard characterCard) {
            upperRowTribute.add(characterCard);
        } else {
            throw new IllegalStateException("Unexpected card type: " + drawn.getClass().getSimpleName());
        }
    }

    private void drawCardToLowerRow(List<Card> deck) {
        Card drawn = deck.remove(0);

        if (drawn instanceof Event event) {
            lowerRowEvents.add(event);
        } else if (drawn instanceof CharacterCard characterCard) {
            lowerRowTribute.add(characterCard);
        } else {
            throw new IllegalStateException("Unexpected card type: " + drawn.getClass().getSimpleName());
        }
    }

    public void setupInitialRows() {
        List<Card> tributeDeck = game.getTributeDeck();

        int upperTarget = game.getPlayers().size() + 4;
        int lowerTarget = game.getPlayers().size() + 1;

        while (getUpperTributeRowSize() < upperTarget && !tributeDeck.isEmpty()) {
            drawCardToUpperRow(tributeDeck);
        }

        while (getLowerTributeRowSize() < lowerTarget && !tributeDeck.isEmpty()) {
            drawCardToLowerRow(tributeDeck);
        }
    }

    public boolean placeTotemOffer(Player player, OfferTile chosenTile) {
        int numPlayers = game.getPlayers().size();

        if (!chosenTile.isAvailableFor(numPlayers)) {
            return false;
        }
        if (chosenTile.isOccupied()) {
            return false;
        }
        chosenTile.placeTotem(player.getTotem());
        return true;
    }


    public void resolveOfferAction(Player player, OfferTile chosenTile) {
        for (ActionSlot slot : chosenTile.getSlots()) {
            SlotType type = slot.getType();
            if (type == SlotType.UPPER_ROW || type == SlotType.LOWER_ROW) {
                int amount = slot.getAmount();
                // Select the correct row and draw the first `amount` cards, giving each one to the player
                List<CharacterCard> row = (type == SlotType.UPPER_ROW) ? upperRowTribute : lowerRowTribute;
                for (int i = 0; i < amount && !row.isEmpty(); i++) {
                    player.addCharacterCard(row.remove(0));
                }
            } else if (type == SlotType.FOOD) {
                player.addFood(slot.getAmount());
            }
        }
    }


    public void placeTotemOrder() {
        Board board = game.getBoard(); //TODO: modify Game.java
        board.getTurnOrderTile().assignTurnOrder(board);
    }


    public List<Event> collectEvent() {
        List<Event> events = new ArrayList<>();
        events.addAll(lowerRowEvents);
        events.addAll(upperRowEvents);
        return events;
    }

    public void resolveEvent() {
        List<Event> events = collectEvent();

        for (Event event : events){
            event.apply(game);
        }
    }

    public List<Card> getCurrentBoardCardsUp() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(upperRowTribute);
        cards.addAll(upperRowEvents);
        cards.addAll(upperRowBuildings);
        return cards;
    }

    public List<Card> getCurrentBoardCardsLow() {
        List<Card> cards = new ArrayList<>();
        cards.addAll(lowerRowTribute);
        cards.addAll(lowerRowEvents);
        cards.addAll(lowerRowBuildings);
        return cards;
    }

    public void discardLowerRow() {
        lowerRowEvents.clear();
        lowerRowTribute.clear();
    }

    public void shiftUpperToLowerRow() {
        lowerRowEvents = upperRowEvents;
        lowerRowTribute = upperRowTribute;

        upperRowEvents = new ArrayList<>();
        upperRowTribute = new ArrayList<>();
    }

    public int getUpperTributeRowSize() {
        return upperRowTribute.size() + upperRowEvents.size();
    }

    public int getLowerTributeRowSize() {
        return lowerRowTribute.size() + lowerRowEvents.size();
    }

    public void refillUpperRow() {
        List<Card> tributeDeck = game.getTributeDeck();
        int targetSize = game.getPlayers().size() + 4;

        while (getUpperTributeRowSize() < targetSize && !tributeDeck.isEmpty()) {
            Card drawnCard = tributeDeck.remove(0);

            if (drawnCard instanceof Event event) {
                upperRowEvents.add(event);
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

        for (Event event : upperRowEvents) {
            if (currentEra == Era.I && event.getEra() == Era.II) {
                game.endEra();
                return;
            }
            if (currentEra == Era.II && event.getEra() == Era.III) {
                game.endEra();
                return;
            }
        }
    }

    public void setupInitialBuildings(List<BuildingCard> buildings) {
        upperRowBuildings = new ArrayList<>(buildings);
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
