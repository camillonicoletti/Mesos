package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.FoodSlot;
import it.polimi.ingsw.model.board.LowerRowSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.UpperRowSlot;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;

import java.util.*;

public class ActionResolutionState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    // giocatori ordinati per lettera tile (A < B < C)
    private List<Map.Entry<Player, OfferTile>> orderedPlayers;
    private int currentIndex;

    // quante carte può ancora prendere il giocatore corrente
    private int remainingUpperPicks;
    private int remainingLowerPicks;

    @Override
    public void onEnter(GameController ctx) {
        Round round = ctx.getGame().getCurrentRound();
        Map<Player, OfferTile> playerChoices = round.getPlayerChoices();

        // ordina i giocatori per lettera tile: A < B < C
        orderedPlayers = new ArrayList<>(playerChoices.entrySet());
        orderedPlayers.sort(Comparator.comparingInt(e -> (int) e.getValue().getLetter()));

        currentIndex = 0;
        prepareCurrentPlayer(ctx);
    }

    private void prepareCurrentPlayer(GameController ctx) {
        // tutti i giocatori hanno risolto → passa a EventResolutionState
        if (currentIndex >= orderedPlayers.size()) {
            ctx.changeState(new EventResolutionState());
            return;
        }

        Player player = orderedPlayers.get(currentIndex).getKey();
        OfferTile tile = orderedPlayers.get(currentIndex).getValue();
        Round round = ctx.getGame().getCurrentRound();

        if (ctx.isPlayerDisconnected(player.getNickname())) {
            round.placeTotemOrderForPlayer(player);
            currentIndex++;
            prepareCurrentPlayer(ctx);
            return;
        }

        remainingUpperPicks = 0;
        remainingLowerPicks = 0;

        for (ActionSlot slot : tile.getSlots()) {
            if (slot.getEffect() instanceof FoodSlot) {
                player.addFood(slot.getAmount()); // automatico
            } else if (slot.getEffect() instanceof UpperRowSlot) {
                remainingUpperPicks += slot.getAmount();
            } else if (slot.getEffect() instanceof LowerRowSlot) {
                remainingLowerPicks += slot.getAmount();
            }
        }

        // se non ha carte da prendere → spostalo sul TurnOrderTile e avanza
        if (remainingUpperPicks == 0 && remainingLowerPicks == 0) {
            round.placeTotemOrderForPlayer(player);
            currentIndex++;
            prepareCurrentPlayer(ctx);
        }
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        if (currentIndex >= orderedPlayers.size()) return;
        if (ctx.isPlayerDisconnected(playerNickname)) {
            throw new IllegalStateException("Disconnected players cannot act");
        }

        Player currentPlayer = orderedPlayers.get(currentIndex).getKey();
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();

        // verifica che sia il turno di questo giocatore
        if (!currentPlayer.getNickname().equals(playerNickname)) {
            throw new IllegalStateException("Non è il turno di " + playerNickname);
        }

        if (action instanceof PickCardAction pickCard) {
            if (pickCard.isFromUpperRow() && remainingUpperPicks <= 0)
                throw new IllegalStateException("Non puoi prendere altre carte dalla riga superiore");
            if (!pickCard.isFromUpperRow() && remainingLowerPicks <= 0)
                throw new IllegalStateException("Non puoi prendere altre carte dalla riga inferiore");

            List<CharacterCard> row = pickCard.isFromUpperRow()
                    ? round.getUpperRowTribute()
                    : round.getLowerRowTribute();

            CharacterCard card = findCharacterCard(row, pickCard.getCardId());
            if (card == null)
                throw new IllegalArgumentException("Carta personaggio non trovata: " + pickCard.getCardId());

            game.takeCharacterCard(currentPlayer, row, card);
            ctx.notifyListeners(l -> l.onCharacterCardTaken(
                    playerNickname, pickCard.getCardId(), pickCard.isFromUpperRow()));

            if (pickCard.isFromUpperRow()) remainingUpperPicks--;
            else remainingLowerPicks--;
        } else if (action instanceof PickBuildingAction pickBuilding) {
            if (pickBuilding.isFromUpperRow() && remainingUpperPicks <= 0)
                throw new IllegalStateException("Non puoi prendere altri edifici dalla riga superiore");
            if (!pickBuilding.isFromUpperRow() && remainingLowerPicks <= 0)
                throw new IllegalStateException("Non puoi prendere altri edifici dalla riga inferiore");

            List<BuildingCard> row = pickBuilding.isFromUpperRow()
                    ? round.getUpperRowBuildings()
                    : round.getLowerRowBuildings();

            BuildingCard card = findBuildingCard(row, pickBuilding.getCardId());
            if (card == null)
                throw new IllegalArgumentException("Edificio non trovato: " + pickBuilding.getCardId());

            game.takeBuildingCard(currentPlayer, row, card);
            ctx.notifyListeners(l -> l.onBuildingCardTaken(
                    playerNickname, pickBuilding.getCardId(), pickBuilding.isFromUpperRow()));

            if (pickBuilding.isFromUpperRow()) remainingUpperPicks--;
            else remainingLowerPicks--;
        } else {
            rejectAction(action);
        }

        // giocatore ha finito tutti i pick → spostalo sul TurnOrderTile e avanza
        if (remainingUpperPicks == 0 && remainingLowerPicks == 0) {
            round.placeTotemOrderForPlayer(currentPlayer);
            currentIndex++;
            prepareCurrentPlayer(ctx);
        }
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }

        ctx.markPlayerDisconnected(playerNickname);
        if (currentIndex < orderedPlayers.size()
                && orderedPlayers.get(currentIndex).getKey().getNickname().equals(playerNickname)
                && !ctx.isSuspended()) {
            ctx.getGame().getCurrentRound().placeTotemOrderForPlayer(orderedPlayers.get(currentIndex).getKey());
            currentIndex++;
            prepareCurrentPlayer(ctx);
        }
        // TODO define gameplay policy for disconnected players during action resolution.
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private CharacterCard findCharacterCard(List<CharacterCard> row, String cardId) {
        for (CharacterCard card : row) {
            if (String.valueOf(card.getId()).equals(cardId)) return card;
        }
        return null;
    }

    private BuildingCard findBuildingCard(List<BuildingCard> row, String cardId) {
        for (BuildingCard card : row) {
            if (String.valueOf(card.getId()).equals(cardId)) return card;
        }
        return null;
    }
}