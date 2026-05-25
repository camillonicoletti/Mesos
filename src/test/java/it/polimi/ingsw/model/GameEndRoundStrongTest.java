package it.polimi.ingsw.model;

import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEndRoundStrongTest {

    /**
     * Verifies that lower-row cleanup discards only characters and events.
     * Buildings already in the lower row must remain available across normal round cleanup.
     */
    @Test
    void discardLowerRowRemovesCharactersAndEventsButKeepsBuildings() {
        Game game = createGame(Era.I, createPlayer("Alice", TotemColor.RED), createPlayer("Bob", TotemColor.BLUE));
        HunterCard lowerCharacter = new HunterCard(1, Era.I, 2, false);
        HuntEventCard lowerEvent = new HuntEventCard(2, Era.I, false, 1, 1);
        BuildingCard lowerBuilding = new BuildingCard(3, Era.I, 0, new EndPPEffect(), 5);

        Round round = createRound(game,
                List.of(lowerEvent),
                List.of(),
                List.of(lowerCharacter),
                List.of(),
                List.of(lowerBuilding),
                List.of());

        round.discardLowerRow();

        assertTrue(round.getLowerRowTribute().isEmpty());
        assertTrue(round.getLowerRowEvents().isEmpty());
        assertEquals(List.of(lowerBuilding), round.getLowerRowBuildings());
    }

    /**
     * Verifies that shifting the upper row to the lower row moves only tribe cards and events.
     * Upper-row buildings must not be moved during normal cleanup; they move only on era transition.
     */
    @Test
    void shiftUpperToLowerMovesOnlyCharactersAndEventsNotBuildings() {
        Game game = createGame(Era.I, createPlayer("Alice", TotemColor.RED), createPlayer("Bob", TotemColor.BLUE));
        HunterCard upperCharacter = new HunterCard(10, Era.I, 2, false);
        HuntEventCard upperEvent = new HuntEventCard(11, Era.I, false, 1, 1);
        BuildingCard upperBuilding = new BuildingCard(12, Era.I, 0, new EndPPEffect(), 5);
        BuildingCard lowerBuilding = new BuildingCard(13, Era.I, 0, new EndPPEffect(), 3);

        Round round = createRound(game,
                List.of(),
                List.of(upperEvent),
                List.of(),
                List.of(upperCharacter),
                List.of(lowerBuilding),
                List.of(upperBuilding));

        round.shiftUpperToLowerRow();

        assertEquals(List.of(upperCharacter), round.getLowerRowTribute());
        assertEquals(List.of(upperEvent), round.getLowerRowEvents());
        assertTrue(round.getUpperRowTribute().isEmpty());
        assertTrue(round.getUpperRowEvents().isEmpty());
        assertEquals(List.of(lowerBuilding), round.getLowerRowBuildings());
        assertEquals(List.of(upperBuilding), round.getUpperRowBuildings());
    }

    /**
     * Verifies that upper-row refill draws until the row reaches players + 4 cards,
     * counting both character cards and event cards as tribe-row cards.
     */
    @Test
    void refillUpperRowDrawsUntilPlayersPlusFourCountingEvents() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Player bob = createPlayer("Bob", TotemColor.BLUE);

        List<Card> deck = new ArrayList<>(List.of(
                new HunterCard(20, Era.I, 2, false),
                new HuntEventCard(21, Era.I, false, 1, 1),
                new HunterCard(22, Era.I, 2, false),
                new HunterCard(23, Era.I, 2, false),
                new HuntEventCard(24, Era.I, false, 1, 1),
                new HunterCard(25, Era.I, 2, false),
                new HunterCard(26, Era.I, 2, false)
        ));

        Game game = createGameWithDeck(Era.I, deck, new ArrayList<>(), new ArrayList<>(), alice, bob);

        Round round = createRound(game,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        round.refillUpperRow();

        assertEquals(6, round.getUpperTributeRowSize(),
                "With two players, the upper row must contain players + 4 tribe/event cards.");
        assertEquals(4, round.getUpperRowTribute().size());
        assertEquals(2, round.getUpperRowEvents().size());
        assertEquals(1, game.getTributeDeck().size());
    }

    /**
     * Verifies that refill does not fail when the tribute deck is empty.
     * The upper row may remain incomplete only because no more cards can be drawn.
     */
    @Test
    void refillUpperRowWithEmptyDeckDoesNotThrowAndKeepsCurrentCards() {
        Game game = createGameWithDeck(
                Era.I,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                createPlayer("Alice", TotemColor.RED),
                createPlayer("Bob", TotemColor.BLUE)
        );

        HunterCard existingUpperCard = new HunterCard(30, Era.I, 2, false);

        Round round = createRound(game,
                List.of(),
                List.of(),
                List.of(),
                List.of(existingUpperCard),
                List.of(),
                List.of());

        assertDoesNotThrow(round::refillUpperRow);

        assertEquals(List.of(existingUpperCard), round.getUpperRowTribute());
        assertTrue(round.getUpperRowEvents().isEmpty());
        assertTrue(game.getTributeDeck().isEmpty());
    }

    /**
     * Verifies the Era I to Era II transition after an Era II tribe card is revealed:
     * current upper buildings move to the lower row and Era II buildings are revealed.
     */
    @Test
    void eraOneToEraTwoTransitionMovesUpperBuildingsToLowerAndRevealsEraTwoBuildings() {
        BuildingCard eraOneBuilding = new BuildingCard(40, Era.I, 0, new EndPPEffect(), 1);
        BuildingCard eraTwoBuildingA = new BuildingCard(41, Era.II, 0, new EndPPEffect(), 2);
        BuildingCard eraTwoBuildingB = new BuildingCard(42, Era.II, 0, new EndPPEffect(), 3);

        Game game = createGameWithDeck(
                Era.I,
                new ArrayList<>(),
                new ArrayList<>(List.of(eraTwoBuildingA, eraTwoBuildingB)),
                new ArrayList<>(),
                createPlayer("Alice", TotemColor.RED),
                createPlayer("Bob", TotemColor.BLUE)
        );

        Round round = createRound(game,
                List.of(),
                List.of(),
                List.of(),
                List.of(new HunterCard(43, Era.II, 2, false)),
                List.of(),
                List.of(eraOneBuilding));

        round.checkEraTransition();

        assertEquals(Era.II, game.getCurrentEra());
        assertEquals(List.of(eraOneBuilding), round.getLowerRowBuildings());
        assertEquals(List.of(eraTwoBuildingA, eraTwoBuildingB), round.getUpperRowBuildings());
        assertTrue(game.getBuildingsForEra(Era.II).isEmpty(),
                "Era II building deck must be consumed after revealing buildings.");
    }

    /**
     * Verifies the Era II to Era III transition rule:
     * lower-row buildings are discarded, current upper buildings move down, and Era III buildings are revealed.
     */
    @Test
    void eraTwoToEraThreeTransitionDiscardsLowerBuildingsThenMovesUpperBuildingsDown() {
        BuildingCard oldLowerBuilding = new BuildingCard(50, Era.I, 0, new EndPPEffect(), 1);
        BuildingCard eraTwoUpperBuilding = new BuildingCard(51, Era.II, 0, new EndPPEffect(), 2);
        BuildingCard eraThreeBuilding = new BuildingCard(52, Era.III, 0, new EndPPEffect(), 3);

        Game game = createGameWithDeck(
                Era.II,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(List.of(eraThreeBuilding)),
                createPlayer("Alice", TotemColor.RED),
                createPlayer("Bob", TotemColor.BLUE)
        );

        Round round = createRound(game,
                List.of(),
                List.of(),
                List.of(),
                List.of(new HunterCard(53, Era.III, 2, false)),
                List.of(oldLowerBuilding),
                List.of(eraTwoUpperBuilding));

        round.checkEraTransition();

        assertEquals(Era.III, game.getCurrentEra());
        assertFalse(round.getLowerRowBuildings().contains(oldLowerBuilding),
                "Old lower-row buildings must be discarded at the beginning of Era III.");
        assertEquals(List.of(eraTwoUpperBuilding), round.getLowerRowBuildings());
        assertEquals(List.of(eraThreeBuilding), round.getUpperRowBuildings());
        assertTrue(game.getBuildingsForEra(Era.III).isEmpty());
    }

    /**
     * Verifies that creating the next round preserves the cleaned board rows
     * but starts with no player offer choices from the previous round.
     */
    @Test
    void nextRoundCopiesRowsButDoesNotCarryPlayerChoices() {
        Player alice = createPlayer("Alice", TotemColor.RED);
        Player bob = createPlayer("Bob", TotemColor.BLUE);
        Game game = createGame(Era.I, alice, bob);

        HunterCard lowerCharacter = new HunterCard(60, Era.I, 2, false);
        HuntEventCard lowerEvent = new HuntEventCard(61, Era.I, false, 1, 1);
        BuildingCard lowerBuilding = new BuildingCard(62, Era.I, 0, new EndPPEffect(), 1);
        BuildingCard upperBuilding = new BuildingCard(63, Era.I, 0, new EndPPEffect(), 2);

        Round round = createRound(game,
                List.of(lowerEvent),
                List.of(),
                List.of(lowerCharacter),
                List.of(),
                List.of(lowerBuilding),
                List.of(upperBuilding));

        Round nextRound = round.createNextRound(2);

        assertEquals(2, nextRound.getNumber());
        assertEquals(RoundPhase.TOTEM_PLACEMENT, nextRound.getPhase());
        assertEquals(List.of(lowerCharacter), nextRound.getLowerRowTribute());
        assertEquals(List.of(lowerEvent), nextRound.getLowerRowEvents());
        assertEquals(List.of(lowerBuilding), nextRound.getLowerRowBuildings());
        assertEquals(List.of(upperBuilding), nextRound.getUpperRowBuildings());
        assertTrue(nextRound.getPlayerChoices().isEmpty(),
                "Player choices from the previous round must never leak into the next round.");
    }

    private Player createPlayer(String nickname, TotemColor color) {
        return new Player(nickname, new Totem(color, "OFFER_TRACK"));
    }

    private Game createGame(Era currentEra, Player... players) {
        return createGameWithDeck(
                currentEra,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                players
        );
    }

    private Game createGameWithDeck(Era currentEra,
                                    List<Card> tributeDeck,
                                    List<BuildingCard> era2Buildings,
                                    List<BuildingCard> era3Buildings,
                                    Player... players) {
        return new Game(
                new ArrayList<>(List.of(players)),
                1,
                null,
                currentEra,
                LocalDateTime.now(),
                (Board) null,
                tributeDeck,
                new ArrayList<>(),
                era2Buildings,
                era3Buildings
        );
    }

    private Round createRound(Game game,
                              List<EventCard> lowerEvents,
                              List<EventCard> upperEvents,
                              List<? extends CharacterCard> lowerCharacters,
                              List<? extends CharacterCard> upperCharacters,
                              List<BuildingCard> lowerBuildings,
                              List<BuildingCard> upperBuildings) {
        Round round = new Round(
                1,
                game,
                RoundPhase.ROUND_CLEANUP,
                game.getCurrentEra(),
                new ArrayList<>(lowerEvents),
                new ArrayList<>(upperEvents),
                new ArrayList<>(lowerCharacters),
                new ArrayList<>(upperCharacters),
                new ArrayList<>(lowerBuildings),
                new ArrayList<>(upperBuildings)
        );

        game.setCurrentRound(round);
        return round;
    }
}