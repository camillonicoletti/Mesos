<h1 align="center">MESOS</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Java-23-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 23">
  <img src="https://img.shields.io/badge/JavaFX-GUI-1E88E5?style=flat-square" alt="JavaFX">
  <img src="https://img.shields.io/badge/players-2--5-8E44AD?style=flat-square" alt="2-5 players">
  <img src="https://img.shields.io/badge/multiplayer-Socket%20%7C%20RMI-2E7D32?style=flat-square" alt="Socket and RMI multiplayer">
  <img src="https://img.shields.io/badge/grade-30L-C62828?style=flat-square" alt="Grade 30 cum laude">
</p>

<p align="center">
  <b>Software Engineering Project</b> · Politecnico di Milano · A.Y. 2025/2026 · <b>Final grade: 30 cum laude</b>
</p>

<p align="center">
  <a href="#quick-start"><img src="https://img.shields.io/badge/%E2%96%B6%20Play-Quick%20start-E4572E?style=for-the-badge" alt="Quick start"></a>
  <a href="docs/Mesos-Rulebook-ITA.pdf"><img src="https://img.shields.io/badge/%F0%9F%93%96%20Rules-Rulebook-2B2D42?style=for-the-badge" alt="Rulebook"></a>
</p>

<p align="center">
  <img src="docs/images/gameplay.gif" alt="Mesos gameplay" width="900">
</p>

## Quick start

Requires **Java 23** or later.

```bash
git clone https://github.com/camillonicoletti/Mesos.git
cd Mesos
java -jar deliverables/final/jar/mesos-server.jar --fresh
```

`--fresh` starts the server with a new match. Without it, the server resumes the last saved match.

Then each player opens a new terminal in the same folder and runs:

```bash
java -jar deliverables/final/jar/mesos-client.jar socket localhost 1234
```

## Play your way

After connecting, each player chooses how to play: type `1` for the **text interface** or `2` for the **graphical interface**. Players using different interfaces can join the same match.

<table>
  <tr>
    <th width="50%">🖥️ Graphical interface (GUI)</th>
    <th width="50%">⌨️ Text interface (TUI)</th>
  </tr>
  <tr>
    <td><img src="docs/images/gui.jpg" alt="Mesos played with the graphical interface"></td>
    <td><img src="docs/images/tui.png" alt="Mesos played from the terminal"></td>
  </tr>
</table>

---

## About

This is a digital version of **Mesos**, the card game by *Yaniv Kahana* and *Simone Luciani* published by **Cranio Creations**.

We followed the **official board game** closely. The rules, the cards, the tiles and the scoring are the ones in the published edition. All 117 cards (characters, events and buildings) have their original values and effects, and the server checks every move, so the game can only be played by the rules.

- **2 to 5 players** over the network
- **Graphical interface** with the original card and board artwork
- **Text interface** for playing from a terminal
- Players using different interfaces can play in the same match
- If your connection drops, you can rejoin the match
- An interrupted match is saved and resumes where it stopped

## Rules

The game follows the official rulebook:

📖 **[Read the official Mesos rulebook (PDF, Italian)](docs/Mesos-Rulebook-ITA.pdf)**

---

## How to run

Requires **Java 23** or later.

**Start the server:**

```bash
java -jar deliverables/final/jar/mesos-server.jar
```

**Connect each player** (use the server's IP address instead of `localhost` when playing across computers):

```bash
java -jar deliverables/final/jar/mesos-client.jar socket localhost 1234
```

or over RMI:

```bash
java -jar deliverables/final/jar/mesos-client.jar rmi localhost 1099 MesosServer
```

After connecting, type `1` for the text interface or `2` for the graphical one.

**Start a new match** without resuming the saved one:

```bash
java -jar deliverables/final/jar/mesos-server.jar --fresh
```

**Rebuild the JAR files** (requires Maven 3.9 or later):

```bash
mvn clean package
```

---

<p align="center">
  <sub>Made by <b>Camillo Nicoletti</b>, <b>Benedetto Oliva</b>, <b>Giorgio Ortona</b> and <b>Alessandro Mazzeo</b></sub>
</p>
