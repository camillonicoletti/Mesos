# Mesos - Final report

## Team members

- Benedetto Oliva
- Giorgio Ortona
- Alessandro Mazzeo
- Camillo Nicoletti

## Implemented features

- Complete Mesos game rules, with management of eras, offer tiles, turn order tiles, events and final scoring.
- TUI interface.
- JavaFX GUI interface.
- Socket communication.
- RMI communication.
- Single server able to accept both Socket and RMI clients in the same game.
- Player disconnection and reconnection handling.
- Game suspension when only one player remains connected, with a timer and automatic victory upon expiration.
- Game state saving and restoring.
- Optional persistent leaderboard via JDBC (loaded through a `db.properties` file).
- Debug mode, enabled by starting the server with `-Dgame.debug=true`, which exposes auxiliary TUI commands to skip rounds or force end-of-game for manual testing.

## Approximate division of work

- Giorgio Ortona: Socket, RMI, reconnections/disconnections, GUI.
- Other members: model, controller, game rules, tests, persistence, TUI support and integration.

## Main implementation choices

- State pattern to distinguish game phases: lobby, ready check, totem placement, action resolution, events, end of round, suspension and end of game.
- Observer pattern via `GameListener` and virtual views, so the controller notifies events without depending directly on the TUI or GUI.
- Network DTOs separated between client-bound and server-bound messages.
- Command pattern to represent the actions sent from the client to the controller.
- Visitor pattern on cards and effects, used to avoid scattered `instanceof` checks.
- Synchronization snapshots to realign clients, especially useful after reconnect or after important events.
- Separation between the Socket/RMI transport and the application logic through common connection interfaces.

## Communication protocols

### Socket

The client opens a TCP socket to the server. Messages are exchanged as serialized objects. Each client is handled on the server side by a dedicated connection, which forwards messages to the `LobbyManager`.

### RMI

In the RMI case the client does not send manual packets to the server: it invokes remote methods exposed by the server. The server in turn uses a remote callback registered by the client to notify game events.

## Database

The leaderboard reads JDBC credentials from `db.properties` in the server working directory. If the file does not exist or the connection is not available, the game remains runnable and the leaderboard is disabled.

The dependency declared in `pom.xml` is `mysql-connector-j`, and `db.properties.example` targets MySQL. The SQL dump in `deliverables/final/dump/schema.sql` currently uses PostgreSQL-specific types (`BIGSERIAL`, `UUID`) and must be adapted to the database engine actually used.

The SQL schema can be found in:

`deliverables/final/dump/schema.sql`

## Tests

Unit and integration tests cover the controller, game states, disconnections, client synchronization, cards, events and edge cases. The final coverage percentage is to be filled in after the last run of the chosen coverage tool.
