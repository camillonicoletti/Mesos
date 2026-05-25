package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.BoardFactory;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.RoundPhase;
import it.polimi.ingsw.model.enums.TotemColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bug-hunter / contract test for {@link OfferTileSlotResolver}: for every
 * offer tile A–G defined in {@code offer_tiles.json} the resolver must
 * produce the expected upper / lower pick budget. Drifts between the JSON
 * resource and the client-side hardcoded budget in {@code ClientModel}
 * surface here.
 */
class OfferTilePickBudgetExhaustiveTest {

    /** Expected (upper, lower) picks per tile letter, derived from the rulebook. */
    private static int[] expectedBudget(char letter) {
        return switch (letter) {
            case 'A' -> new int[]{0, 0};   // FOOD-only, 5p
            case 'B' -> new int[]{0, 1};
            case 'C' -> new int[]{1, 0};
            case 'D' -> new int[]{0, 2};
            case 'E' -> new int[]{1, 1};
            case 'F' -> new int[]{2, 0};
            case 'G' -> new int[]{2, 1};
            default  -> throw new IllegalStateException("Unknown tile: " + letter);
        };
    }

    private GameController controllerWith5Players() {
        List<Player> players = new ArrayList<>(List.of(
                new Player("P1", new Totem(TotemColor.RED,    "OFFER_TRACK")),
                new Player("P2", new Totem(TotemColor.BLUE,   "OFFER_TRACK")),
                new Player("P3", new Totem(TotemColor.PURPLE, "OFFER_TRACK")),
                new Player("P4", new Totem(TotemColor.YELLOW, "OFFER_TRACK")),
                new Player("P5", new Totem(TotemColor.WHITE,  "OFFER_TRACK"))
        ));
        Game game = new Game(players, 1, null, Era.I, null,
                BoardFactory.createBoard(5),
                new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Round round = new Round(1, game, RoundPhase.TOTEM_PLACEMENT, Era.I,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        game.setCurrentRound(round);
        for (Player p : players) p.setGame(game);

        GameController ctrl = new GameController();
        ctrl.setGame(game);
        return ctrl;
    }

    @Test
    void everyTileHasTheExpectedPickBudget() {
        GameController ctrl = controllerWith5Players();
        Board board = ctrl.getGame().getBoard();
        Player p = ctrl.getGame().getPlayers().get(0);

        for (OfferTile tile : board.getOfferTiles()) {
            RowPickBudget budget = OfferTileSlotResolver.resolve(ctrl, p, tile);
            int[] expected = expectedBudget(tile.getLetter());
            assertEquals(expected[0], budget.upperPicks(),
                    "Tile " + tile.getLetter() + ": expected upper=" + expected[0]
                            + ", got " + budget.upperPicks());
            assertEquals(expected[1], budget.lowerPicks(),
                    "Tile " + tile.getLetter() + ": expected lower=" + expected[1]
                            + ", got " + budget.lowerPicks());
        }
    }

    @Test
    void tileAGives3FoodAndZeroPicks_in5pGame() {
        GameController ctrl = controllerWith5Players();
        Player p = ctrl.getGame().getPlayers().get(0);
        OfferTile tileA = ctrl.getGame().getBoard().getOfferTiles().stream()
                .filter(t -> t.getLetter() == 'A').findFirst().orElseThrow();

        int foodBefore = p.getFood();
        RowPickBudget budget = OfferTileSlotResolver.resolve(ctrl, p, tileA);

        assertEquals(0, budget.upperPicks());
        assertEquals(0, budget.lowerPicks());
        assertEquals(foodBefore + 3, p.getFood(),
                "Tile A is the 5p food slot and grants exactly 3 food");
    }

    @Test
    void clientSidePickBudgetMatchesServerSide_forEveryTile() {
        // Mirrors it.polimi.ingsw.network.client.ClientModel.{upperPicksForTile, lowerPicksForTile}.
        // If those switch tables go out of sync with offer_tiles.json, this test fails.
        Map<Character, int[]> client = Map.of(
                'A', new int[]{0, 0},
                'B', new int[]{0, 1},
                'C', new int[]{1, 0},
                'D', new int[]{0, 2},
                'E', new int[]{1, 1},
                'F', new int[]{2, 0},
                'G', new int[]{2, 1}
        );

        GameController ctrl = controllerWith5Players();
        Player p = ctrl.getGame().getPlayers().get(0);
        for (OfferTile tile : ctrl.getGame().getBoard().getOfferTiles()) {
            RowPickBudget budget = OfferTileSlotResolver.resolve(ctrl, p, tile);
            int[] clientBudget = client.get(tile.getLetter());
            assertEquals(clientBudget[0], budget.upperPicks(),
                    "Server/Client drift on upper picks for tile " + tile.getLetter());
            assertEquals(clientBudget[1], budget.lowerPicks(),
                    "Server/Client drift on lower picks for tile " + tile.getLetter());
        }
    }
}
