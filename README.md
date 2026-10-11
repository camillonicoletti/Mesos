<h1 align="center">MESOS</h1>

<p align="center">
  <i>Guida la tua tribù preistorica attraverso tre ere e rendila la più prestigiosa di tutte.</i>
</p>

<p align="center">
  <img src="docs/images/copertina.png" alt="Schermata iniziale di Mesos" width="800">
</p>

<p align="center">
  <b>Progetto di Ingegneria del Software</b> · Politecnico di Milano · A.A. 2025/2026 · <b>Voto finale: 30L</b>
</p>

---

## Il progetto

Abbiamo ricreato in versione digitale **Mesos**, il gioco di carte di *Yaniv Kahana* e *Simone Luciani* pubblicato da **Cranio Creations**.

L'obiettivo era essere fedeli al gioco da tavolo: le regole, le carte, le tessere e i punteggi sono quelli della **versione ufficiale**. Tutte le 117 carte del gioco (personaggi, eventi ed edifici) sono state riprodotte con i loro valori ed effetti originali, e il server controlla ogni mossa, così non si può giocare in modo scorretto.

Si gioca **in rete da 2 a 5 giocatori**. Ognuno può scegliere tra:

- **un'interfaccia grafica** con la grafica originale delle carte e del tabellone;
- **un'interfaccia testuale** da terminale, per giocare anche senza grafica.

Nella stessa partita possono stare giocatori con interfacce diverse. Se cade la connessione si può rientrare nella partita, e una partita interrotta viene salvata e ripresa da dove si era rimasti.

### Il gruppo

| | |
|---|---|
| **Benedetto Oliva** | Regole e meccaniche di gioco, carte ed effetti |
| **Giorgio Ortona** | Rete e connessione tra i giocatori |
| **Alessandro Mazzeo** | Gestione della partita, salvataggi e classifica |
| **Camillo Nicoletti** | Interfaccia grafica e testuale |

---

## Uno sguardo al gioco

<p align="center">
  <img src="docs/images/lobby.png" alt="Lobby: scelta del colore e attesa dei giocatori" width="800">
  <br><sub>La lobby: ogni giocatore sceglie il colore del proprio totem e si dichiara pronto.</sub>
</p>

<p align="center">
  <img src="docs/images/tabellone.png" alt="Tabellone durante una partita" width="800">
  <br><sub>Il tabellone: le due file di carte, le tessere offerta e la tessera dell'ordine di turno.</sub>
</p>

<p align="center">
  <img src="docs/images/tribu.png" alt="La tribù di un giocatore" width="800">
  <br><sub>La tribù di un giocatore con i suoi personaggi ed edifici.</sub>
</p>

<p align="center">
  <img src="docs/images/tui.png" alt="La stessa partita giocata da terminale" width="800">
  <br><sub>La stessa partita giocata dall'interfaccia testuale.</sub>
</p>

<p align="center">
  <img src="docs/images/fine-partita.png" alt="Classifica finale" width="800">
  <br><sub>La classifica di fine partita.</sub>
</p>

---

## Regolamento

### Obiettivo

Ogni giocatore guida una tribù del Mesolitico. Nel corso di **10 round**, divisi in **tre ere**, si reclutano personaggi e si costruiscono edifici, si affrontano gli eventi e si accumula cibo. Alla fine vince chi ha più **Punti Prestigio (PP)**.

### Materiale

