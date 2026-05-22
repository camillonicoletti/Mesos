package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;

import java.util.List;

final class CardAcquisitionService {

    private CardAcquisitionService() {
    }

    static void takeCharacter(GameController ctx, Player player, List<CharacterCard> row,
                              String cardId, boolean fromUpperRow) {
        CharacterCard card = findCharacterCard(row, cardId);
        if (card == null) {
            throw new IllegalArgumentException("Character card not found: " + cardId);
        }

        ctx.getGame().takeCharacterCard(player, row, card);
        ctx.notifyListeners(l -> l.onCharacterCardTaken(player.getNickname(), cardId, fromUpperRow));
    }

    static void takeBuilding(GameController ctx, Player player, List<BuildingCard> row,
                             String cardId, boolean fromUpperRow) {
        BuildingCard card = findBuildingCard(row, cardId);
        if (card == null) {
            throw new IllegalArgumentException("Building not found: " + cardId);
        }
        if (!ctx.getGame().canAffordBuilding(player, card)) {
            throw new IllegalStateException("You do not have enough food to buy this building");
        }

        ctx.getGame().takeBuildingCard(player, row, card);
        ctx.notifyListeners(l -> l.onBuildingCardTaken(player.getNickname(), cardId, fromUpperRow));
    }

    static boolean takeFirstCharacter(GameController ctx, Player player, List<CharacterCard> row,
                                      boolean fromUpperRow) {
        if (row.isEmpty()) {
            return false;
        }

        CharacterCard card = row.get(0);
        String cardId = String.valueOf(card.getId());
        ctx.getGame().takeCharacterCard(player, row, card);
        ctx.notifyListeners(l -> l.onCharacterCardTaken(player.getNickname(), cardId, fromUpperRow));
        return true;
    }

    static boolean takeFirstAffordableBuilding(GameController ctx, Player player, List<BuildingCard> row,
                                               boolean fromUpperRow) {
        if (row.isEmpty()) {
            return false;
        }

        BuildingCard card = row.get(0);
        if (!ctx.getGame().canAffordBuilding(player, card)) {
            return false;
        }

        String cardId = String.valueOf(card.getId());
        ctx.getGame().takeBuildingCard(player, row, card);
        ctx.notifyListeners(l -> l.onBuildingCardTaken(player.getNickname(), cardId, fromUpperRow));
        return true;
    }

    private static CharacterCard findCharacterCard(List<CharacterCard> row, String cardId) {
        for (CharacterCard card : row) {
            if (String.valueOf(card.getId()).equals(cardId)) return card;
        }
        return null;
    }

    private static BuildingCard findBuildingCard(List<BuildingCard> row, String cardId) {
        for (BuildingCard card : row) {
            if (String.valueOf(card.getId()).equals(cardId)) return card;
        }
        return null;
    }
}
