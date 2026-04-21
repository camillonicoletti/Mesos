package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.board.OfferTile;

import java.util.HashSet;
import java.util.Set;

public class TotemPlacementState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    // nicknames dei giocatori che hanno già piazzato il totem
    private final Set<String> playersWhoPlaced = new HashSet<>();

    @Override
    public void onEnter(GameController ctx) {
        playersWhoPlaced.clear();
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
            throw new IllegalArgumentException("Tile non trovata: " + placeTotem.getTileId());
        }

        // chiama il model
        boolean placed = ctx.getGame().getCurrentRound().placeTotemOffer(player, tile);
        if (!placed) {
            throw new IllegalStateException("Tile " + placeTotem.getTileId() + " occupata o non disponibile");
        }

        playersWhoPlaced.add(playerNickname);
        //listener
        ctx.notifyListeners(l -> l.onTotemPlaced(playerNickname, placeTotem.getTileId()));

        // se tutti hanno piazzato → passa ad ActionResolutionState
        if (haveAllConnectedPlayersPlaced(ctx)) {
            ctx.changeState(new ActionResolutionState());
        }
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }

        ctx.markPlayerDisconnected(playerNickname);
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

    private OfferTile findTile(GameController ctx, char tileId) {
        for (OfferTile tile : ctx.getGame().getBoard().getOfferTiles()) {
            if (tile.getLetter() == tileId) return tile;
        }
        return null;
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
}
