/**
 * Vanilla intercepts, kept in one package so that every change to base-game behaviour is
 * traceable (constitution article 3). Created as a skeleton by T006; the individual patches
 * arrive with the user stories that need them:
 *
 * <ul>
 *   <li>US2 (T019): {@code CardGroup.shuffle()} / {@code CardGroup.shuffle(Random)} postfix —
 *       move this mod's curse to the bottom of the shuffled group (research §R-004);</li>
 *   <li>US3 (T024): hand-entry judgment points if a subscriber cannot cover them (R-005);</li>
 *   <li>US5 (T029-T031): {@code CardGroup.getPurgeableCards()} filtering and the
 *       {@code FountainOfCurseRemoval} batch loop (R-008).</li>
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
