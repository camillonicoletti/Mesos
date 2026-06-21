package it.polimi.ingsw.model.board;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the shared board of the match.
 * It contains the offer tiles and the turn-order tile used every round.
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

    /**
     * Clears all totems from the offer tiles at the end of a placement phase.
     */
    public void resetOfferTiles() {
        for (OfferTile tile : offerTiles) {
            tile.removeTotem();
        }
    }


}
