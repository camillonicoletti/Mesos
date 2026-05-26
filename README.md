 # Mesos

Software Engineering project — Politecnico di Milano, A.Y. 2025/2026.

## Overview

Mesos is a distributed, multiplayer implementation of the board game of the same name.
The project follows an MVC architecture with a single authoritative server and
multiple thin clients. Players can join the same match either through a Socket
or an RMI connection, and play with a textual (TUI) or graphical (GUI)
interface.

## Implemented features

- Complete game rules (eras, offer tiles, turn order tiles, character/building/
  event cards, totem placement, final scoring).
- Textual client (TUI).
- Graphical client (JavaFX GUI).
- Socket communication.
- RMI communication.
- Single server that accepts Socket and RMI clients in the same lobby.
- Player disconnection and reconnection.
- Automatic suspension of the match when only one player remains connected,
  with a timer and automatic victory upon expiration.
- Persistent game state: the server saves the current match and can restore it
  after a restart.
- Optional persistent leaderboard backed by a relational database (loaded
  through a `db.properties` file).

## Technologies

- Java 23
- Maven (`maven-shade-plugin` for executable JARs)
- JavaFX 23 (GUI), with AtlantaFX and Ikonli for styling and icons
- Java RMI and TCP Sockets (network layer)
- Gson (JSON resources)
- JUnit Jupiter (tests)
- JDBC (optional leaderboard)

## Requirements

- Java 23 or later (JDK with JavaFX runtime or the JavaFX modules on the
  module path; the shaded client JAR bundles its JavaFX dependencies).
- Maven 3.9 or later (only required to build from source).

The graphical assets are not committed to the repository. To use the GUI with
the full images, place the assets under:

```text
src/main/resources/assets
```

The TUI works without any graphical asset.

## How to build

From the project root:

```bash
mvn clean package
```

To skip the test suite for a faster build:

```bash
mvn clean package -DskipTests
```

Two executable JARs are produced in `deliverables/final/jar/`:

- `mesos-server.jar` — main class `it.polimi.ingsw.network.server.ServerLauncher`
- `mesos-client.jar` — main class `it.polimi.ingsw.network.client.ClientLauncher`

## How to run the server

The server exposes both Socket and RMI on the same lobby.

```bash
java -jar deliverables/final/jar/mesos-server.jar
```

Default endpoints:

- Socket port: `1234`
- RMI registry port: `1099`
- RMI binding name: `MesosServer`

To start ignoring any previous saved game:

```bash
java -jar deliverables/final/jar/mesos-server.jar --fresh
```

To override ports and binding name (positional arguments):

```bash
java -jar deliverables/final/jar/mesos-server.jar [--fresh] [socketPort] [rmiPort] [rmiBindingName]
```

## How to run a client

```bash
# Socket
java -jar deliverables/final/jar/mesos-client.jar socket <host> <socketPort>

# RMI
java -jar deliverables/final/jar/mesos-client.jar rmi <host> [registryPort] [bindingName]
```

Examples:

```bash
java -jar deliverables/final/jar/mesos-client.jar socket localhost 1234
java -jar deliverables/final/jar/mesos-client.jar rmi    localhost 1099 MesosServer
```

After connecting, the client asks which interface to use:

```
Select interface:
  1 — TUI (terminal)
  2 — GUI (graphical interface)
```

## TUI usage

The TUI prompts for a nickname, asks whether to reconnect to an existing
match, and then guides the player through totem color selection, lobby ready
check, totem placement, card picks and event resolution. Commands are read
line by line from standard input; the screen is redrawn after each server
update. Error messages from the server are printed with an `[ERROR]` prefix.

## GUI usage

The GUI is a JavaFX application launched by the same client JAR when option
`2` is chosen at startup. It mirrors the TUI flow with a graphical lobby,
board, card market, player dock and end-game view. If a full lobby is reached
and the user is not already a participant, the GUI shows a message inviting
the user to use the Reconnect flow instead.

The GUI loads card and board artwork from `src/main/resources/assets`. When
assets are missing, placeholders are used and the GUI remains usable, but the
TUI is recommended for examiners who do not have access to the artwork.

## Network protocols

Both transports are exposed by the same `ServerLauncher` and share the same
`LobbyManager`, controller and game state.

### Socket

The client opens a TCP socket toward the server. Messages are exchanged as
serialized Java objects (`ClientMessage` / `ServerMessage` hierarchies under
`it.polimi.ingsw.network.protocol`). Each socket connection is handled by a
dedicated server-side reader that forwards parsed messages to a shared
`MessageDispatcher`.

### RMI

The RMI client retrieves a remote `RemoteServer` from the registry and
invokes its methods directly. The client registers a remote callback that
the server uses to push game events, so both directions of the conversation
remain asynchronous from the user's point of view.

## Game rules summary

- The game supports 2–5 players. Each player controls a tribe and is identified by a nickname and a totem color.
- A match lasts 10 rounds and is divided into three eras (I, II and III).
- Each round has two main phases:
  1. totem placement;
  2. action resolution.
