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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ActionResolutionState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    /** Serializable replacement for Map.Entry<Player, OfferTile>. */
    private static final class OrderedPlayer implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Player player;
        private final OfferTile tile;

        OrderedPlayer(Player player, OfferTile tile) {
            this.player = player;
            this.tile = tile;
        }

        Player getPlayer() { return player; }
        OfferTile getTile() { return tile; }
    }

    /** Serializable replacement for Map.Entry<Player, Integer>. */
    private static final class ExtraPick implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Player player;
        private final int picks;

        ExtraPick(Player player, int picks) {
            this.player = player;
            this.picks = picks;
        }

        Player getPlayer() { return player; }
        int getPicks() { return picks; }
    }

    // players ordered by tile letter (A < B < C)
    private List<OrderedPlayer> orderedPlayers;
    private int currentIndex;

    // how many cards the current player can still take
    private int remainingUpperPicks;
    private int remainingLowerPicks;

    // extra pick phase
    private List<ExtraPick> extraPickQueue;
    private int extraPickIndex;
    private int remainingExtraPicks;

    @Override
    public void onEnter(GameController ctx) {
        Round round = ctx.getGame().getCurrentRound();
        Map<Player, OfferTile> playerChoices = round.getPlayerChoices();

        // Clear the turn-order tile so this round's resolution can repopulate it
        // with the new order based on offer-tile letters.
        ctx.getGame().getBoard().getTurnOrderTile().reset();

        orderedPlayers = new ArrayList<>();
        for (Map.Entry<Player, OfferTile> entry : playerChoices.entrySet()) {
            orderedPlayers.add(new OrderedPlayer(entry.getKey(), entry.getValue()));
        }
        orderedPlayers.sort(Comparator.comparingInt(op -> (int) op.getTile().getLetter()));

        currentIndex = 0;
        extraPickQueue = null;
        prepareCurrentPlayer(ctx);
    }

    private void prepareCurrentPlayer(GameController ctx) {
        if (currentIndex >= orderedPlayers.size()) {
            startExtraPickPhase(ctx);
            return;
        }

        Player player = orderedPlayers.get(currentIndex).getPlayer();
        OfferTile tile = orderedPlayers.get(currentIndex).getTile();
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
                int amount = slot.getAmount();
                player.addFood(amount);
                ctx.notifyListeners(l -> l.onFoodChanged(
                        player.getNickname(), player.getFood(), amount));
            } else if (slot.getEffect() instanceof UpperRowSlot) {
                remainingUpperPicks += slot.getAmount();
            } else if (slot.getEffect() instanceof LowerRowSlot) {
                remainingLowerPicks += slot.getAmount();
            }
        }

        skipImpossibleRowPicks(round);

        if (remainingUpperPicks == 0 && remainingLowerPicks == 0) {
            round.placeTotemOrderForPlayer(player);
            currentIndex++;
            prepareCurrentPlayer(ctx);
            return;
        }

        ctx.notifyListeners(l -> l.onCurrentPlayerChanged(player.getNickname()));
    }

    // ── EXTRA PICK PHASE ─────────────────────────────────────

    private void startExtraPickPhase(GameController ctx) {
        extraPickQueue = new ArrayList<>();
        for (Player p : ctx.getGame().getPlayers()) {
            int count = 0;
            for (BuildingCard b : p.getTribe().getBuildingCards()) {
                count += extraUpperRowPicksFrom(b);
            }
            if (count > 0) extraPickQueue.add(new ExtraPick(p, count));
        }
        extraPickIndex = 0;
        prepareExtraPickPlayer(ctx);
    }

    private void prepareExtraPickPlayer(GameController ctx) {
        if (extraPickIndex >= extraPickQueue.size()) {
            ctx.changeState(new EventResolutionState());
            return;
        }

        Round round = ctx.getGame().getCurrentRound();

        boolean upperRowEmpty = round.getUpperRowTribute().isEmpty()
                && round.getUpperRowBuildings().isEmpty();

        if (upperRowEmpty) {
            extraPickIndex++;
            prepareExtraPickPlayer(ctx);
            return;
        }

        Player player = extraPickQueue.get(extraPickIndex).getPlayer();

        if (ctx.isPlayerDisconnected(player.getNickname())) {
            extraPickIndex++;
            prepareExtraPickPlayer(ctx);
            return;
        }

        remainingExtraPicks = extraPickQueue.get(extraPickIndex).getPicks();
        int picks = remainingExtraPicks;
        ctx.notifyListeners(l -> l.onCurrentPlayerChanged(player.getNickname()));
        ctx.notifyListeners(l -> l.onExtraCardPickAvailable(player.getNickname(), picks));
    }
    private boolean isInExtraPickPhase() {
        return extraPickQueue != null && extraPickIndex < extraPickQueue.size();
    }

    public String getCurrentPlayerNickname() {
        if (isInExtraPickPhase()) {
            return extraPickQueue.get(extraPickIndex).getPlayer().getNickname();
        }
        if (orderedPlayers == null || currentIndex >= orderedPlayers.size()) {
            return null;
        }
        return orderedPlayers.get(currentIndex).getPlayer().getNickname();
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
        if (currentIndex >= orderedPlayers.size()) return;

        Player currentPlayer = orderedPlayers.get(currentIndex).getPlayer();
        Game game = ctx.getGame();
        Round round = game.getCurrentRound();

        if (!currentPlayer.getNickname().equals(playerNickname)) {
            throw new IllegalStateException("It is not " + playerNickname + "'s turn");
        }

        if (action instanceof PickCardAction pickCard) {
            if (pickCard.isFromUpperRow() && remainingUpperPicks <= 0)
                throw new IllegalStateException("You cannot take any more cards from the upper row");
            if (!pickCard.isFromUpperRow() && remainingLowerPicks <= 0)
                throw new IllegalStateException("You cannot take any more cards from the lower row");

            List<CharacterCard> row = pickCard.isFromUpperRow()
                    ? round.getUpperRowTribute()
                    : round.getLowerRowTribute();

            CharacterCard card = findCharacterCard(row, pickCard.getCardId());
            if (card == null)
                throw new IllegalArgumentException("Character card not found: " + pickCard.getCardId());

            game.takeCharacterCard(currentPlayer, row, card);
            ctx.notifyListeners(l -> l.onCharacterCardTaken(
                    playerNickname, pickCard.getCardId(), pickCard.isFromUpperRow()));

            if (pickCard.isFromUpperRow()) remainingUpperPicks--;
            else remainingLowerPicks--;

        } else if (action instanceof PickBuildingAction pickBuilding) {
            if (pickBuilding.isFromUpperRow() && remainingUpperPicks <= 0)
                throw new IllegalStateException("You cannot take any more buildings from the upper row");
            if (!pickBuilding.isFromUpperRow() && remainingLowerPicks <= 0)
                throw new IllegalStateException("You cannot take any more buildings from the lower row");

            List<CharacterCard> tributeRow = pickBuilding.isFromUpperRow()
                    ? round.getUpperRowTribute()
                    : round.getLowerRowTribute();
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

            if (pickBuilding.isFromUpperRow()) remainingUpperPicks--;
            else remainingLowerPicks--;

        } else {
            rejectAction(action);
        }

        skipImpossibleRowPicks(round);

        if (remainingUpperPicks == 0 && remainingLowerPicks == 0) {
            ctx.getGame().getCurrentRound().placeTotemOrderForPlayer(currentPlayer);
            currentIndex++;
            prepareCurrentPlayer(ctx);
        } else {
            ctx.notifyListeners(l -> l.onCurrentPlayerChanged(currentPlayer.getNickname()));
        }
    }

    private void skipImpossibleRowPicks(Round round) {
        boolean upperRowEmpty = round.getUpperRowTribute().isEmpty()
                && round.getUpperRowBuildings().isEmpty();
        boolean lowerRowEmpty = round.getLowerRowTribute().isEmpty()
                && round.getLowerRowBuildings().isEmpty();

        if (upperRowEmpty) {
            remainingUpperPicks = 0;
        }
        if (lowerRowEmpty) {
            remainingLowerPicks = 0;
        }
    }

    private void handleExtraPickAction(GameController ctx, String playerNickname, GameAction action) {
        Player currentPlayer = extraPickQueue.get(extraPickIndex).getPlayer();
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

        remainingExtraPicks--;
        if (remainingExtraPicks == 0) {
            extraPickIndex++;
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
            if (extraPickIndex < extraPickQueue.size()
                    && extraPickQueue.get(extraPickIndex).getPlayer().getNickname().equals(playerNickname)
                    && ctx.canContinueAfterDisconnection()) {
                extraPickIndex++;
                prepareExtraPickPlayer(ctx);
            }
        } else {
            if (currentIndex < orderedPlayers.size()
                    && orderedPlayers.get(currentIndex).getPlayer().getNickname().equals(playerNickname)
                    && ctx.canContinueAfterDisconnection()) {
                ctx.getGame().getCurrentRound().placeTotemOrderForPlayer(
                        orderedPlayers.get(currentIndex).getPlayer());
                currentIndex++;
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
            if (extraPickIndex < extraPickQueue.size()
                    && ctx.isPlayerDisconnected(extraPickQueue.get(extraPickIndex).getPlayer().getNickname())) {
                extraPickIndex++;
                prepareExtraPickPlayer(ctx);
            }
            return;
        }

        if (orderedPlayers != null
                && currentIndex < orderedPlayers.size()
                && ctx.isPlayerDisconnected(orderedPlayers.get(currentIndex).getPlayer().getNickname())) {
            ctx.getGame().getCurrentRound().placeTotemOrderForPlayer(
                    orderedPlayers.get(currentIndex).getPlayer());
            currentIndex++;
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

        if (currentIndex < orderedPlayers.size()) {
            Player player = orderedPlayers.get(currentIndex).getPlayer();
            autoPickForPlayer(ctx, game, round, player, remainingUpperPicks, remainingLowerPicks);
            round.placeTotemOrderForPlayer(player);
            currentIndex++;
        }

        while (currentIndex < orderedPlayers.size()) {
            Player player = orderedPlayers.get(currentIndex).getPlayer();
            OfferTile tile = orderedPlayers.get(currentIndex).getTile();

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
            currentIndex++;
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

        while (extraPickIndex < extraPickQueue.size()) {
            Player player = extraPickQueue.get(extraPickIndex).getPlayer();
            int picks = extraPickQueue.get(extraPickIndex).getPicks();

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
            extraPickIndex++;
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
}
