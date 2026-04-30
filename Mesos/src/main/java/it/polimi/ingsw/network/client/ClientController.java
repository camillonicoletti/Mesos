package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.protocol.clientbound.*;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.PongMessage;
import it.polimi.ingsw.network.transport.ClientSideConnection;

public class ClientController {

    private final ClientModel model;
    private final ClientSideConnection connection;

    public ClientController(ClientModel model, ClientSideConnection connection) {
        this.model = model;
        this.connection = connection;
        connection.setOnMessage(this::onMessage);
        connection.setOnDisconnect(this::onDisconnect);
    }

    public void send(ClientMessage message) {
        try {
            connection.send(message);
        } catch (RuntimeException exception) {
            model.applyConnectionLost("Connessione al server interrotta");
            throw exception;
        }
    }

    public void onMessage(ServerMessage message) {
        if (message instanceof LoginAckMessage m)           model.applyLoginAck(m);
        else if (message instanceof LobbyUpdateMessage m)   model.applyLobbyUpdate(m);
        else if (message instanceof GameStartedMessage m)   model.applyGameStarted(m);
        else if (message instanceof GameSyncMessage m)      model.applyGameSync(m);
        else if (message instanceof StateChangedMessage m)  model.applyStateChanged(m);
        else if (message instanceof TotemPlacedMessage m)   model.applyTotemPlaced(m);
        else if (message instanceof EraChangedMessage m)    model.applyEraChanged(m);
        else if (message instanceof FoodChangedMessage m)   model.applyFoodChanged(m);
        else if (message instanceof PointsChangedMessage m) model.applyPointsChanged(m);
        else if (message instanceof CharacterCardTakenMessage m) model.applyCharacterCardTaken(m);
        else if (message instanceof BuildingCardTakenMessage m)  model.applyBuildingCardTaken(m);
        else if (message instanceof RoundEndedMessage m)    model.applyRoundEnded(m);
        else if (message instanceof GameEndedMessage m)     model.applyGameEnded(m);
        else if (message instanceof ErrorMessage m)         model.applyError(m);
        else if (message instanceof PingMessage)            connection.send(new PongMessage());
        else if (message instanceof GameSuspendedMessage m)        model.applyGameSuspended(m);
        else if (message instanceof GameResumedMessage m)          model.applyGameResumed(m);
        else if (message instanceof PlayerDisconnectedMessage m)   model.applyPlayerDisconnected(m);
        else if (message instanceof PlayerReconnectedMessage m)    model.applyPlayerReconnected(m);
        else if (message instanceof ExtraCardPickAvailableMessage m) model.applyExtraCardPickAvailable(m);
    }

    private void onDisconnect() {
        model.applyConnectionLost("Connessione al server interrotta");
    }
}