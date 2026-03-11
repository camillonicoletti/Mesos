package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.PlayerCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;

import java.util.List;

public class Round {

    private int number;
    private Game game;
    private RoundPhase phase;
    private Era era;
    private List<Event> lowerRowEvents;
    private List<Event> upperRowEvents;
    private List<CharacterCard> lowerRowTribute;
    private List<CharacterCard> upperRowTribute;

    public Round(int number, Game game, RoundPhase phase, Era era,
                 List<Event> lowerRowEvents, List<Event> upperRowEvents, List<CharacterCard> lowerRowTribute, List<CharacterCard> upperRowTribute) {
        this.number = number;
        this.game = game;
        this.phase = phase;
        this.era = era;
        this.lowerRowEvents = lowerRowEvents;
        this.upperRowEvents = upperRowEvents;
        this.lowerRowTribute = lowerRowTribute;
        this.upperRowTribute = upperRowTribute;
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

    public void execute() {
        // TODO: implement round flow
    }

    public void placeTotemOffer() {
        // TODO: implement totem placement on offer track
    }

    public void resolveOfferAction() {
        // TODO: implement offer action resolution
    }

    public void placeTotemOrder() {
        // TODO: implement totem placement on turn order track
    }

    public List<Event> collectEvent() {
        // TODO: collect visible events
        return null;
    }

    public void resolveEvent() {
        // TODO: resolve events
    }

    public List<PlayerCard> getCurrentBoardCardUp() {
        // TODO: replace Object with Card when Card model is ready
        return null;
    }

    public List<PlayerCard> getCurrentBoardCardLow() {
        // TODO: replace Object with Card when Card model is ready
        return null;
    }

    public void discardLowreRow() {
        //TODO: Discard Tribute Lower Row
    }

    public void shiftUpperToLowerRow() {
        // TODO: shift cards from upper row to lower row
    }

    public void refillUpperRow() {
        // TODO: refill upper row
    }

    public void checkEraTransition() {
        // TODO: implement era transition check
    }
}
