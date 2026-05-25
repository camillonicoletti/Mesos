package it.polimi.ingsw.view.tui;

import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.network.protocol.serverbound.ClientMessage;
import it.polimi.ingsw.network.protocol.serverbound.GameActionMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep bug-hunter tests for the TUI {@link CommandParser}.
 *
 * Focus on subtle parsing behaviours:
 *   - whitespace handling
 *   - case sensitivity
 *   - non-letter tile arguments
 *   - extra spaces between tokens
 *   - missing or partial arguments
 */
class CommandParserAdditionalCasesTest {

    private final CommandParser parser = new CommandParser();

    // ── Empty / whitespace inputs ────────────────────────────────────────────

    @Test
    void blankInputReturnsNull() {
        assertNull(parser.parse(""));
        assertNull(parser.parse("   "));
        assertNull(parser.parse("\t"));
        assertNull(parser.parse(null));
    }

    // ── Place command edge cases ─────────────────────────────────────────────

    /**
     * The parser must accept lowercase tile letters and normalise them.
     * Bug indicator: returning null on lowercase would be a UX regression
     * since users frequently type lowercase.
     */
    @Test
    void placeAcceptsLowercaseAndUppercaseLetters() {
        ClientMessage upper = parser.parse("place A");
        ClientMessage lower = parser.parse("place a");

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
        // Ground truth check: the parser DOES build an action even for digits.
        // This documents existing behaviour and surfaces a possible UX bug —
        // the server will reject "place 8" with an error, which is noisier
        // than rejecting it locally.
        ClientMessage msg = parser.parse("place 8");
        assertInstanceOf(GameActionMessage.class, msg,
                "Currently the parser accepts digits for 'place' — consider tightening it");
    }

    @Test
    void placeWithMultipleCharacterTileIsRejected() {
        assertNull(parser.parse("place AB"));
        assertNull(parser.parse("place AAA"));
    }

    // ── Pick / build with edge cases ─────────────────────────────────────────

    @Test
    void pickAcceptsMixedCaseRow() {
        ClientMessage upper = parser.parse("pick 101 UPPER");
        ClientMessage upperMixed = parser.parse("pick 101 Upper");
        ClientMessage lower = parser.parse("pick 101 LOWER");

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
        ClientMessage msg = parser.parse("build 201 UpPeR");
        assertInstanceOf(GameActionMessage.class, msg);
        PickBuildingAction act = (PickBuildingAction) ((GameActionMessage) msg).getAction();
        assertTrue(act.isFromUpperRow());
        assertEquals("201", act.getCardId());
    }

    @Test
    void pickPreservesIdAsString() {
        ClientMessage msg = parser.parse("pick   42   upper");
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
        ClientMessage msg = parser.parse("pick foo upper");
        assertInstanceOf(GameActionMessage.class, msg,
                "Currently parser accepts non-numeric ids — server will reject them");
    }

    @Test
    void pickWithoutAnyArgumentsIsRejected() {
        assertNull(parser.parse("pick"));
        assertNull(parser.parse("build"));
    }

    @Test
    void pickWithOnlyIdIsRejected() {
        assertNull(parser.parse("pick 101"));
        assertNull(parser.parse("build 201"));
    }

    // ── Players command edge cases ───────────────────────────────────────────

    @Test
    void playersZeroOrNegativeRejected() {
        assertNull(parser.parse("players 0"));
        assertNull(parser.parse("players 1"));
        assertNull(parser.parse("players -1"));
        assertNull(parser.parse("players 6"));
        assertNull(parser.parse("players 100"));
    }

    @Test
    void playersWithLetterRejected() {
        assertNull(parser.parse("players two"));
        assertNull(parser.parse("players abc"));
        assertNull(parser.parse("players 2a"));
    }

    @Test
    void playersBoundariesAccepted() {
        assertNotNull(parser.parse("players 2"));
        assertNotNull(parser.parse("players 5"));
    }

    // ── Whitespace / multi-space tolerance ───────────────────────────────────

    @Test
    void leadingAndTrailingWhitespaceIsIgnored() {
        // Trailing whitespace
        assertNotNull(parser.parse("ready   "));
        assertNotNull(parser.parse("  ready"));
        assertNotNull(parser.parse("  ready  "));
    }

    @Test
    void multipleSpacesBetweenTokensAreCollapsed() {
        ClientMessage a = parser.parse("place A");
        ClientMessage b = parser.parse("place    A");
        // both should produce equivalent actions
        assertInstanceOf(GameActionMessage.class, a);
        assertInstanceOf(GameActionMessage.class, b);
    }

    // ── Fully unknown commands ───────────────────────────────────────────────

    @Test
    void completelyUnknownCommandReturnsNull() {
        assertNull(parser.parse("foobar"));
        assertNull(parser.parse("attack 101"));
        assertNull(parser.parse("steal 5"));
        assertNull(parser.parse("12345"));
    }

    // ── Help is NOT a known parser command (must be handled by Tui) ──────────

    /**
     * The parser must NOT treat 'help' as a known command — Tui handles it
     * before delegating to the parser.  If the parser silently accepted
     * 'help', it would emit a null result with a misleading warning.
     */
    @Test
    void helpIsNotConvertedToAClientMessage() {
        assertNull(parser.parse("help"));
    }
}
