package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIClientConnection;
import it.polimi.ingsw.network.rmi.RemoteServer;
import it.polimi.ingsw.network.transport.ClientSideConnection;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/** Factory for RMI client connections. */
public final class RmiClientConnectionFactory implements ClientConnectionFactory {
    private final String host;
    private final int registryPort;
    private final String bindingName;

    public RmiClientConnectionFactory(String host, int registryPort, String bindingName) {
        this.host = host;
        this.registryPort = registryPort;
        this.bindingName = bindingName;
    }

    @Override
    public ClientSideConnection create() throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, registryPort);
        RemoteServer remoteServer = (RemoteServer) registry.lookup(bindingName);
        return new RMIClientConnection(remoteServer);
    }

    public String endpointDescription() {
        return host + ":" + registryPort + "/" + bindingName;
    }
}
