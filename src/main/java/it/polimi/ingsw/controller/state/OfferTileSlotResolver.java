package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.LowerRowSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.SlotEffect;
import it.polimi.ingsw.model.board.SlotEffectVisitor;
import it.polimi.ingsw.model.board.UpperRowSlot;

// Internal helper that applies the immediate effects of an offer tile's slots to the acting player
// (food gains) and accumulates the upper/lower row pick budget the player is entitled to.
final class OfferTileSlotResolver {

    private OfferTileSlotResolver() {
    }

    static RowPickBudget resolve(GameController ctx, Player player, OfferTile tile) {
        RowPickBudget budget = new RowPickBudget();

        for (ActionSlot slot : tile.getSlots()) {
            slot.getEffect().accept(new SlotEffectVisitor<Void>() {
                @Override
                public Void visitFood(FoodSlot effect) {
                    int amount = slot.getAmount();
                    player.addFood(amount);
                    ctx.notifyListeners(l -> l.onFoodChanged(
                            player.getNickname(), player.getFood(), amount));
                    return null;
                }

                @Override
                public Void visitUpperRow(UpperRowSlot effect) {
                    budget.addUpper(slot.getAmount());
                    return null;
                }

                @Override
                public Void visitLowerRow(LowerRowSlot effect) {
                    budget.addLower(slot.getAmount());
                    return null;
                }

                @Override
                public Void visitEffect(SlotEffect effect) {
                    return null;
                }
            });
        }

        return budget;
    }
}
