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

        // accetta solo PLACE_TOTEM
        if (!action.getType().equals("PLACE_TOTEM")) {
            throw new IllegalStateException("Azione non valida in questa fase: " + action.getType());
        }

        // verifica che il giocatore non abbia già piazzato
        if (playersWhoPlaced.contains(playerNickname)) {
            throw new IllegalStateException(playerNickname + " ha già piazzato il totem");
        }

        PlaceTotemAction placeTotem = (PlaceTotemAction) action;

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
        if (playersWhoPlaced.size() == ctx.getGame().getPlayers().size()) {
            ctx.changeState(new ActionResolutionState());
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
}