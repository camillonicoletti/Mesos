package it.polimi.ingsw.model.card;

/**
 * Visitor over the {@link Card} hierarchy. Each concrete card forwards to the
 * most specific {@code visitXxx} hook; default implementations cascade up to
 * the broader category (character, event, generic card) so that visitors only
 * override the cases they care about.
 *
 * @param <R> result type produced by the visit
 */
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
