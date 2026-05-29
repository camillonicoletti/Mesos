package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.card.BuilderCard;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.effect.EndPPEffect;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameController card picking action.
 *
 * <p>Verifies: pick-card flow including upper/lower row, building picks and event-card rejection.
 */
class GameControllerCardPickingStrongTest {

    /**
     * Verifies that a non-current player cannot pick cards during action resolution.
     * The selected row and both tribes must remain unchanged after the rejected action.
     */
    @Test
    void nonCurrentPlayerCannotPickCardAndDoesNotMutateModel() {
        GameController controller = createActionResolutionWithFirstPlayerOn('B', 'C');
        String currentPlayer = controller.getCurrentActionPlayerNickname();
        String wrongPlayer = otherPlayer(currentPlayer);
        Round round = controller.getGame().getCurrentRound();
        CharacterCard lowerCard = round.getLowerRowTribute().get(0);

        int lowerSizeBefore = round.getLowerRowTribute().size();
        int currentTribeBefore = findPlayer(controller, currentPlayer).getTribe().countCharacterCards();
        int wrongTribeBefore = findPlayer(controller, wrongPlayer).getTribe().countCharacterCards();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived(wrongPlayer, new PickCardAction(String.valueOf(lowerCard.getId()), false))
        );

        assertEquals(lowerSizeBefore, round.getLowerRowTribute().size());
        assertTrue(round.getLowerRowTribute().contains(lowerCard));
        assertEquals(currentTribeBefore, findPlayer(controller, currentPlayer).getTribe().countCharacterCards());
        assertEquals(wrongTribeBefore, findPlayer(controller, wrongPlayer).getTribe().countCharacterCards());
        assertEquals(currentPlayer, controller.getCurrentActionPlayerNickname());
    }

    /**
     * Verifies that a player cannot take from the upper row when the chosen offer tile
     * grants only a lower-row pick.
     */
    @Test
    void upperRowPickIsRejectedWhenTileAllowsOnlyLowerRow() {
        GameController controller = createActionResolutionWithFirstPlayerOn('B', 'C');
        String currentPlayer = controller.getCurrentActionPlayerNickname();
        Round round = controller.getGame().getCurrentRound();

        int upperCharactersBefore = round.getUpperRowTribute().size();
        int upperBuildingsBefore = round.getUpperRowBuildings().size();
        int tribeBefore = findPlayer(controller, currentPlayer).getTribe().countCharacterCards();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived(currentPlayer, new PickCardAction("9999", true))
        );

        assertEquals(upperCharactersBefore, round.getUpperRowTribute().size());
        assertEquals(upperBuildingsBefore, round.getUpperRowBuildings().size());
        assertEquals(tribeBefore, findPlayer(controller, currentPlayer).getTribe().countCharacterCards());
        assertEquals(currentPlayer, controller.getCurrentActionPlayerNickname());
    }

    /**
     * Verifies that event cards cannot be picked as normal cards.
     * Events must remain on their event row and the player tribe must not change.
     */
    @Test
    void eventCardIdCannotBePickedFromLowerRow() {
        GameController controller = createActionResolutionWithFirstPlayerOn('B', 'C');
        String currentPlayer = controller.getCurrentActionPlayerNickname();
        Round round = controller.getGame().getCurrentRound();
        HuntEventCard event = new HuntEventCard(777, Era.I, false, 1, 1);

        addLowerRowEvent(round, event);

        int lowerCharactersBefore = round.getLowerRowTribute().size();
        int lowerEventsBefore = round.getLowerRowEvents().size();
        int tribeBefore = findPlayer(controller, currentPlayer).getTribe().countCharacterCards();

        assertThrows(IllegalArgumentException.class, () ->
                controller.onMessageReceived(currentPlayer, new PickCardAction("777", false))
        );

        assertEquals(lowerCharactersBefore, round.getLowerRowTribute().size());
        assertEquals(lowerEventsBefore, round.getLowerRowEvents().size());
        assertTrue(round.getLowerRowEvents().contains(event));
        assertEquals(tribeBefore, findPlayer(controller, currentPlayer).getTribe().countCharacterCards());
    }

    /**
     * Verifies the happy path for taking a lower-row character card.
     * The card must be removed from the board and added to the current player's tribe.
     */
    @Test
    void validLowerRowCharacterPickMovesCardToCurrentPlayerTribe() {
        GameController controller = createActionResolutionWithFirstPlayerOn('B', 'C');
        String currentPlayer = controller.getCurrentActionPlayerNickname();
        Round round = controller.getGame().getCurrentRound();
        CharacterCard chosen = round.getLowerRowTribute().get(0);

        int lowerSizeBefore = round.getLowerRowTribute().size();
        int tribeBefore = findPlayer(controller, currentPlayer).getTribe().countCharacterCards();

        controller.onMessageReceived(currentPlayer, new PickCardAction(String.valueOf(chosen.getId()), false));

        assertEquals(lowerSizeBefore - 1, round.getLowerRowTribute().size());
        assertFalse(round.getLowerRowTribute().contains(chosen));
        assertTrue(findPlayer(controller, currentPlayer).getTribe().getCharacterCards().contains(chosen));
        assertEquals(tribeBefore + 1, findPlayer(controller, currentPlayer).getTribe().countCharacterCards());
    }

    /**
     * Verifies that an unaffordable building is rejected before removing it from the board.
     * The player's food and building collection must remain unchanged.
     */
    @Test
    void unaffordableUpperBuildingDoesNotMutateModel() {
        GameController controller = createActionResolutionWithFirstPlayerOn('C', 'F');
        String currentPlayer = controller.getCurrentActionPlayerNickname();
        Round round = controller.getGame().getCurrentRound();
        Player player = findPlayer(controller, currentPlayer);

        BuildingCard expensiveBuilding = new BuildingCard(900, Era.I, 999, new EndPPEffect(), 0);

        addUpperRowBuilding(round, expensiveBuilding);

        int foodBefore = player.getFood();
        int boardBuildingsBefore = round.getUpperRowBuildings().size();
        int tribeBuildingsBefore = player.getTribe().getBuildingCards().size();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived(currentPlayer, new PickBuildingAction("900", true))
        );

        assertEquals(foodBefore, player.getFood());
        assertEquals(boardBuildingsBefore, round.getUpperRowBuildings().size());
        assertTrue(round.getUpperRowBuildings().contains(expensiveBuilding));
        assertEquals(tribeBuildingsBefore, player.getTribe().getBuildingCards().size());
    }

    /**
     * Verifies that builder discounts are applied when buying a building.
     * The building is unaffordable at base cost, but the purchase must succeed thanks to the builder discount.
     */
    @Test
    void builderDiscountAllowsBuyingOtherwiseUnaffordableBuilding() {
        GameController controller = createActionResolutionWithFirstPlayerOn('C', 'F');
        String currentPlayer = controller.getCurrentActionPlayerNickname();
        Round round = controller.getGame().getCurrentRound();
        Player player = findPlayer(controller, currentPlayer);

        player.addCharacterCard(new BuilderCard(901, Era.I, 2, 0, 2));

        int foodBefore = player.getFood();

        BuildingCard discountedBuilding = new BuildingCard(
                902,
                Era.I,
                foodBefore + 2,
                new EndPPEffect(),
                0
        );

        clearUpperRowBuildings(round);
        addUpperRowBuilding(round, discountedBuilding);
        // No characters must remain in the upper row; otherwise the mandatory-character-pick
        // rule would reject the building pick before the discount logic is exercised.
        clearUpperRowTribute(round);

        int boardBuildingsBefore = round.getUpperRowBuildings().size();
        int tribeBuildingsBefore = player.getTribe().getBuildingCards().size();

        assertTrue(discountedBuilding.getFoodCost() > foodBefore,
                "The building must be unaffordable without applying any discount.");

        controller.onMessageReceived(currentPlayer, new PickBuildingAction("902", true));

        assertTrue(player.getFood() < foodBefore,
                "Buying the building must spend some food.");
        assertTrue(player.getFood() >= 0,
                "Food must never become negative after buying a discounted building.");
        assertEquals(boardBuildingsBefore - 1, round.getUpperRowBuildings().size());
        assertFalse(round.getUpperRowBuildings().contains(discountedBuilding));
        assertTrue(player.getTribe().getBuildingCards().contains(discountedBuilding));
        assertEquals(tribeBuildingsBefore + 1, player.getTribe().getBuildingCards().size());
    }

    /**
     * Verifies that a player cannot pick twice from the same row if the offer tile grants
     * only one pick from that row. The player is still active because tile E also grants
     * an upper-row pick.
     */
    @Test
    void playerCannotPickMoreCardsFromSameRowThanGrantedByOfferTile() {
        GameController controller = createActionResolutionWithFirstPlayerOn('E', 'F');
        String currentPlayer = controller.getCurrentActionPlayerNickname();
        Round round = controller.getGame().getCurrentRound();
        CharacterCard firstCard = round.getLowerRowTribute().get(0);

        controller.onMessageReceived(currentPlayer, new PickCardAction(String.valueOf(firstCard.getId()), false));

        assertEquals(currentPlayer, controller.getCurrentActionPlayerNickname());

        CharacterCard secondCard = round.getLowerRowTribute().get(0);
        int lowerSizeBefore = round.getLowerRowTribute().size();

        assertThrows(IllegalStateException.class, () ->
                controller.onMessageReceived(currentPlayer, new PickCardAction(String.valueOf(secondCard.getId()), false))
        );

        assertEquals(currentPlayer, controller.getCurrentActionPlayerNickname());
        assertEquals(lowerSizeBefore, round.getLowerRowTribute().size());
        assertTrue(round.getLowerRowTribute().contains(secondCard));
    }

    @Test
    void threePlayerTileDAllowsTwoLowerRowCardsBeforeTurnAdvances() {
        GameController controller = createStartedThreePlayerGame();
        String tileDPlayer = controller.getCurrentTurnPlayerNickname();
        controller.onMessageReceived(tileDPlayer, new PlaceTotemAction('D'));

        String tileCPlayer = controller.getCurrentTurnPlayerNickname();
        controller.onMessageReceived(tileCPlayer, new PlaceTotemAction('C'));

        String tileFPlayer = controller.getCurrentTurnPlayerNickname();
        controller.onMessageReceived(tileFPlayer, new PlaceTotemAction('F'));

        assertInstanceOf(ActionResolutionState.class, controller.getCurrentState());
        assertEquals(tileCPlayer, controller.getCurrentActionPlayerNickname());

        Round round = controller.getGame().getCurrentRound();
        CharacterCard upperCard = round.getUpperRowTribute().get(0);
        controller.onMessageReceived(tileCPlayer, new PickCardAction(String.valueOf(upperCard.getId()), true));

        assertEquals(tileDPlayer, controller.getCurrentActionPlayerNickname());

        CharacterCard firstLower = round.getLowerRowTribute().get(0);
        controller.onMessageReceived(tileDPlayer, new PickCardAction(String.valueOf(firstLower.getId()), false));

        assertEquals(tileDPlayer, controller.getCurrentActionPlayerNickname());

        CharacterCard secondLower = round.getLowerRowTribute().get(0);
        controller.onMessageReceived(tileDPlayer, new PickCardAction(String.valueOf(secondLower.getId()), false));

        Player player = findPlayer(controller, tileDPlayer);
        assertTrue(player.getTribe().getCharacterCards().contains(firstLower));
        assertTrue(player.getTribe().getCharacterCards().contains(secondLower));
        assertEquals(tileFPlayer, controller.getCurrentActionPlayerNickname());
    }

    private GameController createActionResolutionWithFirstPlayerOn(char firstTile, char secondTile) {
        GameController controller = createStartedTwoPlayerGame();
        String firstPlayer = controller.getCurrentTurnPlayerNickname();
        String secondPlayer = otherPlayer(firstPlayer);

        controller.onMessageReceived(firstPlayer, new PlaceTotemAction(firstTile));
        controller.onMessageReceived(secondPlayer, new PlaceTotemAction(secondTile));

        assertInstanceOf(ActionResolutionState.class, controller.getCurrentState());
        return controller;
    }

    private GameController createStartedTwoPlayerGame() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(2));
        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));

        return controller;
    }

    private GameController createStartedThreePlayerGame() {
        GameController controller = new GameController();

        controller.onMessageReceived("Alice", new JoinGameAction(createTotem(TotemColor.RED)));
        controller.onMessageReceived("Bob", new JoinGameAction(createTotem(TotemColor.BLUE)));
        controller.onMessageReceived("Charlie", new JoinGameAction(createTotem(TotemColor.PURPLE)));
        controller.onMessageReceived("Alice", new SetPlayerCountAction(3));
        controller.onMessageReceived("Alice", new ReadyAction(true));
        controller.onMessageReceived("Bob", new ReadyAction(true));
        controller.onMessageReceived("Charlie", new ReadyAction(true));

        return controller;
    }

    private Totem createTotem(TotemColor color) {
        return new Totem(color, "OFFER_TRACK");
    }

    private String otherPlayer(String nickname) {
        return "Alice".equals(nickname) ? "Bob" : "Alice";
    }

    private Player findPlayer(GameController controller, String nickname) {
        return controller.getGame().getPlayers()
                .stream()
                .filter(player -> player.getNickname().equals(nickname))
                .findFirst()
                .orElseThrow();
    }

    private void addLowerRowEvent(Round round, EventCard event) {
        roundInternalList(round, "lowerRowEventCards").add(event);
    }

    private void addUpperRowBuilding(Round round, BuildingCard building) {
        roundInternalList(round, "upperRowBuildings").add(building);
    }

    private void clearUpperRowBuildings(Round round) {
        roundInternalList(round, "upperRowBuildings").clear();
    }

    private void clearUpperRowTribute(Round round) {
        roundInternalList(round, "upperRowTribute").clear();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> roundInternalList(Round round, String fieldName) {
        try {
            Field field = Round.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (List<T>) field.get(round);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot access Round field: " + fieldName, e);
        }
    }
}
