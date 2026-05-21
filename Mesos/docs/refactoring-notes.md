# Refactoring notes

These notes collect the main cleanup steps made around client/server startup
and network message handling. They are meant as study material for the final
discussion: what changed, why it is cleaner, and which design idea is behind it.

## Client runtime bootstrap

`ClientApp` and `RMIClientApp` use different transports, but once the
connection exists they follow the same lifecycle:

1. create `ClientModel`;
2. create `ClientController`;
3. choose TUI or GUI through `ClientViewSelector`;
4. run the selected view;
5. close the connection in `finally`.

That shared lifecycle now lives in `ClientRuntime`.

The useful design choice is separation of concerns: transport-specific classes
only know how to connect, while `ClientRuntime` knows how a client session is
assembled. This also makes Socket and RMI easier to compare during the oral
exam: they differ in connection creation, not in the MVC client wiring.

`ClientLaunchOptions` applies the same idea to command-line arguments: the
client launchers no longer parse host, port and RMI binding directly in their
`main` methods. They ask for parsed options, create the correct connection and
then delegate to `ClientRuntime`.

## Server lobby bootstrap

`Server` and `ServerLauncher` both need the same startup policy:

1. if `--fresh` is used, clear the persisted game;
2. otherwise try to restore a saved `GameController`;
3. create a `LobbyManager` from the restored controller or create a new lobby;
4. print the startup message.

That logic now lives in `LobbyBootstrap`.

`Server` still starts the socket-only server. `ServerLauncher` starts both
Socket and RMI, sharing the same `LobbyManager`, and passes the leaderboard
service to the lobby factory. `LobbyBootstrap` keeps those two cases aligned
without forcing them to duplicate persistence code.

The design idea is a small factory/bootstrap object: it centralizes object
creation that depends on startup state, without putting persistence details
inside the transport classes.

`ServerLaunchOptions` is the value object used by `Server`, `ServerLauncher`
and `RMIServerMain` to parse launch arguments. This keeps command-line parsing
away from the actual transport startup code, and makes defaults such as socket
port, RMI registry port and binding name explicit and testable.

## Message dispatching

The socket side receives DTO messages from the client and dispatches them in
`MessageDispatcher`. Instead of a long chain of `instanceof` checks, the class
uses a map from message class to handler.

This is cleaner because adding a new client command means registering a new
handler, not extending a long conditional block. The pattern is close to a
Command/Dispatcher style: every message type is routed to the operation that
implements that command.

## Client message handling

`ClientController` applies the same idea on the client side: server messages
are routed through a handler map. The controller remains responsible for
updating `ClientModel`, but the dispatch mechanism is explicit and local.

This is useful for GUI/TUI consistency because both views read from the same
`ClientModel`; the controller is the only place where protocol events become
client state.

## RMI transport

The RMI client does not send a generic message envelope to the server. It calls
specific remote methods such as login, ready state changes and game actions.

`RMIClientConnection` keeps the same high-level `ClientSideConnection`
interface used by the rest of the client, but internally maps each supported
client DTO to the corresponding remote method. This preserves the shared client
architecture while keeping the RMI boundary method-based, as expected from RMI.

`RMIServer` receives those method calls and delegates to `LobbyManager`, the
same application service used by the socket dispatcher. This means the game
rules are not duplicated between Socket and RMI.

## Card visitor

The card hierarchy now exposes `Card.accept(CardVisitor<R>)`. Concrete cards
call the specific visitor method for their real type, for example
`HunterCard` calls `visitHunter`, `BuildingCard` calls `visitBuilding`, and
`SustenanceEventCard` calls `visitSustenanceEvent`.

This moves type-specific behavior away from chains of `instanceof`. The first
domain usages are:

- `Tribe`, for character counting, builder discounts, shaman stars and inventor
  icon aggregation;
- `Game`, for builder final points and hunter immediate food effects;
- `Round`, for routing drawn tribe cards to event or character rows and for
  recognizing sustenance events during event ordering;
- `EventResolutionState`, for event titles and descriptions.

The important design point is that card-dependent behavior is now expressed as
a visitor operation over the card hierarchy. The next cleanup should introduce
the same idea for building `Effect` classes, because many remaining
`instanceof` checks are not on cards but on effects.

## Effect visitor

