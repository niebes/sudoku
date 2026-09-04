# Sudoku solving techniques: a prioritised implementation plan

A catalogue of every technique worth adding as a `FieldProcessor`, ordered by
payoff-per-line-of-code, excluding brute force. Written to answer one question:
**if we are not allowed to guess, what do we have to build?**

Sources are listed at the bottom. The measurements in "What this repo's puzzles
need" were produced against a reference implementation, not against this codebase.

---

## 1. The honest boundary

Three facts, in tension, that shape everything below.

**Every proper puzzle is deducible in principle.** A proper Sudoku has exactly one
solution, so every cell's value is entailed by the givens. There is always *a* chain
of reasoning; nothing is left to chance.

**No small fixed rule set is complete.** The technique ladder is open-ended. Each
tier below solves puzzles the tier under it cannot, and there is no point at which
you have them all. The most credible completeness claim in the literature is
HoDoKu's: *"Every known sudoku can be solved using only chains of various degrees of
complexity and singles."* Note the wording — *known*, and *chains of various degrees
of complexity*, which is not a bounded set.

**The top of the ladder is search wearing a proof coat.** A forcing chain picks a
bivalue cell, follows both branches, and concludes from the outcome — if both
branches place the same digit somewhere, it is true; if one branch contradicts, it is
false. That is exactly what `SudokuSolver.search` does. The difference is bookkeeping:
a forcing chain converts a branch's outcome into an elimination in the original grid
and never keeps a tentative state, so it yields certainty rather than a trial that
might be abandoned. The puzzle community classifies these as logic, not guessing, and
HoDoKu is explicit that none of its last-resort methods "qualify as guessing".

So the answer to "can we drop `search`?" depends on what *guess* means:

| Reading of "no guessing" | Achievable? |
|---|---|
| No abandoned branches; every deduction is a certainty when made | Yes, for any proper puzzle — but only by climbing as far as forcing chains |
| No `Field.assign` of an unproven value anywhere in the implementation | **No.** Nishio and forcing chains assign internally; they just don't keep the result |
| Solve the puzzles people actually publish without branching | Yes, comfortably, with tiers 1–5 below |

**Recommendation:** build tiers 1–5, keep `search` as the backstop, and use
`RecordingDeductionListener.guesses == 0` as the regression test that a new technique
earned its place. Do not aim for a search-free solver; aim for one where search never
fires on a real puzzle.

---

## 2. What this repo's puzzles need

Measured with a reference implementation of tiers 0–5, verified sound at every step
(no technique ever eliminated a candidate that appears in the true solution):

| Puzzle | Givens | Minimum tier that finishes it |
|---|---|---|
| `testIsSolvable` | 30 | singles |
| `compact` (parser test) | 30 | singles |
| `unsolved` | 22 | singles + locked candidates |
| `round15` | 24 | singles + locked candidates + subsets |
| `unsolved2` | 18 | stalls at 53/81 — needs chain-strength techniques |
| `unsolved3` | 17 | stalls at 59/81 — needs chain-strength techniques |

`unsolved3` has 17 givens, the proven minimum for a uniquely-solvable Sudoku. Puzzles
at that clue count essentially always require chains. Treat `unsolved2` and
`unsolved3` as the long-term targets, not the next milestone.

**Locked candidates alone would take us from one puzzle solved without guessing to
three.** That is the single highest-value processor to write next.

---

## 3. The ladder

Tier 0 is what we have. Each tier assumes the ones above it.

### Tier 0 — implemented

| Technique | Deduces |
|---|---|
| Peer elimination (`HouseCandidateEliminator`) | A value placed in a house is not a candidate elsewhere in it |
| Hidden single (`SingleCandidateMarker`) | A value fitting in only one cell of a house belongs there |
| Naked single (`SolveSingleCandidateTransformer`) | A cell with one candidate is solved |

*Worth adding for free:* **Full House / Last Digit** — a house with exactly one
unsolved cell. It is a special case of naked single, but it is the cheapest check in
the game and makes solution traces read the way a human would write them.

### Tier 1 — Locked candidates (intersections)

The best value in the whole list. Two rules, both one pass over the 27 houses.

- **Pointing (Type 1).** If in a box all candidates of digit *v* lie in one row or
  column, *v* cannot appear in that line outside the box. Eliminate it there.
- **Claiming / Box-Line Reduction (Type 2).** If in a row or column all candidates of
  *v* lie in one box, eliminate *v* from the rest of that box.

