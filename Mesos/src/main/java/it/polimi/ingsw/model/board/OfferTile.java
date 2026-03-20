package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.enums.TotemPosition;

import java.util.List;

public class OfferTile {
    private char letter;
    private int minPlayers;
    private Totem totemSlot;
    private List<ActionSlot> slots;

    public OfferTile(char letter, int minPlayers, List<ActionSlot> slots) {
        this.letter = letter;
        this.minPlayers = minPlayers;
        this.totemSlot = null;
        this.slots = slots;
    }

    public boolean isAvailableFor(int numPlayers) {
        return numPlayers >= minPlayers;
    }

    public boolean isOccupied() {
        return totemSlot != null;
    }

    public char getLetter() {
        return letter;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public List<ActionSlot> getSlots() {
        return slots;
    }

    public Totem getTotemSlot() {
        return totemSlot;
    }

    public void placeTotem(Totem totem) {
        this.totemSlot = totem;
        totem.setPosition(TotemPosition.OFFER_TRACK);
    }

    public Totem removeTotem() {
        Totem removed = this.totemSlot;
        this.totemSlot = null;
        return removed;
    }














}