The building-effect hierarchy now exposes `Effect.accept(EffectVisitor<R>)`.
Concrete effects override it to call the specific visitor method, for example
`FullSetFoodEffect` calls `visitFullSetFood`, `ExtraCardUpperRowEffect` calls
`visitExtraCardUpperRow`, and end-game effects call their own end scoring
visitor methods.

`Effect.accept` also has a default implementation. This keeps `Effect` usable
as a functional interface in tests and preserves the old behavior where a
building with `null` effect simply did not match any specific effect type.
`BuildingCard.acceptEffect(...)` is the null-safe adapter used by the domain
code.

The first usages replace effect-specific `instanceof` checks in:

- `Player`, for effects triggered when character cards are added and for
  initializing stored progress on newly built buildings;
- `Game`, for end-game scoring effects;
- event cards such as Hunt, Sustenance, Shamanic Ritual and Cave Paintings;
- `Round`, for the totem-slot food bonus;
- `ActionResolutionState`, for counting extra upper-row picks.

The remaining `instanceof` checks in the action resolution flow are on board
slot effects (`FoodSlot`, `UpperRowSlot`, `LowerRowSlot`, `GainFoodSlotEffect`),
so they should be handled by a separate slot-effect visitor rather than by the
card or building-effect visitors.

## Controller services

`GameController` is being reduced back toward its intended role: coordinating
state transitions instead of owning every supporting responsibility directly.

`GamePersistenceService` now owns the controller-level rule for saving after an
action has settled. The controller still decides when an action has been
handled, but the persistence decision is no longer embedded in the action
entrypoint.

`SuspensionManager` now owns suspension data and timeout rules:

- whether the game is suspended;
- which state must be resumed after suspension;
- suspension start time and timeout duration;
- forced winner after timeout.

`GameController` keeps the same public methods used by server, GUI, TUI and
tests, but delegates the suspension calculations to the manager. This keeps the
external contract stable while making the internal responsibilities easier to
explain and test.

`DisconnectionManager` now owns the operational rules around disconnected
players: marking a player disconnected/connected, checking whether a nickname
is disconnected, marking every known player disconnected after restore, counting
connected game players and finding the only connected player for suspension
timeouts.

The actual `disconnectedPlayers` set currently remains in `GameController`.
This is intentional for this step: the controller was already serialized with
that field, so keeping it avoids unnecessary persistence-format churn while
still removing the disconnection logic from the controller body.

`TurnRecoveryService` now owns the read-only lookup of the current player across
normal and suspended states. Before this extraction, `GameController` had to
know that a `SuspendedState` wraps the real state and then inspect
`TotemPlacementState` or `ActionResolutionState` directly every time the UI
needed the current turn/action player.

The service does not advance the turn and does not mutate the state. This is
important because earlier bugs came from getter-like methods that accidentally
changed turn state. For now it is a small boundary: the controller asks the
service for the current visible player, while the state classes still own the
actual turn progression rules.

## Totem placement queue

`TotemPlacementState` no longer owns the raw turn-order list, the current index
and the set of players that already placed their totem. That bookkeeping now
lives in `TotemPlacementTurnQueue`.

The state still decides how to react to actions and when to move to
`ActionResolutionState`; the queue owns only the small data structure needed to:

- build the placement order from the previous turn-order tile;
- fall back to the game player order during the first round;
- skip disconnected players;
- remember who has already placed;
- answer whether every connected player has placed.

This is a conservative extraction: the game rules did not move into a generic
utility, but the state is no longer forced to mix action validation with list
index manipulation. It also makes the disconnection behavior easier to explain:
when a player disconnects, the queue advances past unavailable players without
changing the rule that only connected players are required to place.

## Action pick budget

`ActionResolutionState` no longer stores the regular upper-row and lower-row
pick counters as loose integers. Those counters now live in `RowPickBudget`.

The state still performs the gameplay checks: current player, card existence,
building affordability and listener notifications. `RowPickBudget` owns only
the small accounting rules:

- how many upper/lower row picks remain;
- whether a row can still be picked;
- consuming one pick only after the card/building was actually taken;
- clearing a row budget when that row has no cards or buildings left.

This avoids scattering `remainingUpperPicks` / `remainingLowerPicks` checks
through the state and makes the end-of-turn condition explicit:
`regularPickBudget.isDepleted()`.
