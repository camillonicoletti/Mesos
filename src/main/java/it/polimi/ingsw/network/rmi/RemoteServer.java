package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.enums.TotemColor;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote entrypoint exposed by the server.
 * Clients invoke domain-level operations through this interface and receive
 * asynchronous updates via {@link RemoteClient}.
 */
public interface RemoteServer extends Remote {

    /**
     * Registers a remote client with the server before any operation is invoked.
     * Triggers the same lobby registration path used by socket clients on accept.
     */
    void register(RemoteClient client) throws RemoteException;

    void login(RemoteClient client, String nickname, TotemColor color) throws RemoteException;

    void reconnect(RemoteClient client, String nickname) throws RemoteException;

    void setPlayersCount(RemoteClient client, int count) throws RemoteException;

    void setReady(RemoteClient client, boolean ready) throws RemoteException;

    void performAction(RemoteClient client, GameAction action) throws RemoteException;

    void forceEndGame(RemoteClient client) throws RemoteException;

    void getLeaderboard(RemoteClient client, int playersCount, int limit) throws RemoteException;

    void quit(RemoteClient client) throws RemoteException;

    void pong(RemoteClient client) throws RemoteException;

    /**
     * Explicitly disconnects a client callback from the server.
     *
     * @param client the remote client to disconnect
     * @throws RemoteException if the invocation fails
     */
    void disconnect(RemoteClient client) throws RemoteException;

    /**
     * Lightweight liveness probe used by clients to detect server crashes.
     * The implementation must be a no-op: clients only care whether the call
     * returns normally or raises a {@link RemoteException}.
     */
    void ping() throws RemoteException;
}
