package it.polimi.ingsw.model.card.effect;

/**
 * Visitor over building {@link Effect}s. Specific effects forward to a typed
 * {@code visitXxx} method; the default implementations cascade up to
 * {@link #visitEndGame(EndGameEffect)} for end-game effects and to
 * {@link #visitEffect(Effect)} for the generic case, so visitors only override
 * the cases they care about.
 *
 * @param <R> result type produced by the visit
 */
public interface EffectVisitor<R> {

    default R visitCavePaintingsFood(CavePaintingsFoodEffect effect) {
        return visitEffect(effect);
    }

    default R visitEndBuilderPP(EndBuilderPPEffect effect) {
        return visitEndGame(effect);
    }

    default R visitEndCharacterCountPP(EndCharacterCountPPEffect effect) {
        return visitEndGame(effect);
    }

    default R visitEndFullSetPP(EndFullSetPPEffect effect) {
        return visitEndGame(effect);
    }

    default R visitEndPP(EndPPEffect effect) {
        return visitEndGame(effect);
    }

    default R visitExtraCardUpperRow(ExtraCardUpperRowEffect effect) {
        return visitEffect(effect);
    }

    default R visitFullSetFood(FullSetFoodEffect effect) {
        return visitEffect(effect);
    }

    default R visitHunt1Food1PP(Hunt1Food1PPEffect effect) {
        return visitEffect(effect);
    }

    default R visitInventorPairFood(InventorPairFoodEffect effect) {
        return visitEffect(effect);
    }

    default R visitShaman3ExtraStars(Shaman3ExtraStarsEffect effect) {
        return visitEffect(effect);
    }

    default R visitShamanicRitualDoublePP(ShamanicRitualDoublePPEffect effect) {
        return visitEffect(effect);
    }

    default R visitShamanicRitualImmunity(ShamanicRitualImmunityEffect effect) {
        return visitEffect(effect);
    }

    default R visitSustenanceFoodDiscount(SustenanceFoodDiscountEffect effect) {
        return visitEffect(effect);
    }

    default R visitTotemSlotFoodBonus(TotemSlotFoodBonusEffect effect) {
        return visitEffect(effect);
    }

    default R visitEndGame(EndGameEffect effect) {
        return visitEffect(effect);
    }

    R visitEffect(Effect effect);
}
