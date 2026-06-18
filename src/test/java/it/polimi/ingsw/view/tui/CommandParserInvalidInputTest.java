package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug hunter tests for malformed TUI commands: a rejected command must send
 * nothing to the server.
 */
class CommandParserInvalidInputTest {

    private CapturingClientConnection connection;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        connection = new CapturingClientConnection();
        ClientController controller = new ClientController(new ClientModel(), connection);
        parser = new CommandParser(controller);
    }

    /** Parses {@code input} and asserts that nothing was dispatched to the server. */
    private void assertNothingSent(String input) {
        connection.sent.clear();
        parser.parse(input, () -> { });
        assertTrue(connection.sent.isEmpty(),
                "Command '" + input + "' should not dispatch any message");
    }

    /**
     * Invalid row names must not be silently interpreted as lower row.
     */
    @Test
    void invalidPickRowArgumentIsRejected() {
        assertNothingSent("pick 101 banana");
        assertNothingSent("pick 101 up");
        assertNothingSent("pick 101 123");
    }

    /**
     * Invalid row names must not create a building action on the lower row.
     */
    @Test
    void invalidBuildRowArgumentIsRejected() {
        assertNothingSent("build 201 banana");
        assertNothingSent("build 201 down");
        assertNothingSent("build 201 123");
    }

    /**
     * Commands with extra arguments are usually player mistakes and must not be accepted silently.
     */
    @Test
    void commandsWithTooManyArgumentsAreRejected() {
        assertNothingSent("ready now");
        assertNothingSent("unready please");
        assertNothingSent("players 2 now");
        assertNothingSent("place A B");
        assertNothingSent("pick 101 upper extra");
        assertNothingSent("build 201 lower extra");
        assertNothingSent("skip extra");
        assertNothingSent("endgame now");
    }

    /**
     * A bare 'quit' returns true (and dispatches nothing itself: the caller
     * performs the disconnect).
     */
    @Test
    void quitIsReportedAndSendsNothing() {
        connection.sent.clear();
        assertTrue(parser.parse("quit", () -> { }));
        assertTrue(connection.sent.isEmpty());
    }
}
