package it.polimi.ingsw.model.card.effect;

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
