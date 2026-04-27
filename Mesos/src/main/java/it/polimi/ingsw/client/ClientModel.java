package it.polimi.ingsw.client;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.network.protocol.clientbound.*;

import java.util.*;

public class ClientModel {

    public interface Listener {
        void onModelChanged();
    }

    private boolean gameSuspended;
    private String lastDisconnectedPlayer;

    // --- login ---
    private boolean loginSuccess;
    private String lastError;

    // --- lobby ---
    private List<String> lobbyPlayers = new ArrayList<>();
    private int expectedPlayers;

    // --- partita ---
    private String gameState;
    private Era currentEra;
    private int roundNumber;
    private List<String> turnOrder = new ArrayList<>();
    private Map<Character, String> totemOnTiles = new HashMap<>();

    // righe carte sul tavolo
    private List<Integer> upperRowCharacterIds = new ArrayList<>();
    private List<Integer> lowerRowCharacterIds = new ArrayList<>();
    private List<Integer> upperRowBuildingIds   = new ArrayList<>();
    private List<Integer> lowerRowBuildingIds   = new ArrayList<>();

    // giocatori: nickname -> PlayerView
    private Map<String, PlayerView> players = new LinkedHashMap<>();

    // --- fine partita ---
    private String winnerNickname;
    private Map<String, Integer> finalScores = new HashMap<>();

    // --- observer ---
    private final List<Listener> listeners = new ArrayList<>();

    public void addListener(Listener l) { listeners.add(l); }

    private void notifyChanged() {
        for (Listener l : listeners) l.onModelChanged();
    }

    // -------------------------------------------------------
    // APPLY — uno per ogni ServerMessage che modifica stato
    // -------------------------------------------------------

    public void applyLoginAck(LoginAckMessage m) {
        loginSuccess = m.isSuccess();
        lastError = m.isSuccess() ? null : m.getErrorReason();
        notifyChanged();
    }

    public void applyLobbyUpdate(LobbyUpdateMessage m) {
        lobbyPlayers = new ArrayList<>(m.getCurrentPlayers());
        expectedPlayers = m.getExpectedPlayers();
        notifyChanged();
    }

    public void applyGameStarted(GameStartedMessage m) {
        applySnapshot(m.getSnapshot());
    }

    public void applySnapshot(GameSnapshot snap) {
        players.clear();
        for (PlayerSnapshot ps : snap.getPlayers()) {
            players.put(ps.getNickname(), new PlayerView(
                    ps.getNickname(), ps.getTotemColor(), ps.getFood(),
                    ps.getPrestigePoints(), ps.getCharacterCardIds(), ps.getBuildingCardIds()
            ));
        }
        currentEra             = snap.getCurrentEra();
        roundNumber            = snap.getRoundNumber();
        totemOnTiles           = new HashMap<>(snap.getTotemOnTiles());
        turnOrder              = new ArrayList<>(snap.getTurnOrder());
        upperRowCharacterIds   = new ArrayList<>(snap.getUpperRowCharacterAndEventIds());
        lowerRowCharacterIds   = new ArrayList<>(snap.getLowerRowCharacterAndEventIds());
        upperRowBuildingIds    = new ArrayList<>(snap.getUpperRowBuildingIds());
        lowerRowBuildingIds    = new ArrayList<>(snap.getLowerRowBuildingIds());
        notifyChanged();
    }

    public void applyStateChanged(StateChangedMessage m) {
        gameState = m.getStateName();
        notifyChanged();
    }

    public void applyTotemPlaced(TotemPlacedMessage m) {
        totemOnTiles.put(m.getTileId(), m.getNickname());
        notifyChanged();
    }

    public void applyEraChanged(EraChangedMessage m) {
        currentEra = m.getNewEra();
        notifyChanged();
    }

    public void applyFoodChanged(FoodChangedMessage m) {
        replacePlayer(m.getNickname(), old -> new PlayerView(
                old.getNickname(), old.getTotemColor(), m.getNewAmount(),
                old.getPrestigePoints(), old.getCharacterCardIds(), old.getBuildingCardIds()
        ));
    }

    public void applyPointsChanged(PointsChangedMessage m) {
        replacePlayer(m.getNickname(), old -> new PlayerView(
                old.getNickname(), old.getTotemColor(), old.getFood(),
                m.getNewPoints(), old.getCharacterCardIds(), old.getBuildingCardIds()
        ));
    }

