<h1 align="center">MESOS</h1>

<!-- GAMEPLAY VIDEO: the video goes here, in place of the cover image. -->

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
  <a href="https://github.com/camillonicoletti/ing-sw-2026-Ortona-Mazzeo-Oliva-Nicoletti/releases/latest"><img src="https://img.shields.io/badge/Download-Play%20Mesos-E4572E?style=for-the-badge&logo=github&logoColor=white" alt="Download Mesos"></a>
</p>

## Quick start

1. Install **Java 23** or later and download `mesos-server.jar` and `mesos-client.jar` from the [latest release](https://github.com/camillonicoletti/ing-sw-2026-Ortona-Mazzeo-Oliva-Nicoletti/releases/latest).
2. Start the server: `java -jar mesos-server.jar`
3. Each player starts the client: `java -jar mesos-client.jar socket localhost 1234`

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
  <sub><i>Mesos</i> is a game by Yaniv Kahana and Simone Luciani, published by Cranio Creations. This project was made for educational purposes only.</sub>
</p>
