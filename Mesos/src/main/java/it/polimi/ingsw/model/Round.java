package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.SlotType;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;


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
        // allowing execute() to resolve the correct action for each player
        phase = RoundPhase.ACTION_RESOLUTION;
        for (Map.Entry<Player, OfferTile> entry : playerChoices.entrySet()) {
            resolveOfferAction(entry.getKey(), entry.getValue());
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
        Board board = game.getBoard();
        board.getTurnOrderTile().assignTurnOrder(board);
    }


    public List<EventCard> collectEvent() {
        List<EventCard> eventCards = new ArrayList<>();
        eventCards.addAll(lowerRowEventCards);
        eventCards.addAll(upperRowEventCards);
        return eventCards;
    }

    public void resolveEvent() {
        List<EventCard> eventCards = collectEvent();

        for (EventCard eventCard : eventCards){
            eventCard.apply(game);
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
        lowerRowEventCards = upperRowEventCards;
        lowerRowTribute = upperRowTribute;

        upperRowEventCards = new ArrayList<>();
        upperRowTribute = new ArrayList<>();
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
