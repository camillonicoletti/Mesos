package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.network.protocol.clientbound.ServerMessage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Exported client-side RMI callback used by the server to push updates back to the client.
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
        try {
            // RMI calls this method from a remote thread; the owner forwards it to ClientController.
            owner.handleInbound(message);
        } catch (RuntimeException exception) {
            System.err.println("Error while handling RMI server message: " + exception.getMessage());
        }
    }

    public void shutdown() {
        try {
            // The callback must be unexported when the client closes.
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception ignored) {
            // Best-effort cleanup of the exported callback.
        }
    }
}
