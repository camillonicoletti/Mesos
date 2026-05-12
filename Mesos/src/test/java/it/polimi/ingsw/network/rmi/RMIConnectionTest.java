package it.polimi.ingsw.network.rmi;

import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.LoginAckMessage;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.LoginMessage;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RMIConnectionTest {

    @Test
    void clientReceivesLoginAckThroughRemoteCallback() throws Exception {
        EchoingRemoteServer server = new EchoingRemoteServer();
        RMIClientConnection client = new RMIClientConnection(server);
        CountDownLatch ackReceived = new CountDownLatch(1);
        AtomicReference<LoginAckMessage> ack = new AtomicReference<>();

        try {
            client.setOnMessage(message -> {
                if (message instanceof LoginAckMessage loginAck) {
                    ack.set(loginAck);
                    ackReceived.countDown();
                }
            });

            client.send(new LoginMessage("RmiSmoke", TotemColor.RED));

            assertTrue(ackReceived.await(2, TimeUnit.SECONDS));
            assertEquals(LoginAckMessage.Outcome.LOGIN_OK, ack.get().getOutcome());
        } finally {
            client.close();
        }
    }

    private static class EchoingRemoteServer implements RemoteServer {
        @Override
        public void send(RemoteClient client, ClientMessage message) throws RemoteException {
            if (message instanceof LoginMessage login
                    && "RmiSmoke".equals(login.getNickname())
                    && login.getTotemColor() == TotemColor.RED) {
                client.onServerMessage(new LoginAckMessage(LoginAckMessage.Outcome.LOGIN_OK, null));
            }
        }

        @Override
        public void disconnect(RemoteClient client) {
        }

        @Override
        public void register(RemoteClient client) {
        }

        @Override
        public void ping() {
        }
    }
}
