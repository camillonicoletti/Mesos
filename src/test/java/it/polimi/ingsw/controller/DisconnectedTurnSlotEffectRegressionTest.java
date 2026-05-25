package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: when a player gets disconnected during ActionResolutionState
 * AND it is their turn, the controller still places their totem on the turn
 * order tile and applies the slot effect (food gain / food cost / PP loss).
 *
 * <p>This behaviour was confirmed as intentional during Phase 1 of the bug
 * hunt: a disconnected player still "completes" their action by routing the
 * totem to the next available turn-order slot. The test pins it down so that
 * future refactors don't silently change it.
 */
class DisconnectedTurnSlotEffectRegressionTest {

    private GameController bootstrap3pToActionResolution() {
        Player alice = new Player("Alice",   new Totem(TotemColor.RED,    "OFFER_TRACK"));
        Player bob = new Player("Bob",       new Totem(TotemColor.BLUE,   "OFFER_TRACK"));
        Player carol = new Player("Carol",   new Totem(TotemColor.PURPLE, "OFFER_TRACK"));
        List<Player> players = new ArrayList<>(List.of(alice, bob, carol));

        Game game = new Game(
                players, 1, null, Era.I, null,
                BoardFactory.createBoard(3),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );
        // Two character cards in upper so Alice and Bob each pick one before Carol's turn.
        List<CharacterCard> upper = new ArrayList<>();
        upper.add(new HunterCard(556, Era.I, 0, false));
        upper.add(new HunterCard(557, Era.I, 0, false));
        // One character card in lower for Carol's tile.
        List<CharacterCard> lower = new ArrayList<>();
        lower.add(new HunterCard(558, Era.I, 0, false));
        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                lower, upper,
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);

        // Force a deterministic initial turn order: Alice → Bob → Carol.
        game.getBoard().getTurnOrderTile().reset();
        var slots = game.getBoard().getTurnOrderTile().getSlots();
        slots.get(0).placeTotem(alice.getTotem());
        slots.get(1).placeTotem(bob.getTotem());
        slots.get(2).placeTotem(carol.getTotem());

        GameController ctrl = new GameController();
        ctrl.setGame(game);
        ctrl.changeState(new TotemPlacementState());

        // Place totems in order Alice→Bob→Carol on tiles C (upper-only), F (upper-only),
        // and B (lower-only). Action resolution order is by offer-tile letter, so
        // B (Carol) acts first, then C (Alice), then F (Bob).
        ctrl.onMessageReceived("Alice", new PlaceTotemAction('C'));
        ctrl.onMessageReceived("Bob",   new PlaceTotemAction('F'));
        ctrl.onMessageReceived("Carol", new PlaceTotemAction('B'));

        assertTrue(ctrl.getCurrentState() instanceof ActionResolutionState,
                "Expected ActionResolutionState, got " + ctrl.getCurrentState().getClass().getSimpleName());
        return ctrl;
    }

    @Test
    void disconnectionDuringActionResolution_appliesTurnSlotPenalty() {
        GameController ctrl = bootstrap3pToActionResolution();
        Game game = ctrl.getGame();
        Player bob = game.getPlayers().stream().filter(p -> p.getNickname().equals("Bob"))
                .findFirst().orElseThrow();

        // Action order is B → C → F  ⇒  Carol → Alice → Bob.
        // Carol picks from lower, Alice picks from upper, then it would be Bob's turn.
        ctrl.onMessageReceived("Carol", new PickCardAction("558", false));
        ctrl.onMessageReceived("Alice", new PickCardAction("556", true));

        // Bob's turn — disconnect him.
        assertEquals("Bob", ((ActionResolutionState) ctrl.getCurrentState()).getCurrentPlayerNickname());
        int bobPPBefore = bob.getPrestigePoints();
        int bobFoodBefore = bob.getFood();
        ctrl.onPlayerDisconnection("Bob");

        // 3-player TurnOrderTile: slots 0=GainFood(2), 1=NoSlot, 2=PayFood(1,2).
        // Carol's totem (acting first via tile B) went to slot 0 → +2 food.
        // Alice (acting via tile C) went to slot 1 → no effect.
        // Bob (now disconnected) must be routed to slot 2 → PayFood. Bob.food=0 ⇒ -2 PP.
        assertEquals(bobFoodBefore, bob.getFood(),
                "Bob has no food: PayFoodSlotEffect must not change food when player cannot afford it");
        assertEquals(bobPPBefore - 2, bob.getPrestigePoints(),
                "Bob must lose 2 PP because PayFoodSlotEffect runs even when player is disconnected");
        assertTrue(ctrl.isPlayerDisconnected("Bob"));
    }

    @Test
    void disconnectionDuringActionResolution_appliesGainFoodWhenLandingOnBonusSlot() {
        // Variant: disconnect Carol before any picks happen, so Carol (whose tile B
        // resolves first) is routed straight to slot 0 = GainFood(2).
        // We still need Carol to have been current player at some point; to simulate
        // that, we use the prepareCurrentPlayer disconnected branch via setup.
        Player alice = new Player("Alice", new Totem(TotemColor.RED, "OFFER_TRACK"));
        Player bob   = new Player("Bob",   new Totem(TotemColor.BLUE, "OFFER_TRACK"));
        Player carol = new Player("Carol", new Totem(TotemColor.PURPLE, "OFFER_TRACK"));
        List<Player> players = new ArrayList<>(List.of(alice, bob, carol));

        Game game = new Game(players, 1, null, Era.I, null,
                BoardFactory.createBoard(3),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(List.of(new HunterCard(558, Era.I, 0, false))),
                new ArrayList<>(List.of(new HunterCard(556, Era.I, 0, false),
                                       new HunterCard(557, Era.I, 0, false))),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);
        game.getBoard().getTurnOrderTile().reset();
        var slots = game.getBoard().getTurnOrderTile().getSlots();
        slots.get(0).placeTotem(alice.getTotem());
        slots.get(1).placeTotem(bob.getTotem());
        slots.get(2).placeTotem(carol.getTotem());

        GameController ctrl = new GameController();
        ctrl.setGame(game);
        ctrl.changeState(new TotemPlacementState());
        ctrl.onMessageReceived("Alice", new PlaceTotemAction('C'));
        ctrl.onMessageReceived("Bob",   new PlaceTotemAction('F'));
        ctrl.onMessageReceived("Carol", new PlaceTotemAction('B'));

        // Disconnect Carol immediately. She is the player on tile B (first to act).
        // prepareCurrentPlayer sees Carol disconnected ⇒ places totem on slot 0
        // ⇒ GainFood(2) applies, granting her +2 food despite being offline.
        int carolFoodBefore = carol.getFood();
        ctrl.onPlayerDisconnection("Carol");
        assertEquals(carolFoodBefore + 2, carol.getFood(),
                "Carol must receive GainFood(2) on slot 0 even when disconnected");
    }

}
