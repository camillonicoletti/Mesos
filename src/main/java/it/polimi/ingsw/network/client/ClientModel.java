package it.polimi.ingsw.network.client;

import it.polimi.ingsw.model.enums.Era;
import it.polimi.ingsw.model.enums.TotemColor;
import it.polimi.ingsw.network.protocol.clientbound.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClientModel {

    private static final long CONNECTION_EVENT_VISIBLE_MILLIS = 10_000L;

    private int lastFinishedRound = 0;
    private int    lastResolvedEventCardId = -1;
    private String lastResolvedEventTitle;
    private String lastResolvedEventDescription;
    private List<EventResolvedMessage.PlayerEffect> lastResolvedEventEffects = List.of();
    private final List<EventResolvedMessage> resolvedEvents = new ArrayList<>();

    private boolean gameSuspended;
    private long suspensionDeadlineMillis = -1L;
    private boolean connected = true;
    private boolean reconnecting;
    // Set only by applyServerDisconnected (involuntary drop). Stays false on voluntary quit/disconnect
    // so the GUI can show a dedicated "server disconnected" overlay only when appropriate. Terminal.
    private boolean serverDisconnected = false;
    private String lastDisconnectedPlayer;
    private String lastConnectionEvent;
    private long lastConnectionEventExpiresAtMillis = -1L;
    private long lastConnectionEventVersion = 0L;
    private Map<String, TotemColor> lobbyPlayerColors = new LinkedHashMap<>();
    private boolean promotedToLobbyLeader;


    private boolean loginSuccess;
    private String lastError;
    private long lastErrorVersion = 0L;

    private String myNickname;

    private List<String> lobbyPlayers = new ArrayList<>();
    private List<String> readyPlayers = new ArrayList<>();
    private int expectedPlayers;

    private String gameState;
    private boolean gameEndedReceived = false;
    private String currentActionPlayer;
    private String pickBudgetPlayer;
    private int remainingUpperPicks = -1;
    private int remainingLowerPicks = -1;
    // True when this client has just received an ExtraCardPickAvailableMessage
    // targeted at its own nickname. Cleared as soon as the current player
    // changes or the state leaves ActionResolutionState.
    private boolean extraPickAvailableForMe = false;
    private String lastTurnEvent;
    private long lastTurnEventExpiresAtMillis = -1L;
    private Era currentEra;
    private int roundNumber;
    private List<String> turnOrder = new ArrayList<>();
    private Map<String, Integer> turnOrderPositions = new HashMap<>();
    private List<String> actionResolvedPlayers = new ArrayList<>();
    private Map<Character, String> totemOnTiles = new HashMap<>();
    private Set<String> disconnectedPlayers = new HashSet<>();

    private List<Integer> upperRowCharacterIds = new ArrayList<>();
    private List<Integer> lowerRowCharacterIds = new ArrayList<>();
    private List<Integer> upperRowBuildingIds   = new ArrayList<>();
    private List<Integer> lowerRowBuildingIds   = new ArrayList<>();
    private Set<Integer> eventCardIds = new HashSet<>();
    private int remainingTribeDeckCards = -1;

    private Map<String, PlayerView> players = new LinkedHashMap<>();
    private long takenCardEventSequence = 0L;
    private TakenCardEvent lastTakenCardEvent;

    private String winnerNickname;
    private Map<String, Integer> finalScores = new HashMap<>();
    private Map<String, Integer> finalFood = new HashMap<>();

    // ── apply ────────────────────────────────────────────────────────────────

    public void applyLoginAck(LoginAckMessage m) {
        connected = m.isSuccess();
        reconnecting = false;
        if (m.isSuccess()) {
            serverDisconnected = false;
        }
        loginSuccess = m.isSuccess();
        lastError = m.isSuccess() ? null : m.getErrorReason();
        if (lastError != null) {
            lastErrorVersion++;
        }
    }

    public void applyLobbyUpdate(LobbyUpdateMessage m) {
        String previousLeader = lobbyPlayers.isEmpty() ? null : lobbyPlayers.get(0);
        boolean wasInLobby = myNickname != null && lobbyPlayers.contains(myNickname);

        lobbyPlayers = new ArrayList<>();
        readyPlayers = new ArrayList<>();
        lobbyPlayerColors = new LinkedHashMap<>();

        for (LobbyPlayerSnapshot player : m.getPlayers()) {
            lobbyPlayers.add(player.getNickname());
            lobbyPlayerColors.put(player.getNickname(), player.getTotemColor());

            if (player.isReady()) {
                readyPlayers.add(player.getNickname());
            }
        }

        expectedPlayers = m.getExpectedPlayers();

        boolean iAmCurrentLeader = myNickname != null
                && !lobbyPlayers.isEmpty()
                && myNickname.equals(lobbyPlayers.get(0));
        if (wasInLobby && previousLeader != null && !previousLeader.equals(myNickname)) {
            promotedToLobbyLeader = true;
        }
        if (!iAmCurrentLeader) {
            promotedToLobbyLeader = false;
        }

    }


    public void applyGameStarted(GameStartedMessage m) {
        resolvedEvents.clear();
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
        turnOrderPositions   = new HashMap<>(snap.getTurnOrderPositions());
        ensureTurnOrderPositions();
        disconnectedPlayers  = new HashSet<>(snap.getDisconnectedPlayers());
        // On snapshot, treat all players as already resolved to avoid showing stale pending indicators.
        if (gamePhase().isActionResolution()) {
            actionResolvedPlayers = new ArrayList<>(turnOrder);
        }
        upperRowCharacterIds = new ArrayList<>(snap.getUpperRowCharacterAndEventIds());
        lowerRowCharacterIds = new ArrayList<>(snap.getLowerRowCharacterAndEventIds());
        upperRowBuildingIds  = new ArrayList<>(snap.getUpperRowBuildingIds());
        lowerRowBuildingIds  = new ArrayList<>(snap.getLowerRowBuildingIds());
        eventCardIds = new HashSet<>();
        eventCardIds.addAll(snap.getUpperRowEventIds());
        eventCardIds.addAll(snap.getLowerRowEventIds());
        remainingTribeDeckCards = snap.getRemainingTribeDeckCards();
        clearCurrentActionPlayerIfDisconnected();
    }

    public void applyStateChanged(StateChangedMessage m) {
        gameState = m.getStateName();
        GamePhase phase = gamePhase();
        // Clear the current player when the new state does not actively manage turns.
        if (!phase.managesCurrentTurn()) {
            currentActionPlayer = null;
            clearPickBudget();
        }
        // Reset phase-specific progress when entering a new action or placement phase.
        if (phase.managesCurrentTurn()) {
            actionResolvedPlayers = new ArrayList<>();
            clearPickBudget();
        }
        // The extra-pick prompt is only valid inside ActionResolutionState.
        if (!phase.isActionResolution()) {
            extraPickAvailableForMe = false;
        }
        if (phase.isEventResolution()) {
            lastResolvedEventCardId = -1;
            lastResolvedEventTitle = null;
            lastResolvedEventDescription = null;
            lastResolvedEventEffects = List.of();
        }
        lastError = null;
    }

    public void applyCurrentPlayerChanged(CurrentPlayerChangedMessage m) {
        String previousPlayer = currentActionPlayer;
        currentActionPlayer = m.getNickname();
        // Any current-player change clears a lingering extra-pick prompt: the
        // server emits ExtraCardPickAvailableMessage right after onCurrentPlayerChanged
        // when the next player has an extra pick, so we'd be re-set then.
        extraPickAvailableForMe = false;
        if (gamePhase().isActionResolution()
                && (!Objects.equals(previousPlayer, currentActionPlayer)
                || !Objects.equals(pickBudgetPlayer, currentActionPlayer))) {
            initializeRegularPickBudget(currentActionPlayer);
        }
        if (currentActionPlayer != null && !currentActionPlayer.equals(previousPlayer)) {
            setTurnEvent("Turn passed to " + currentActionPlayer);
        }
    }

    public void applyTotemPlaced(TotemPlacedMessage m) {
        lastError = null;
        turnOrder.remove(m.getNickname());
        totemOnTiles.put(m.getTileId(), m.getNickname());
    }

    public void applyEventResolved(EventResolvedMessage m) {
        lastError = null;
        lastResolvedEventCardId       = m.getCardId();
        lastResolvedEventTitle        = m.getTitle();
        lastResolvedEventDescription  = m.getDescription();
        lastResolvedEventEffects     = m.getEffects() != null ? m.getEffects() : List.of();
        resolvedEvents.add(m);
        for (EventResolvedMessage.PlayerEffect effect : lastResolvedEventEffects) {
            replacePlayer(effect.getNickname(), old -> new PlayerView(
                    old.getNickname(), old.getTotemColor(),
                    effect.getNewFood(), effect.getNewPoints(),
                    old.getCharacterCardIds(), old.getBuildingCardIds()));
        }
    }

    public void applyEraChanged(EraChangedMessage m) {
        currentEra = m.getNewEra();
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
        lastError = null;
        rememberTakenCard(m.getNickname(), m.getCardId(), m.isFromUpperRow(), false);
        if (m.isFromUpperRow()) upperRowCharacterIds.remove(Integer.valueOf(m.getCardId()));
        else                    lowerRowCharacterIds.remove(Integer.valueOf(m.getCardId()));
        decrementPickBudget(m.getNickname(), m.isFromUpperRow());

        replacePlayer(m.getNickname(), old -> {
            List<Integer> cards = new ArrayList<>(old.getCharacterCardIds());
            cards.add(m.getCardId());
            return new PlayerView(old.getNickname(), old.getTotemColor(), old.getFood(),
                    old.getPrestigePoints(), cards, old.getBuildingCardIds());
        });
    }

    public void applyBuildingCardTaken(BuildingCardTakenMessage m) {
        lastError = null;
        rememberTakenCard(m.getNickname(), m.getCardId(), m.isFromUpperRow(), true);
        if (m.isFromUpperRow()) upperRowBuildingIds.remove(Integer.valueOf(m.getCardId()));
        else                    lowerRowBuildingIds.remove(Integer.valueOf(m.getCardId()));
        decrementPickBudget(m.getNickname(), m.isFromUpperRow());

        replacePlayer(m.getNickname(), old -> {
            List<Integer> cards = new ArrayList<>(old.getBuildingCardIds());
            cards.add(m.getCardId());
            return new PlayerView(old.getNickname(), old.getTotemColor(), old.getFood(),
                    old.getPrestigePoints(), old.getCharacterCardIds(), cards);
        });
    }

    public void applyRoundEnded(RoundEndedMessage m) {
        lastFinishedRound = m.getRoundNumber();
        roundNumber = Math.max(roundNumber, m.getRoundNumber());
    }

    public void applyGameEnded(GameEndedMessage m) {
        winnerNickname = m.getWinnerNickname();
        finalScores    = new HashMap<>(m.getFinalScores());
        gameState      = GamePhase.END_GAME.stateName();
        gameEndedReceived = true;
        if (m.getFinalFood() != null) {
            finalFood = new HashMap<>(m.getFinalFood());
        }
    }

    public void applyError(ErrorMessage m) {
        lastError = m.getReason();
        lastErrorVersion++;
    }

    public void applyGameSuspended(GameSuspendedMessage m) {
        gameSuspended = true;
        suspensionDeadlineMillis = m.getRemainingMillis() >= 0L
                ? System.currentTimeMillis() + m.getRemainingMillis()
                : -1L;
    }

    public void applyGameResumed(GameResumedMessage m) {
        gameSuspended = false;
        suspensionDeadlineMillis = -1L;
    }

    public void applyPlayerDisconnected(PlayerDisconnectedMessage m) {
        lastDisconnectedPlayer = m.getNickname();
        disconnectedPlayers.add(m.getNickname());
        clearCurrentActionPlayerIfActor(m.getNickname());
        setConnectionEvent(m.getNickname() + " disconnected");
    }

    public void applyPlayerReconnected(PlayerReconnectedMessage m) {
        lastDisconnectedPlayer = null;
        disconnectedPlayers.remove(m.getNickname());
        setConnectionEvent(m.getNickname() + " reconnected");
    }

    public void applyExtraCardPickAvailable(ExtraCardPickAvailableMessage m) {
        currentActionPlayer = m.getNickname();
        pickBudgetPlayer = m.getNickname();
        remainingUpperPicks = Math.max(0, m.getPicks());
        remainingLowerPicks = 0;
        // Flag the local player so the GUI/TUI can offer the Skip button/command.
        extraPickAvailableForMe = myNickname != null
                && myNickname.equals(m.getNickname())
                && remainingUpperPicks > 0;
    }

    /**
     * Returns {@code true} when the server has notified that the local player
     * has an optional extra pick available (building #13). Cleared as soon as
     * the current player changes or the game leaves ActionResolutionState.
     */
    public boolean isExtraPickAvailableForMe() {
        return extraPickAvailableForMe;
    }

    public void applyConnectionLost(String reason) {
        connected = false;
        reconnecting = false;
        lastError = reason;
        lastErrorVersion++;
    }

    public void applyReconnecting(String reason) {
        connected = false;
        reconnecting = true;
        if (!Objects.equals(lastError, reason)) {
            lastError = reason;
            lastErrorVersion++;
        }
    }

    /**
     * Marks the connection to the server as lost due to an involuntary drop
     * (socket failure, heartbeat timeout, remote exception). Distinct from
     * {@link #applyConnectionLost(String)}, which is also reused for voluntary
     * exits ({@code quit}/{@code disconnect}); the flag set here lets the view
     * tell the two cases apart and show a dedicated "server disconnected"
     * overlay only when the user did not choose to leave.
     *
     * <p>The client keeps trying to reconnect; a later successful
     * {@link LoginAckMessage} clears this flag.</p>
     *
     * @param reason short human-readable cause, surfaced as the last error
     */
    public void applyServerDisconnected(String reason) {
        serverDisconnected = true;
        applyReconnecting(reason);
    }

    /**
     * @return {@code true} only after an involuntary server drop was applied
     *         via {@link #applyServerDisconnected(String)}. Voluntary
     *         {@code quit}/{@code disconnect} leave this flag {@code false}.
     */
    public boolean isServerDisconnected() {
        return serverDisconnected;
    }

    // ── setters ──────────────────────────────────────────────────────────────

    public void setMyNickname(String nickname) {
        this.myNickname = nickname;
    }

    // ── getters ──────────────────────────────────────────────────────────────

    public boolean isLoginSuccess()                { return loginSuccess; }
    public boolean isConnected()                   { return connected; }
    public boolean isReconnecting()                { return reconnecting; }
    public String getLastError()                   { return lastError; }
    public long getLastErrorVersion()              { return lastErrorVersion; }
    public String getLastConnectionEvent() {
        if (lastConnectionEvent == null) {
            return null;
        }
        if (lastConnectionEventExpiresAtMillis >= 0L
                && System.currentTimeMillis() >= lastConnectionEventExpiresAtMillis) {
            return null;
        }
        return lastConnectionEvent;
    }
    public long getLastConnectionEventVersion()    { return lastConnectionEventVersion; }
    public String getMyNickname()                  { return myNickname; }
    public List<String> getLobbyPlayers()          { return Collections.unmodifiableList(lobbyPlayers); }
    public List<String> getReadyPlayers()          { return Collections.unmodifiableList(readyPlayers); }
    public boolean isPromotedToLobbyLeader()       { return promotedToLobbyLeader; }
    public int getExpectedPlayers()                { return expectedPlayers; }
    public String getGameState()                   { return gameState; }
    public String getCurrentActionPlayer()         { return currentActionPlayer; }
    public String getLastTurnEvent() {
        if (lastTurnEvent == null) {
            return null;
        }
        if (lastTurnEventExpiresAtMillis >= 0L
                && System.currentTimeMillis() >= lastTurnEventExpiresAtMillis) {
            return null;
        }
        return lastTurnEvent;
    }
    public Era getCurrentEra()                     { return currentEra; }
    public int getRoundNumber()                    { return roundNumber; }
    public List<String> getTurnOrder()             { return Collections.unmodifiableList(turnOrder); }
    public Map<String, Integer> getTurnOrderPositions() {
        return Collections.unmodifiableMap(turnOrderPositions);
    }
    public List<String> getActionResolvedPlayers() { return Collections.unmodifiableList(actionResolvedPlayers); }
    public Map<Character, String> getTotemOnTiles(){ return Collections.unmodifiableMap(totemOnTiles); }
    public Map<String, PlayerView> getPlayers()    { return Collections.unmodifiableMap(players); }
    public List<Integer> getUpperRowCharacterIds() { return Collections.unmodifiableList(upperRowCharacterIds); }
    public List<Integer> getLowerRowCharacterIds() { return Collections.unmodifiableList(lowerRowCharacterIds); }
    public List<Integer> getUpperRowBuildingIds()  { return Collections.unmodifiableList(upperRowBuildingIds); }
    public List<Integer> getLowerRowBuildingIds()  { return Collections.unmodifiableList(lowerRowBuildingIds); }
    public boolean isEventCard(int id)             { return eventCardIds.contains(id); }
    public int getRemainingTribeDeckCards()        { return remainingTribeDeckCards; }
    public TakenCardEvent getLastTakenCardEvent()  { return lastTakenCardEvent; }
    public String getWinnerNickname()              { return winnerNickname; }
    public Map<String, Integer> getFinalScores()   { return Collections.unmodifiableMap(finalScores); }
    public Map<String, Integer> getFinalFood()     { return Collections.unmodifiableMap(finalFood); }
    public boolean isGameSuspended()               { return gameSuspended; }
    public long getSuspensionRemainingMillis() {
        if (!gameSuspended || suspensionDeadlineMillis < 0L) {
            return -1L;
        }
        return Math.max(0L, suspensionDeadlineMillis - System.currentTimeMillis());
    }
    public String getLastDisconnectedPlayer()      { return lastDisconnectedPlayer; }
    public boolean isPlayerDisconnected(String nickname) {
        return nickname != null && disconnectedPlayers.contains(nickname);
    }
    public Map<String, TotemColor> getLobbyPlayerColors() {
        return Collections.unmodifiableMap(lobbyPlayerColors);
    }

    public List<TotemColor> getAvailableTotemColors() {
        Set<TotemColor> taken = new HashSet<>(lobbyPlayerColors.values());
        List<TotemColor> available = new ArrayList<>();
        for (TotemColor color : TotemColor.values()) {
            if (!taken.contains(color)) {
                available.add(color);
            }
        }
        return Collections.unmodifiableList(available);
    }

    public boolean isTotemColorAvailable(TotemColor color) {
        return color != null && getAvailableTotemColors().contains(color);
    }

    public boolean areAllTotemColorsTaken() {
        return getAvailableTotemColors().isEmpty();
    }

    public TotemColor firstAvailableTotemColorOr(TotemColor fallback) {
        List<TotemColor> available = getAvailableTotemColors();
        return available.isEmpty() ? fallback : available.get(0);
    }


    public boolean isMyTurn() {
        return myNickname != null
                && myNickname.equals(getCurrentActionResolutionPlayer());
    }

    public boolean canCurrentActionPlayerPickFromRow(boolean upper) {
        if (!isMyTurn()) {
            return false;
        }

        int remaining = upper ? remainingUpperPicks : remainingLowerPicks;
        // Prefer server-provided pick budget; fall back to tile-based inference when it is unavailable.
        if (remaining >= 0) {
            return remaining > 0;
        }

        char tile = offerTileForPlayer(myNickname);
        return tileAllowsRow(tile, upper);
    }

    public String getCurrentActionResolutionPlayer() {
        if (!gamePhase().isActionResolution()) {
            return null;
        }
        if (currentActionPlayer != null && !disconnectedPlayers.contains(currentActionPlayer)) {
            return currentActionPlayer;
        }
        return null;
    }
    public boolean isLobbyLeader() {
        return myNickname != null
                && !lobbyPlayers.isEmpty()
                && myNickname.equals(lobbyPlayers.get(0));
    }

    public boolean isGameStarted() {
        return GamePhase.hasGameStarted(gameState);
    }

    public boolean isInPreGame() {
        return GamePhase.isPreGameState(gameState);
    }

    public boolean isGameEnded() {
        return gameEndedReceived;
    }

    public int    getLastResolvedEventCardId()      { return lastResolvedEventCardId; }
    public String getLastResolvedEventTitle()       { return lastResolvedEventTitle; }
    public String getLastResolvedEventDescription() { return lastResolvedEventDescription; }

    public List<EventResolvedMessage.PlayerEffect> getLastResolvedEventEffects() {
        return Collections.unmodifiableList(lastResolvedEventEffects);
    }

    public List<EventResolvedMessage> getResolvedEvents() {
        return Collections.unmodifiableList(resolvedEvents);
    }

    public int getLastFinishedRound() { return lastFinishedRound; }

    public String getCurrentTotemPlacementPlayer() {
        if (!gamePhase().isTotemPlacement()) {
            return null;
        }
        if (currentActionPlayer != null && !disconnectedPlayers.contains(currentActionPlayer)) {
            return currentActionPlayer;
        }
        if (turnOrder.isEmpty()) {
            return null;
        }

        // If the server has not sent a current player, infer the next placement player from turn order.
        Set<String> playersWhoAlreadyPlaced = new HashSet<>(totemOnTiles.values());
        for (String nickname : turnOrder) {
            if (!playersWhoAlreadyPlaced.contains(nickname)
                    && !disconnectedPlayers.contains(nickname)) {
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
    }

    private GamePhase gamePhase() {
        return GamePhase.fromStateName(gameState);
    }

    private void setConnectionEvent(String message) {
        lastConnectionEvent = message;
        lastConnectionEventExpiresAtMillis = System.currentTimeMillis() + CONNECTION_EVENT_VISIBLE_MILLIS;
        lastConnectionEventVersion++;
    }

    private void setTurnEvent(String message) {
        lastTurnEvent = message;
        lastTurnEventExpiresAtMillis = System.currentTimeMillis() + CONNECTION_EVENT_VISIBLE_MILLIS;
    }

    private void clearCurrentActionPlayerIfActor(String nickname) {
        if (currentActionPlayer != null
                && currentActionPlayer.equals(nickname)) {
            currentActionPlayer = null;
            clearPickBudget();
        }
    }

    private void ensureTurnOrderPositions() {
        int nextPosition = 1;
        for (String nickname : turnOrder) {
            turnOrderPositions.putIfAbsent(nickname, nextPosition);
            nextPosition++;
        }
    }

    private void clearCurrentActionPlayerIfDisconnected() {
        if (currentActionPlayer != null && disconnectedPlayers.contains(currentActionPlayer)) {
            currentActionPlayer = null;
            clearPickBudget();
        }
    }

    private void initializeRegularPickBudget(String nickname) {
        if (nickname == null) {
            clearPickBudget();
            return;
        }

        char tile = offerTileForPlayer(nickname);
        pickBudgetPlayer = nickname;
        remainingUpperPicks = upperPicksForTile(tile);
        remainingLowerPicks = lowerPicksForTile(tile);
    }

    private void decrementPickBudget(String nickname, boolean upper) {
        if (!Objects.equals(pickBudgetPlayer, nickname)) {
            return;
        }

        if (upper) {
            remainingUpperPicks = decrementIfPositive(remainingUpperPicks);
            return;
        }

        remainingLowerPicks = decrementIfPositive(remainingLowerPicks);
    }

    private void clearPickBudget() {
        pickBudgetPlayer = null;
        remainingUpperPicks = -1;
        remainingLowerPicks = -1;
    }

    private int decrementIfPositive(int value) {
        return value > 0 ? value - 1 : value;
    }

    private void rememberTakenCard(String nickname, int cardId, boolean fromUpperRow, boolean building) {
        lastTakenCardEvent = new TakenCardEvent(++takenCardEventSequence, nickname, cardId, fromUpperRow, building);
    }

    private char offerTileForPlayer(String nickname) {
        if (nickname == null) {
            return 0;
        }
        for (Map.Entry<Character, String> entry : totemOnTiles.entrySet()) {
            if (nickname.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return 0;
    }

    private boolean tileAllowsRow(char tile, boolean upper) {
        return upper ? upperPicksForTile(tile) > 0 : lowerPicksForTile(tile) > 0;
    }

    private int upperPicksForTile(char tile) {
        return switch (tile) {
            case 'C' -> 1;
            case 'E' -> 1;
            case 'F' -> 2;
            case 'G' -> 2;
            default -> 0;
        };
    }

    private int lowerPicksForTile(char tile) {
        return switch (tile) {
            case 'B' -> 1;
            case 'D' -> 2;
            case 'E' -> 1;
            case 'G' -> 1;
            default -> 0;
        };
    }

    public record TakenCardEvent(long sequence, String nickname, int cardId, boolean fromUpperRow, boolean building) {}
}
