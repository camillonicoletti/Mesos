package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.network.client.ClientController;
import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep bug-hunter tests for the TUI {@link CommandParser}.
 *
 * <p>The parser now executes commands through the {@link ClientController}
 * façade rather than returning a message, so these tests capture what was
 * dispatched (via {@link CapturingClientConnection}) and inspect it. A rejected
 * command must dispatch nothing.
 *
 * Focus on subtle parsing behaviours:
 *   - whitespace handling
 *   - case sensitivity
 *   - non-letter tile arguments
 *   - extra spaces between tokens
 *   - missing or partial arguments
 */
class CommandParserAdditionalCasesTest {

    private CapturingClientConnection connection;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        connection = new CapturingClientConnection();
        ClientController controller = new ClientController(new ClientModel(), connection);
        parser = new CommandParser(controller);
    }

    /** Parses {@code input} and returns the single message it dispatched, or null. */
    private ClientMessage dispatched(String input) {
        connection.sent.clear();
        parser.parse(input, () -> { });
        return connection.sent.isEmpty()
                ? null
                : connection.sent.get(connection.sent.size() - 1);
    }

    // ── Empty / whitespace inputs ────────────────────────────────────────────

    @Test
    void blankInputDispatchesNothing() {
        assertNull(dispatched(""));
        assertNull(dispatched("   "));
        assertNull(dispatched("\t"));
        assertNull(dispatched(null));
        assertFalse(parser.parse("", () -> { }));
    }

    // ── Place command edge cases ─────────────────────────────────────────────

    /**
     * The parser must accept lowercase tile letters and normalise them.
     * Bug indicator: returning null on lowercase would be a UX regression
     * since users frequently type lowercase.
     */
    @Test
    void placeAcceptsLowercaseAndUppercaseLetters() {
        ClientMessage upper = dispatched("place A");
        ClientMessage lower = dispatched("place a");

        assertInstanceOf(GameActionMessage.class, upper);
        assertInstanceOf(GameActionMessage.class, lower);

        PlaceTotemAction upAct = (PlaceTotemAction) ((GameActionMessage) upper).getAction();
        PlaceTotemAction loAct = (PlaceTotemAction) ((GameActionMessage) lower).getAction();
        assertEquals('A', upAct.getTileId());
        assertEquals('A', loAct.getTileId());
    }

    /**
     * Numeric or symbol "tiles" should not generate a placement action — the
     * server would reject them but the parser already has the information to
     * reject them client-side.  Currently it doesn't, which is a UX gap.
     */
    @Test
    void placeWithNonLetterArgumentIsAcceptedByParser() {
        // Ground truth check: the parser DOES dispatch an action even for digits.
        ClientMessage msg = dispatched("place 8");
        assertInstanceOf(GameActionMessage.class, msg,
                "Currently the parser accepts digits for 'place' — consider tightening it");
    }

    @Test
    void placeWithMultipleCharacterTileIsRejected() {
        assertNull(dispatched("place AB"));
        assertNull(dispatched("place AAA"));
    }

    // ── Pick / build with edge cases ─────────────────────────────────────────

    @Test
    void pickAcceptsMixedCaseRow() {
        ClientMessage upper = dispatched("pick 101 UPPER");
        ClientMessage upperMixed = dispatched("pick 101 Upper");
        ClientMessage lower = dispatched("pick 101 LOWER");

        assertInstanceOf(GameActionMessage.class, upper);
        assertInstanceOf(GameActionMessage.class, upperMixed);
        assertInstanceOf(GameActionMessage.class, lower);

        PickCardAction upAct = (PickCardAction) ((GameActionMessage) upper).getAction();
        PickCardAction loAct = (PickCardAction) ((GameActionMessage) lower).getAction();
        assertTrue(upAct.isFromUpperRow());
        assertFalse(loAct.isFromUpperRow());
    }

    @Test
    void buildAcceptsMixedCaseRow() {
        ClientMessage msg = dispatched("build 201 UpPeR");
        assertInstanceOf(GameActionMessage.class, msg);
        PickBuildingAction act = (PickBuildingAction) ((GameActionMessage) msg).getAction();
        assertTrue(act.isFromUpperRow());
        assertEquals("201", act.getCardId());
    }

    @Test
    void pickPreservesIdAsString() {
        ClientMessage msg = dispatched("pick   42   upper");
        assertInstanceOf(GameActionMessage.class, msg);
        PickCardAction act = (PickCardAction) ((GameActionMessage) msg).getAction();
        assertEquals("42", act.getCardId(),
                "Card id must be preserved as string regardless of whitespace");
    }

    /**
     * Pick with non-numeric id — the parser does not validate that the id
     * is a number, it passes the string through.  This is documented behaviour
     * because the server identifies cards by numeric id but the parser is
     * row-agnostic and simply forwards the string.
     */
    @Test
    void pickWithNonNumericIdIsAcceptedByParser() {
        ClientMessage msg = dispatched("pick foo upper");
        assertInstanceOf(GameActionMessage.class, msg,
                "Currently parser accepts non-numeric ids — server will reject them");
    }

    @Test
    void pickWithoutAnyArgumentsIsRejected() {
        assertNull(dispatched("pick"));
        assertNull(dispatched("build"));
    }

    @Test
    void pickWithOnlyIdIsRejected() {
        assertNull(dispatched("pick 101"));
        assertNull(dispatched("build 201"));
    }

    // ── Players command edge cases ───────────────────────────────────────────

    @Test
    void playersZeroOrNegativeRejected() {
        assertNull(dispatched("players 0"));
        assertNull(dispatched("players 1"));
        assertNull(dispatched("players -1"));
        assertNull(dispatched("players 6"));
        assertNull(dispatched("players 100"));
    }

    @Test
    void playersWithLetterRejected() {
        assertNull(dispatched("players two"));
        assertNull(dispatched("players abc"));
        assertNull(dispatched("players 2a"));
    }

    @Test
    void playersBoundariesAccepted() {
        assertNotNull(dispatched("players 2"));
        assertNotNull(dispatched("players 5"));
    }

    // ── Whitespace / multi-space tolerance ───────────────────────────────────

    @Test
    void leadingAndTrailingWhitespaceIsIgnored() {
        assertNotNull(dispatched("ready   "));
        assertNotNull(dispatched("  ready"));
        assertNotNull(dispatched("  ready  "));
    }

    @Test
    void multipleSpacesBetweenTokensAreCollapsed() {
        ClientMessage a = dispatched("place A");
        ClientMessage b = dispatched("place    A");
        assertInstanceOf(GameActionMessage.class, a);
        assertInstanceOf(GameActionMessage.class, b);
    }

    // ── Fully unknown commands ───────────────────────────────────────────────

    @Test
    void completelyUnknownCommandDispatchesNothing() {
        assertNull(dispatched("foobar"));
        assertNull(dispatched("attack 101"));
        assertNull(dispatched("steal 5"));
        assertNull(dispatched("12345"));
        boolean[] unknown = {false};
        assertFalse(parser.parse("foobar", () -> unknown[0] = true));
        assertTrue(unknown[0]);
    }

    // ── Help is NOT a known parser command (must be handled by Tui) ──────────

    /**
     * The parser must NOT treat 'help' as a known command — Tui handles it
     * before delegating to the parser.
     */
    @Test
    void helpIsNotAKnownParserCommand() {
        assertNull(dispatched("help"));
        boolean[] unknown = {false};
        assertFalse(parser.parse("help", () -> unknown[0] = true));
        assertTrue(unknown[0]);
    }
}
