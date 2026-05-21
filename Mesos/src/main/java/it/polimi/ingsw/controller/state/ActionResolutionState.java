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
import it.polimi.ingsw.model.card.effect.Effect;
import it.polimi.ingsw.model.card.effect.EffectVisitor;
import it.polimi.ingsw.model.card.effect.ExtraCardUpperRowEffect;

import java.util.List;

/** Resolves offer-tile actions in tile order, then handles extra upper-row picks granted by buildings. */
public class ActionResolutionState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    private ActionTurnQueue actionTurnQueue = new ActionTurnQueue();
    private RowPickBudget regularPickBudget = new RowPickBudget();
    private ExtraPickQueue extraPickQueue = new ExtraPickQueue();

    @Override
    public void onEnter(GameController ctx) {
        Round round = ctx.getGame().getCurrentRound();

        // Clear the turn-order tile so this round's resolution can repopulate it
        // with the new order based on offer-tile letters.
        ctx.getGame().getBoard().getTurnOrderTile().reset();

        getActionTurnQueue().initialize(round.getPlayerChoices());
        getExtraPickQueue().clear();
        prepareCurrentPlayer(ctx);
    }

    private void prepareCurrentPlayer(GameController ctx) {
        ActionTurnQueue queue = getActionTurnQueue();
        if (!queue.hasCurrentPlayer()) {
            startExtraPickPhase(ctx);
            return;
        }

        Player player = queue.currentPlayer();
        OfferTile tile = queue.currentTile();
        Round round = ctx.getGame().getCurrentRound();

        if (ctx.isPlayerDisconnected(player.getNickname())) {
            round.placeTotemOrderForPlayer(player);
            queue.advance();
            prepareCurrentPlayer(ctx);
            return;
        }

        getRegularPickBudget().reset();

        for (ActionSlot slot : tile.getSlots()) {
            if (slot.getEffect() instanceof FoodSlot) {
                int amount = slot.getAmount();
                player.addFood(amount);
                ctx.notifyListeners(l -> l.onFoodChanged(
                        player.getNickname(), player.getFood(), amount));
            } else if (slot.getEffect() instanceof UpperRowSlot) {
                getRegularPickBudget().addUpper(slot.getAmount());
            } else if (slot.getEffect() instanceof LowerRowSlot) {
                getRegularPickBudget().addLower(slot.getAmount());
            }
        }

        getRegularPickBudget().skipUnavailableRows(round);

        if (getRegularPickBudget().isDepleted()) {
            round.placeTotemOrderForPlayer(player);
            queue.advance();
            prepareCurrentPlayer(ctx);
            return;
        }

        ctx.notifyListeners(l -> l.onCurrentPlayerChanged(player.getNickname()));
    }

    // ── EXTRA PICK PHASE ─────────────────────────────────────

    private void startExtraPickPhase(GameController ctx) {
        ExtraPickQueue queue = getExtraPickQueue();
        queue.clear();
        for (Player p : ctx.getGame().getPlayers()) {
            int count = 0;
            for (BuildingCard b : p.getTribe().getBuildingCards()) {
                count += extraUpperRowPicksFrom(b);
            }
            queue.add(p, count);
        }
        queue.start();
        prepareExtraPickPlayer(ctx);
    }

    private void prepareExtraPickPlayer(GameController ctx) {
        ExtraPickQueue queue = getExtraPickQueue();
        if (!queue.isActive()) {
            ctx.changeState(new EventResolutionState());
            return;
        }

        Round round = ctx.getGame().getCurrentRound();

        boolean upperRowEmpty = round.getUpperRowTribute().isEmpty()
                && round.getUpperRowBuildings().isEmpty();

        if (upperRowEmpty) {
            queue.advance();
            prepareExtraPickPlayer(ctx);
            return;
        }

        Player player = queue.currentPlayer();

        if (ctx.isPlayerDisconnected(player.getNickname())) {
            queue.advance();
            prepareExtraPickPlayer(ctx);
            return;
        }

        int picks = queue.prepareCurrentPicks();
        ctx.notifyListeners(l -> l.onCurrentPlayerChanged(player.getNickname()));
        ctx.notifyListeners(l -> l.onExtraCardPickAvailable(player.getNickname(), picks));
    }
    private boolean isInExtraPickPhase() {
        return getExtraPickQueue().isActive();
    }

    public String getCurrentPlayerNickname() {
        if (isInExtraPickPhase()) {
            return getExtraPickQueue().currentPlayer().getNickname();
        }
        return getActionTurnQueue().currentPlayerNickname();
    }

    // ── HANDLE ACTION ────────────────────────────────────────

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        if (ctx.isPlayerDisconnected(playerNickname)) {
            throw new IllegalStateException("Disconnected players cannot act");
        }

        if (isInExtraPickPhase()) {
            handleExtraPickAction(ctx, playerNickname, action);
        } else {
            handleRegularPickAction(ctx, playerNickname, action);
        }
    }

    private void handleRegularPickAction(GameController ctx, String playerNickname, GameAction action) {
        ActionTurnQueue queue = getActionTurnQueue();
        if (!queue.hasCurrentPlayer()) return;

        Player currentPlayer = queue.currentPlayer();
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();

        if (!currentPlayer.getNickname().equals(playerNickname)) {
            throw new IllegalStateException("It is not " + playerNickname + "'s turn");
        }

        if (action instanceof PickCardAction pickCard) {
            getRegularPickBudget().requireCanPick(pickCard.isFromUpperRow(), "cards");

            List<CharacterCard> row = pickCard.isFromUpperRow()
                    ? round.getUpperRowTribute()
                    : round.getLowerRowTribute();

            CharacterCard card = findCharacterCard(row, pickCard.getCardId());
            if (card == null)
                throw new IllegalArgumentException("Character card not found: " + pickCard.getCardId());

            game.takeCharacterCard(currentPlayer, row, card);
            ctx.notifyListeners(l -> l.onCharacterCardTaken(
                    playerNickname, pickCard.getCardId(), pickCard.isFromUpperRow()));
            getRegularPickBudget().consume(pickCard.isFromUpperRow());

        } else if (action instanceof PickBuildingAction pickBuilding) {
            getRegularPickBudget().requireCanPick(pickBuilding.isFromUpperRow(), "buildings");

            List<CharacterCard> tributeRow = pickBuilding.isFromUpperRow()
                    ? round.getUpperRowTribute()
                    : round.getLowerRowTribute();
            // Buildings can be picked from a row only after all character cards in that row are gone.
            if (!tributeRow.isEmpty())
                throw new IllegalStateException(
                        "Must pick a Character card before a Building when Characters are available in the same row");

            List<BuildingCard> row = pickBuilding.isFromUpperRow()
                    ? round.getUpperRowBuildings()
                    : round.getLowerRowBuildings();

            BuildingCard card = findBuildingCard(row, pickBuilding.getCardId());
            if (card == null)
                throw new IllegalArgumentException("Building not found: " + pickBuilding.getCardId());

            if (!game.canAffordBuilding(currentPlayer, card))
                throw new IllegalStateException("You do not have enough food to buy this building");

            game.takeBuildingCard(currentPlayer, row, card);
            ctx.notifyListeners(l -> l.onBuildingCardTaken(
                    playerNickname, pickBuilding.getCardId(), pickBuilding.isFromUpperRow()));
            getRegularPickBudget().consume(pickBuilding.isFromUpperRow());

        } else {
            rejectAction(action);
        }

        getRegularPickBudget().skipUnavailableRows(round);

        if (getRegularPickBudget().isDepleted()) {
            ctx.getGame().getCurrentRound().placeTotemOrderForPlayer(currentPlayer);
            queue.advance();
            prepareCurrentPlayer(ctx);
        } else {
            ctx.notifyListeners(l -> l.onCurrentPlayerChanged(currentPlayer.getNickname()));
        }
    }

    private void handleExtraPickAction(GameController ctx, String playerNickname, GameAction action) {
        ExtraPickQueue queue = getExtraPickQueue();
        Player currentPlayer = queue.currentPlayer();
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();

        if (!currentPlayer.getNickname().equals(playerNickname)) {
            throw new IllegalStateException("It is not " + playerNickname + "'s turn");
        }

        List<CharacterCard> upperTribute = round.getUpperRowTribute();
        List<BuildingCard> upperBuildings = round.getUpperRowBuildings();

        if (action instanceof PickCardAction pickCard) {
            if (!pickCard.isFromUpperRow())
                throw new IllegalStateException("The extra card must be taken from the upper row");

            CharacterCard card = findCharacterCard(upperTribute, pickCard.getCardId());
            if (card == null)
                throw new IllegalArgumentException("Character card not found: " + pickCard.getCardId());

            game.takeCharacterCard(currentPlayer, upperTribute, card);
            ctx.notifyListeners(l -> l.onCharacterCardTaken(playerNickname, pickCard.getCardId(), true));

        } else if (action instanceof PickBuildingAction pickBuilding) {
            if (!pickBuilding.isFromUpperRow())
                throw new IllegalStateException("The extra building must be taken from the upper row");

            BuildingCard card = findBuildingCard(upperBuildings, pickBuilding.getCardId());
            if (card == null)
                throw new IllegalArgumentException("Building not found: " + pickBuilding.getCardId());

            if (!game.canAffordBuilding(currentPlayer, card))
                throw new IllegalStateException("You do not have enough food to buy this building");

            game.takeBuildingCard(currentPlayer, upperBuildings, card);
            ctx.notifyListeners(l -> l.onBuildingCardTaken(playerNickname, pickBuilding.getCardId(), true));

        } else {
            rejectAction(action);
        }

        queue.consumePick();
        if (queue.currentPicksDepleted()) {
            queue.advance();
            prepareExtraPickPlayer(ctx);
        } else {
            ctx.notifyListeners(l -> l.onCurrentPlayerChanged(currentPlayer.getNickname()));
        }
    }

    // ── DISCONNECTION ────────────────────────────────────────

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) return;

        ctx.markPlayerDisconnected(playerNickname);

        if (isInExtraPickPhase()) {
            if (getExtraPickQueue().isCurrentPlayer(playerNickname)
                    && ctx.canContinueAfterDisconnection()) {
                getExtraPickQueue().advance();
                prepareExtraPickPlayer(ctx);
            }
        } else {
            ActionTurnQueue queue = getActionTurnQueue();
            if (queue.isCurrentPlayer(playerNickname)
                    && ctx.canContinueAfterDisconnection()) {
                ctx.getGame().getCurrentRound().placeTotemOrderForPlayer(queue.currentPlayer());
                queue.advance();
                prepareCurrentPlayer(ctx);
            }
        }
    }

    @Override
    public void onPlayerReconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }
    }

    @Override
    public void onResume(GameController ctx) {
        if (isInExtraPickPhase()) {
            Player currentPlayer = getExtraPickQueue().currentPlayer();
            if (currentPlayer != null && ctx.isPlayerDisconnected(currentPlayer.getNickname())) {
                getExtraPickQueue().advance();
                prepareExtraPickPlayer(ctx);
            }
            return;
        }

        ActionTurnQueue queue = getActionTurnQueue();
        Player currentPlayer = queue.currentPlayer();
        if (currentPlayer != null && ctx.isPlayerDisconnected(currentPlayer.getNickname())) {
            ctx.getGame().getCurrentRound().placeTotemOrderForPlayer(currentPlayer);
            queue.advance();
            prepareCurrentPlayer(ctx);
        }
    }

    // ── DEBUG ────────────────────────────────────────────────

    public void debugAutoComplete(GameController ctx) {
        if (isInExtraPickPhase()) {
            autoCompleteExtraPickPhase(ctx);
            return;
        }
        autoCompleteFromCurrentIndex(ctx);
    }

    private void autoCompleteFromCurrentIndex(GameController ctx) {
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();
        ActionTurnQueue queue = getActionTurnQueue();

        if (queue.hasCurrentPlayer()) {
            Player player = queue.currentPlayer();
            autoPickForPlayer(ctx, game, round, player,
                    getRegularPickBudget().upperPicks(), getRegularPickBudget().lowerPicks());
            round.placeTotemOrderForPlayer(player);
            queue.advance();
        }

        while (queue.hasCurrentPlayer()) {
            Player player = queue.currentPlayer();
            OfferTile tile = queue.currentTile();

            int upperPicks = 0;
            int lowerPicks = 0;

            for (ActionSlot slot : tile.getSlots()) {
                if (slot.getEffect() instanceof FoodSlot) {
                    int amount = slot.getAmount();
                    player.addFood(amount);
                    final Player fp = player;
                    ctx.notifyListeners(l -> l.onFoodChanged(fp.getNickname(), fp.getFood(), amount));
                } else if (slot.getEffect() instanceof UpperRowSlot) {
                    upperPicks += slot.getAmount();
                } else if (slot.getEffect() instanceof LowerRowSlot) {
                    lowerPicks += slot.getAmount();
                }
            }

            boolean upperEmpty = round.getUpperRowTribute().isEmpty() && round.getUpperRowBuildings().isEmpty();
            boolean lowerEmpty = round.getLowerRowTribute().isEmpty() && round.getLowerRowBuildings().isEmpty();
            if (upperEmpty) upperPicks = 0;
            if (lowerEmpty) lowerPicks = 0;

            autoPickForPlayer(ctx, game, round, player, upperPicks, lowerPicks);
            round.placeTotemOrderForPlayer(player);
            queue.advance();
        }

        startExtraPickPhase(ctx);
    }

    private void autoPickForPlayer(GameController ctx, Game game, Round round,
                                   Player player, int upperPicks, int lowerPicks) {
        while (upperPicks > 0) {
            if (!round.getUpperRowTribute().isEmpty()) {
                CharacterCard card = round.getUpperRowTribute().get(0);
                game.takeCharacterCard(player, round.getUpperRowTribute(), card);
                String cardId = String.valueOf(card.getId());
                final Player fp = player;
                ctx.notifyListeners(l -> l.onCharacterCardTaken(fp.getNickname(), cardId, true));
            } else if (!round.getUpperRowBuildings().isEmpty()) {
                BuildingCard card = round.getUpperRowBuildings().get(0);
                if (!game.canAffordBuilding(player, card)) break;
                game.takeBuildingCard(player, round.getUpperRowBuildings(), card);
                String cardId = String.valueOf(card.getId());
                final Player fp = player;
                ctx.notifyListeners(l -> l.onBuildingCardTaken(fp.getNickname(), cardId, true));
            } else {
                break;
            }
            upperPicks--;
        }

        while (lowerPicks > 0) {
            if (!round.getLowerRowTribute().isEmpty()) {
                CharacterCard card = round.getLowerRowTribute().get(0);
                game.takeCharacterCard(player, round.getLowerRowTribute(), card);
                String cardId = String.valueOf(card.getId());
                final Player fp = player;
                ctx.notifyListeners(l -> l.onCharacterCardTaken(fp.getNickname(), cardId, false));
            } else if (!round.getLowerRowBuildings().isEmpty()) {
                BuildingCard card = round.getLowerRowBuildings().get(0);
                if (!game.canAffordBuilding(player, card)) break;
                game.takeBuildingCard(player, round.getLowerRowBuildings(), card);
                String cardId = String.valueOf(card.getId());
                final Player fp = player;
                ctx.notifyListeners(l -> l.onBuildingCardTaken(fp.getNickname(), cardId, false));
            } else {
                break;
            }
            lowerPicks--;
        }
    }

    private void autoCompleteExtraPickPhase(GameController ctx) {
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();

        ExtraPickQueue queue = getExtraPickQueue();
        while (queue.isActive()) {
            Player player = queue.currentPlayer();
            int picks = queue.remainingPicks();

            while (picks > 0) {
                if (!round.getUpperRowTribute().isEmpty()) {
                    CharacterCard card = round.getUpperRowTribute().get(0);
                    game.takeCharacterCard(player, round.getUpperRowTribute(), card);
                    String cardId = String.valueOf(card.getId());
                    final Player fp = player;
                    ctx.notifyListeners(l -> l.onCharacterCardTaken(fp.getNickname(), cardId, true));
                } else if (!round.getUpperRowBuildings().isEmpty()) {
                    BuildingCard card = round.getUpperRowBuildings().get(0);
                    if (!game.canAffordBuilding(player, card)) break;
                    game.takeBuildingCard(player, round.getUpperRowBuildings(), card);
                    String cardId = String.valueOf(card.getId());
                    final Player fp = player;
                    ctx.notifyListeners(l -> l.onBuildingCardTaken(fp.getNickname(), cardId, true));
                } else {
                    break;
                }
                picks--;
            }
            queue.advance();
        }

        ctx.changeState(new EventResolutionState());
    }

    // ── HELPERS ──────────────────────────────────────────────

    private int extraUpperRowPicksFrom(BuildingCard building) {
        return building.acceptEffect(new EffectVisitor<>() {
            @Override public Integer visitExtraCardUpperRow(ExtraCardUpperRowEffect effect) {
                return 1;
            }

            @Override public Integer visitEffect(Effect effect) {
                return 0;
            }
        });
    }

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

    private RowPickBudget getRegularPickBudget() {
        if (regularPickBudget == null) {
            regularPickBudget = new RowPickBudget();
        }
        return regularPickBudget;
    }

    private ExtraPickQueue getExtraPickQueue() {
        if (extraPickQueue == null) {
            extraPickQueue = new ExtraPickQueue();
        }
        return extraPickQueue;
    }

    private ActionTurnQueue getActionTurnQueue() {
        if (actionTurnQueue == null) {
            actionTurnQueue = new ActionTurnQueue();
        }
        return actionTurnQueue;
    }
}
