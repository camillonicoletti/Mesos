package it.polimi.ingsw.network.client;

import it.polimi.ingsw.view.View;
import it.polimi.ingsw.view.gui.Gui;
import it.polimi.ingsw.view.tui.Tui;

import java.util.Scanner;

final class ClientViewSelector {

    private ClientViewSelector() {
    }

    static View select(ClientController controller, ClientModel model) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Select interface:");
            System.out.println("  1 — TUI (terminal)");
            System.out.println("  2 — GUI (graphical interface)");
            System.out.print("> ");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    return new Tui(controller, model);
                }
                case "2" -> {
                    return new Gui(controller, model);
                }
                default -> System.out.println("Invalid choice, type 1 or 2.");
            }
        }
    }
}
