package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote callback exposed by a client so the server can push {@link ServerMessage}s.
 */
public interface RemoteClient extends Remote {

    /**
     * Delivers a server-side message to the remote client.
     *
     * @param message the message to deliver
     * @throws RemoteException if the callback cannot be reached
     */
    void onServerMessage(ServerMessage message) throws RemoteException;
}
