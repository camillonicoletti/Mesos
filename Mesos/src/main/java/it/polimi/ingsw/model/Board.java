package it.polimi.ingsw.model;

import java.util.List;

public class Board {

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
}
