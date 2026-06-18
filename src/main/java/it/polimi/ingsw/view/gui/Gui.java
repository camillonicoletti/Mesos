package it.polimi.ingsw.view.gui;

import it.polimi.ingsw.controller.leaderboard.LeaderboardEntry;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.view.View;

import java.util.List;
import java.util.function.Consumer;

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

    @Override public void onLoginResult()             { forward(GuiApplication::onLoginResult); }
    @Override public void onLobbyChanged()            { forward(GuiApplication::onBoardEvent); }
    @Override public void onGameStarted()             { forward(GuiApplication::onBoardEvent); }
    @Override public void onGameStateChanged()        { forward(GuiApplication::onBoardEvent); }
    @Override public void onBoardChanged()            { forward(GuiApplication::onBoardEvent); }
    @Override public void onEventResolved()           { forward(GuiApplication::onBoardEvent); }
    @Override public void onGameEnded()               { forward(GuiApplication::onBoardEvent); }
    @Override public void onError()                   { forward(GuiApplication::onBoardEvent); }
    @Override public void onConnectionLost()          { forward(GuiApplication::onBoardEvent); }
    @Override public void onGameSuspended()           { forward(GuiApplication::onBoardEvent); }
    @Override public void onGameResumed()             { forward(GuiApplication::onBoardEvent); }
    @Override public void onPlayerConnectionChanged() { forward(GuiApplication::onBoardEvent); }

    @Override
    public void showLeaderboard(int playersCount, List<LeaderboardEntry> entries) {
        forward(app -> app.onLeaderboard(playersCount, entries));
    }

    @Override
    public void showMyRank(int rank, int playersCount) {
        forward(app -> app.onMyRank(rank, playersCount));
    }

    private void forward(Consumer<GuiApplication> action) {
        GuiApplication app = GuiApplication.current();
        if (app != null) {
            action.accept(app);
        }
    }
}
