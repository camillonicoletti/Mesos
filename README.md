# Mesos

Progetto di Ingegneria del Software 2026 - Politecnico di Milano.

## Componenti del gruppo

- Giorgio Ortona
- Mazzeo
- Oliva
- Nicoletti

## Funzionalita implementate

- Regole complete del gioco.
- Client TUI.
- Client GUI JavaFX.
- Comunicazione Socket.
- Comunicazione RMI.
- Server unico Socket + RMI.
- Riconnessione dei giocatori dopo disconnessione o crash del client.
- Sospensione della partita quando rimane un solo giocatore connesso.
- Salvataggio e ripristino della partita.
- Classifica persistente opzionale su PostgreSQL.

## Requisiti

- Java 23 o superiore.
- Maven 3.9 o superiore.

Le risorse grafiche del gioco non sono incluse nella repository. Per usare la GUI con le immagini complete, copiare localmente la cartella degli asset in:

```text
src/main/resources/assets
```

La TUI funziona anche senza risorse grafiche.

## Creazione dei JAR

Dalla root del progetto Maven:

```bash
mvn clean package -DskipTests
```

I JAR eseguibili vengono generati in:

```text
deliverables/final/jar/
```

File prodotti:

- `mesos-server.jar`
- `mesos-client.jar`

## Avvio del server

Il server finale avvia sia Socket sia RMI sulla stessa lobby.

```bash
java -jar deliverables/final/jar/mesos-server.jar
```

Porte di default:

- Socket: `1234`
- RMI registry: `1099`
- Nome binding RMI: `MesosServer`

Per partire senza caricare un eventuale salvataggio precedente:

```bash
java -jar deliverables/final/jar/mesos-server.jar --fresh
```

Per specificare porte e binding:

```bash
java -jar deliverables/final/jar/mesos-server.jar [--fresh] [socketPort] [rmiPort] [rmiBindingName]
```

## Avvio del client Socket

```bash
java -jar deliverables/final/jar/mesos-client.jar socket localhost 1234
```

## Avvio del client RMI

```bash
java -jar deliverables/final/jar/mesos-client.jar rmi localhost
```

Oppure, specificando porta registry e binding:

```bash
java -jar deliverables/final/jar/mesos-client.jar rmi localhost 1099 MesosServer
```

All'avvio del client viene chiesto se usare TUI o GUI.

## Classifica PostgreSQL opzionale

La classifica si abilita solo se nella directory di avvio e' presente `db.properties`.
Un esempio di configurazione si trova in:

```text
db.properties.example
```

Schema del database:

```text
deliverables/final/dump/schema.sql
```

Se il database non e' configurato, il server stampa un avviso e continua senza classifica.
