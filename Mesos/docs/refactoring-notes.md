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
