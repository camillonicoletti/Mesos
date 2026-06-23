package it.polimi.ingsw.network.server;

import it.polimi.ingsw.network.rmi.RMIServerMain;
import org.junit.jupiter.api.Test;

import java.rmi.registry.Registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ServerLaunchOptions (network.server).
 *
 * Verifies: command-line option parsing for the server entry point.
 */
class ServerLaunchOptionsTest {

    @Test
    void socketOnlyUsesDefaults() {
        ServerLaunchOptions options = ServerLaunchOptions.socketOnly(new String[0]);

        assertFalse(options.freshStart());
        assertEquals(ServerLaunchOptions.DEFAULT_SOCKET_PORT, options.socketPort());
    }

    @Test
    void socketOnlyParsesFreshStartAndPort() {
        ServerLaunchOptions options = ServerLaunchOptions.socketOnly(new String[] {"--fresh", "4242"});

        assertTrue(options.freshStart());
        assertEquals(4242, options.socketPort());
    }

    @Test
    void rmiOnlyUsesDefaults() {
        ServerLaunchOptions options = ServerLaunchOptions.rmiOnly(new String[0]);

        assertFalse(options.freshStart());
        assertEquals(Registry.REGISTRY_PORT, options.rmiPort());
        assertEquals(RMIServerMain.DEFAULT_BINDING_NAME, options.rmiBindingName());
    }

    @Test
    void rmiOnlyParsesFreshStartPortAndBindingName() {
        ServerLaunchOptions options = ServerLaunchOptions.rmiOnly(
                new String[] {"--fresh", "2099", "CustomServer"});

        assertTrue(options.freshStart());
        assertEquals(2099, options.rmiPort());
        assertEquals("CustomServer", options.rmiBindingName());
    }

    @Test
    void rmiOnlyParsesAdvertisedHost() {
        ServerLaunchOptions options = ServerLaunchOptions.rmiOnly(
                new String[] {"2099", "CustomServer", "172.20.10.2"});

        assertEquals("172.20.10.2", options.rmiHost());
    }

    @Test
    void unifiedParsesSocketRmiAndBindingName() {
        ServerLaunchOptions options = ServerLaunchOptions.unified(
                new String[] {"--fresh", "1235", "2099", "UnifiedServer"});

        assertTrue(options.freshStart());
        assertEquals(1235, options.socketPort());
        assertEquals(2099, options.rmiPort());
        assertEquals("UnifiedServer", options.rmiBindingName());
    }

    @Test
    void unifiedParsesAdvertisedHost() {
        ServerLaunchOptions options = ServerLaunchOptions.unified(
                new String[] {"1235", "2099", "UnifiedServer", "172.20.10.2"});

        assertEquals("172.20.10.2", options.rmiHost());
    }

    @Test
    void invalidPortIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ServerLaunchOptions.unified(new String[] {"not-a-port"}));
    }

    @Test
    void tooManyArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ServerLaunchOptions.socketOnly(new String[] {"1234", "extra"}));
    }
}
