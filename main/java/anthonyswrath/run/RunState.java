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
 * <h2>One record per run (T026)</h2>
 *
 * <p>User story 4 requires every feature component to consult the same authority and to
 * short-circuit when the option was not selected (FR-011). Instead of re-reading the static
 * {@code AbstractPlayer.customMods} list at every call site, the selection is captured once
 * per run in {@link #beginRun()}, which the {@code StartGame} subscriber calls (new run and
 * loaded run both trigger it, after the card pools have been rebuilt — research §R-007).
 * The captured flag is what {@link #isOptionSelected()} returns, so a later change of the
 * game's static fields can no longer flip the behaviour in the middle of a run.</p>
 *
 * <p>New-run injection is the one call that happens <em>before</em> {@code StartGame}
 * ({@code initializeStarterDeck} → {@code PostCreateStartingDeck} → {@code StartGame}), so
 * {@link StartDeckInjector} keeps using the stricter {@link #isOptionSelectedForNewRun()},
 * which reads the authoritative fields directly at that moment (a new custom run always
 * carries a {@link CustomTrial}, so a stale list cannot turn it on).</p>
 *
 * <h2>Terminal state (user story 3)</h2>
 *
 * <p>{@link #isDefeated()} is the only "this run already ended because of this feature"
 * source, and {@link #tryMarkDefeated()} is the one-shot guard that settles the defeat
 * exactly once (FR-010). Both are plain runtime flags: nothing is written to the save, and
 * {@link #beginRun()} clears them at every {@code StartGame} because the save from a
 * defeated run is gone and the next run must start undefeated.</p>
 */
public final class RunState {

    /**
     * Captured once per run by {@link #beginRun()} (T026); the only "selected this run"
     * source used by the runtime components (FR-011).
     */
    private static boolean selectedThisRun;

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
     * Development-only switch for the FR-005 verification (T032 / research §R-013).
     *
     * <p>FR-005 ("cannot be played / cannot be actively consumed") can only be observed
     * while the curse stays in the hand, which the normal build never allows — the card
     * ends the run on entry. The probe build can temporarily suspend the defeat through the
     * {@code awprobe defeatoff} / {@code awprobe defeaton} console commands so the reference
     * card and this card can be compared under identical conditions (QS-12).</p>
     *
     * <p><b>Development only.</b> This flag and its console commands are removed together
     * with the probe package by T035; the release build has no way to disable the defeat.</p>
     */
    private static boolean defeatSuspendedForProbe;

    /**
     * Starts the bookkeeping of a run: captures the selection and clears the terminal flags.
     *
     * <p>Called from {@code StartGameSubscriber.receiveStartGame}, the one BaseMod hook that
     * fires both for a new run and for a loaded run and that runs after the card pools have
     * been rebuilt (research §R-007, memory fact f011). Nothing is persisted: the state is
     * runtime-only by design (data-model entity 4, constitution article 4).</p>
     */
    public static void beginRun() {
        selectedThisRun = readSavedSelection();
        defeated = false;
        defeatGuard = false;
    }

    /**
     * Returns whether this mod's option was selected for the current run (FR-011).
     *
     * <p>The value is the per-run record captured in {@link #beginRun()} — see the class
     * comment for why it is not read live. Before the first {@code StartGame} of the process
     * it is {@code false}, which is correct: no run exists yet, so no feature component may
     * intervene.</p>
     *
     * @return {@code true} only when this run was started (or restored) with the option
     */
    public static boolean isOptionSelected() {
        return selectedThisRun;
    }

    /**
     * Returns whether the option is selected for a run whose starting deck is being created
     * right now (T012 / T026).
     *
     * <p>{@code PostCreateStartingDeck} fires before {@code StartGame}, so the per-run record
     * is not available yet. The strict {@link CustomTrial} check is sufficient here: a new
     * custom run always carries the trial built by the custom-mode screen, so a stale
     * {@code customMods} list can never make a non-custom run look selected, and a loaded
     * save never fires this hook (its deck already contains the curse if it was injected).</p>
     *
     * @return {@code true} when this new run was started from the custom-mode screen with
     *         the option selected
     */
    public static boolean isOptionSelectedForNewRun() {
        return CardCrawlGame.trial instanceof CustomTrial && readSavedSelection();
    }

    /**
     * Reads the selection from the fields the game restores from the save (the authority
     * named by FR-011): the custom-mode identifier list plus the "this run can carry
     * custom-mode modifiers" gate.
     *
     * <p>The saved custom-mode list alone is not enough: it is a static field that the game
     * does not clear when a standard run starts, so a standard run started after a custom
     * run would still see the identifier. The run therefore also has to be a trial run
     * ({@code Settings.isTrial}, restored from the save on load) or still carry the
     * {@link CustomTrial} built by the custom-mode screen (a new custom run). Daily climbs
     * and standard runs use a different trial / no trial, so their stale list cannot turn
     * the option on (FR-011, SC-004).</p>
     */
    private static boolean readSavedSelection() {
        ArrayList<String> selectedOptionIds = AbstractPlayer.customMods;
        if (selectedOptionIds == null
                || !selectedOptionIds.contains(AnthonysWrathOption.OPTION_ID)) {
            return false;
        }
        return Settings.isTrial || CardCrawlGame.trial instanceof CustomTrial;
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
     * Returns whether the development-only FR-005 comparison mode is active (T032).
     *
     * <p>Only the {@code awprobe} console commands toggle this flag; the release build (after
     * T035 removes the probe) never sets it, so {@code run.DefeatOnDraw} always settles the
     * defeat there. See the field comment for the purpose and the removal note.</p>
     */
    public static boolean isDefeatSuspendedForProbe() {
        return defeatSuspendedForProbe;
    }

    /**
     * Toggles the development-only FR-005 comparison mode (T032).
     *
     * <p>Called only by the {@code awprobe defeatoff} / {@code awprobe defeaton} console
     * commands (probe package, removed by T035).</p>
     *
     * @param suspended {@code true} to keep the curse in the hand without ending the run
     */
    public static void setDefeatSuspendedForProbe(boolean suspended) {
        defeatSuspendedForProbe = suspended;
    }

    private RunState() {
        // utility class
    }
}
