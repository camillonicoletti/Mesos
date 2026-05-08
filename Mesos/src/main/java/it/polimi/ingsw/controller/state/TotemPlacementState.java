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

    // nicknames dei giocatori che hanno già piazzato il totem
    private final Set<String> playersWhoPlaced = new HashSet<>();
    private final List<String> placementOrder = new ArrayList<>();
    private int currentTurnIndex;

    @Override
    public void onEnter(GameController ctx) {
        playersWhoPlaced.clear();
        placementOrder.clear();
        for (Totem totem : ctx.getGame().getBoard().getTurnOrderTile().getTurnOrder()) {
            Player owner = findPlayerByTotem(ctx, totem);
            if (owner != null) {
                placementOrder.add(owner.getNickname());
            }
        }
        currentTurnIndex = 0;
        advanceToNextConnectedPlayer(ctx);
        notifyCurrentPlayer(ctx);
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

        String currentPlayerNickname = getCurrentPlayerNickname(ctx);
        if (currentPlayerNickname == null) {
            throw new IllegalStateException("Nessun giocatore disponibile per il piazzamento");
        }
        if (!currentPlayerNickname.equals(playerNickname)) {
            throw new IllegalStateException("Non è il turno di " + playerNickname);
        }

        // verifica che il giocatore non abbia già piazzato
        if (playersWhoPlaced.contains(playerNickname)) {
            throw new IllegalStateException(playerNickname + " ha già piazzato il totem");
        }

        // trova il giocatore nel model
        Player player = findPlayer(ctx, playerNickname);
        if (player == null) {
            throw new IllegalStateException("Giocatore non trovato: " + playerNickname);
        }

        // trova la tile scelta sulla board
        OfferTile tile = findTile(ctx, placeTotem.getTileId());
        if (tile == null) {
            if (isKnownTileId(placeTotem.getTileId())) {
                throw new IllegalArgumentException(
                        "Tile " + placeTotem.getTileId() + " non disponibile per questa partita");
            }
            throw new IllegalArgumentException("Tile non valida: " + placeTotem.getTileId());
        }

        // chiama il model
        boolean placed = ctx.getGame().getCurrentRound().placeTotemOffer(player, tile);
        if (!placed) {
            throw new IllegalStateException("Tile " + placeTotem.getTileId() + " occupata o non disponibile");
        }

        playersWhoPlaced.add(playerNickname);
        //listener
        ctx.notifyListeners(l -> l.onTotemPlaced(playerNickname, placeTotem.getTileId()));
        currentTurnIndex++;
        advanceToNextConnectedPlayer(ctx);

        // se tutti hanno piazzato → passa ad ActionResolutionState
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
            currentTurnIndex++;
            advanceToNextConnectedPlayer(ctx);
            notifyCurrentPlayer(ctx);
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
        advanceToNextConnectedPlayer(ctx);
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
