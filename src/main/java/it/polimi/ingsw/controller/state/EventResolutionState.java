package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.Round;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CardVisitor;
import it.polimi.ingsw.model.card.CavePaintingsEventCard;
import it.polimi.ingsw.model.card.EventCard;
import it.polimi.ingsw.model.card.HuntEventCard;
import it.polimi.ingsw.model.card.ShamanicRitualEventCard;
import it.polimi.ingsw.model.card.SustenanceEventCard;

import java.io.Serial;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.network.protocol.clientbound.EventResolvedMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State that resolves the events triggered at the end of the action phase.
 * Drives the model through the deterministic event order (sustenance last)
 * and emits one client-facing message per event, including per-player effect
 * deltas, before transitioning to {@link EndRoundState}.
 */
public class EventResolutionState extends AbstractGameState {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final long EVENT_DISPLAY_MS = 5_000L;

    @Override
    public void onEnter(GameController ctx) {
        Round round = ctx.getGame().getCurrentRound();
        // Upper-row events are only resolved on the last round of the game.
        boolean includeUpperRow = ctx.getGame().isLastRound();
        List<EventCard> events = round.getOrderedEventsForResolution(includeUpperRow);

        for (EventCard event : events) {
            // Capture values before resolving to compute per-player deltas for the event notification.
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
        ctx.waitUntilNextRound(playerNickname);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void sleep() {
        // Pause between events so clients have time to display each result before the next one fires.
        try {
            Thread.sleep(EVENT_DISPLAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String describeTitle(EventCard event) {
        return event.accept(new CardVisitor<>() {
            @Override public String visitHuntEvent(HuntEventCard card) {
                return "HUNT";
            }

            @Override public String visitShamanicRitualEvent(ShamanicRitualEventCard card) {
                return "SHAMANIC RITUAL";
            }

            @Override public String visitSustenanceEvent(SustenanceEventCard card) {
                return "SUSTENANCE";
            }

            @Override public String visitCavePaintingsEvent(CavePaintingsEventCard card) {
                return "CAVE PAINTINGS";
            }

            @Override public String visitCard(Card card) {
                return "EVENT";
            }
        });
    }

    private String describeDetails(EventCard event) {
        return event.accept(new CardVisitor<>() {
            @Override public String visitHuntEvent(HuntEventCard card) {
                return "Each Hunter grants " + card.getFoodPerHunter() + " food and "
                        + card.getPpPerHunter() + " PP.";
            }

            @Override public String visitShamanicRitualEvent(ShamanicRitualEventCard card) {
                return "Players with the most Shaman stars gain " + card.getPpGain()
                        + " PP; players with the fewest lose " + card.getPpLoss() + " PP.";
            }

            @Override public String visitSustenanceEvent(SustenanceEventCard card) {
                return "Each character requires 1 food. Missing food costs "
                        + card.getPpPenaltyPerUnfed() + " PP per unfed character.";
            }

            @Override public String visitCavePaintingsEvent(CavePaintingsEventCard card) {
                return "Cave Paintings event resolved.";
            }

            @Override public String visitCard(Card card) {
                return "Event resolved.";
            }
        });
    }
}
