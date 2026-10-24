package anthonyswrath.run;

import anthonyswrath.options.AnthonysWrathOption;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.trials.CustomTrial;
import java.util.ArrayList;

/**
 * Run-scoped state of the「安东尼之怒」feature (data-model entity 4).
 *
 * <p>{@link #isOptionSelected()} is the only "was the option selected this run" source; it
 * reads the list the game itself restores from the save, never a display name (FR-011,
 * EC-007). The list is {@code AbstractPlayer.customMods}: on a new custom run it is filled
 * by {@code CustomModeScreen.updateEmbarkButton} from {@code CustomTrial.dailyModIDs()}, and
 * on a loaded run it is restored from {@code SaveFile.custom_mods} by
 * {@code CardCrawlGame.loadPlayerSave}. Registering the option as a run-long modifier
 * ({@code new CustomMod(OPTION_ID, "r", true)}) is what puts it into that list on both
 * paths (research §R-014).</p>
 *
 * <p>Tolerance (T014, route a): the list may contain identifiers this mod knows nothing
 * about (options from other mods, or the option itself after being uninstalled). Reading it
 * with {@code contains(...)} means unknown identifiers are simply ignored; a {@code null}
 * list (no custom-mode information at all) is treated as "not selected" instead of
 * throwing.</p>
 *
 * <p>User story 3 (T021 / T023) adds the terminal state from data-model entity 4:
 * {@link #isDefeated()} is the only "this run already ended because of this feature" source,
 * and {@link #tryMarkDefeated()} is the one-shot guard that settles the defeat exactly once
 * (FR-010). Both are plain runtime flags: nothing is written to the save, and
 * {@link #resetDefeatState()} clears them at every {@code StartGame} (new run and loaded run,
 * research §R-007), because {@code StartGame} is the one BaseMod hook that runs on both
 * paths.</p>
 */
public final class RunState {

    /**
     * Set by the first hand entry of this mod's curse; never cleared until the next run
     * (FR-008, FR-009). Data-model entity 4: the only terminal flag and query source.
     */
    private static boolean defeated;

    /**
     * One-shot guard for writing {@link #defeated} (FR-010, EC-004). Not an independent
     * query source: readers use {@link #isDefeated()}.
     */
    private static boolean defeatGuard;

    /**
     * Returns whether the player selected this option for the current run.
     *
     * <p>The saved custom-mode list alone is not enough: it is a static field that the game
     * does not clear when a standard run starts, so a standard run started after a custom
     * run would still see the identifier. The run therefore also has to be a trial run
     * ({@code Settings.isTrial}, restored from the save on load) or still carry the
     * {@link CustomTrial} built by the custom-mode screen (a new custom run). Daily climbs
     * and standard runs use a different trial / no trial, so their stale list cannot turn
     * the option on (FR-011, SC-004).</p>
     *
     * @return {@code true} only when the identifier is present and the current run can carry
     *         custom-mode modifiers
     *
     * <p>Known limitation, to be removed by T026: the game does not clear the static
     * {@code AbstractPlayer.customMods} list when a run ends, so a same-session trial-seed
     * run (which sets {@code Settings.isTrial}) started after a selected custom run can still
     * look selected here. User story 1 is unaffected — its injection path uses the stricter
     * {@link #isOptionSelectedForNewRun()} — and the later stories are presence-driven
     * (they only act when the card is actually in the deck), so this cannot change their
     * outcome. T026 still has to replace this with a per-run record before they are wired.</p>
     */
    public static boolean isOptionSelected() {
        ArrayList<String> selectedOptionIds = AbstractPlayer.customMods;
        if (selectedOptionIds == null
                || !selectedOptionIds.contains(AnthonysWrathOption.OPTION_ID)) {
            return false;
        }
        return Settings.isTrial || CardCrawlGame.trial instanceof CustomTrial;
    }

    /**
     * Returns whether the option is selected for a run whose starting deck is being created
     * right now (T012).
     *
     * <p>{@code PostCreateStartingDeck} only fires for new runs — a loaded save keeps its
     * saved deck, which already contains the curse if it was injected — so the strict
     * {@link CustomTrial} check is sufficient here and cannot be fooled by a stale
     * {@code customMods} list.</p>
     *
     * @return {@code true} when this new run was started from the custom-mode screen with
     *         the option selected
     */
    public static boolean isOptionSelectedForNewRun() {
        return CardCrawlGame.trial instanceof CustomTrial && isOptionSelected();
    }

    /**
     * Returns whether this run has already ended because this mod's curse entered the hand
     * (FR-008 / FR-009; data-model entity 4).
     *
     * <p>Used by the terminal short-circuit in {@code patches.vanilla.AbstractPlayerDrawPatch}:
     * after the defeat has been settled, every later draw is skipped as a whole (EC-004).
     * </p>
     *
     * @return {@code true} once the defeat from this feature has been settled in this run
     */
    public static boolean isDefeated() {
        return defeated;
    }

    /**
     * Settles "this run already lost because of this feature" exactly once (FR-010).
     *
     * <p>The guard covers the case where several copies of the curse enter the hand within
     * the same draw call: only the first call returns {@code true}; every later call returns
     * {@code false} before any defeat logic runs, so the base game's defeat flow is entered
     * once and only once (EC-004).</p>
     *
     * @return {@code true} when this call is the one that may settle the defeat;
     *         {@code false} when it was already settled
     */
    public static boolean tryMarkDefeated() {
        if (defeatGuard) {
            return false;
        }
        defeatGuard = true;
        defeated = true;
        return true;
    }

    /**
     * Clears the terminal state at the start of a run (T024).
     *
     * <p>{@code StartGameSubscriber.receiveStartGame} fires both for a new run and for a
     * loaded run, after the card pools are rebuilt (research §R-007), so it is the single
     * place where the runtime-only flags have to be reset. Nothing here is persisted: after
     * a defeat the save is gone and the next run must start undefeated.</p>
     */
    public static void resetDefeatState() {
        defeated = false;
        defeatGuard = false;
    }

    private RunState() {
        // utility class
    }
}
