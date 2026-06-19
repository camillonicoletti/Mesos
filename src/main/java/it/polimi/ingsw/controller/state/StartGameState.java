package it.polimi.ingsw.controller.state;

import it.polimi.ingsw.controller.GameController;
import it.polimi.ingsw.controller.action.GameAction;
import it.polimi.ingsw.model.Game;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.card.BuildingCard;
import it.polimi.ingsw.model.card.Card;
import it.polimi.ingsw.model.card.CardFactory;
import it.polimi.ingsw.model.enums.Era;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// One-shot state that materialises the Game: builds the decks via CardFactory, instantiates the
// model, calls start() on it and transitions to TotemPlacementState. Rejects any incoming action.
public class StartGameState extends AbstractGameState {

    private static final long serialVersionUID = 1L;

    @Override
    public void onEnter(GameController ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Controller context cannot be null");
        }

        if (!ctx.isLobbyFull()) {
            throw new IllegalStateException("Cannot start the game before the lobby is full");
        }

        if (!ctx.areAllPlayersReady()) {
            throw new IllegalStateException("Cannot start the game until all players are ready");
        }

        List<Player> players = new ArrayList<>(ctx.getLobbyPlayers());
        List<Card> tributeDeck = CardFactory.createTribeDeck(players.size());
        List<BuildingCard> era1BuildingDeck = CardFactory.createBuildingCards(Era.I, players.size());
        List<BuildingCard> era2BuildingDeck = CardFactory.createBuildingCards(Era.II, players.size());
        List<BuildingCard> era3BuildingDeck = CardFactory.createBuildingCards(Era.III, players.size());

        Game game = new Game(
                players,
                0,
                null,
                null,
                LocalDateTime.now(),
                null,
                tributeDeck,
                era1BuildingDeck,
                era2BuildingDeck,
                era3BuildingDeck
        );

        game.start();
        ctx.setGame(game);
        ctx.notifyListeners(l -> l.onGameStarted());

        ctx.changeState(new TotemPlacementState());
    }

    @Override
    public void handleAction(GameController ctx, String playerNickname, GameAction action) {
        rejectAction(action);
    }

    @Override
    public void onPlayerDisconnected(GameController ctx, String playerNickname) {
        // StartGameState is transitory: by the time a disconnection is processed,
        // the controller is expected to have already moved to the next state.
    }

    @Override
    public void onPlayerReconnected(GameController ctx, String playerNickname) {
        // StartGameState is transitory: by the time a reconnection is processed,
        // the controller is expected to have already moved to the next state.
    }
}
