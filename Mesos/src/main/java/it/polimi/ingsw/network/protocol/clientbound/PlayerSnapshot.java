package it.polimi.ingsw.network.protocol.clientbound;
import java.io.Serializable;
import java.util.List;

public class PlayerSnapshot implements Serializable {
    private final String nickname;
    private final String totemColor;
    private final int food;
    private final int prestigePoints;
    private final List<Integer> characterCardIds;
    private final List<Integer> buildingCardIds;

    public PlayerSnapshot(String nickname, String totemColor, int food,
                          int prestigePoints, List<Integer> characterCardIds,
                          List<Integer> buildingCardIds) {
        this.nickname = nickname;
        this.totemColor = totemColor;
        this.food = food;
        this.prestigePoints = prestigePoints;
        this.characterCardIds = characterCardIds;
        this.buildingCardIds = buildingCardIds;
    }
    public String getNickname() { return nickname; }
    public String getTotemColor() { return totemColor; }
    public int getFood() { return food; }
    public int getPrestigePoints() { return prestigePoints; }
    public List<Integer> getCharacterCardIds() { return characterCardIds; }
    public List<Integer> getBuildingCardIds() { return buildingCardIds; }
}