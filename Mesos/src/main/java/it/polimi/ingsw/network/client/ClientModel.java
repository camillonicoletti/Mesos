package it.polimi.ingsw.network.client;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.network.protocol.clientbound.*;

import java.util.*;

public class ClientModel {

    public interface Listener {
        void onModelChanged();
    }

    private boolean gameSuspended;
    private boolean connected = true;
    private String lastDisconnectedPlayer;
    private String lastConnectionEvent;

    private boolean loginSuccess;
    private String lastError;

    private String myNickname;

    private List<String> lobbyPlayers = new ArrayList<>();
    private List<String> readyPlayers = new ArrayList<>();
    private int expectedPlayers;

    private String gameState;
    private Era currentEra;
    private int roundNumber;
    private List<String> turnOrder = new ArrayList<>();
    private Map<Character, String> totemOnTiles = new HashMap<>();

    private List<Integer> upperRowCharacterIds = new ArrayList<>();
    private List<Integer> lowerRowCharacterIds = new ArrayList<>();
    private List<Integer> upperRowBuildingIds   = new ArrayList<>();
    private List<Integer> lowerRowBuildingIds   = new ArrayList<>();

    private Map<String, PlayerView> players = new LinkedHashMap<>();

    private String winnerNickname;
    private Map<String, Integer> finalScores = new HashMap<>();

    private final List<Listener> listeners = new ArrayList<>();

    public void addListener(Listener l) { listeners.add(l); }

    private void notifyChanged() {
        for (Listener l : listeners) l.onModelChanged();
    }

    // ── apply ────────────────────────────────────────────────────────────────

    public void applyLoginAck(LoginAckMessage m) {
        connected = true;
        loginSuccess = m.isSuccess();
        lastError = m.isSuccess() ? null : m.getErrorReason();
        notifyChanged();
    }

    public void applyLobbyUpdate(LobbyUpdateMessage m) {
        lobbyPlayers = new ArrayList<>();
        readyPlayers = new ArrayList<>();
        for (LobbyPlayerSnapshot player : m.getPlayers()) {
            lobbyPlayers.add(player.getNickname());
            if (player.isReady()) {
                readyPlayers.add(player.getNickname());
            }
        }
        expectedPlayers = m.getExpectedPlayers();
        notifyChanged();
    }

    public void applyGameStarted(GameStartedMessage m) {
        applySnapshot(m.getSnapshot());
    }

    public void applyGameSync(GameSyncMessage m) {
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
        currentEra           = snap.getCurrentEra();
        roundNumber          = snap.getRoundNumber();
        totemOnTiles         = new HashMap<>(snap.getTotemOnTiles());
        turnOrder            = new ArrayList<>(snap.getTurnOrder());
        upperRowCharacterIds = new ArrayList<>(snap.getUpperRowCharacterAndEventIds());
        lowerRowCharacterIds = new ArrayList<>(snap.getLowerRowCharacterAndEventIds());
        upperRowBuildingIds  = new ArrayList<>(snap.getUpperRowBuildingIds());
        lowerRowBuildingIds  = new ArrayList<>(snap.getLowerRowBuildingIds());
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
        lastConnectionEvent = m.getNickname() + " si è disconnesso";
        notifyChanged();
    }

    public void applyPlayerReconnected(PlayerReconnectedMessage m) {
        lastDisconnectedPlayer = null;
        lastConnectionEvent = m.getNickname() + " si è riconnesso";
        notifyChanged();
    }

    public void applyExtraCardPickAvailable(ExtraCardPickAvailableMessage m) {
        notifyChanged();
    }

    public void applyConnectionLost(String reason) {
        connected = false;
        lastError = reason;
        notifyChanged();
    }

    // ── setters ──────────────────────────────────────────────────────────────

    public void setMyNickname(String nickname) {
        this.myNickname = nickname;
        notifyChanged();
    }

    // ── getters ──────────────────────────────────────────────────────────────

    public boolean isLoginSuccess()                { return loginSuccess; }
    public boolean isConnected()                   { return connected; }
    public String getLastError()                   { return lastError; }
    public String getLastConnectionEvent()         { return lastConnectionEvent; }
    public String getMyNickname()                  { return myNickname; }
    public List<String> getLobbyPlayers()          { return Collections.unmodifiableList(lobbyPlayers); }
    public List<String> getReadyPlayers()          { return Collections.unmodifiableList(readyPlayers); }
    public int getExpectedPlayers()                { return expectedPlayers; }
    public String getGameState()                   { return gameState; }
    public Era getCurrentEra()                     { return currentEra; }
    public int getRoundNumber()                    { return roundNumber; }
    public List<String> getTurnOrder()             { return Collections.unmodifiableList(turnOrder); }
    public Map<Character, String> getTotemOnTiles(){ return Collections.unmodifiableMap(totemOnTiles); }
    public Map<String, PlayerView> getPlayers()    { return Collections.unmodifiableMap(players); }
    public List<Integer> getUpperRowCharacterIds() { return Collections.unmodifiableList(upperRowCharacterIds); }
    public List<Integer> getLowerRowCharacterIds() { return Collections.unmodifiableList(lowerRowCharacterIds); }
    public List<Integer> getUpperRowBuildingIds()  { return Collections.unmodifiableList(upperRowBuildingIds); }
    public List<Integer> getLowerRowBuildingIds()  { return Collections.unmodifiableList(lowerRowBuildingIds); }
    public String getWinnerNickname()              { return winnerNickname; }
    public Map<String, Integer> getFinalScores()   { return Collections.unmodifiableMap(finalScores); }
    public boolean isGameSuspended()               { return gameSuspended; }
    public String getLastDisconnectedPlayer()      { return lastDisconnectedPlayer; }

    public boolean isMyTurn() {
        return myNickname != null
                && !turnOrder.isEmpty()
                && myNickname.equals(turnOrder.get(0));
    }

    public String getCurrentTotemPlacementPlayer() {
        if (!"TotemPlacementState".equals(gameState) || turnOrder.isEmpty()) {
            return null;
        }

        Set<String> playersWhoAlreadyPlaced = new HashSet<>(totemOnTiles.values());
        for (String nickname : turnOrder) {
            if (!playersWhoAlreadyPlaced.contains(nickname)) {
                return nickname;
            }
        }
        return null;
    }

    public boolean isMyTotemPlacementTurn() {
        return myNickname != null && myNickname.equals(getCurrentTotemPlacementPlayer());
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private void replacePlayer(String nickname, java.util.function.UnaryOperator<PlayerView> updater) {
        PlayerView old = players.get(nickname);
        if (old != null) players.put(nickname, updater.apply(old));
        notifyChanged();
    }
}
