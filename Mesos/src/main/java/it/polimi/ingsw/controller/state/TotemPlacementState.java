package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.Totem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TotemPlacementState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    // nicknames of the players who have already placed their totem
    private final Set<String> playersWhoPlaced = new HashSet<>();
    private final List<String> placementOrder = new ArrayList<>();
    private int currentTurnIndex;

    @Override
    public void onEnter(GameController ctx) {
        playersWhoPlaced.clear();
        placementOrder.clear();

        var turnTile = ctx.getGame().getBoard().getTurnOrderTile();
        for (Totem totem : turnTile.getTurnOrder()) {
            Player owner = findPlayerByTotem(ctx, totem);
            if (owner != null) {
                placementOrder.add(owner.getNickname());
            }
        }

        // First round of the match: the turn-order tile is empty, fall back to the
        // game's own player order (which respects the original setup randomization).
        if (placementOrder.isEmpty()) {
            for (Player p : ctx.getGame().getPlayers()) {
                placementOrder.add(p.getNickname());
            }
        }

        currentTurnIndex = 0;
        advanceToNextConnectedPlayer(ctx);
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

        advanceToNextConnectedPlayer(ctx);
        String currentPlayerNickname = getCurrentPlayerNickname(ctx);
        if (currentPlayerNickname == null) {
            throw new IllegalStateException("No player available for placement");
        }
        if (!currentPlayerNickname.equals(playerNickname)) {
            throw new IllegalStateException("It is not " + playerNickname + "'s turn");
        }

        // checks that the player has not already placed
        if (playersWhoPlaced.contains(playerNickname)) {
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

        playersWhoPlaced.add(playerNickname);
        // listener
        ctx.notifyListeners(l -> l.onTotemPlaced(playerNickname, placeTotem.getTileId()));
        currentTurnIndex++;
        advanceToNextConnectedPlayer(ctx);

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
        advanceToNextConnectedPlayer(ctx);
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

    private Player findPlayerByTotem(GameController ctx, Totem totem) {
        if (totem == null || totem.getColor() == null) {
            return null;
        }
        for (Player p : ctx.getGame().getPlayers()) {
            if (p.getTotem() != null && p.getTotem().getColor() == totem.getColor()) {
                return p;
            }
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
        if (currentTurnIndex >= placementOrder.size()) {
            return null;
        }
        return placementOrder.get(currentTurnIndex);
    }

    private void notifyCurrentPlayer(GameController ctx) {
        String currentPlayer = getCurrentPlayerNickname(ctx);
        if (currentPlayer != null) {
            ctx.notifyListeners(l -> l.onCurrentPlayerChanged(currentPlayer));
        }
    }

    private void advanceToNextConnectedPlayer(GameController ctx) {
        while (currentTurnIndex < placementOrder.size()) {
            String nickname = placementOrder.get(currentTurnIndex);
            if (!ctx.isPlayerDisconnected(nickname) && !playersWhoPlaced.contains(nickname)) {
                return;
            }
            currentTurnIndex++;
        }
    }

    private void skipCurrentPlayer(GameController ctx) {
        currentTurnIndex++;
        advanceToNextConnectedPlayer(ctx);
        notifyCurrentPlayer(ctx);
    }

    private boolean haveAllConnectedPlayersPlaced(GameController ctx) {
        for (Player player : ctx.getGame().getPlayers()) {
            if (!ctx.isPlayerDisconnected(player.getNickname())
                    && !playersWhoPlaced.contains(player.getNickname())) {
                return false;
            }
        }
        return true;
    }

    private boolean isKnownTileId(char tileId) {
        char normalizedTileId = Character.toUpperCase(tileId);
        return normalizedTileId >= 'A' && normalizedTileId <= 'G';
    }
}
