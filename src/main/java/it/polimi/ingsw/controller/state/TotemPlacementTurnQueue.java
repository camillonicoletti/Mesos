package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Totem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Internal helper of TotemPlacementState: yields the next player who still has to place their totem
// this round, following the turn-order tile, and skips already-placed players on resume.
final class TotemPlacementTurnQueue implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Set<String> playersWhoPlaced = new HashSet<>();
    private final List<String> placementOrder = new ArrayList<>();
    private int currentTurnIndex;

    void initialize(GameController ctx) {
        playersWhoPlaced.clear();
        placementOrder.clear();

        for (Totem totem : ctx.getGame().getBoard().getTurnOrderTile().getTurnOrder()) {
            Player owner = findPlayerByTotem(ctx, totem);
            if (owner != null) {
                placementOrder.add(owner.getNickname());
            }
        }

        // Defensive fallback if the turn-order tile has no occupants or no totem matches; join order guarantees initialization completes.
        if (placementOrder.isEmpty()) {
            for (Player player : ctx.getGame().getPlayers()) {
                placementOrder.add(player.getNickname());
            }
        }

        currentTurnIndex = 0;
        advanceToNextAvailablePlayer(ctx);
    }

    String currentPlayerNickname() {
        if (currentTurnIndex >= placementOrder.size()) {
            return null;
        }
        return placementOrder.get(currentTurnIndex);
    }

    boolean hasPlaced(String nickname) {
        return playersWhoPlaced.contains(nickname);
    }

    void unmarkPlaced(String nickname) {
        playersWhoPlaced.remove(nickname);
    }

    void markPlaced(String nickname, GameController ctx) {
        playersWhoPlaced.add(nickname);
        currentTurnIndex++;
        advanceToNextAvailablePlayer(ctx);
    }

    void skipCurrentPlayer(GameController ctx) {
        currentTurnIndex++;
        advanceToNextAvailablePlayer(ctx);
    }

    void advanceToNextAvailablePlayer(GameController ctx) {
        // Skip players who are disconnected or have already placed their totem this round.
        while (currentTurnIndex < placementOrder.size()) {
            String nickname = placementOrder.get(currentTurnIndex);
            if (!ctx.isPlayerUnavailableThisRound(nickname) && !playersWhoPlaced.contains(nickname)) {
                return;
            }
            currentTurnIndex++;
        }
    }

    boolean haveAllConnectedPlayersPlaced(GameController ctx) {
        for (Player player : ctx.getGame().getPlayers()) {
            if (!ctx.isPlayerUnavailableThisRound(player.getNickname())
                    && !playersWhoPlaced.contains(player.getNickname())) {
                return false;
            }
        }
        return true;
    }

    private Player findPlayerByTotem(GameController ctx, Totem totem) {
        if (totem == null || totem.getColor() == null) {
            return null;
        }
        for (Player player : ctx.getGame().getPlayers()) {
            if (player.getTotem() != null && player.getTotem().getColor() == totem.getColor()) {
                return player;
            }
        }
        return null;
    }
}