    public void applyCharacterCardTaken(CharacterCardTakenMessage m) {
        if (m.isFromUpperRow()) upperRowCharacterIds.remove(Integer.valueOf(m.getCardId()));
        else                    lowerRowCharacterIds.remove(Integer.valueOf(m.getCardId()));

        replacePlayer(m.getNickname(), old -> {
            List<Integer> cards = new ArrayList<>(old.getCharacterCardIds());
            cards.add(m.getCardId());
            return new PlayerView(old.getNickname(), old.getTotemColor(), old.getFood(),
                    old.getPrestigePoints(), cards, old.getBuildingCardIds());
        });
    }

    public void applyBuildingCardTaken(BuildingCardTakenMessage m) {
        if (m.isFromUpperRow()) upperRowBuildingIds.remove(Integer.valueOf(m.getCardId()));
        else                    lowerRowBuildingIds.remove(Integer.valueOf(m.getCardId()));

        replacePlayer(m.getNickname(), old -> {
            List<Integer> cards = new ArrayList<>(old.getBuildingCardIds());
            cards.add(m.getCardId());
            return new PlayerView(old.getNickname(), old.getTotemColor(), old.getFood(),
                    old.getPrestigePoints(), old.getCharacterCardIds(), cards);
        });
    }

    public void applyRoundEnded(RoundEndedMessage m) {
        roundNumber = m.getRoundNumber();
        notifyChanged();
    }

    public void applyGameEnded(GameEndedMessage m) {
        winnerNickname = m.getWinnerNickname();
        finalScores    = new HashMap<>(m.getFinalScores());
        gameState      = "EndGameState";
        notifyChanged();
    }

    public void applyError(ErrorMessage m) {
        lastError = m.getReason();
        notifyChanged();
    }

    public void applyGameSuspended(GameSuspendedMessage m) {
        gameSuspended = true;
        notifyChanged();
    }

    public void applyGameResumed(GameResumedMessage m) {
        gameSuspended = false;
        notifyChanged();
    }

    public void applyPlayerDisconnected(PlayerDisconnectedMessage m) {
        lastDisconnectedPlayer = m.getNickname();
        notifyChanged();
    }

    public void applyPlayerReconnected(PlayerReconnectedMessage m) {
        lastDisconnectedPlayer = null;
        notifyChanged();
    }

    public void applyExtraCardPickAvailable(ExtraCardPickAvailableMessage m) {
        // il server gestisce il flusso — il client mostra solo che può pescare
        notifyChanged();
    }


    // -------------------------------------------------------
    // GETTER
    // -------------------------------------------------------

    public boolean isLoginSuccess()              { return loginSuccess; }
    public String getLastError()                 { return lastError; }
    public List<String> getLobbyPlayers()        { return Collections.unmodifiableList(lobbyPlayers); }
    public int getExpectedPlayers()              { return expectedPlayers; }
    public String getGameState()                 { return gameState; }
    public Era getCurrentEra()                   { return currentEra; }
    public int getRoundNumber()                  { return roundNumber; }
    public List<String> getTurnOrder()           { return Collections.unmodifiableList(turnOrder); }
    public Map<Character, String> getTotemOnTiles() { return Collections.unmodifiableMap(totemOnTiles); }
    public Map<String, PlayerView> getPlayers()  { return Collections.unmodifiableMap(players); }
    public List<Integer> getUpperRowCharacterIds(){ return Collections.unmodifiableList(upperRowCharacterIds); }
    public List<Integer> getLowerRowCharacterIds(){ return Collections.unmodifiableList(lowerRowCharacterIds); }
    public List<Integer> getUpperRowBuildingIds() { return Collections.unmodifiableList(upperRowBuildingIds); }
    public List<Integer> getLowerRowBuildingIds() { return Collections.unmodifiableList(lowerRowBuildingIds); }
    public String getWinnerNickname()            { return winnerNickname; }
    public Map<String, Integer> getFinalScores() { return Collections.unmodifiableMap(finalScores); }
    public boolean isGameSuspended()          { return gameSuspended; }
    public String getLastDisconnectedPlayer() { return lastDisconnectedPlayer; }


    // -------------------------------------------------------
    // HELPER
    // -------------------------------------------------------

    private void replacePlayer(String nickname, java.util.function.UnaryOperator<PlayerView> updater) {
        PlayerView old = players.get(nickname);
        if (old != null) players.put(nickname, updater.apply(old));
        notifyChanged();
    }
}