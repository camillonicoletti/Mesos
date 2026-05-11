package it.polimi.ingsw.network.server;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.Totem;
import it.polimi.ingsw.model.board.Board;
import it.polimi.ingsw.model.board.OfferTile;
import it.polimi.ingsw.model.board.TurnSlot;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.CharacterCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.network.protocol.clientbound.GameSnapshot;
import it.polimi.ingsw.network.protocol.clientbound.PlayerSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SnapshotBuilder {
    private SnapshotBuilder() {}

    public static GameSnapshot build(GameController controller) {
        Game game = controller.getGame();
        if (game == null) {
            throw new IllegalStateException("Cannot build snapshot: game not started");
        }

        List<PlayerSnapshot> players = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            List<Integer> charIds = p.getTribe().getCharacterCards().stream()
                    .map(CharacterCard::getId).toList();
            List<Integer> buildIds = p.getTribe().getBuildingCards().stream()
                    .map(BuildingCard::getId).toList();
            players.add(new PlayerSnapshot(
                    p.getNickname(),
                    p.getTotem().getColor(),
                    p.getFood(),
                    p.getPrestigePoints(),
                    charIds,
                    buildIds
            ));
        }

        Round round = game.getCurrentRound();
        List<Integer> upperCharAndEvent = new ArrayList<>();
        List<Integer> lowerCharAndEvent = new ArrayList<>();
        List<Integer> upperBuild = new ArrayList<>();
        List<Integer> lowerBuild = new ArrayList<>();
        List<Integer> upperEvents = new ArrayList<>();
        List<Integer> lowerEvents = new ArrayList<>();
        if (round != null) {
            for (CharacterCard c : round.getUpperRowTribute()) upperCharAndEvent.add(c.getId());
            for (EventCard e : round.getUpperRowEvents()) {
                upperCharAndEvent.add(e.getId());
                upperEvents.add(e.getId());
            }
            for (CharacterCard c : round.getLowerRowTribute()) lowerCharAndEvent.add(c.getId());
            for (EventCard e : round.getLowerRowEvents()) {
                lowerCharAndEvent.add(e.getId());
                lowerEvents.add(e.getId());
            }
            for (BuildingCard b : round.getUpperRowBuildings()) upperBuild.add(b.getId());
            for (BuildingCard b : round.getLowerRowBuildings()) lowerBuild.add(b.getId());
        }

        Board board = game.getBoard();
        Map<Totem, String> ownerByTotem = new HashMap<>();
        for (Player p : game.getPlayers()) ownerByTotem.put(p.getTotem(), p.getNickname());

        Map<Character, String> totemOnTiles = new HashMap<>();
        if (board != null) {
            for (OfferTile tile : board.getOfferTiles()) {
                Totem t = tile.getTotemSlot();
                if (t != null) {
                    String owner = ownerByTotem.get(t);
                    if (owner != null) totemOnTiles.put(tile.getLetter(), owner);
                }
            }
        }

        List<String> turnOrder = new ArrayList<>();
        if (board != null && board.getTurnOrderTile() != null) {
            for (TurnSlot slot : board.getTurnOrderTile().getSlots()) {
                if (slot.isOccupied()) {
                    String nick = ownerByTotem.get(slot.getOccupiedBy());
                    if (nick != null) turnOrder.add(nick);
                }
            }
        }
        if (turnOrder.isEmpty()) {
            for (Player p : game.getPlayers()) turnOrder.add(p.getNickname());
        }

        List<String> disconnectedPlayers = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            if (controller.isPlayerDisconnected(p.getNickname())) {
                disconnectedPlayers.add(p.getNickname());
            }
        }

        return new GameSnapshot(
                players,
                upperCharAndEvent, lowerCharAndEvent,
                upperBuild, lowerBuild,
                game.getCurrentEra(),
                round != null ? round.getNumber() : 0,
                totemOnTiles,
                turnOrder,
                disconnectedPlayers,
                upperEvents, lowerEvents,
                game.getRemainingTributeDeck()
        );
    }
}