- **Tessere offerta** (A–G): ogni round ognuno ci mette sopra il proprio totem. Indicano quante carte si prendono e da quale fila.
- **Tessera ordine di turno**: stabilisce chi sceglie per primo nel round successivo, con un bonus o una penalità di cibo.
- **Mazzo tributo**: contiene i personaggi e gli eventi delle tre ere, mescolati per era (prima l'era I, poi la II, poi la III). In fondo ci sono gli eventi di chiusura dell'era III. Si usano solo le carte adatte al numero di giocatori.
- **Edifici**: un mazzo separato per ciascuna era.
- **Cibo e Punti Prestigio** di ogni giocatore.

### Preparazione

1. Si scopre la **fila inferiore** con *numero di giocatori + 1* carte del mazzo tributo e poi la **fila superiore** con *numero di giocatori + 4* carte. Gli eventi rimangono in fila insieme ai personaggi.
2. Si mettono in fila superiore gli **edifici dell'era I**.
3. L'ordine di turno iniziale si decide a caso. Chi parte più tardi riceve più cibo:

| Posizione | 1° | 2° | 3° | 4° | 5° |
|---|:-:|:-:|:-:|:-:|:-:|
| Cibo iniziale | 2 | 3 | 3 | 4 | 4 |

### Svolgimento di un round

#### 1. Piazzamento dei totem

Seguendo l'ordine di turno, ogni giocatore sposta il proprio totem su una **tessera offerta libera**. Il numero di tessere in gioco dipende da quanti giocatori ci sono:

| Tessera | Giocatori | Effetto |
|:-:|:-:|---|
| **A** | 5 | Guadagni **3 cibo** |
| **B** | 2+ | Prendi **1 carta** dalla fila inferiore |
| **C** | 2+ | Prendi **1 carta** dalla fila superiore |
| **D** | 3+ | Prendi **2 carte** dalla fila inferiore |
| **E** | 2+ | Prendi **1 carta** dalla fila inferiore e **1** dalla superiore |
| **F** | 2+ | Prendi **2 carte** dalla fila superiore |
| **G** | 4+ | Prendi **1 carta** dalla fila inferiore e **2** dalla superiore |

C'è un compromesso: le tessere con la lettera più bassa danno meno carte, però fanno agire prima e fanno partire per primi anche nel round dopo.

#### 2. Risoluzione delle azioni

Le tessere si risolvono **in ordine alfabetico**. Quando tocca a lui, il giocatore:

1. esegue l'effetto della tessera, cioè prende il cibo oppure sceglie le carte dalle file indicate. Si possono prendere sia **personaggi** sia **edifici**;
2. sposta il totem nel **primo spazio libero della tessera ordine di turno** e riceve l'effetto di quello spazio.

| Giocatori | 1° posto | 2° posto | 3° posto | 4° posto | 5° posto |
|:-:|:-:|:-:|:-:|:-:|:-:|
| 2 | +1 cibo | paga 1 cibo* | | | |
| 3 | +2 cibo | — | paga 1 cibo* | | |
| 4 | +2 cibo | +1 cibo | — | paga 1 cibo* | |
| 5 | +3 cibo | +1 cibo | — | — | paga 1 cibo* |

<sub>* Chi non ha il cibo per pagare perde invece **2 PP**.</sub>

**Gli edifici si pagano in cibo.** Il costo è scritto sulla carta e i Costruttori della tribù lo riducono.

#### 3. Eventi

Gli **eventi presenti nella fila inferiore** si risolvono e valgono per tutti i giocatori. Il **Sostentamento si risolve sempre per ultimo**. Nell'ultimo round si risolvono anche gli eventi della fila superiore.

#### 4. Fine del round

1. Le carte rimaste nella fila inferiore vengono scartate.
2. La fila superiore scende e diventa la nuova fila inferiore.
3. La fila superiore si riempie di nuovo dal mazzo tributo.
4. **Cambio d'era**: se nella nuova fila superiore compare una carta dell'era successiva, comincia la nuova era. Gli edifici della fila superiore scendono in quella inferiore e si scoprono gli edifici della nuova era. Nel passaggio dall'era II all'era III gli edifici rimasti in fila inferiore vengono scartati.

### I personaggi

| Personaggio | Cosa fa |
|---|---|
| **Cacciatore** | Durante la **Caccia** fa ottenere cibo e PP. Quelli con l'icona del cibo, appena presi, danno 1 cibo per ogni Cacciatore della tribù, compreso quello nuovo. |
| **Raccoglitore** | Durante il **Sostentamento** sfama **3 personaggi** della tribù. |
| **Sciamano** | Ha da 1 a 3 **stelle**, che contano nel **Rituale Sciamanico**. |
| **Artista** | Serve per le **Pitture Rupestri**. A fine partita ogni **coppia** di Artisti vale **10 PP**. |
| **Inventore** | Ha un'icona invenzione. A fine partita ogni Inventore vale tanti PP quante sono le **icone diverse** possedute. |
| **Costruttore** | Fa **pagare meno cibo** gli edifici (1 o 2 cibo in meno) e dà PP a fine partita. |

### Gli eventi

| Evento | Era I | Era II | Era III |
|---|:-:|:-:|:-:|
| **Caccia**: per ogni Cacciatore ricevi 1 cibo e… | +1 PP | +2 PP | +3 PP |
| **Sostentamento**: ogni personaggio mangia 1 cibo. Per ogni cibo che manca perdi… | −1 PP | −2 PP | −3 PP |
| **Rituale Sciamanico**: chi ha più stelle guadagna, chi ne ha meno perde | +5 / −3 | +10 / −5 | +15 / −7 |
| **Pitture Rupestri**: se hai almeno *N* Artisti guadagni PP per ognuno, altrimenti perdi 2 PP | N=1, +1 a testa | N=2, +2 a testa | N=3, +3 a testa |

### Gli edifici

Ogni edificio costa cibo, vale dei PP a fine partita e ha un **effetto speciale** che resta attivo fino alla fine.

<details>
<summary><b>Elenco completo degli edifici</b></summary>

| Era | Costo | PP | Effetto |
|:-:|:-:|:-:|---|
| I | 4 | 3 | +5 cibo per ogni nuovo set completo dei 6 tipi di personaggio |
| I | 4 | 4 | Ogni Raccoglitore sfama 1 personaggio in più al Sostentamento |
| I | 5 | 3 | Ogni Artista sfama 1 personaggio al Sostentamento |
| I | 5 | 2 | Immunità alla penalità del Rituale Sciamanico |
| I | 3 | 3 | +1 cibo quando il totem finisce su uno spazio che dà cibo |
| I | 3 | 4 | +3 cibo per ogni nuova coppia di Inventori con la stessa icona |
| II | 7 | 0 | Raddoppia i PP vinti nel Rituale Sciamanico |
| II | 6 | 4 | +3 stelle nel Rituale Sciamanico |
| II | 7 | 4 | Ogni Inventore sfama 1 personaggio al Sostentamento |
| II | 7 | 2 | Nella Caccia, +1 cibo e +1 PP in più per ogni Cacciatore |
| II | 6 | 4 | A fine partita i PP dei Costruttori si contano due volte |
| II | 5 | 6 | Nelle Pitture Rupestri, +1 cibo per ogni Artista |
| II | 5 | 0 | A fine partita, 6 PP per ogni set completo dei 6 personaggi |
| III | 8 | 8 | A fine partita, 3 PP per ogni Cacciatore |
| III | 7 | 6 | A fine partita, 4 PP per ogni Raccoglitore |
| III | 7 | 6 | A fine partita, 4 PP per ogni Sciamano |
| III | 6 | 3 | A fine partita, 4 PP per ogni Costruttore |
| III | 7 | 6 | A fine partita, 4 PP per ogni Artista |
| III | 6 | 6 | A fine partita, 2 PP per ogni Inventore |
| III | 9 | 3 | Ogni round puoi prendere 1 carta in più dalla fila superiore |
| III | 10 | 0 | A fine partita, +25 PP |

</details>

### Fine della partita e punteggio

La partita finisce dopo il **10° round**. Al punteggio di ciascuno si sommano:

- i **PP** guadagnati durante la partita;
- **Inventori**: numero di Inventori × numero di icone diverse;
- **Artisti**: 10 PP per ogni coppia;
- **Costruttori**: i PP scritti su ogni carta;
- **Edifici**: i PP scritti su ogni carta, più quelli dati dai loro effetti di fine partita.

Vince chi ha **più PP**. In caso di parità vince chi ha **più cibo**, e se sono pari anche lì la vittoria è condivisa.

---

## Come giocare

Serve **Java 23** o superiore. Uno dei giocatori avvia il server:

```bash
java -jar deliverables/final/jar/mesos-server.jar
```

Gli altri si collegano indicando l'indirizzo del server (`localhost` se si gioca sullo stesso computer):

```bash
java -jar deliverables/final/jar/mesos-client.jar socket localhost 1234
```

Dopo la connessione si sceglie l'interfaccia: `1` per quella testuale, `2` per quella grafica. Si entra nella lobby, si sceglie il colore del totem e la partita comincia quando tutti sono pronti.

> Le immagini originali del gioco non sono incluse nel repository perché sono protette da copyright. Per vedere la grafica bisogna copiarle in `src/main/resources/assets/`.

<details>
<summary>Opzioni avanzate</summary>

- Avviare il server senza riprendere la partita salvata: `java -jar deliverables/final/jar/mesos-server.jar --fresh`
- Collegarsi tramite RMI: `java -jar deliverables/final/jar/mesos-client.jar rmi <host> [porta] [nome]`
- Ricompilare il progetto: `mvn clean package` (serve Maven 3.9 o superiore)
- Classifica dei giocatori (facoltativa): mettere un file `db.properties` nella cartella da cui si avvia il server. C'è un esempio in `deliverables/final/dump/`.

</details>

---

<p align="center">
  <sub><i>Mesos</i> è un gioco di Yaniv Kahana e Simone Luciani pubblicato da Cranio Creations. Questo progetto è stato realizzato solo a scopo didattico.</sub>
</p>
