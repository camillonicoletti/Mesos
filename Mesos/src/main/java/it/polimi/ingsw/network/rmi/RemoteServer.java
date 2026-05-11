package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote entrypoint exposed by the server.
 * Clients push {@link ClientMessage}s through this interface and receive
 * asynchronous updates via {@link RemoteClient}.
 */
public interface RemoteServer extends Remote {

    /**
     * Delivers a client-side message to the server.
     *
     * @param client  the remote callback associated with the sender
     * @param message the message to process
     * @throws RemoteException if the invocation fails
     */
    void send(RemoteClient client, ClientMessage message) throws RemoteException;

    /**
     * Explicitly disconnects a client callback from the server.
     *
     * @param client the remote client to disconnect
     * @throws RemoteException if the invocation fails
     */
    void disconnect(RemoteClient client) throws RemoteException;

    /**
     * Registers a remote client with the server before any message is sent.
     * Triggers the same lobby registration path used by socket clients on accept.
     */
    void register(RemoteClient client) throws RemoteException;

    /**
     * Lightweight liveness probe used by clients to detect server crashes.
     * The implementation must be a no-op: clients only care whether the call
     * returns normally or raises a {@link RemoteException}.
     */
    void ping() throws RemoteException;
}
