package it.polimi.ingsw.network.client.tui;
import it.polimi.ingsw.network.protocol.serverbound.*;
import it.polimi.ingsw.network.protocol.clientbound.*;
import it.polimi.ingsw.model.Totem;
// + import GameAction concrete

public class CommandParser {

    /**
     * Prende una stringa input dell'utente (es. "place A") e ritorna il
     * ClientMessage corrispondente. Tira IllegalArgumentException se invalido.
     */
    public static ClientMessage parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Comando vuoto");
        }
        String[] tok = input.trim().toLowerCase().split("\\s+");
        String cmd = tok[0];

        return switch (cmd) {
            case "login" -> parseLogin(tok);
            case "count" -> parseCount(tok);
            case "ready" -> new ReadyMessage();
            case "place" -> parsePlace(tok);
            case "pick"  -> parsePick(tok);
            case "quit"  -> new DisconnectMessage();
            default -> throw new IllegalArgumentException("Comando sconosciuto: " + cmd);
        };
    }

    private static ClientMessage parseLogin(String[] tok) {
        // formato: login NICKNAME COLOR  (es. login mario red)
        if (tok.length != 3) throw new IllegalArgumentException("Uso: login <nick> <colore>");
        Totem totem = Totem.valueOf(tok[2].toUpperCase()); // assumi enum Totem
        return new LoginMessage(tok[1], totem);
    }

    private static ClientMessage parseCount(String[] tok) {
        // formato: count N
        if (tok.length != 2) throw new IllegalArgumentException("Uso: count <2-5>");
        int n = Integer.parseInt(tok[1]);
        return new SetPlayersCountMessage(n);
    }

    private static ClientMessage parsePlace(String[] tok) {
        // formato: place A
        if (tok.length != 2 || tok[1].length() != 1) {
            throw new IllegalArgumentException("Uso: place <A-G>");
        }
        char tileId = Character.toUpperCase(tok[1].charAt(0));
        // wrappato in PlaceTotemAction
        var action = new it.polimi.ingsw.controller.action.PlaceTotemAction(tileId);
        return new GameActionMessage(action);
    }

    private static ClientMessage parsePick(String[] tok) {
        // formato: pick upper|lower|building <id_o_indice>
        if (tok.length != 3) throw new IllegalArgumentException("Uso: pick <upper|lower|building> <id>");
        // TODO: serve da chiarire con Persona C come è strutturata PickCardAction
        // per ora throw not-implemented
        throw new UnsupportedOperationException("pick non ancora implementato");
    }
}