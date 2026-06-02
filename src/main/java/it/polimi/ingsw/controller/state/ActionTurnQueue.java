package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.board.OfferTile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Internal helper of {@link ActionResolutionState}: orders players by the
 * offer tile they chose during totem placement and yields them in tile order
 * so the action phase resolves deterministically.
 */
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

    boolean hasPendingPlayer(String nickname) {
        if (nickname == null) {
            return false;
        }
        for (int i = currentIndex; i < orderedPlayers.size(); i++) {
            if (nickname.equals(orderedPlayers.get(i).player.getNickname())) {
                return true;
            }
        }
        return false;
    }

    void advance() {
        if (hasCurrentPlayer()) {
            currentIndex++;
        }
    }
}
