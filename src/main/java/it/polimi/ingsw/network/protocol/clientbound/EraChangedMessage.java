package it.polimi.ingsw.network.protocol.clientbound;
import it.polimi.ingsw.model.enums.Era;
public class EraChangedMessage implements ServerMessage {
    private final Era newEra;
    public EraChangedMessage(Era newEra) { this.newEra = newEra; }
    public Era getNewEra() { return newEra; }
}