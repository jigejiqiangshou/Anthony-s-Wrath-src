/**
 * Vanilla intercepts, kept in one package so that every change to base-game behaviour is
 * traceable (constitution article 3). Created as a skeleton by T006; the individual patches
 * arrive with the user stories that need them:
 *
 * <ul>
 *   <li>US2 (T019): {@code CardGroup.shuffle()} / {@code CardGroup.shuffle(Random)} postfix —
 *       move this mod's curse to the bottom of the shuffled group (research §R-004);
 *       implemented in {@code CardGroupShufflePatch}, with the ordering rule itself in
 *       {@code run.DrawPileOrdering};</li>
 *   <li>US3 (T022 / T024): hand-entry judgment points (R-005) — implemented in
 *       {@code CardGroupHandEntryPatch} (postfix on {@code CardGroup.addToHand} and on
 *       hand-targeted {@code CardGroup.addToTop}); the terminal short-circuit that skips
 *       later draws once the run is defeated is {@code AbstractPlayerDrawPatch} (prefix on
 *       {@code AbstractPlayer.draw(int)}), and the defeat landing point itself is
 *       {@code run.DefeatOnDraw} (vanilla {@code DeathScreen} flow);</li>
 *   <li>US4 (T027): random curse sources (R-007) — the card class carries BaseMod's
 *       {@code @NoPools} annotation for the card pools, and
 *       {@code CardLibraryGetCursePatch} filters the two {@code CardLibrary.getCurse}
 *       overloads, the actual origin of every vanilla random-curse source;</li>
 *   <li>US5 (T029-T031): unremovable behaviour (R-008) —
 *       {@code CardGroupGetPurgeableCardsPatch} keeps the card out of every removal /
 *       transform candidate list, and {@code FountainOfCurseRemovalPatch} brackets the
 *       bulk-removal loop of the "Fountain of Cleansing" event (which bypasses the
 *       candidate list). The remaining vanilla removal paths identified by the T031
 *       enumeration already exclude curses by type / rarity or go through the candidate
 *       list, so no further intercept is needed (research §R-008).</li>
 * </ul>
 *
 * <h2>Naming convention (T006)</h2>
 *
 * <ul>
 *   <li>One class per intercepted vanilla target, named
 *       {@code <VanillaClass><Behaviour>Patch} — for example
 *       {@code CardGroupGetPurgeableCardsPatch} or {@code FountainOfCurseRemovalPatch}.</li>
 *   <li>One static inner class per interception site inside that file; each site carries its
 *       own {@code @SpirePatch} annotation.</li>
 *   <li>A patch only compares against this mod's card id
 *       ({@code AnthonysWrathCurse.CARD_ID}) and must not change how the base game treats any
 *       other card (FR-012).</li>
 *   <li>No player-visible strings inside patches: all text stays in
 *       {@code strings/anthonyswrath/} (constitution article 2).</li>
 *   <li>If a BaseMod subscriber can do the job, prefer the subscriber; a patch here is the
 *       fallback, not the default (research §R-003).</li>
 * </ul>
 */
package anthonyswrath.patches.vanilla;
