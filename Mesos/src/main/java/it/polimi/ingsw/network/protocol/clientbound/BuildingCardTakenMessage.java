package it.polimi.ingsw.network.protocol.clientbound;
public class BuildingCardTakenMessage implements ServerMessage {
    private final String nickname;
    private final int cardId;
    private final boolean fromUpperRow;
    public BuildingCardTakenMessage(String nickname, int cardId, boolean fromUpperRow) {
        this.nickname = nickname;
        this.cardId = cardId;
        this.fromUpperRow = fromUpperRow;
    }
    public String getNickname() { return nickname; }
    public int getCardId() { return cardId; }
    public boolean isFromUpperRow() { return fromUpperRow; }
}