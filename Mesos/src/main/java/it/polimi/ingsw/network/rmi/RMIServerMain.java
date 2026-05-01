package it.polimi.ingsw.network.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Bootstrap for the RMI transport.
 */
public final class RMIServerMain {

    public static final String DEFAULT_BINDING_NAME = "MesosServer";

    private RMIServerMain() {
    }

    public static void main(String[] args) throws Exception {
        int registryPort = args.length > 0 ? Integer.parseInt(args[0]) : Registry.REGISTRY_PORT;
        String bindingName = args.length > 1 ? args[1] : DEFAULT_BINDING_NAME;

        Registry registry = LocateRegistry.createRegistry(registryPort);
        RMIServer server = new RMIServer();
        registry.rebind(bindingName, server);

        System.out.println("RMI server avviato sulla registry port " + registryPort
                + " con nome '" + bindingName + "'");
    }
}