*Needs from the model:* house **intersections** — which cells a box and a line share.
`houses()` currently returns an untyped `List<List<Cell>>`, so a technique cannot ask
"is this a box?". See §4a.

### Tier 2 — Subsets

- **Naked subset of size k.** *k* cells in a house whose candidate union is exactly
  *k* values → remove those values from the house's other cells.
- **Hidden subset of size k.** *k* values in a house occurring in exactly *k* cells →
  remove every other candidate from those cells.
- **Locked pair/triple.** A naked subset whose cells lie in both a box and a line —
  eliminates in both houses at once.

*Duality worth knowing:* in a house with *n* unsolved cells, a naked *k*-subset is a
hidden *(n−k)*-subset. Implementing both directions for *k* = 2, 3, 4 therefore
covers subsets far past size 4, and is where everyone stops.

*Needs from the model:* nothing new. This is where the `Candidates` bitmask pays off —
a naked pair is two cells in a house with equal masks and `size == 2`; a naked triple
is three masks whose `or` has `bitCount == 3`. Subset enumeration is
`combinations(unsolved cells of the house, k)`, at most C(9,4) = 126 per house.

### Tier 3 — Basic fish

For a single digit *v*: pick *N* base units (all rows, or all columns) in which *v*'s
candidates lie within *N* cover units of the opposite orientation. Then *v* is
confined to the intersections, so eliminate it from the cover units outside the base.

| N | Name |
|---|---|
| 2 | X-Wing |
| 3 | Swordfish |
| 4 | Jellyfish |
| 5 | Squirmbag (rarely worth it) |

*Needs from the model:* rows and columns as two **orthogonal, typed** families. A flat
list of 27 houses cannot express "base sets from one orientation, cover sets from the
other". See §4a.

### Tier 4 — Single-digit patterns

Cheap chain-flavoured patterns that need no chain machinery. All operate on one digit
and on *conjugate pairs* (houses where *v* has exactly two candidate cells).

- **Skyscraper** — two lines where *v* has exactly two positions, sharing one
  cross-line; eliminate *v* from cells seeing both far ends.
- **2-String Kite** — a row and a column each with two positions for *v*, one from
  each in the same box; eliminate *v* from the cell seeing the other two ends.
- **Turbot Fish** — the general two-strong-link form; Skyscraper and 2-String Kite are
  its special cases, so implementing Turbot Fish subsumes both.
- **Empty Rectangle** — in a box, *v*'s candidates fit within one row plus one column;
  combine with a strong link elsewhere to eliminate.

*Needs from the model:* a **strong-link index** — for each digit, the houses in which
it has exactly two candidate positions. Building this once per pass is the first step
towards the chain machinery in tier 7.

### Tier 5 — Wings

- **XY-Wing** — pivot with candidates `{x,y}`; two pincers `{x,z}` and `{y,z}`, each
  seeing the pivot. Whatever the pivot takes, one pincer is *z*, so eliminate *z* from
  every cell seeing both pincers.
- **XYZ-Wing** — pivot `{x,y,z}`, pincers `{x,z}` and `{y,z}`; eliminate *z* from cells
  seeing all three.
- **W-Wing** — two cells with the identical pair `{x,y}` that do not see each other,
  joined by a strong link on *x*; eliminate *y* from cells seeing both.
- **WXYZ-Wing** — the four-cell generalisation.

*Needs from the model:* a **bivalue-cell index** and cheap "do these two cells see each
other?" — the latter is already there as the precomputed `PEERS` table behind
`solvedPeers`, it just needs exposing as `Field.sees(a, b)`.

> **Soundness trap.** Wings and everything below re-derive facts about cells while
> eliminating from other cells in the same pass. A reference implementation of XY-Wing
> written for this document snapshotted the bivalue cells once and then kept using that
> stale list after its own eliminations had changed them. See §4b — this is the same
> bug class that has already bitten this codebase twice.

### Tier 6 — Uniqueness

- **Unique Rectangle, types 1–6** — four cells spanning two rows, two columns and
  exactly two boxes cannot all hold the same two candidates, or the puzzle would have
  two solutions. The types differ by which extra candidates are present.
- **Hidden / Avoidable Rectangle** — the same idea over hidden candidates, and over
  already-placed values.
- **BUG+1** — if every unsolved cell has exactly two candidates except one with three,
  the extra candidate must be the answer in that cell.

