package anthonyswrath.cards;

import basemod.abstracts.CustomCard;
import basemod.patches.com.megacrit.cardcrawl.dungeons.AbstractDungeon.NoPools;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * The curse card「安东尼之怒」/ "Anthony's Wrath" (T010).
 *
 * <p>Parameters mirror the reference card "Curse of the Bell" (see research §R-009 and
 * memory fact f005): {@code CardType.CURSE}, {@code CardColor.CURSE}, cost -2,
 * {@code CardRarity.SPECIAL}, not upgradable, no use effect. Cost -2 plus the curse type is
 * what makes the base game's {@code AbstractCard.canUse(...)} refuse to play it (with the
 * same Blue Candle exception as the reference card).</p>
 *
 * <p>Name and description come from {@code strings/anthonyswrath/<language>/cards.json},
 * keyed by {@link #CARD_ID}; {@code BaseMod.loadCustomStringsFile(CardStrings.class, ...)}
 * in BaseMod 5.56 does not add a mod-id prefix, so the JSON key is the full card id.</p>
 *
 * <h2>Random curse sources (T027 / EC-010)</h2>
 *
 * <p>The card must never be produced by a random curse source. Two layers are used:
 * BaseMod's {@link NoPools} annotation makes BaseMod strip this card class from every card
 * pool (including {@code curseCardPool} and {@code srcCurseCardPool}) after each
 * {@code initializeCardPools}, and
 * {@code patches.vanilla.CardLibraryGetCursePatch} filters the two
 * {@code CardLibrary.getCurse} overloads — which is where the vanilla random-curse sources
 * actually draw from (a private curse map, not the pools; research §R-007 correction).
 * Together they match the reference card, which is hard-coded out of both the pool and the
 * curse map in the base game (memory facts f005, f023).</p>
 */
@NoPools
public class AnthonysWrathCurse extends CustomCard {

    /** Registrable card identifier; also the {@code CardStrings} key (FR-004). */
    public static final String CARD_ID = "anthonyswrath:AnthonysWrath";

    /**
     * Final portrait art (T036), tracked in the {@code src} layer at
     * {@code src/main/resources/anthonyswrath/img/cards/AnthonysWrath.jpg}. The path is a
     * jar entry relative to the mod classpath: {@code CustomCard} hands it to
     * {@code ImageMaster.loadImage}, and libGDX's {@code FileHandle.read()} falls back to
     * {@code Class.getResourceAsStream} for an internal path that does not exist on disk, so
     * the classpath of the mod jar wins in game (research §R-010).
     *
     * <p>This 250x190 file is the in-card portrait only. The single-card zoom popup asks
     * BaseMod for a large portrait, which {@code CustomCard.getPortraitImage()} derives by
     * inserting {@code _p} before the extension (500x380) — shipped next to this file as
     * {@code AnthonysWrath_p.jpg} (T042). Without that sibling the popup shows no art
     * because the derived load fails and returns null.</p>
     */
    private static final String IMAGE_PATH = "anthonyswrath/img/cards/AnthonysWrath.jpg";

    private static final CardStrings CARD_STRINGS =
            CardCrawlGame.languagePack.getCardStrings(CARD_ID);

    /** Localised card name, resolved from {@code cards.json} (constitution: no literals). */
    public static final String NAME = CARD_STRINGS.NAME;

    /** Localised card description, resolved from {@code cards.json} (FR-013). */
    public static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;

    public AnthonysWrathCurse() {
        // T010: CURSE / CURSE / cost -2 / SPECIAL, target NONE — same as the reference card
        super(
                CARD_ID,
                NAME,
                IMAGE_PATH,
                -2,
                DESCRIPTION,
                CardType.CURSE,
                CardColor.CURSE,
                CardRarity.SPECIAL,
                CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer player, AbstractMonster monster) {
        // FR-005: no use effect; the base game's canUse() already refuses to play a curse
        // with cost -2 (Blue Candle being the only vanilla exception, as for the reference).
    }

    @Override
    public void upgrade() {
        // FR-005: not upgradable. CardType.CURSE already makes canUpgrade() false; this
        // body only exists because AbstractCard.upgrade() is abstract.
    }

    @Override
    public AbstractCard makeCopy() {
        // FR-004: a fresh instance keeps the card's state independent of the original.
        return new AnthonysWrathCurse();
    }
}
