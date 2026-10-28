package anthonyswrath;

import anthonyswrath.cards.AnthonysWrathCurse;
import anthonyswrath.options.AnthonysWrathOption;
import anthonyswrath.run.RunState;
import anthonyswrath.run.StartDeckInjector;
import anthonyswrath.util.ModStrings;
import basemod.BaseMod;
import basemod.interfaces.AddCustomModeModsSubscriber;
import basemod.interfaces.EditCardsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.ISubscriber;
import basemod.interfaces.PostCreateStartingDeckSubscriber;
import basemod.interfaces.StartGameSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.custom.CustomMod;
import java.util.List;

/**
 * Mod entry point.
 *
 * <p>Owns BaseMod initialisation and every subscription used by user story 1:</p>
 * <ul>
 *   <li>{@link EditStringsSubscriber} — bilingual card text and option text (T009 / T013);</li>
 *   <li>{@link EditCardsSubscriber} — registers the curse card (T011);</li>
 *   <li>{@link AddCustomModeModsSubscriber} — registers the custom-mode option (T008);</li>
 *   <li>{@link PostCreateStartingDeckSubscriber} — injects one copy into the starting deck
 *       (T012);</li>
 *   <li>{@link StartGameSubscriber} — captures the per-run selection and resets the
 *       run-scoped terminal state at every run start, new or loaded (T024 / T026).</li>
 * </ul>
 *
 * <p>All interfaces are the ones present in the shipped BaseMod 5.56.0 jar (memory fact
 * f009); the subscription is done once through {@code BaseMod.subscribe(this)}. Later user
 * stories add their own subscribers here; vanilla intercepts live in
 * {@code anthonyswrath.patches.vanilla}.</p>
 *
 * <p><b>User story 2 (T017).</b> The bottom-of-deck rule deliberately has <em>no</em>
 * subscriber to register here: every shuffle path converges on the two
 * {@code CardGroup} shuffle primitives, so the intervention is the vanilla postfix in
 * {@code patches.vanilla.CardGroupShufflePatch} (T019), which ModTheSpire discovers and
 * applies automatically when it loads the mod jar (it scans the jar for {@code @SpirePatch}
 * classes — the T004 probe patches work the same way without being referenced from code).
 * The three paths named by T017 are therefore covered without an entry-point hook:</p>
 * <ul>
 *   <li>battle start: {@code AbstractPlayer.preBattlePrep} calls
 *       {@code CardGroup.initializeDeck}, which shuffles a temporary {@code DRAW_PILE} group
 *       with {@code shuffle(Random)};</li>
 *   <li>exhausted draw pile: {@code EmptyDeckShuffleAction} shuffles {@code discardPile} with
 *       {@code shuffle(Random)};</li>
 *   <li>card / relic triggered (non-exhausted): {@code ShuffleAction} (used by {@code Reboot}
 *       and {@code DeepBreath}) calls {@code shuffle()} on {@code drawPile}, and
 *       {@code ShuffleAllAction} (also {@code Reboot}) calls {@code shuffle(Random)} on
 *       {@code discardPile}.</li>
 * </ul>
 * <p>Those are all of the {@code CardGroup.shuffle} call sites in the game jar that touch a
 * draw pile or discard pile; the other call sites shuffle scratch groups of type
 * {@code UNSPECIFIED} ({@code DrawPileToHandAction}, {@code UpgradeRandomCardAction},
 * {@code TwistingMind}, {@code CardLibrary.getAnyColorCard}) and are intentionally not
 * reordered. No vanilla code shuffles a pile's {@code group} list directly. Evidence:
 * {@code memory/log/023}.</p>
 *
 * <p><b>User story 4 (T026 / T027).</b> Two separate mechanisms keep unselected runs
 * untouched and keep the card out of random curse sources:</p>
 * <ul>
 *   <li>the per-run selection record lives in {@code run.RunState.beginRun()}, called from
 *       {@code receiveStartGame} below; every feature component consults it (FR-011);</li>
 *   <li>the random-curse exclusion does <em>not</em> need this hook: the card class carries
 *       BaseMod's {@code @NoPools} annotation, which makes BaseMod remove the card from all
 *       card pools (including {@code curseCardPool} / {@code srcCurseCardPool}) after every
 *       {@code initializeCardPools}, and {@code patches.vanilla.CardLibraryGetCursePatch}
 *       filters the two {@code CardLibrary.getCurse} overloads — the actual origin of every
 *       vanilla random-curse source, which reads the card library's private curse map and
 *       bypasses the pools (research §R-007 correction; memory fact f023).</li>
 * </ul>
 * <p>The T027 note "exclude the card in the {@code StartGame} hook" was superseded by this
 * split: a pool-only removal would not stop {@code CardLibrary.getCurse()}, and the
 * {@code @NoPools} hook removes the pools on both the new-run and the loaded-run path
 * (both rebuild the pools — memory fact f011).</p>
 *
 * <p><b>User story 3 (T024).</b> The hand-entry judgment points are vanilla intercepts, so
 * they are applied through {@code patches.vanilla.CardGroupHandEntryPatch} (postfix on
 * {@code CardGroup.addToHand} and on hand-targeted {@code CardGroup.addToTop}, research
 * §R-005) rather than through a subscriber: the only BaseMod hook in that area,
 * {@code PostDrawSubscriber}, cannot see effect-based retrieval (EC-008) and therefore
 * cannot be the judgment point. The terminal short-circuit for later draws is the prefix in
 * {@code patches.vanilla.AbstractPlayerDrawPatch}. What is registered here is only the
 * {@code StartGame} subscription that clears the runtime-only defeat flags at every run
 * start — both the new-run and the loaded-run path fire it (research §R-007).</p>
 */
@SpireInitializer
public class AnthonysWrathMod
        implements ISubscriber,
                EditStringsSubscriber,
                EditCardsSubscriber,
                AddCustomModeModsSubscriber,
                PostCreateStartingDeckSubscriber,
                StartGameSubscriber {

    public AnthonysWrathMod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new AnthonysWrathMod();
    }

    @Override
    public void receiveEditStrings() {
        ModStrings.loadEditStrings();
    }

    @Override
    public void receiveEditCards() {
        BaseMod.addCard(new AnthonysWrathCurse());
    }

    @Override
    public void receiveCustomModeMods(List<CustomMod> mods) {
        mods.add(AnthonysWrathOption.create());
    }

    @Override
    public void receivePostCreateStartingDeck(
            AbstractPlayer.PlayerClass playerClass, CardGroup startingDeck) {
        StartDeckInjector.injectIfSelected(playerClass, startingDeck);
    }

    @Override
    public void receiveStartGame() {
        // T024: both the new-run and the loaded-run path reach StartGame (research §R-007),
        // so this is where the runtime-only terminal state of the previous run is cleared.
        // T026: the same hook captures the per-run selection record (the saved custom-mode
        // list plus the trial gate) that every feature component consults before acting.
        RunState.beginRun();
    }
}
