package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.board.ActionSlot;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.enums.SlotType;

import java.util.*;

public class ActionResolutionState implements GameState {

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

        // trasformo mappa in lista e ordino i giocatori per lettera tile: A < B < C
        orderedPlayers = new ArrayList<>(playerChoices.entrySet());
        orderedPlayers.sort(Comparator.comparingInt(e -> (int) e.getValue().getLetter()));

        currentIndex = 0;
        prepareCurrentPlayer(ctx);
    }

    /**2
     * Prepara il turno del giocatore corrente:
     * - dà il cibo automaticamente dagli slot FOOD
     * - calcola quante carte può ancora prendere
     * - se non ha carte da prendere, avanza automaticamente
     */
    private void prepareCurrentPlayer(GameController ctx) {
        // tutti i giocatori hanno risolto → passa a TurnOrderState
        if (currentIndex >= orderedPlayers.size()) {


            ctx.changeState(new EventResolutionState());

            return;
        }

        Player player = orderedPlayers.get(currentIndex).getKey();
        OfferTile tile = orderedPlayers.get(currentIndex).getValue();

        remainingUpperPicks = 0;
        remainingLowerPicks = 0;

        for (ActionSlot slot : tile.getSlots()) {
            switch (slot.getType()) {
                case FOOD      -> player.addFood(slot.getAmount()); // automatico
                case UPPER_ROW -> remainingUpperPicks += slot.getAmount();
                case LOWER_ROW -> remainingLowerPicks += slot.getAmount();
            }
        }

        // se non ha carte da prendere, avanza automaticamente al prossimo
        if (remainingUpperPicks == 0 && remainingLowerPicks == 0) {
            currentIndex++;
            prepareCurrentPlayer(ctx);
        }
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        if (currentIndex >= orderedPlayers.size()) return;

        Player currentPlayer = orderedPlayers.get(currentIndex).getKey();
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();

        // verifica che sia il turno di questo giocatore
        if (!currentPlayer.getNickname().equals(playerNickname)) {
            throw new IllegalStateException("Non è il turno di " + playerNickname);
        }

        switch (action.getType()) {

            case "PICK_CARD" -> {
                PickCardAction pickCard = (PickCardAction) action;

                // verifica che abbia ancora pick disponibili per quella riga
                if (pickCard.isFromUpperRow() && remainingUpperPicks <= 0)
                    throw new IllegalStateException("Non puoi prendere altre carte dalla riga superiore");
                if (!pickCard.isFromUpperRow() && remainingLowerPicks <= 0)
                    throw new IllegalStateException("Non puoi prendere altre carte dalla riga inferiore");

                // trova la carta nella riga giusta
                List<CharacterCard> row = pickCard.isFromUpperRow()
                        ? round.getUpperRowTribute()
                        : round.getLowerRowTribute();

                CharacterCard card = findCharacterCard(row, pickCard.getCardId());
                if (card == null)
                    throw new IllegalArgumentException("Carta personaggio non trovata: " + pickCard.getCardId());

                game.takeCharacterCard(currentPlayer, row, card);

                if (pickCard.isFromUpperRow()) remainingUpperPicks--;
                else remainingLowerPicks--;
            }

            case "PICK_BUILDING" -> {
                PickBuildingAction pickBuilding = (PickBuildingAction) action;

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

                if (pickBuilding.isFromUpperRow()) remainingUpperPicks--;
                else remainingLowerPicks--;
            }

            default -> throw new IllegalStateException("Azione non valida in questa fase: " + action.getType());
        }

        // se il giocatore ha finito tutti i suoi pick → avanza al prossimo
        if (remainingUpperPicks == 0 && remainingLowerPicks == 0) {
            currentIndex++;
            prepareCurrentPlayer(ctx);
        }
    }

    @Override
    public void onExit(GameController ctx) {
        // niente da fare
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