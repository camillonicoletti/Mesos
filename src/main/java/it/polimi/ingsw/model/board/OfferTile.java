package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Totem;

import java.io.Serializable;
import java.util.List;

/**
 * Represents one offer tile on the board.
 * A player places a totem here to choose the action slots printed on the tile.
 */
public class OfferTile implements Serializable {
    private static final long serialVersionUID = 1L;
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

    /**
     * Returns true if this tile is used with the current number of players.
     */
    public boolean isAvailableFor(int numPlayers) {
        return numPlayers >= minPlayers;
    }

    /**
     * Returns true when a player's totem is already on this tile.
     */
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

    /**
     * Places a totem on this tile during the offer phase.
     */
    public void placeTotem(Totem totem) {
        this.totemSlot = totem;
        totem.setPosition("OFFER_TRACK");
    }

    /**
     * Removes and returns the totem currently placed on this tile.
     */
    public Totem removeTotem() {
        Totem removed = this.totemSlot;
        this.totemSlot = null;
        return removed;
    }














}

