package anthonyswrath.run;

import anthonyswrath.cards.AnthonysWrathCurse;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.DeathScreen;

/**
 * Ends the run as soon as this mod's curse enters the player's hand (T021; FR-008 / FR-009,
 * EC-008).
 *
 * <h2>Entry points (T022)</h2>
 *
 * <p>The T004 probe established that there is no single common hand-entry point
 * (research §R-005, memory fact f013): normal draws, temporary cards and discard-to-hand
 * retrieval all go through {@code CardGroup.addToHand}, while draw-pile retrieval
 * ({@code SkillFromDeckToHandAction}) and Retain go through
 * {@code CardGroup.addToTop(player.hand)}. The two judgment points therefore live in
 * {@code patches.vanilla.CardGroupHandEntryPatch}, which only calls this class after the
 * vanilla body has finished (the card is already in the hand at that moment).</p>
 *
 * <h2>Landing point: the base game's own defeat flow</h2>
 *
 * <p>Harvested from the shipped V2.3.4 bytecode ({@code javap -c -p
 * com.megacrit.cardcrawl.characters.AbstractPlayer}, method {@code damage}, offsets
 * 1102-1136): when the player is out of lives the vanilla body itself performs exactly
 * this sequence —</p>
 * <ol>
 *   <li>{@code putfield isDead = true};</li>
 *   <li>{@code AbstractDungeon.deathScreen = new DeathScreen(AbstractDungeon.getMonsters())};</li>
 *   <li>{@code putfield currentHealth = 0};</li>
 *   <li>{@code if (currentBlock > 0) loseBlock()}.</li>
 * </ol>
 * <p>{@code DeathScreen} performs the rest of the vanilla defeat flow in its constructor:
 * it switches {@code AbstractDungeon.screen} to {@code DEATH} (so the combat update loop
 * stops and only {@code DeathScreen.update()} runs), resets score checks, increments the
 * character's death stat, submits the defeat metrics, updates the save and shows the
 * return-to-menu button. Doing the same sequence here therefore reuses the shipped flow
 * instead of re-implementing it (FR-009).</p>
 *
 * <p><b>Why not {@code player.damage(...)}?</b> The same vanilla {@code damage} method
 * checks the player's revive resources before that branch (offsets 960-1101: Lizard Tail,
 * Fairy Potion, Mark of the Bloom). A reviving relic would turn a lethal damage call into
 * a heal, so the run would not end — the spec requires the run to end the moment the card
 * is obtained, without extra conditions (FR-008), so the revive checks must not apply.
 * Calling {@code damage} would also route through on-damage relics and powers, which this
 * feature does not require.</p>
 *
 * <h2>Once only (T023)</h2>
 *
 * <p>{@link RunState#tryMarkDefeated()} settles the defeat exactly once: several copies of
 * the curse entering the hand in the same draw call reach the second call and return
 * without touching the game (FR-010). After that, {@code patches.vanilla.AbstractPlayerDrawPatch}
 * skips every later draw as a whole (EC-004), and the game's own {@code DEATH} screen means
 * the player cannot act any more.</p>
 */
public final class DefeatOnDraw {

    private DefeatOnDraw() {
        // utility class
    }

    /**
     * Settles the defeat when {@code card} is this mod's curse (FR-008).
     *
     * <p>Called by the postfix handlers of {@code CardGroup.addToHand} and
     * {@code CardGroup.addToTop} when the target group is the player's hand. Cards other
     * than {@link AnthonysWrathCurse#CARD_ID} return immediately, so the vanilla treatment of
     * every other card is untouched (FR-012 spirit).</p>
     *
     * @param card the card that has just been added to the player's hand
     */
    public static void onCurseEnteredHand(AbstractCard card) {
        if (card == null || !AnthonysWrathCurse.CARD_ID.equals(card.cardID)) {
            return;
        }
        if (RunState.isDefeated()) {
            // A second copy in the same draw call: already settled (FR-010).
            return;
        }
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null || player.isDead) {
            // Nothing to end: no player, or the run already ended through the base game.
            return;
        }
        if (AbstractDungeon.getCurrRoom() == null || AbstractDungeon.getMonsters() == null) {
            // Hand entries only make sense in combat; outside it there is no defeat screen
            // to open and touching the run would be wrong.
            return;
        }
        if (!RunState.tryMarkDefeated()) {
            return;
        }
        endRunInDefeat(player);
    }

    /**
     * Executes the base game's defeat branch (mirrors {@code AbstractPlayer.damage}, offsets
     * 1102-1136 of the shipped V2.3.4 bytecode; see the class comment).
     */
    private static void endRunInDefeat(AbstractPlayer player) {
        player.isDead = true;
        // The constructor drives the whole vanilla defeat flow (screen, stats, metrics, save).
        AbstractDungeon.deathScreen = new DeathScreen(AbstractDungeon.getMonsters());
        player.currentHealth = 0;
        if (player.currentBlock > 0) {
            player.loseBlock();
        }
    }
}
