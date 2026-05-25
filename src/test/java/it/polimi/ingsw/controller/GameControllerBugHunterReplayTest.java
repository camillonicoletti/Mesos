package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.JoinGameAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.action.ReadyAction;
import it.polimi.ingsw.controller.action.SetPlayerCountAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug hunter tests for duplicated/replayed game actions.
 */
class GameControllerBugHunterReplayTest {

    /**
     * Replaying the same card-pick action after it has already been accepted must be rejected.
     * The replay must not add the same card twice and must not advance the action resolution state again.
     */
    @Test
    void replayedPickCardActionFromPreviousPlayerIsRejectedWithoutMutatingTwice() {
        GameController controller = createActionResolutionWithFirstPlayerOn('B', 'C');

        String firstPlayer = controller.getCurrentActionPlayerNickname();
        Round round = controller.getGame().getCurrentRound();
        Player player = findPlayer(controller, firstPlayer);

        CharacterCard chosenCard = round.getLowerRowTribute().get(0);
        String chosenCardId = String.valueOf(chosenCard.getId());

        int lowerSizeBeforeFirstPick = round.getLowerRowTribute().size();
        int tribeSizeBeforeFirstPick = player.getTribe().countCharacterCards();

        PickCardAction replayedAction = new PickCardAction(chosenCardId, false);

        controller.onMessageReceived(firstPlayer, replayedAction);

        String currentPlayerAfterFirstPick = controller.getCurrentActionPlayerNickname();
        int lowerSizeAfterFirstPick = round.getLowerRowTribute().size();
        int tribeSizeAfterFirstPick = player.getTribe().countCharacterCards();

        assertEquals(lowerSizeBeforeFirstPick - 1, lowerSizeAfterFirstPick);
        assertEquals(tribeSizeBeforeFirstPick + 1, tribeSizeAfterFirstPick);
        assertTrue(player.getTribe().getCharacterCards().contains(chosenCard));

        assertThrows(RuntimeException.class,
                () -> controller.onMessageReceived(firstPlayer, replayedAction));

        assertEquals(currentPlayerAfterFirstPick, controller.getCurrentActionPlayerNickname());
        assertEquals(lowerSizeAfterFirstPick, round.getLowerRowTribute().size());
        assertEquals(tribeSizeAfterFirstPick, player.getTribe().countCharacterCards());
        assertTrue(player.getTribe().getCharacterCards().contains(chosenCard));
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
}