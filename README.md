# Mesos

Progetto di Ingegneria del Software - Politecnico di Milano, A.A. 2025/2026.

## Gruppo

Benedetto Oliva, Giorgio Ortona, Alessandro Mazzeo, Camillo Nicoletti.

## Funzionalita

Regole complete, TUI, GUI JavaFX, Socket, RMI, server unico Socket+RMI, disconnessione/riconnessione, sospensione partita, persistenza su disco e leaderboard JDBC opzionale.

## Requisiti

- Java 23 o superiore.
- Maven 3.9 o superiore solo se si vuole ricompilare il progetto.

## Build dei JAR

Dal root del progetto:

```bash
mvn clean package
```

I JAR eseguibili vengono generati in:

```text
deliverables/final/jar/
```

File attesi:

```text
deliverables/final/jar/mesos-server.jar
deliverables/final/jar/mesos-client.jar
```

## Esecuzione server

Avvio con valori di default:

```bash
java -jar deliverables/final/jar/mesos-server.jar
```

Default:

```text
Socket port: 1234
RMI registry port: 1099
RMI binding name: MesosServer
```

Avvio ignorando un eventuale salvataggio precedente:

```bash
java -jar deliverables/final/jar/mesos-server.jar --fresh
```

Avvio con parametri personalizzati:

```bash
java -jar deliverables/final/jar/mesos-server.jar [--fresh] [socketPort] [rmiPort] [rmiBindingName]
```

Esempio:

```bash
java -jar deliverables/final/jar/mesos-server.jar --fresh 1234 1099 MesosServer
```

## Esecuzione client

Client Socket:

```bash
java -jar deliverables/final/jar/mesos-client.jar socket <host> <socketPort>
```

Esempio:

```bash
java -jar deliverables/final/jar/mesos-client.jar socket localhost 1234
```

Client RMI:

```bash
java -jar deliverables/final/jar/mesos-client.jar rmi <host> [registryPort] [bindingName]
```

Esempio:

```bash
java -jar deliverables/final/jar/mesos-client.jar rmi localhost 1099 MesosServer
```

Dopo la connessione, il client chiede quale interfaccia usare:

```text
1 - TUI
2 - GUI
```

## Leaderboard opzionale

Per abilitare la leaderboard JDBC, copiare un file `db.properties` nella directory da cui viene avviato il server. Un esempio si trova in:

```text
deliverables/final/dump/db.properties.example
```

Se `db.properties` non e presente o il database non e raggiungibile, il gioco resta eseguibile e la leaderboard viene disabilitata.

## Test

Per eseguire la suite:

```bash
mvn test
```

Per generare anche il report JaCoCo:

```bash
mvn clean verify
```
