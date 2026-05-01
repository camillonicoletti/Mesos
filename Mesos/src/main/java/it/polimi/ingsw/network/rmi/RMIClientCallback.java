package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Client-side RMI callback implementation used by {@link RMIClientConnection}.
 */
public class RMIClientCallback extends UnicastRemoteObject implements RemoteClient {

    private final RMIClientConnection owner;

    public RMIClientCallback(RMIClientConnection owner) throws RemoteException {
        super();
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        this.owner = owner;
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        owner.handleInbound(message);
    }

    public void shutdown() {
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception ignored) {
            // Best-effort cleanup of the exported callback.
        }
    }
}
