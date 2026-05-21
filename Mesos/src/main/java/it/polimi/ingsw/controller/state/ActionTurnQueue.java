package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.board.OfferTile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class ActionTurnQueue implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final class OrderedPlayer implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Player player;
        private final OfferTile tile;

        private OrderedPlayer(Player player, OfferTile tile) {
            this.player = player;
            this.tile = tile;
        }
    }

    private final List<OrderedPlayer> orderedPlayers = new ArrayList<>();
    private int currentIndex;

    void initialize(Map<Player, OfferTile> playerChoices) {
        orderedPlayers.clear();
        for (Map.Entry<Player, OfferTile> entry : playerChoices.entrySet()) {
            orderedPlayers.add(new OrderedPlayer(entry.getKey(), entry.getValue()));
        }
        orderedPlayers.sort(Comparator.comparingInt(orderedPlayer -> orderedPlayer.tile.getLetter()));
        currentIndex = 0;
    }

    boolean hasCurrentPlayer() {
        return currentIndex < orderedPlayers.size();
    }

    Player currentPlayer() {
        if (!hasCurrentPlayer()) {
            return null;
        }
        return orderedPlayers.get(currentIndex).player;
    }

    OfferTile currentTile() {
        if (!hasCurrentPlayer()) {
            return null;
        }
        return orderedPlayers.get(currentIndex).tile;
    }

    String currentPlayerNickname() {
        Player player = currentPlayer();
        return player != null ? player.getNickname() : null;
    }

    boolean isCurrentPlayer(String nickname) {
        Player player = currentPlayer();
        return player != null && player.getNickname().equals(nickname);
    }

    void advance() {
        if (hasCurrentPlayer()) {
            currentIndex++;
        }
    }
}
