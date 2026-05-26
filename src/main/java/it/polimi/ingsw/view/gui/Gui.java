package it.polimi.ingsw.view.gui;

import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.View;

import java.util.List;

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

    @Override
    public void showLeaderboard(int playersCount, List<LeaderboardEntry> entries) {
        // TODO: forward to a LeaderboardView JavaFX component once it exists.
        //   Platform.runLater(() -> ...);
    }

    @Override
    public void showMyRank(int rank, int playersCount) {
        // TODO: forward to EndGameView.setMyRank(rank, playersCount).
        //   Platform.runLater(() -> ...);
    }
}
