package it.polimi.ingsw.network.client;

import java.util.List;

public class PlayerView {

    private final String nickname;
    private final String totemColor;
    private final int food;
    private final int prestigePoints;
    private final List<Integer> characterCardIds;
    private final List<Integer> buildingCardIds;

    public PlayerView(String nickname, String totemColor, int food, int prestigePoints,
                      List<Integer> characterCardIds, List<Integer> buildingCardIds) {
        this.nickname = nickname;
        this.totemColor = totemColor;
        this.food = food;
        this.prestigePoints = prestigePoints;
        this.characterCardIds = List.copyOf(characterCardIds);
        this.buildingCardIds = List.copyOf(buildingCardIds);
    }

    public String getNickname()                { return nickname; }
    public String getTotemColor()              { return totemColor; }
    public int getFood()                       { return food; }
    public int getPrestigePoints()             { return prestigePoints; }
    public List<Integer> getCharacterCardIds() { return characterCardIds; }
    public List<Integer> getBuildingCardIds()  { return buildingCardIds; }
}