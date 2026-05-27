package it.polimi.ingsw.model.board;

import java.io.Serializable;
import java.util.List;

/**
 * The shared playing surface of a match. Owns the offer tiles used during
 * totem placement and the turn-order tile that ranks players each round.
 */
public class Board implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<OfferTile> offerTiles;
    private TurnOrderTile turnOrderTile;

    public Board(List<OfferTile> offerTiles, TurnOrderTile turnOrderTile) {
        this.offerTiles = offerTiles;
        this.turnOrderTile = turnOrderTile;
    }

    public List<OfferTile> getOfferTiles() {
        return offerTiles;
    }

    public TurnOrderTile getTurnOrderTile() {
        return turnOrderTile;
    }

    public void setTurnOrderTile(TurnOrderTile turnOrderTile) {
        this.turnOrderTile = turnOrderTile;
    }

    public void resetOfferTiles() {
        for (OfferTile tile : offerTiles) {
            tile.removeTotem();
        }
    }


}
