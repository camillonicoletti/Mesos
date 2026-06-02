package it.polimi.ingsw.controller;

import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.controller.action.PickBuildingAction;
import it.polimi.ingsw.controller.action.PickCardAction;
import it.polimi.ingsw.controller.action.PlaceTotemAction;
import it.polimi.ingsw.controller.state.ActionResolutionState;
import it.polimi.ingsw.controller.state.GameState;
import it.polimi.ingsw.controller.state.TotemPlacementState;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.HunterCard;
import it.polimi.ingsw.model.card.InventorCard;
import it.polimi.ingsw.model.card.GathererCard;
import it.polimi.ingsw.model.card.ShamanCard;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.InventorIcons;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.GameSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.PlayerSnapshot;
import it.polimi.ingsw.network.server.SnapshotBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Deterministic fuzz of a single round: random but legal actions are sent to
 * the controller from {@link TotemPlacementState} through
 * {@link ActionResolutionState}. After every action snapshot invariants are
 * checked. The test bails out as soon as the round leaves
 * ActionResolutionState (so the 5s per-event/end-round sleeps do not run).
 *
 * <p>Seed is fixed so failures are reproducible; the recorded action log is
 * printed when an invariant breaks.
 */
class SnapshotInvariantFuzzTest {

    private static final long SEED = 42L;
    private static final int MAX_STEPS = 60;

    @Test
    void singleRoundFuzz_preservesInvariants() {
        Random rng = new Random(SEED);
        GameController ctrl = buildRichGame(3);
        List<String> log = new ArrayList<>();

        try {
            for (int step = 0; step < MAX_STEPS; step++) {
                checkInvariants(ctrl, step, log);

                GameState state = ctrl.getCurrentState();
                if (state instanceof TotemPlacementState ts) {
                    String currentNick = ts.getCurrentPlayerNickname(ctrl);
                    if (currentNick == null) break;
                    GameAction action = randomPlacement(ctrl, rng);
                    if (action == null) break;
                    log.add(step + ": " + currentNick + " " + action);
                    try {
                        ctrl.onMessageReceived(currentNick, action);
                    } catch (IllegalStateException | IllegalArgumentException ignored) {
                        // Tile may have been taken concurrently in a previous iteration
                        // when the rng repeated; retry next step is fine.
                    }
                } else if (state instanceof ActionResolutionState ars) {
                    String currentNick = ars.getCurrentPlayerNickname();
                    if (currentNick == null) break;
                    GameAction action = randomPick(ctrl, currentNick, rng);
                    if (action == null) break;
                    log.add(step + ": " + currentNick + " " + action);
                    try {
                        ctrl.onMessageReceived(currentNick, action);
                    } catch (IllegalStateException | IllegalArgumentException ignored) {
                        // Picks may have already drained the chosen row — retry next loop.
                    }
                } else {
                    // Left ActionResolutionState — round is over (EventResolution / EndRound).
                    break;
                }
            }
            checkInvariants(ctrl, -1, log);
        } catch (AssertionError e) {
            System.err.println("[FuzzTest] SEED=" + SEED);
            System.err.println("[FuzzTest] action log:");
            for (String l : log) System.err.println("  " + l);
            throw e;
        }
    }

    // ── action generators ────────────────────────────────────────────────────

    private GameAction randomPlacement(GameController ctrl, Random rng) {
        List<Character> free = new ArrayList<>();
        for (OfferTile tile : ctrl.getGame().getBoard().getOfferTiles()) {
            if (!tile.isOccupied()) free.add(tile.getLetter());
        }
        if (free.isEmpty()) return null;
        return new PlaceTotemAction(free.get(rng.nextInt(free.size())));
    }

    private GameAction randomPick(GameController ctrl, String currentNick, Random rng) {
        Round round = ctrl.getGame().getCurrentRound();
        // Try upper first half the time, lower the other half — fall back to the other.
        boolean preferUpper = rng.nextBoolean();
        GameAction first = pickFromRow(round, preferUpper, rng);
        if (first != null) return first;
        return pickFromRow(round, !preferUpper, rng);
    }

    private GameAction pickFromRow(Round round, boolean upper, Random rng) {
        List<CharacterCard> chars = upper ? round.getUpperRowTribute() : round.getLowerRowTribute();
        List<BuildingCard> builds = upper ? round.getUpperRowBuildings() : round.getLowerRowBuildings();
        if (!chars.isEmpty()) {
            CharacterCard c = chars.get(rng.nextInt(chars.size()));
            return new PickCardAction(String.valueOf(c.getId()), upper);
        }
        if (!builds.isEmpty()) {
            BuildingCard b = builds.get(rng.nextInt(builds.size()));
            return new PickBuildingAction(String.valueOf(b.getId()), upper);
        }
        return null;
    }

    // ── invariants ───────────────────────────────────────────────────────────

