package it.polimi.ingsw.network.protocol.clientbound;
public class FoodChangedMessage implements ServerMessage {
    private final String nickname;
    private final int newAmount;
    private final int delta;
    public FoodChangedMessage(String nickname, int newAmount, int delta) {
        this.nickname = nickname;
        this.newAmount = newAmount;
        this.delta = delta;
    }
    public String getNickname() { return nickname; }
    public int getNewAmount() { return newAmount; }
    public int getDelta() { return delta; }
}