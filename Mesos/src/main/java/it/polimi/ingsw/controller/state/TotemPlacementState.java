package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.board.OfferTile;

public class TotemPlacementState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    private final TotemPlacementTurnQueue turnQueue = new TotemPlacementTurnQueue();

    @Override
    public void onEnter(GameController ctx) {
        turnQueue.initialize(ctx);
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        if (!(action instanceof PlaceTotemAction)) {
            rejectAction(action);
        }
        if (ctx.isPlayerDisconnected(playerNickname)) {
            throw new IllegalStateException("Disconnected players cannot act");
        }
        PlaceTotemAction placeTotem = (PlaceTotemAction) action;

        turnQueue.advanceToNextAvailablePlayer(ctx);
        String currentPlayerNickname = getCurrentPlayerNickname(ctx);
        if (currentPlayerNickname == null) {
            throw new IllegalStateException("No player available for placement");
        }
        if (!currentPlayerNickname.equals(playerNickname)) {
            throw new IllegalStateException("It is not " + playerNickname + "'s turn");
        }

        // checks that the player has not already placed
        if (turnQueue.hasPlaced(playerNickname)) {
            throw new IllegalStateException(playerNickname + " has already placed the totem");
        }

        // finds the player in the model
        Player player = findPlayer(ctx, playerNickname);
        if (player == null) {
            throw new IllegalStateException("Player not found: " + playerNickname);
        }

        // finds the chosen tile on the board
        OfferTile tile = findTile(ctx, placeTotem.getTileId());
        if (tile == null) {
            if (isKnownTileId(placeTotem.getTileId())) {
                throw new IllegalArgumentException(
                        "Tile " + placeTotem.getTileId() + " not available for this game");
            }
            throw new IllegalArgumentException("Invalid tile: " + placeTotem.getTileId());
        }

        // calls the model
        boolean placed = ctx.getGame().getCurrentRound().placeTotemOffer(player, tile);
        if (!placed) {
            throw new IllegalStateException("Tile " + placeTotem.getTileId() + " occupied or not available");
        }

        // listener
        ctx.notifyListeners(l -> l.onTotemPlaced(playerNickname, placeTotem.getTileId()));
        turnQueue.markPlaced(playerNickname, ctx);

        // if everyone has placed → move to ActionResolutionState
        if (haveAllConnectedPlayersPlaced(ctx)) {
            ctx.changeState(new ActionResolutionState());
        } else {
            notifyCurrentPlayer(ctx);
        }
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }

        boolean wasCurrentPlayer = playerNickname.equals(getCurrentPlayerNickname(ctx));
        ctx.markPlayerDisconnected(playerNickname);
        if (wasCurrentPlayer && ctx.canContinueAfterDisconnection()) {
            skipCurrentPlayer(ctx);
        }
        if (ctx.canContinueAfterDisconnection() && haveAllConnectedPlayersPlaced(ctx)) {
            ctx.changeState(new ActionResolutionState());
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
        if (haveAllConnectedPlayersPlaced(ctx)) {
            ctx.changeState(new ActionResolutionState());
            return;
        }
        String previousCurrentPlayer = getCurrentPlayerNickname(ctx);
        turnQueue.advanceToNextAvailablePlayer(ctx);
        String currentPlayer = getCurrentPlayerNickname(ctx);
        if (currentPlayer != null && !currentPlayer.equals(previousCurrentPlayer)) {
            notifyCurrentPlayer(ctx);
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private Player findPlayer(GameController ctx, String nickname) {
        for (Player p : ctx.getGame().getPlayers()) {
            if (p.getNickname().equals(nickname)) return p;
        }
        return null;
    }

    private OfferTile findTile(GameController ctx, char tileId) {
        for (OfferTile tile : ctx.getGame().getBoard().getOfferTiles()) {
            if (tile.getLetter() == tileId) return tile;
        }
        return null;
    }

    public String getCurrentPlayerNickname(GameController ctx) {
        return turnQueue.currentPlayerNickname();
    }

    private void notifyCurrentPlayer(GameController ctx) {
        String currentPlayer = getCurrentPlayerNickname(ctx);
        if (currentPlayer != null) {
            ctx.notifyListeners(l -> l.onCurrentPlayerChanged(currentPlayer));
        }
    }

    private void skipCurrentPlayer(GameController ctx) {
        turnQueue.skipCurrentPlayer(ctx);
        notifyCurrentPlayer(ctx);
    }

    private boolean haveAllConnectedPlayersPlaced(GameController ctx) {
        return turnQueue.haveAllConnectedPlayersPlaced(ctx);
    }

    private boolean isKnownTileId(char tileId) {
        char normalizedTileId = Character.toUpperCase(tileId);
        return normalizedTileId >= 'A' && normalizedTileId <= 'G';
    }
}
