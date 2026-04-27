package it.polimi.ingsw.network.client.tui;
import it.polimi.ingsw.network.client.ClientModel;

public class Renderer {

    public static void render(ClientModel model) {
        clearScreen();
        printHeader(model);
        printRows(model);
        printPlayers(model);
        printPrompt(model);
    }

    private static void clearScreen() {
        // semplice: stampa 50 righe vuote
        for (int i = 0; i < 50; i++) System.out.println();
    }

    private static void printHeader(ClientModel m) {
        System.out.println("=== MESOS - Era " + m.getEra() + " - Round " + m.getRound() + " ===");
        System.out.println("Stato: " + m.getCurrentState());
        System.out.println();
    }

    private static void printRows(ClientModel m) {
        System.out.println("Fila Superiore: " + m.getUpperRowIds());
        System.out.println("Fila Inferiore: " + m.getLowerRowIds());
        System.out.println();
    }

    private static void printPlayers(ClientModel m) {
        for (var p : m.getPlayers()) {
            System.out.println("[" + p.getNickname() + "] Cibo:" + p.getFood()
                    + " PP:" + p.getPp()
                    + " Carte:" + p.getCharacterCardIds().size()
                    + " Edifici:" + p.getBuildingCardIds().size());
        }
        System.out.println();
    }

    private static void printPrompt(ClientModel m) {
        System.out.print("> ");
    }
}