package it.polimi.ingsw.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class TurnOrderTile {

    private int numPlayers;
    private List<TurnSlot> slots;

    public TurnOrderTile(int numPlayers, List<TurnSlot> slots) {
        this.numPlayers = numPlayers;
        this.slots = slots;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public List<TurnSlot> getSlots() {
        return slots;
    }

    public TurnSlot getNextFreeSlot() {
        for (TurnSlot slot : slots) {
            if (!slot.isOccupied()) {
                return slot;
            }
        }
        return null;
    }

    public boolean isFull() {
        for (TurnSlot slot : slots) {
            if (!slot.isOccupied()) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        for (TurnSlot slot : slots) {
            slot.removeTotem();
        }
    }

    public void assignTurnOrder(Board board) {
        reset();

        List<OfferTile> occupiedTiles = new ArrayList<>();
        for (OfferTile tile : board.getOfferTiles()) {
            if (tile.isOccupied()) {
                occupiedTiles.add(tile);
            }
        }
        occupiedTiles.sort(Comparator.comparingInt(t -> (int) t.getLetter()));
        List<TurnSlot> sortedSlots = new ArrayList<>(slots);
        sortedSlots.sort(Comparator.comparingInt(TurnSlot::getPosition));

        for (int i = 0; i < occupiedTiles.size(); i++) {
            Totem totem = occupiedTiles.get(i).getTotemSlot();
            if (totem != null) {
                sortedSlots.get(i).placeTotem(totem);
            }
        }
    }

    public List<Totem> getTurnOrder() {
        List<Totem> order = new ArrayList<>();
        List<TurnSlot> sortedSlots = new ArrayList<>(slots);
        sortedSlots.sort(Comparator.comparingInt(TurnSlot::getPosition));
        for (TurnSlot slot : sortedSlots) {
            if (slot.isOccupied()) {
                order.add(slot.getOccupiedBy());
            }
        }
        return order;
    }
}