    private void checkInvariants(GameController ctrl, int step, List<String> log) {
        GameSnapshot snap;
        try {
            snap = SnapshotBuilder.build(ctrl);
        } catch (RuntimeException e) {
            fail("SnapshotBuilder threw at step " + step + ": " + e);
            return;
        }

        // (I1) every player has food ≥ 0
        for (PlayerSnapshot ps : snap.getPlayers()) {
            if (ps.getFood() < 0) {
                fail("Negative food for " + ps.getNickname() + " at step " + step);
            }
        }

        // (I2) no duplicate nicknames
        Set<String> nicks = new HashSet<>();
        for (PlayerSnapshot ps : snap.getPlayers()) {
            if (!nicks.add(ps.getNickname())) {
                fail("Duplicate nickname in snapshot: " + ps.getNickname());
            }
        }

        // (I3) each card id appears in at most one location (rows or any tribe).
        Set<Integer> seen = new HashSet<>();
        for (int id : snap.getUpperRowCharacterAndEventIds()) {
            if (!seen.add(id)) fail("Duplicate id " + id + " in upper row at step " + step);
        }
        for (int id : snap.getLowerRowCharacterAndEventIds()) {
            if (!seen.add(id)) fail("Duplicate id " + id + " in lower row at step " + step);
        }
        for (int id : snap.getUpperRowBuildingIds()) {
            if (!seen.add(id)) fail("Duplicate building id " + id + " in upper at step " + step);
        }
        for (int id : snap.getLowerRowBuildingIds()) {
            if (!seen.add(id)) fail("Duplicate building id " + id + " in lower at step " + step);
        }
        for (PlayerSnapshot ps : snap.getPlayers()) {
            for (int id : ps.getCharacterCardIds()) {
                if (!seen.add(id)) fail("Duplicate id " + id + " in " + ps.getNickname() + " tribe at step " + step);
            }
            for (int id : ps.getBuildingCardIds()) {
                if (!seen.add(id)) fail("Duplicate building id " + id + " in " + ps.getNickname() + " tribe at step " + step);
            }
        }

        // (I4) event ids are subset of upper+lower combined row ids
        Set<Integer> rowIds = new HashSet<>();
        rowIds.addAll(snap.getUpperRowCharacterAndEventIds());
        rowIds.addAll(snap.getLowerRowCharacterAndEventIds());
        for (int eid : snap.getUpperRowEventIds()) {
            if (!rowIds.contains(eid)) fail("Event " + eid + " not in any row at step " + step);
        }
        for (int eid : snap.getLowerRowEventIds()) {
            if (!rowIds.contains(eid)) fail("Event " + eid + " not in any row at step " + step);
        }

        // (I5) at least one of turnOrder / totemOnTiles describes where players are
        // During ActionResolutionState the turn-order tile is reset and rebuilt
        // incrementally as each player completes their action, so turnOrder can
        // legitimately be empty if every totem is still parked on an offer tile.
        if (snap.getTurnOrder().isEmpty() && snap.getTotemOnTiles().isEmpty()) {
            fail("Both turnOrder and totemOnTiles are empty at step " + step);
        }

        // (I6) round number is positive
        if (snap.getRoundNumber() < 1) {
            fail("Non-positive round number " + snap.getRoundNumber() + " at step " + step);
        }
    }

    // ── bootstrap ────────────────────────────────────────────────────────────

    private GameController buildRichGame(int numPlayers) {
        List<Player> players = new ArrayList<>();
        TotemColor[] colors = TotemColor.values();
        for (int i = 0; i < numPlayers; i++) {
            players.add(new Player("P" + (i + 1), new Totem(colors[i], "OFFER_TRACK")));
        }

        Game game = new Game(players, 1, null, Era.I, null,
                BoardFactory.createBoard(numPlayers),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        // Pre-populate rows so picks are meaningful.
        List<CharacterCard> upper = new ArrayList<>(List.of(
                new HunterCard(1001, Era.I, 0, false),
                new HunterCard(1002, Era.I, 0, true),
                new GathererCard(1003, Era.I, 0),
                new InventorCard(1004, Era.I, 0, InventorIcons.INVENTION_1),
                new ShamanCard(1005, Era.I, 0, 1)
        ));
        List<CharacterCard> lower = new ArrayList<>(List.of(
                new HunterCard(2001, Era.I, 0, false),
                new GathererCard(2002, Era.I, 0),
                new InventorCard(2003, Era.I, 0, InventorIcons.INVENTION_2)
        ));
        List<BuildingCard> upperB = new ArrayList<>(List.of(
                new BuildingCard(3001, Era.I, 0, null, 1)
        ));
        Round round = new Round(1, game, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                lower, upper,
                new ArrayList<>(), upperB);
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);

        // Deterministic initial turn order.
        game.getBoard().getTurnOrderTile().reset();
        var slots = game.getBoard().getTurnOrderTile().getSlots();
        for (int i = 0; i < numPlayers; i++) {
            slots.get(i).placeTotem(players.get(i).getTotem());
        }

        GameController ctrl = new GameController();
        ctrl.setGame(game);
        ctrl.changeState(new TotemPlacementState());
        return ctrl;
    }
}
