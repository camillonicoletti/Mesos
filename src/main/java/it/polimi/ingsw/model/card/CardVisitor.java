package it.polimi.ingsw.model.card;

public interface CardVisitor<R> {

    default R visitArtist(ArtistCard card) {
        return visitCharacter(card);
    }

    default R visitBuilder(BuilderCard card) {
        return visitCharacter(card);
    }

    default R visitGatherer(GathererCard card) {
        return visitCharacter(card);
    }

    default R visitHunter(HunterCard card) {
        return visitCharacter(card);
    }

    default R visitInventor(InventorCard card) {
        return visitCharacter(card);
    }

    default R visitShaman(ShamanCard card) {
        return visitCharacter(card);
    }

    default R visitBuilding(BuildingCard card) {
        return visitCard(card);
    }

    default R visitCavePaintingsEvent(CavePaintingsEventCard card) {
        return visitEvent(card);
    }

    default R visitHuntEvent(HuntEventCard card) {
        return visitEvent(card);
    }

    default R visitShamanicRitualEvent(ShamanicRitualEventCard card) {
        return visitEvent(card);
    }

    default R visitSustenanceEvent(SustenanceEventCard card) {
        return visitEvent(card);
    }

    default R visitCharacter(CharacterCard card) {
        return visitCard(card);
    }

    default R visitEvent(EventCard card) {
        return visitCard(card);
    }

    R visitCard(Card card);
}
