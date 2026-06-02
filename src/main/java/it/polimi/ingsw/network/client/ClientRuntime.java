package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.transport.ClientSideConnection;
import it.polimi.ingsw.view.View;

final class ClientRuntime {

    private ClientRuntime() {
    }

    static void run(ClientSideConnection connection) {
        run(connection, () -> {
        });
    }

    static void run(ClientSideConnection connection, StartupStep beforeViewRun) {
        ClientModel model = new ClientModel();
        ClientController controller = new ClientController(model, connection);
        View view = ClientViewSelector.select(controller, model);
        controller.setView(view);

        try {
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
