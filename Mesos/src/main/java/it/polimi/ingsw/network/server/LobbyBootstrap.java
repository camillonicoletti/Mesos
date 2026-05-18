package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.Persistence;
import it.polimi.ingsw.controller.leaderboard.LeaderboardService;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Centralizes server startup state: fresh starts, saved-game restore and lobby
 * creation must behave the same way for every transport bootstrap.
 */
public final class LobbyBootstrap {

    private LobbyBootstrap() {}

    public static LobbyManager create(boolean freshStart) {
        return create(freshStart, LobbyManager::new, LobbyManager::new);
    }

    public static LobbyManager create(boolean freshStart, LeaderboardService leaderboard) {
        return create(freshStart,
                () -> new LobbyManager(leaderboard),
                controller -> new LobbyManager(controller, leaderboard));
    }

    private static LobbyManager create(boolean freshStart,
                                       Supplier<LobbyManager> freshLobbyFactory,
                                       Function<GameController, LobbyManager> restoredLobbyFactory) {
        if (freshStart) {
            Persistence.clear();
        }

        Optional<GameController> restored = freshStart ? Optional.empty() : Persistence.load();
        LobbyManager lobby = restored
                .map(restoredLobbyFactory)
                .orElseGet(freshLobbyFactory);

        printStartupMessage(freshStart, restored.isPresent());
        return lobby;
    }

    private static void printStartupMessage(boolean freshStart, boolean restored) {
        if (restored) {
            System.out.println("Previous state restored from disk - "
                    + "waiting for players to reconnect.");
        } else if (freshStart) {
            System.out.println("Clean startup requested - no previous save will be restored.");
        }
    }
}
