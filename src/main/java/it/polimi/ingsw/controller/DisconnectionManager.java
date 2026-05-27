package it.polimi.ingsw.controller;

import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Owns the disconnect bookkeeping on behalf of the {@link GameController}:
 * marks players connected/disconnected, counts how many game players are
 * still reachable and exposes the only-connected-player lookup used by the
 * suspension timeout flow (FA4).
 */
public class DisconnectionManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public void forget(String nickname, Set<String> disconnectedPlayers) {
        disconnectedPlayers.remove(nickname);
    }

    public void markDisconnected(String nickname, Set<String> disconnectedPlayers, boolean knownPlayer) {
        validateNickname(nickname);
        if (!knownPlayer) {
            throw new IllegalArgumentException("Unknown player");
        }

        disconnectedPlayers.add(nickname);
    }

    public void markConnected(String nickname, Set<String> disconnectedPlayers) {
        validateNickname(nickname);
        disconnectedPlayers.remove(nickname);
    }

    public boolean isDisconnected(String nickname, Set<String> disconnectedPlayers) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        return disconnectedPlayers.contains(nickname);
    }

    public void markAllDisconnected(List<Player> lobbyPlayers,
                                    Game game,
                                    Set<String> disconnectedPlayers) {
        for (Player player : lobbyPlayers) {
            disconnectedPlayers.add(player.getNickname());
        }
        if (game != null) {
            for (Player player : game.getPlayers()) {
                disconnectedPlayers.add(player.getNickname());
            }
        }
    }

    public int countConnectedGamePlayers(Game game, Set<String> disconnectedPlayers) {
        if (game == null) {
            return 0;
        }

        int connectedPlayersCount = 0;
        for (Player player : game.getPlayers()) {
            if (!isDisconnected(player.getNickname(), disconnectedPlayers)) {
                connectedPlayersCount++;
            }
        }
        return connectedPlayersCount;
    }

    public Player onlyConnectedGamePlayer(Game game, Set<String> disconnectedPlayers) {
        if (game == null) {
            return null;
        }

        Player connectedPlayer = null;
        for (Player player : game.getPlayers()) {
            if (isDisconnected(player.getNickname(), disconnectedPlayers)) {
                continue;
            }

            if (connectedPlayer != null) {
                return null;
            }

            connectedPlayer = player;
        }

        return connectedPlayer;
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be null or blank");
        }
    }
}
