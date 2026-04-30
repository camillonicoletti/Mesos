package it.polimi.ingsw.model.board;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CharacterCard;

import java.io.Serializable;
import java.util.List;

public class UpperRowSlot implements SlotEffect{
    private static final long serialVersionUID = 1L;
    @Override
    public void resolve(Player player, int amount, List<Card> chosenCards, Round round) {
        List<CharacterCard> tributeRow = round.getUpperRowTribute();
        List<BuildingCard> buildingRow = round.getUpperRowBuildings();
        int taken = 0;
        for (Card chosen : chosenCards) {
            if (taken >= amount) break;
            if (chosen instanceof CharacterCard cc && tributeRow.contains(cc)) {
                tributeRow.remove(cc);
                player.addCharacterCard(cc);
                taken++;
            } else if (chosen instanceof BuildingCard bc && buildingRow.contains(bc)) {
                int actualCost = Math.max(0, bc.getFoodCost() - player.getTribe().getBuildingDiscount());
                if (player.canAfford(actualCost)) {
                    player.spendFood(actualCost);
                    buildingRow.remove(bc);
                    player.addBuildingCard(bc);
                    taken++;
                }
            }
        }
    }
}