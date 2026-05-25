package it.polimi.ingsw.view.tui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Bug hunter tests for malformed TUI commands.
 */
class CommandParserInvalidInputTest {

    /**
     * Invalid row names must not be silently interpreted as lower row.
     */
    @Test
    void invalidPickRowArgumentIsRejected() {
        CommandParser parser = new CommandParser();

        assertNull(parser.parse("pick 101 banana"));
        assertNull(parser.parse("pick 101 up"));
        assertNull(parser.parse("pick 101 123"));
    }

    /**
     * Invalid row names must not create a building action on the lower row.
     */
    @Test
    void invalidBuildRowArgumentIsRejected() {
        CommandParser parser = new CommandParser();

        assertNull(parser.parse("build 201 banana"));
        assertNull(parser.parse("build 201 down"));
        assertNull(parser.parse("build 201 123"));
    }
    /**
     * Commands with extra arguments are usually player mistakes and must not be accepted silently.
     */
    @Test
    void commandsWithTooManyArgumentsAreRejected() {
        CommandParser parser = new CommandParser();

        assertNull(parser.parse("ready now"));
        assertNull(parser.parse("unready please"));
        assertNull(parser.parse("players 2 now"));
        assertNull(parser.parse("place A B"));
        assertNull(parser.parse("pick 101 upper extra"));
        assertNull(parser.parse("build 201 lower extra"));
    }
}

