package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.LowerRowSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.UpperRowSlot;

// Internal helper that applies the immediate effects of an offer tile's slots to the acting player
// (food gains) and accumulates the upper/lower row pick budget the player is entitled to.
final class OfferTileSlotResolver {

    private OfferTileSlotResolver() {
    }

    static RowPickBudget resolve(GameController ctx, Player player, OfferTile tile) {
        RowPickBudget budget = new RowPickBudget();

        for (ActionSlot slot : tile.getSlots()) {
            if (slot.getEffect() instanceof FoodSlot) {
                int amount = slot.getAmount();
                player.addFood(amount);
                ctx.notifyListeners(l -> l.onFoodChanged(
                        player.getNickname(), player.getFood(), amount));
            } else if (slot.getEffect() instanceof UpperRowSlot) {
                budget.addUpper(slot.getAmount());
            } else if (slot.getEffect() instanceof LowerRowSlot) {
                budget.addLower(slot.getAmount());
            }
        }

        return budget;
    }
}
