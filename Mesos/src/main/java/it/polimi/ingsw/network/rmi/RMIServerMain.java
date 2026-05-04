package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.Persistence;
import it.polimi.ingsw.network.server.LobbyManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Optional;

/**
 * Bootstrap for the RMI transport.
 */
public final class RMIServerMain {

    public static final String DEFAULT_BINDING_NAME = "MesosServer";

    private RMIServerMain() {
    }

    public static void main(String[] args) throws Exception {
        boolean freshStart = false;
        Integer registryPort = null;
        String bindingName = null;

        for (String arg : args) {
            if ("--fresh".equalsIgnoreCase(arg)) {
                freshStart = true;
            } else if (registryPort == null) {
                registryPort = Integer.parseInt(arg);
            } else if (bindingName == null) {
                bindingName = arg;
            }
        }

        if (registryPort == null) {
            registryPort = Registry.REGISTRY_PORT;
        }
        if (bindingName == null) {
            bindingName = DEFAULT_BINDING_NAME;
        }

        if (freshStart) {
            Persistence.clear();
        }

        Optional<GameController> restored = freshStart ? Optional.empty() : Persistence.load();
        LobbyManager lobbyManager = restored
                .map(LobbyManager::new)
                .orElseGet(LobbyManager::new);

        Registry registry = LocateRegistry.createRegistry(registryPort);
        RMIServer server = new RMIServer(lobbyManager);
        registry.rebind(bindingName, server);

        if (restored.isPresent()) {
            System.out.println("Stato precedente ripristinato da disco — "
                    + "in attesa che i giocatori si riconnettano.");
        } else if (freshStart) {
            System.out.println("Avvio pulito richiesto — nessun salvataggio precedente verra ripristinato.");
        }

        System.out.println("RMI server avviato sulla registry port " + registryPort
                + " con nome '" + bindingName + "'");
    }
}
