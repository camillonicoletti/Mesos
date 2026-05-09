package it.polimi.ingsw.model;

import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bug hunter tests for Round board encapsulation.
 * External code must not be able to mutate board rows directly through getters.
 */
class RoundEncapsulationBugHunterTest {

    /**
     * External code must not be able to clear lower row character cards.
     */
    @Test
    void getLowerRowTributeDoesNotExposeMutableInternalList() {
        CharacterCard hunter = new HunterCard(1, Era.I, 1, false);

        Round round = createRoundWithRows(
                List.of(hunter),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        List<CharacterCard> exposed = round.getLowerRowTribute();

        assertThrows(UnsupportedOperationException.class, exposed::clear);
        assertEquals(1, round.getLowerRowTribute().size());
    }

    /**
     * External code must not be able to clear upper row character cards.
     */
    @Test
    void getUpperRowTributeDoesNotExposeMutableInternalList() {
        CharacterCard hunter = new HunterCard(2, Era.I, 1, false);

        Round round = createRoundWithRows(
                List.of(),
                List.of(hunter),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        List<CharacterCard> exposed = round.getUpperRowTribute();

        assertThrows(UnsupportedOperationException.class, exposed::clear);
        assertEquals(1, round.getUpperRowTribute().size());
    }

    /**
     * External code must not be able to clear lower row event cards.
     */
    @Test
    void getLowerRowEventsDoesNotExposeMutableInternalList() {
        EventCard event = new HuntEventCard(10, Era.I, false, 1, 2);

        Round round = createRoundWithRows(
                List.of(),
                List.of(),
                List.of(event),
                List.of(),
                List.of(),
                List.of()
        );

        List<EventCard> exposed = round.getLowerRowEvents();

        assertThrows(UnsupportedOperationException.class, exposed::clear);
        assertEquals(1, round.getLowerRowEvents().size());
    }

    /**
     * External code must not be able to clear upper row event cards.
     */
    @Test
    void getUpperRowEventsDoesNotExposeMutableInternalList() {
        EventCard event = new HuntEventCard(11, Era.I, false, 1, 2);

        Round round = createRoundWithRows(
                List.of(),
                List.of(),
                List.of(),
                List.of(event),
                List.of(),
                List.of()
        );

        List<EventCard> exposed = round.getUpperRowEvents();

        assertThrows(UnsupportedOperationException.class, exposed::clear);
        assertEquals(1, round.getUpperRowEvents().size());
    }

    /**
     * External code must not be able to clear lower row buildings.
     */
    @Test
    void getLowerRowBuildingsDoesNotExposeMutableInternalList() {
        BuildingCard building = new BuildingCard(100, Era.I, 2, null, 5);

        Round round = createRoundWithRows(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(building),
                List.of()
        );

        List<BuildingCard> exposed = round.getLowerRowBuildings();

        assertThrows(UnsupportedOperationException.class, exposed::clear);
        assertEquals(1, round.getLowerRowBuildings().size());
    }

    /**
     * External code must not be able to clear upper row buildings.
     */
    @Test
    void getUpperRowBuildingsDoesNotExposeMutableInternalList() {
        BuildingCard building = new BuildingCard(101, Era.I, 2, null, 5);

        Round round = createRoundWithRows(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(building)
        );

        List<BuildingCard> exposed = round.getUpperRowBuildings();

        assertThrows(UnsupportedOperationException.class, exposed::clear);
        assertEquals(1, round.getUpperRowBuildings().size());
    }

    private Round createRoundWithRows(
            List<CharacterCard> lowerTribute,
            List<CharacterCard> upperTribute,
            List<EventCard> lowerEvents,
            List<EventCard> upperEvents,
            List<BuildingCard> lowerBuildings,
            List<BuildingCard> upperBuildings
    ) {
        return new Round(
                1,
                null,
                RoundPhase.TOTEM_PLACEMENT,
                Era.I,
                new ArrayList<>(lowerEvents),
                new ArrayList<>(upperEvents),
                new ArrayList<>(lowerTribute),
                new ArrayList<>(upperTribute),
                new ArrayList<>(lowerBuildings),
                new ArrayList<>(upperBuildings)
        );
    }
}