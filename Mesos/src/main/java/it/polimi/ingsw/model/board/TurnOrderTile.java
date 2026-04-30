package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Totem;


import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class TurnOrderTile implements Serializable {

    private static final long serialVersionUID = 1L;
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

    public void assignRandomTurnOrder(List<Totem> totems) {
        reset();
        List<Totem> shuffled = new ArrayList<>(totems);
        Collections.shuffle(shuffled);
        List<TurnSlot> sortedSlots = new ArrayList<>(slots);
        sortedSlots.sort(Comparator.comparingInt(TurnSlot::getPosition));
        for (int i = 0; i < shuffled.size(); i++) {
            // assigns each totem to a position
            sortedSlots.get(i).placeTotem(shuffled.get(i));
        }
    }

    public static TurnOrderTile createForPlayers(int numPlayers) {
        List<TurnSlot> slots = new ArrayList<>();

        switch (numPlayers) {
            case 2 -> {
                slots.add(new TurnSlot(1, new GainFoodSlotEffect(1)));
                slots.add(new TurnSlot(2, new PayFoodSlotEffect(1, 2)));
            }
            case 3 -> {
                slots.add(new TurnSlot(1, new GainFoodSlotEffect(2)));
                slots.add(new TurnSlot(2, new NoSlotEffect()));
                slots.add(new TurnSlot(3, new PayFoodSlotEffect(1, 2)));
            }
            case 4 -> {
                slots.add(new TurnSlot(1, new GainFoodSlotEffect(2)));
                slots.add(new TurnSlot(2, new GainFoodSlotEffect(1)));
                slots.add(new TurnSlot(3, new NoSlotEffect()));
                slots.add(new TurnSlot(4, new PayFoodSlotEffect(1, 2)));
            }
            case 5 -> {
                slots.add(new TurnSlot(1, new GainFoodSlotEffect(3)));
                slots.add(new TurnSlot(2, new GainFoodSlotEffect(1)));
                slots.add(new TurnSlot(3, new NoSlotEffect()));
                slots.add(new TurnSlot(4, new NoSlotEffect()));
                slots.add(new TurnSlot(5, new PayFoodSlotEffect(1, 2)));
            }
            default -> throw new IllegalArgumentException("Unsupported number of players: " + numPlayers);
        }

        return new TurnOrderTile(numPlayers, slots);
    }


}
