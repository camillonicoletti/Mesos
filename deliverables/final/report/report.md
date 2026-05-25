# Mesos - Report finale

## Componenti del gruppo

- Giorgio Ortona
- Mazzeo
- Oliva
- Nicoletti

## Funzionalita implementate

- Regole complete del gioco Mesos, con gestione delle ere, delle tessere offerta, delle tessere ordine turno, degli eventi e del punteggio finale.
- Interfaccia TUI.
- Interfaccia GUI JavaFX.
- Comunicazione Socket.
- Comunicazione RMI.
- Server unico capace di accettare client Socket e RMI nella stessa partita.
- Gestione di disconnessione e riconnessione dei giocatori.
- Sospensione della partita quando rimane un solo giocatore connesso, con timer e vittoria automatica allo scadere.
- Salvataggio e ripristino dello stato della partita.
- Classifica persistente opzionale tramite PostgreSQL.
- Modalita debug per velocizzare alcune prove manuali.

## Divisione indicativa del lavoro

- Giorgio Ortona: Socket, RMI, riconnessioni/disconnessioni, GUI.
- Altri componenti: modello, controller, regole di gioco, test, persistenza, supporto TUI e integrazione.

## Scelte implementative principali

- Pattern State per distinguere le fasi della partita: lobby, ready check, piazzamento totem, risoluzione azioni, eventi, fine round, sospensione e fine partita.
- Pattern Observer tramite `GameListener` e viste virtuali, cosi il controller notifica gli eventi senza dipendere direttamente da TUI o GUI.
- DTO di rete separati tra messaggi client-bound e server-bound.
- Pattern Command per rappresentare le azioni inviate dal client al controller.
- Visitor sulle carte e sugli effetti, usato per evitare controlli sparsi con `instanceof`.
- Snapshot di sincronizzazione per riallineare i client, utile soprattutto dopo reconnect o dopo eventi importanti.
- Separazione tra trasporto Socket/RMI e logica applicativa tramite interfacce comuni di connessione.

## Protocolli di comunicazione

### Socket

Il client apre una socket TCP verso il server. I messaggi vengono scambiati tramite oggetti serializzati. Ogni client e' gestito lato server da una connessione dedicata, che inoltra i messaggi al `LobbyManager`.

### RMI

Nel caso RMI il client non invia pacchetti manuali al server: invoca metodi remoti esposti dal server. Il server usa a sua volta una callback remota registrata dal client per notificare gli eventi di gioco.

## Database

La classifica usa PostgreSQL quando e' presente `db.properties`. Se il file non esiste o la connessione non e' disponibile, il gioco rimane avviabile e la classifica viene disabilitata.

Lo schema SQL si trova in:

`deliverables/final/dump/schema.sql`

## Test

Sono presenti test unitari e di integrazione su controller, stati di gioco, disconnessioni, sincronizzazione client, carte, eventi e casi limite. La percentuale di copertura definitiva va inserita dopo l'ultima esecuzione dello strumento scelto per la coverage.
