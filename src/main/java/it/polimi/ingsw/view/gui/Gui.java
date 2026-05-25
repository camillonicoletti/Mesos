package it.polimi.ingsw.view.gui;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.View;

public final class Gui implements View {
    private final ClientController controller;
    private final ClientModel model;

    public Gui(ClientController controller, ClientModel model) {
        this.controller = controller;
        this.model = model;
    }

    @Override
    public void run() {
        GuiApplication.launchWith(controller, model);
    }
}
