package anthonyswrath.probe;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.common.EmptyDeckShuffleAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.FountainOfCurseRemoval;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * T004 vanilla-side observation patches (development only, removed by T035).
 *
 * <p>Each nested class patches exactly one vanilla method; handlers only log, they
 * never change behaviour. This is the evidence side of the timing probe: which
 * vanilla call site runs, in which order, and with what pile contents.</p>
 */
public class ProbePatches {

    private ProbePatches() {
    }

    private static void log(String tag, String msg) {
        try {
            ProbeLog.log(tag, msg);
        } catch (Throwable ignored) {
        }
    }

    /** Q1: all player-pile shuffles pass through {@code CardGroup.shuffle()} (no-arg). */
    @SpirePatch(clz = CardGroup.class, method = "shuffle", paramtypez = {})
    public static class ShuffleNoArg {
        @SpirePrefixPatch
        public static void Prefix(CardGroup __instance) {
            log("patch.shuffle().before", ProbeLog.groupTag(__instance) + " " + ProbeLog.ids(__instance)
                    + " | " + ProbeLog.caller());
        }

        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance) {
            log("patch.shuffle().after", ProbeLog.groupTag(__instance) + " " + ProbeLog.ids(__instance)
                    + " " + ProbeLog.trackedPositions(__instance));
        }
    }

    /** Q1: first shuffle (battle start) and exhausted-discard reshuffle use this overload. */
    @SpirePatch(clz = CardGroup.class, method = "shuffle",
            paramtypez = {com.megacrit.cardcrawl.random.Random.class})
    public static class ShuffleRng {
        @SpirePrefixPatch
        public static void Prefix(CardGroup __instance, com.megacrit.cardcrawl.random.Random rng) {
            log("patch.shuffle(Random).before", ProbeLog.groupTag(__instance) + " " + ProbeLog.ids(__instance)
                    + " | " + ProbeLog.caller());
        }

        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance, com.megacrit.cardcrawl.random.Random rng) {
            log("patch.shuffle(Random).after", ProbeLog.groupTag(__instance) + " " + ProbeLog.ids(__instance)
                    + " " + ProbeLog.trackedPositions(__instance));
        }
    }

    /** Q1/Q3: battle-start deck initialisation (copies masterDeck into a new draw pile, then shuffles it). */
    @SpirePatch(clz = CardGroup.class, method = "initializeDeck", paramtypez = {CardGroup.class})
    public static class InitializeDeck {
        @SpirePrefixPatch
        public static void Prefix(CardGroup __instance, CardGroup masterDeck) {
            log("patch.initializeDeck.before", "target=" + ProbeLog.groupTag(__instance)
                    + " source=" + ProbeLog.ids(masterDeck));
        }

        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance, CardGroup masterDeck) {
            log("patch.initializeDeck.after", "target=" + ProbeLog.groupTag(__instance)
                    + " " + ProbeLog.ids(__instance) + " " + ProbeLog.trackedPositions(__instance));
        }
    }

    /** Q1: battle-start preparation order (preBattlePrep contains the first shuffle). */
    @SpirePatch(clz = AbstractPlayer.class, method = "preBattlePrep")
    public static class PreBattlePrep {
        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance) {
            log("patch.preBattlePrep.before", ProbeLog.pileSizes() + ProbeLog.pileDump());
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance) {
            log("patch.preBattlePrep.after", ProbeLog.pileSizes() + ProbeLog.pileDump());
        }
    }

    /** Q3: when the starting deck is created, before the first shuffle. */
    @SpirePatch(clz = AbstractPlayer.class, method = "initializeStarterDeck")
    public static class InitializeStarterDeck {
        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance) {
            log("patch.initializeStarterDeck.before", "masterDeck=" + ProbeLog.ids(__instance.masterDeck)
                    + " | " + ProbeLog.mods());
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance) {
            log("patch.initializeStarterDeck.after", "masterDeck=" + ProbeLog.ids(__instance.masterDeck)
                    + " | " + ProbeLog.mods());
        }
    }

    /** Q2: draw path and ShowCardAndAddToHandEffect both end in {@code hand.addToHand}. */
    @SpirePatch(clz = CardGroup.class, method = "addToHand", paramtypez = {AbstractCard.class})
    public static class AddToHand {
        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance, AbstractCard card) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null || __instance != p.hand) {
                return;
            }
            log("patch.addToHand", "card=" + card.cardID + " hand=" + ProbeLog.sizeOf(__instance)
                    + " " + ProbeLog.pileSizes() + " | " + ProbeLog.caller());
        }
    }

    /** Q2: direct from-pile-to-hand actions use {@code hand.addToTop}. */
    @SpirePatch(clz = CardGroup.class, method = "addToTop", paramtypez = {AbstractCard.class})
    public static class AddToTop {
        @SpirePostfixPatch
        public static void Postfix(CardGroup __instance, AbstractCard card) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null || __instance != p.hand) {
                return;
            }
            log("patch.addToTop(hand)", "card=" + card.cardID + " hand=" + ProbeLog.sizeOf(__instance)
                    + " " + ProbeLog.pileSizes() + " | " + ProbeLog.caller());
        }
    }

    /** Q5: every call site that asks "which cards may be removed" (shop / rest / events / relics). */
    @SpirePatch(clz = CardGroup.class, method = "getPurgeableCards")
    public static class GetPurgeableCards {
        @SpirePrefixPatch
        public static void Prefix(CardGroup __instance) {
            log("patch.getPurgeableCards", ProbeLog.groupTag(__instance) + " " + ProbeLog.ids(__instance)
                    + " | " + ProbeLog.caller());
        }
    }

    /** Q5: enumerate removal paths that actually delete a card. */
    @SpirePatch(clz = CardGroup.class, method = "removeCard", paramtypez = {AbstractCard.class})
    public static class RemoveCardObject {
        @SpirePrefixPatch
        public static void Prefix(CardGroup __instance, AbstractCard card) {
            log("patch.removeCard(obj)", ProbeLog.groupTag(__instance) + " card=" + card.cardID
                    + " tracked=" + isTracked(card.cardID) + " | " + ProbeLog.caller());
        }
    }

    /** Q5: the String overload (does not fire onRemoveFromMasterDeck in vanilla). */
    @SpirePatch(clz = CardGroup.class, method = "removeCard", paramtypez = {String.class})
    public static class RemoveCardId {
        @SpirePrefixPatch
        public static void Prefix(CardGroup __instance, String cardID) {
            log("patch.removeCard(id)", ProbeLog.groupTag(__instance) + " cardID=" + cardID
                    + " tracked=" + isTracked(cardID) + " | " + ProbeLog.caller());
        }
    }

    /** Q5: "Fountain of Cleansing" bulk curse removal. */
    @SpirePatch(clz = FountainOfCurseRemoval.class, method = "buttonEffect", paramtypez = {int.class})
    public static class FountainButton {
        @SpirePrefixPatch
        public static void Prefix(FountainOfCurseRemoval __instance, int buttonIdx) {
            log("patch.fountain.before", "button=" + buttonIdx + " masterDeck="
                    + ProbeLog.ids(AbstractDungeon.player == null ? null : AbstractDungeon.player.masterDeck));
        }

        @SpirePostfixPatch
        public static void Postfix(FountainOfCurseRemoval __instance, int buttonIdx) {
            log("patch.fountain.after", "button=" + buttonIdx + " masterDeck="
                    + ProbeLog.ids(AbstractDungeon.player == null ? null : AbstractDungeon.player.masterDeck));
        }
    }

    /** Q6: card-pool initialisation (curse pool is rebuilt here on new run AND on load). */
    @SpirePatch(clz = AbstractDungeon.class, method = "initializeCardPools")
    public static class InitializeCardPools {
        @SpirePrefixPatch
        public static void Prefix(AbstractDungeon __instance) {
            log("patch.initializeCardPools.before", "floor=" + AbstractDungeon.floorNum);
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractDungeon __instance) {
            log("patch.initializeCardPools.after", "floor=" + AbstractDungeon.floorNum
                    + " common=" + sizeOfGroup(AbstractDungeon.commonCardPool)
                    + " uncommon=" + sizeOfGroup(AbstractDungeon.uncommonCardPool)
                    + " rare=" + sizeOfGroup(AbstractDungeon.rareCardPool)
                    + " colorless=" + sizeOfGroup(AbstractDungeon.colorlessCardPool)
                    + "\n    curseCardPool " + ProbeLog.ids(AbstractDungeon.curseCardPool));
        }
    }

    /** Q4: transform resolves to a global field; the caller then removes the original card. */
    @SpirePatch(clz = AbstractDungeon.class, method = "transformCard",
            paramtypez = {AbstractCard.class, boolean.class, com.megacrit.cardcrawl.random.Random.class})
    public static class TransformCard {
        @SpirePrefixPatch
        public static void Prefix(AbstractCard card, boolean useRng,
                                  com.megacrit.cardcrawl.random.Random rng) {
            log("patch.transformCard.before", "card=" + (card == null ? "null" : card.cardID)
                    + (card == null ? "" : " type=" + card.type + " color=" + card.color)
                    + " | " + ProbeLog.caller());
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractCard card, boolean useRng,
                                   com.megacrit.cardcrawl.random.Random rng) {
            AbstractCard result = AbstractDungeon.transformedCard;
            String resultDesc = result == null ? "null" : (result.cardID + " type=" + result.type);
            log("patch.transformCard.after", "card=" + (card == null ? "null" : card.cardID)
                    + " result=" + resultDesc);
        }
    }

    /** Q1: exhausted draw pile -> discard pile reshuffle action. */
    @SpirePatch(clz = EmptyDeckShuffleAction.class, method = "update")
    public static class EmptyDeckShuffle {
        @SpirePrefixPatch
        public static void Prefix(EmptyDeckShuffleAction __instance) {
            log("patch.emptyDeckShuffle.before", ProbeLog.pileSizes() + " | " + ProbeLog.caller());
        }

        @SpirePostfixPatch
        public static void Postfix(EmptyDeckShuffleAction __instance) {
            log("patch.emptyDeckShuffle.after", ProbeLog.pileSizes()
                    + " drawPile=" + ProbeLog.ids(AbstractDungeon.player == null ? null : AbstractDungeon.player.drawPile));
        }
    }

    /** Q2: raw draw entry point (before hand.addToHand), useful to bracket the draw path. */
    @SpirePatch(clz = AbstractPlayer.class, method = "draw", paramtypez = {int.class})
    public static class DrawInt {
        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance, int numCards) {
            log("patch.draw(int).before", "n=" + numCards + " " + ProbeLog.pileSizes());
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance, int numCards) {
            log("patch.draw(int).after", "n=" + numCards + " " + ProbeLog.pileSizes());
        }
    }

    /** Room context helper for Q1 logs. */
    public static String roomTag() {
        try {
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            return room == null ? "null" : room.getClass().getSimpleName();
        } catch (Throwable t) {
            return "err";
        }
    }

    private static boolean isTracked(String cardID) {
        return "CurseOfTheBell".equals(cardID) || "Necronomicurse".equals(cardID)
                || "AscendersBane".equals(cardID) || "Pride".equals(cardID)
                || "AnthonysWrath".equals(cardID);
    }

    private static int sizeOfGroup(CardGroup g) {
        return (g == null || g.group == null) ? -1 : g.group.size();
    }
}
