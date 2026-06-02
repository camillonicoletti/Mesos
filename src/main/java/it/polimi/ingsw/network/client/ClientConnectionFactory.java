package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.transport.ClientSideConnection;

/** Creates a fresh client connection using the same transport chosen at launch. */
@FunctionalInterface
public interface ClientConnectionFactory {
    ClientSideConnection create() throws Exception;
}
