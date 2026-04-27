package it.polimi.ingsw.network.client.tui;

import it.polimi.ingsw.network.client.ClientModel;
import it.polimi.ingsw.network.client.PlayerView;

import java.util.List;
import java.util.Map;

public class Renderer {

    private static final String LINE = "─".repeat(60);

    public Renderer(ClientModel model) {

    }



    public void render(ClientModel model) {
        String state = model.getGameState();

        System.out.println("\n" + LINE);

        if (state == null) {
            renderLogin(model);
        } else switch (state) {
            case "LobbyState"            -> renderLobby(model);
            case "ReadyCheckState"       -> renderReadyCheck(model);
            case "TotemPlacementState"   -> renderTotemPlacement(model);
            case "ActionResolutionState" -> renderActionResolution(model);
            case "EventResolutionState"  -> renderEventResolution(model);
            case "EndRoundState"         -> renderEndRound(model);
            case "SuspendedState"        -> renderSuspended(model);
            case "EndGameState"          -> renderEndGame(model);
            default                      -> renderGame(model);
        }

        if (model.getLastError() != null) {
            System.out.println("\n[!] " + model.getLastError());
        }

        System.out.println(LINE);
        System.out.print("> ");
    }

    // -------------------------------------------------------

    private void renderLogin(ClientModel model) {
        System.out.println("  MESOS — Inserisci nickname e colore totem per entrare.");
    }

    // -------------------------------------------------------

    private void renderLobby(ClientModel model) {
        System.out.printf("  LOBBY  %d / %d giocatori%n",
                model.getLobbyPlayers().size(), model.getExpectedPlayers());
        System.out.println();
        for (String p : model.getLobbyPlayers()) {
            System.out.println("  · " + p);
        }
        System.out.println();
        if (model.getExpectedPlayers() == 0) {
            System.out.println("  Sei il primo — digita: players <2-5>");
        } else {
            System.out.println("  Digita 'ready' quando sei pronto.");
        }
    }

    // -------------------------------------------------------

    private void renderReadyCheck(ClientModel model) {
        System.out.printf("  PRONTO?  %d / %d giocatori connessi%n",
                model.getLobbyPlayers().size(), model.getExpectedPlayers());
        System.out.println();
        for (String p : model.getLobbyPlayers()) {
            System.out.println("  · " + p);
        }
        System.out.println();
        System.out.println("  Digita 'ready' per confermare.");
    }

    // -------------------------------------------------------

    private void renderTotemPlacement(ClientModel model) {
        printHeader("POSIZIONAMENTO TOTEM", model);
        printBoard(model);
        System.out.println();
        printPlayers(model);
        System.out.println();
        System.out.println("  Digita: place <lettera>   es. place A");
    }

    // -------------------------------------------------------

    private void renderActionResolution(ClientModel model) {
        printHeader("RISOLUZIONE AZIONI", model);
        printBoard(model);
        System.out.println();
        printCardRows(model);
        System.out.println();
        printPlayers(model);
        System.out.println();
        System.out.println("  Digita: pick <id> upper|lower   oppure   build <id> upper|lower");
    }

    // -------------------------------------------------------

    private void renderEventResolution(ClientModel model) {
        printHeader("RISOLUZIONE EVENTI", model);
        System.out.println("  Il server sta risolvendo gli eventi...");
        System.out.println();
        printPlayers(model);
    }

    // -------------------------------------------------------

    private void renderEndRound(ClientModel model) {
        printHeader("FINE ROUND " + model.getRoundNumber(), model);
        System.out.println("  Fine del round. Preparazione del prossimo...");
        System.out.println();
        printPlayers(model);
    }

    // -------------------------------------------------------

    private void renderSuspended(ClientModel model) {
        System.out.println("  *** PARTITA IN PAUSA ***");
        System.out.println("  In attesa che tutti i giocatori si riconnettano...");
    }

    // -------------------------------------------------------

    private void renderEndGame(ClientModel model) {
        System.out.println("  *** FINE PARTITA ***");
        System.out.println();
        System.out.println("  Vincitore: " + model.getWinnerNickname());
        System.out.println();
        System.out.println("  Classifica finale:");
        model.getFinalScores().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("    %-15s %3d PP%n", e.getKey(), e.getValue()));
    }

    // -------------------------------------------------------

    private void renderGame(ClientModel model) {
        printHeader(model.getGameState(), model);
        printBoard(model);
        System.out.println();
        printPlayers(model);
    }

    // -------------------------------------------------------
    // SEZIONI RIUTILIZZABILI
    // -------------------------------------------------------

    private void printHeader(String phase, ClientModel model) {
        System.out.printf("  %s  |  Era %s  |  Round %d%n",
                phase,
                model.getCurrentEra() != null ? model.getCurrentEra().name() : "-",
                model.getRoundNumber());
    }

    private void printBoard(ClientModel model) {
        System.out.println();
        System.out.println("  Tabellone:");
        System.out.println("  Ordine di turno: " + model.getTurnOrder());
        System.out.println();
        System.out.println("  Tile:");

        Map<Character, String> tiles = model.getTotemOnTiles();
        if (tiles.isEmpty()) {
            System.out.println("    (nessun totem posizionato)");
        } else {
            tiles.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> System.out.printf("    [%c] → %s%n", e.getKey(), e.getValue()));
        }
    }

    private void printCardRows(ClientModel model) {
        System.out.println("  Carte personaggio:");
        System.out.println("    Upper: " + formatIds(model.getUpperRowCharacterIds()));
        System.out.println("    Lower: " + formatIds(model.getLowerRowCharacterIds()));
        System.out.println("  Edifici:");
        System.out.println("    Upper: " + formatIds(model.getUpperRowBuildingIds()));
        System.out.println("    Lower: " + formatIds(model.getLowerRowBuildingIds()));
    }

    private void printPlayers(ClientModel model) {
        System.out.println("  Giocatori:");
        System.out.printf("    %-15s  %6s  %5s  %s  %s%n",
                "Nickname", "Cibo", "PP", "Personaggi", "Edifici");
        System.out.println("    " + "·".repeat(55));
        for (PlayerView p : model.getPlayers().values()) {
            System.out.printf("    %-15s  %6d  %5d  %-20s  %s%n",
                    p.getNickname(),
                    p.getFood(),
                    p.getPrestigePoints(),
                    formatIds(p.getCharacterCardIds()),
                    formatIds(p.getBuildingCardIds()));
        }
    }

    private String formatIds(List<Integer> ids) {
        if (ids.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}