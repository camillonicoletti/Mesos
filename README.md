# Mesos

Progetto di Ingegneria del Software - Politecnico di Milano, A.A. 2025/2026. Voto Finale: 30L

## Gruppo

Benedetto Oliva, Giorgio Ortona, Alessandro Mazzeo, Camillo Nicoletti.

## Funzionalità

Regole complete, TUI, GUI JavaFX, Socket, RMI, server unico Socket+RMI, disconnessione/riconnessione, sospensione partita, persistenza su disco e leaderboard JDBC opzionale.

## Requisiti

- Java 23 o superiore.
- Maven 3.9 o superiore solo se si vuole ricompilare il progetto.

## Librerie utilizzate

- **JavaFX**: interfaccia grafica.
- **ControlsFX**: componenti GUI aggiuntivi.
- **AtlantaFX**: tema/stile grafico.
- **Ikonli**: icone per la GUI.
- **Gson**: lettura dati JSON.
- **MySQL Connector/J**: leaderboard JDBC opzionale.
- **JUnit Jupiter**: test automatici.
- **JaCoCo**: report di copertura.
- **Maven Shade Plugin**: creazione JAR eseguibili.

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
java -jar deliverables/final/jar/mesos-server.jar [--fresh] [socketPort] [rmiPort] [rmiBindingName] [rmiHost]
```

Esempio:

```bash
java -jar deliverables/final/jar/mesos-server.jar --fresh 1234 1099 MesosServer
```

Se i client RMI si collegano da un altro computer, indicare anche l'indirizzo IP del server da pubblicare negli stub RMI:

```bash
java -jar deliverables/final/jar/mesos-server.jar --fresh 1234 1099 MesosServer 172.20.10.2
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
java -jar deliverables/final/jar/mesos-client.jar rmi <host> [registryPort] [bindingName] [clientRmiHost]
```

Esempio:

```bash
java -jar deliverables/final/jar/mesos-client.jar rmi localhost 1099 MesosServer
```

Da un altro computer, usare l'IP del server come `<host>`; se anche il client deve ricevere callback RMI su rete, indicare come ultimo argomento l'IP del client.

Dopo la connessione, il client chiede quale interfaccia usare:

```text
1 - TUI
2 - GUI
```

## Risorse grafiche

Le risorse grafiche non sono incluse nei JAR e devono essere presenti localmente nella cartella:

```text
src/main/resources/assets/
```

## Leaderboard opzionale

Per abilitare la leaderboard JDBC, copiare un file `db.properties` nella directory da cui viene avviato il server. Un esempio si trova in:

```text
deliverables/final/dump/db.properties.example
```

Per preparare il database locale MySQL:

```bash
mysql -u root -p < deliverables/final/dump/setup.sql
cp deliverables/final/dump/db.properties.example db.properties
```

Il file `setup.sql` crea il database `mesos`, l'utente `mesos` e assegna i permessi necessari. Il server crea automaticamente la tabella della leaderboard al primo avvio.

Il server deve essere avviato dalla directory che contiene `db.properties`, ad esempio dal root del progetto:

```bash
java -jar deliverables/final/jar/mesos-server.jar --fresh
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
