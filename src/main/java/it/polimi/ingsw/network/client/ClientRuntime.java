package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.view.View;

/**
 * Builds the common client objects after a transport connection has been created.
 */
final class ClientRuntime {

    private ClientRuntime() {
    }

    static void run(ClientSideConnection connection, ClientConnectionFactory connectionFactory) {
        run(connection, connectionFactory, () -> {
        });
    }

    static void run(ClientSideConnection connection, ClientConnectionFactory connectionFactory,
                    StartupStep beforeViewRun) {
        // From here on, socket and RMI clients use the same model, controller and view.
        ClientModel model = new ClientModel();
        ClientController controller = new ClientController(model, connection, connectionFactory);
        View view = ClientViewSelector.select(controller, model);
        controller.setView(view);

        try {
            // RMI uses this hook to register its callback; socket leaves it empty.
            beforeViewRun.execute();
            view.run();
        } catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            exception.printStackTrace(System.err);
            System.exit(1);
        } finally {
            connection.close();
        }
    }

    @FunctionalInterface
    interface StartupStep {
        void execute() throws Exception;
    }
}
