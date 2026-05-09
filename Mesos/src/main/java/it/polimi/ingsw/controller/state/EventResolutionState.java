package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.card.CavePaintingsEventCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.ShamanicRitualEventCard;
import it.polimi.ingsw.model.card.SustenanceEventCard;

import java.io.Serial;
import java.util.List;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventResolutionState extends AbstractGameState {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final long EVENT_DISPLAY_MS = 5_000L;

    @Override
    public void onEnter(GameController ctx) {
        Round round = ctx.getGame().getCurrentRound();
        boolean includeUpperRow = ctx.getGame().isLastRound();
        List<EventCard> events = round.getOrderedEventsForResolution(includeUpperRow);

        for (EventCard event : events) {
            // Capture food/pp BEFORE resolving
            Map<String, int[]> before = new HashMap<>();
            for (Player p : ctx.getGame().getPlayers()) {
                before.put(p.getNickname(), new int[]{ p.getFood(), p.getPrestigePoints() });
            }

            event.resolve(ctx.getGame());

            // Build per-player effects with deltas + new totals
            List<EventResolvedMessage.PlayerEffect> effects = new ArrayList<>();
            for (Player p : ctx.getGame().getPlayers()) {
                int[] prev = before.get(p.getNickname());
                int newFood = p.getFood();
                int newPP = p.getPrestigePoints();
                effects.add(new EventResolvedMessage.PlayerEffect(
                        p.getNickname(),
                        newFood - prev[0], newFood,
                        newPP - prev[1], newPP
                ));
            }

            final int cardId = event.getId();
            final String title = describeTitle(event);
            final String description = describeDetails(event);
            ctx.notifyListeners(l -> l.onEventResolved(cardId, title, description, effects));

            sleep();
        }

        ctx.changeState(new EndRoundState());
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        rejectAction(action);
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }

        ctx.markPlayerDisconnected(playerNickname);
    }

    @Override
    public void onPlayerReconnected(GameController ctx, String playerNickname) {
        if (!ctx.containsGamePlayer(playerNickname)) {
            return;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void sleep() {
        try {
            Thread.sleep(EVENT_DISPLAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String describeTitle(EventCard event) {
        if (event instanceof HuntEventCard) return "HUNT";
        if (event instanceof ShamanicRitualEventCard) return "SHAMANIC RITUAL";
        if (event instanceof SustenanceEventCard) return "SUSTENANCE";
        if (event instanceof CavePaintingsEventCard) return "CAVE PAINTINGS";
        return "EVENT";
    }

    private String describeDetails(EventCard event) {
        if (event instanceof HuntEventCard h) {
            return "Each Hunter grants " + h.getFoodPerHunter() + " food and "
                    + h.getPpPerHunter() + " PP.";
        }
        if (event instanceof ShamanicRitualEventCard s) {
            return "Players with the most Shaman stars gain " + s.getPpGain()
                    + " PP; players with the fewest lose " + s.getPpLoss() + " PP.";
        }
        if (event instanceof SustenanceEventCard s) {
            return "Each character requires 1 food. Missing food costs "
                    + s.getPpPenaltyPerUnfed() + " PP per unfed character.";
        }
        if (event instanceof CavePaintingsEventCard) {
            return "Cave Paintings event resolved.";
        }
        return "Event resolved.";
    }
}