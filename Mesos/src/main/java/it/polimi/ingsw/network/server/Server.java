package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.Persistence;

import java.io.IOException;
import java.util.Optional;

public class Server {
    public static void main(String[] args) throws IOException {
        boolean freshStart = false;
        int port = 1234;

        for (String arg : args) {
            if ("--fresh".equalsIgnoreCase(arg)) {
                freshStart = true;
            } else {
                port = Integer.parseInt(arg);
            }
        }

        if (freshStart) {
            Persistence.clear();
        }

        Optional<GameController> restored = freshStart ? Optional.empty() : Persistence.load();
        LobbyManager lobby = restored
                .map(LobbyManager::new)
                .orElseGet(LobbyManager::new);

        if (restored.isPresent()) {
            System.out.println("Previous state restored from disk — "
                    + "waiting for players to reconnect.");
        } else if (freshStart) {
            System.out.println("Clean startup requested — no previous save will be restored.");
        }

        MessageDispatcher dispatcher = new MessageDispatcher(lobby);

        SocketAcceptLoop.runBlocking(port, lobby, dispatcher,
                "Server started on port ",
                "New connection: ");
    }
}
