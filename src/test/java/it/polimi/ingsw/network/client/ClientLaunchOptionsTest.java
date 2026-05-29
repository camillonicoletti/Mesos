package it.polimi.ingsw.network.client;

import it.polimi.ingsw.network.rmi.RMIServerMain;
import org.junit.jupiter.api.Test;

import java.rmi.registry.Registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for ClientLaunchOptions (network.client).
 *
 * <p>Verifies: command-line option parsing for the client entry point.
 */
class ClientLaunchOptionsTest {

    @Test
    void socketParsesHostAndPort() {
        ClientLaunchOptions options = ClientLaunchOptions.socket(new String[] {"localhost", "1234"});

        assertEquals("localhost", options.host());
        assertEquals(1234, options.socketPort());
    }

    @Test
    void socketRequiresExactlyHostAndPort() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientLaunchOptions.socket(new String[] {"localhost"}));
        assertThrows(IllegalArgumentException.class,
                () -> ClientLaunchOptions.socket(new String[] {"localhost", "1234", "extra"}));
    }

    @Test
    void rmiUsesDefaultPortAndBindingName() {
        ClientLaunchOptions options = ClientLaunchOptions.rmi(new String[] {"localhost"});

        assertEquals("localhost", options.host());
        assertEquals(Registry.REGISTRY_PORT, options.registryPort());
        assertEquals(RMIServerMain.DEFAULT_BINDING_NAME, options.rmiBindingName());
    }

    @Test
    void rmiParsesPortAndBindingName() {
        ClientLaunchOptions options = ClientLaunchOptions.rmi(
                new String[] {"localhost", "2099", "CustomServer"});

        assertEquals("localhost", options.host());
        assertEquals(2099, options.registryPort());
        assertEquals("CustomServer", options.rmiBindingName());
    }

    @Test
    void invalidPortsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientLaunchOptions.socket(new String[] {"localhost", "not-a-port"}));
        assertThrows(IllegalArgumentException.class,
                () -> ClientLaunchOptions.rmi(new String[] {"localhost", "not-a-port"}));
    }

    @Test
    void rmiRejectsTooManyArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> ClientLaunchOptions.rmi(new String[] {"localhost", "1099", "MesosServer", "extra"}));
    }
}
