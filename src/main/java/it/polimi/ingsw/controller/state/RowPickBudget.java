package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.model.Round;

import java.io.Serializable;

final class RowPickBudget implements Serializable {

    private static final long serialVersionUID = 1L;

    private int upperPicks;
    private int lowerPicks;

    void reset() {
        upperPicks = 0;
        lowerPicks = 0;
    }

    void addUpper(int amount) {
        upperPicks += amount;
    }

    void addLower(int amount) {
        lowerPicks += amount;
    }

    int upperPicks() {
        return upperPicks;
    }

    int lowerPicks() {
        return lowerPicks;
    }

    boolean canPick(boolean fromUpperRow) {
        return fromUpperRow ? upperPicks > 0 : lowerPicks > 0;
    }

    void requireCanPick(boolean fromUpperRow, String itemName) {
        if (!canPick(fromUpperRow)) {
            throw new IllegalStateException(fromUpperRow
                    ? "You cannot take any more " + itemName + " from the upper row"
                    : "You cannot take any more " + itemName + " from the lower row");
        }
    }

    void consume(boolean fromUpperRow) {
        if (fromUpperRow) {
            upperPicks--;
        } else {
            lowerPicks--;
        }
    }

    void skipUnavailableRows(Round round) {
        boolean upperRowEmpty = round.getUpperRowTribute().isEmpty()
                && round.getUpperRowBuildings().isEmpty();
        boolean lowerRowEmpty = round.getLowerRowTribute().isEmpty()
                && round.getLowerRowBuildings().isEmpty();

        if (upperRowEmpty) {
            upperPicks = 0;
        }
        if (lowerRowEmpty) {
            lowerPicks = 0;
        }
    }

    boolean isDepleted() {
        return upperPicks == 0 && lowerPicks == 0;
    }
}