- During totem placement, players follow the order shown on the turn order tile and place their totem on a free offer tile.
- During action resolution, offer tiles are resolved from left to right on the offer track, not according to the turn order tile.
- After resolving the action, the player returns the totem to the turn order tile. Depending on the slot reached, the player may receive a food bonus or, on the last slot, pay 1 food if possible; otherwise lose 2 Prestige Points.
- Most offer tiles allow the player to take Character and/or Building cards from the upper row and/or lower row, as specified by the tile. Event cards can never be taken.
- Character cards are added to the player's tribe for free.
- Building cards can be taken only if the player can pay their food cost, after applying any Builder discounts. The final cost can never be lower than zero.
- Some Building effects trigger during the game, during Events, when the totem returns to the turn order tile, or at final scoring.
- At the end of each round, visible Event cards in the lower row are resolved. Sustenance is always resolved last.
- At the end of the final round, all visible Events are resolved, including Events in the upper row. Final scoring is then computed by adding Prestige Points from Builders, Inventors, Artists and Buildings.
- The player with the highest Prestige Points wins. Ties are broken by remaining food; if players are still tied, the victory is shared.

The full rules (in Italian) are available in
`Mesos Regolamento (ITA)-3.pdf` at the repository root.

## Additional implemented features

### Implemented requirements

- Complete game rules.
- TUI client.
- JavaFX GUI client.
- Socket communication.
- RMI communication.
- Single server that handles Socket and RMI clients in the same lobby.

### Optional advanced features

- Player disconnection and reconnection, including reconnection during the
  player's own turn.
- Game suspension when only one player remains connected, with a timer that
  awards an automatic victory if no one reconnects in time.
- Persistence of the match: state is saved on disk and restored on server
  restart (unless `--fresh` is used).
- Optional persistent leaderboard via JDBC, enabled by dropping a
  `db.properties` file next to the server JAR.

## Testing

Unit and integration tests cover the controller, game states, model rules,
network protocol, persistence and reconnection. The full test suite lives
under `src/test/java`.

Run the full test suite:

```bash
mvn test
```

Run a single test class:

```bash
mvn test -Dtest=GameControllerTotemPlacementStrongTest
```

## Project structure

```
src/main/java/it/polimi/ingsw/
├── App.java                          — placeholder entry point
├── model/                            — game model
│   ├── Game, Player, Round, Tribe, Totem
│   ├── card/                         — character, building, event cards and effects
│   ├── board/                        — board, offer tile, turn order tile, turn slots
│   └── enums/                        — eras, colors, phases, …
├── controller/
│   ├── GameController                — orchestrates matches
│   ├── state/                        — game-state classes (Lobby, ReadyCheck,
│   │                                   TotemPlacement, ActionResolution,
│   │                                   EventResolution, EndRound, Suspended,
│   │                                   EndGame, …)
│   ├── action/                       — server-bound actions (commands)
│   ├── listener/                     — observer notifications toward views
│   ├── leaderboard/                  — optional JDBC leaderboard service
│   ├── Persistence, GamePersistenceService
│   ├── DisconnectionManager, SuspensionManager, TurnRecoveryService
├── network/
│   ├── server/                       — ServerLauncher, LobbyManager,
│   │                                   MessageDispatcher, SocketAcceptLoop
│   ├── client/                       — ClientLauncher, ClientRuntime,
│   │                                   ClientController, ClientModel,
│   │                                   ClientViewSelector
│   ├── protocol/                     — clientbound and serverbound DTOs
│   ├── transport/                    — Socket transport
│   └── rmi/                          — RMI server and client adapters
└── view/
    ├── View (common interface)
    ├── tui/                          — Tui, Renderer, CommandParser, CardCatalog
    └── gui/                          — JavaFX entry point, components, utilities

src/main/resources/
├── characters.json, buildings.json,
│   events.json, offer_tiles.json     — card definitions
├── assets/                           — GUI artwork (not committed)
└── gui/                              — JavaFX stylesheet
```

## Troubleshooting

- **`Address already in use`** on server startup: another process is bound
  to port `1234` (Socket) or `1099` (RMI registry). Free the ports or start
  the server with custom ones:
  `java -jar deliverables/final/jar/mesos-server.jar 2345 1100 MesosServer`.
- **RMI client cannot connect** even though the server is running: make sure
  the RMI registry port (default `1099`) matches on both sides and that the
  binding name (default `MesosServer`) is identical.
- **GUI starts without images / placeholders only**: the assets folder is
  missing. Copy the artwork under `src/main/resources/assets` (see the
  README inside that folder for the expected layout).
- **The server starts the previous match instead of a new one**: a saved
  game was found on disk. Restart the server with `--fresh` to discard it.
- **Leaderboard disabled at startup**: the server logs
  `[Leaderboard] …` and continues without it when `db.properties` is missing
  or incomplete. This is expected — the game runs without a database.

## Known limitations / assumptions

- The optional leaderboard reads JDBC credentials from `db.properties`. The
  provided `db.properties.example` and the `mysql-connector-j` dependency in
  `pom.xml` target **MySQL**, while the SQL dump in
  `deliverables/final/dump/schema.sql` uses PostgreSQL-specific types
  (`BIGSERIAL`, `UUID`). Adapt the schema to the database engine you point
  the server at before enabling the feature.
- Graphical assets are not redistributable and are not included in the
  repository.
- The `App.java` class at the root of the `it.polimi.ingsw` package is a
  placeholder; the actual entry points are `ServerLauncher` and
  `ClientLauncher` under `it.polimi.ingsw.network`.

## Authors

- Benedetto Oliva
- Giorgio Ortona
- Alessandro Mazzeo
- Camillo Nicoletti