> **These are not sound as general `Field` transforms.** They assume the grid has
> exactly one solution. That is true of a puzzle as given, and *false* of a grid inside
> a search branch, where a wrong assumption may have created multiple completions or
> none. If they are implemented they must be gated so they never run under `search`.
> Given tiers 1–5 cover more ground for less risk, this tier is optional.

### Tier 7 — Colouring and chains

The point at which `FieldProcessor` stops being the right shape (§4c).

- **Simple Colours** (Colour Trap / Colour Wrap) — for one digit, two-colour the graph
  of conjugate pairs. Two same-coloured cells in one house ⇒ that colour is false. Any
  cell seeing both colours loses the candidate.
- **Multi-Colours** — relations between separate colour clusters.
- **3D Medusa** — colouring over `(cell, value)` nodes across all digits at once.
- **Remote Pair** — a chain of four or more bivalue cells sharing the same pair;
  the ends are opposite, so eliminate both digits from cells seeing both ends.
- **X-Chain** — alternating strong/weak links on a single digit, starting and ending
  strong; eliminate that digit from cells seeing both endpoints.
- **XY-Chain** — a chain of bivalue cells whose ends share a digit; same elimination.
- **Nice Loop / AIC** — the general form. A discontinuous loop yields one elimination
  or placement; a continuous loop upgrades every weak link in it to strong, yielding
  many.
- **Grouped Nice Loop / AIC** — nodes may be *groups* of candidates (a box-line
  intersection) or Almost Locked Sets.

### Tier 8 — Almost Locked Sets, and exotic fish

- **ALS-XZ** — two almost-locked sets sharing a restricted common candidate.
- **ALS-XY-Wing**, **ALS Chain**, **Death Blossom** — a stem cell whose every candidate
  links into an ALS.
- **Sue de Coq** — a box-line intersection whose candidates decompose to constrain the
  cells outside it.
- **Finned / Sashimi fish** — a fish that is valid except for extra candidates (fins)
  in one base unit; eliminations restrict to cells that also see every fin.
- **Franken / Mutant fish** — base and cover sets that mix boxes with lines.

### Tier 9 — Methods of last resort

Included for completeness; all are search with a notebook.

- **Templates / Pattern Overlay** — enumerate the 46,656 placement patterns of a single
  digit and intersect with the constraints. HoDoKu is blunt: *"Templates are not meant
  for human players."*
- **Forcing Chain** — any chain leading to a contradiction or a verity.
- **Forcing Net** — the branching version; *"can be found manually only by very
  experienced players."*
- **Kraken Fish** — finned fish plus chains.
- **Bowman's Bingo**, **Nishio** — systematic trial.

This is where our `SudokuSolver.search` already sits. Anything we build in this tier
is a re-presentation of what search does, formatted as an explanation.

---

## 4. What the ladder demands of the architecture

Four changes, each forced by a specific tier. Worth making *before* the technique that
needs them, not after.

### a. Houses must be typed

```kotlin
enum class HouseKind { ROW, COLUMN, SEGMENT }
data class House(val kind: HouseKind, val index: Int, val cells: List<Cell>)
```

`Field.houses(): List<List<Cell>>` cannot express what tiers 1 and 3 need. Locked
candidates must ask "box, and which line crosses it"; fish must draw base sets from one
orientation and cover sets from the other. Add `Field.intersections()` yielding the 54
box-line pairs while you are there — it is what locked candidates iterates.

### b. Techniques must compute against a snapshot and apply eliminations atomically

**This is the recurring bug in this codebase.** It has now appeared three times:

1. `SingleCandidateMarker` decided hidden singles against a stale grouped snapshot, so
   two candidates could claim the same cell (fixed in `6d5906d`).
2. Search accepted complete-but-invalid grids because nothing compared two solved cells
   to each other (fixed in `d5cb2c2`).
3. A reference `claiming` implementation written for this document filtered its
   candidate list by "not yet solved", and its own eliminations reduced cells to
   singletons mid-pass — so a cell silently dropped out of the list and made a digit
   look confined when it was not. It removed a true candidate.

All three are the same mistake: **deriving "is this cell decided?" from mutable
candidate state while mutating it.** Every technique from tier 1 up eliminates
candidates, and every one of them is exposed to this.

The fix is structural, not vigilance:

```kotlin
data class Elimination(val at: CellPosition, val values: Candidates)

interface EliminationTechnique : FieldProcessor {
    /** Reads `field` only. Must not observe its own partial results. */
    fun eliminations(field: Field): List<Elimination>
}
```

