package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote callback object exported by a client.
 * The server invokes it to push clientbound {@link ServerMessage} DTOs to that client.
 */
public interface RemoteClient extends Remote {

    /**
     * Delivers a clientbound update produced by the server.
     *
     * @param message the message to deliver
     * @throws RemoteException if the callback cannot be reached
     */
    void onServerMessage(ServerMessage message) throws RemoteException;
}
