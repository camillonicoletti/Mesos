package it.polimi.ingsw.network.protocol.clientbound;
public class PointsChangedMessage implements ServerMessage {
    private final String nickname;
    private final int newPoints;
    private final int delta;
    public PointsChangedMessage(String nickname, int newPoints, int delta) {
        this.nickname = nickname;
        this.newPoints = newPoints;
        this.delta = delta;
    }
    public String getNickname() { return nickname; }
    public int getNewPoints() { return newPoints; }
    public int getDelta() { return delta; }
}