The processor computes everything against the immutable field it was handed, then the
framework applies the whole batch. A technique physically cannot see its own partial
results, and the bug class disappears. This also makes each technique trivially
testable: assert on the returned eliminations, not on a resulting `Field`.

### c. Chains need a different abstraction entirely

From tier 7 up, the unit of reasoning is not a cell but a `(cell, value)` node, and the
structure is a graph of strong and weak links, not a grid transform. `FieldProcessor`'s
`Field -> Field` shape does not fit.

```kotlin
data class Node(val at: CellPosition, val value: Int)
class LinkGraph(field: Field) {
    fun strong(node: Node): List<Node>   // exactly-two-positions conjugate pairs
    fun weak(node: Node): List<Node>     // shares a house, so not both true
}
```

Build it once per pass and let colouring, chains and AICs all traverse it. Tier 4's
strong-link index is the first half of this, which is a good reason to build tier 4
before tier 7.

### d. The solver loop should restart at the cheapest technique

```kotlin
// current: every processor runs on every pass
processor.fold(field) { acc, p -> p.process(acc, deductions) }
```

Once the chain is a dozen techniques deep, this runs Jellyfish on every iteration even
when a naked single was available. Change it to: try processors in order, stop at the
first that changes anything, restart from the top.

Two payoffs beyond speed. It keeps expensive techniques off the hot path, and it makes
the deduction trace record *the easiest technique that was available at each step* —
which is what a difficulty rating actually is. `RecordingDeductionListener` already
collects the trace; this is what makes it meaningful.

---

## 5. Recommended order

| # | Step | Why now |
|---|---|---|
| 1 | Type the houses; add `intersections()` and `sees(a, b)` (§4a) | Prerequisite for 2 and 4 |
| 2 | **Locked candidates** (pointing + claiming) | Biggest single win: takes this repo from 1 to 3 puzzles solved without guessing |
| 3 | Introduce `Elimination` and batch application (§4b) | Do it before the technique count grows; retrofit locked candidates onto it |
| 4 | Naked + hidden subsets, sizes 2–4 | Solves `round15`; pure bitmask work, no new model concepts |
| 5 | Restart-at-cheapest in the solver loop (§4d) | Cheap, and makes the difficulty trace meaningful |
| 6 | Basic fish (X-Wing, Swordfish, Jellyfish) | First technique needing typed orientation; validates §4a |
| 7 | Turbot Fish (subsumes Skyscraper and 2-String Kite) | Builds the strong-link index that tier 7 will reuse |
| 8 | XY-Wing, then XYZ-Wing and W-Wing | High hit rate on published "expert" puzzles |
| 9 | `LinkGraph`, then Simple Colours and X-Chain (§4c) | Only worth it if `unsolved2` / `unsolved3` matter |

Stop after step 8 unless the two 17–18-clue puzzles are the goal. Everything past it
costs more than it returns for puzzles anyone actually publishes.

## 6. How to know a technique earned its place

Every technique gets a `Technique` enum constant and reports through
`DeductionListener`, so coverage is directly testable:

```kotlin
val listener = RecordingDeductionListener()
SudokuSolver(listener).solve(puzzle)
assertThat(listener.guesses).isZero()          // solved by logic alone
assertThat(listener.deductions.map { it.technique }).contains(Technique.POINTING)
```

A technique that does not move some puzzle from `guesses > 0` to `guesses == 0`, or
does not shorten an existing trace, has not paid for itself. Add the puzzle to the
suite with the assertion at the same time as the processor.

---

## Sources

- [HoDoKu — Human Style Solving Techniques](https://hodoku.sourceforge.net/en/techniques.php) — the most complete taxonomy available; 70+ techniques
- [HoDoKu — Intersections (Locked Candidates)](https://hodoku.sourceforge.net/en/tech_intersections.php)
- [HoDoKu — Chains and Loops](https://hodoku.sourceforge.net/en/tech_chains.php) — source of the "chains + singles solve every known sudoku" claim
- [HoDoKu — Methods of Last Resort](https://hodoku.sourceforge.net/en/tech_last.php) — Templates, Forcing Chain/Net, Kraken, Brute Force
- [SudokuWiki — Strategy list](https://www.sudokuwiki.org/sudoku.htm) — 42 strategies grouped Basic / Tough / Diabolical / Extreme
- [Sudopedia — Solving Technique](https://www.sudopedia.org/wiki/Solving_Technique) — category index
- [Sudoku A Day — Strategies by difficulty](https://sudokuaday.com/sudoku-strategies) — which techniques each difficulty band needs
