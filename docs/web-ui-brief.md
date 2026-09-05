# Brief: an educational Sudoku solving website

Paste this into a fresh session working in this repository. It assumes no prior context.

---

## What to build

Two new Gradle modules alongside the existing `lib`:

- **`api`** — an HTTP service wrapping the existing solver. Takes a puzzle, returns the solution
  **and the full reasoning trace**: which technique fired, where, why, and what the grid looked like
  at that moment.
- **`web`** — the site people use. Enter a puzzle, watch it being solved one deduction at a time,
  and learn the technique behind each step.

The point is not the answer. The point is teaching how the answer is reached. A user should finish a
puzzle understanding what a *hidden single* or a *pointing pair* is, because they watched one work.

## What already exists

`lib` is a complete, well-tested Sudoku solver: constraint propagation with 20 techniques, backed by
backtracking search. Read `CLAUDE.md` first — it covers the architecture, the package layout and the
two rules every technique follows. `docs/solving-techniques.md` is a standalone levelled reference
for every technique with worked examples; **it is your teaching content**, written to be read by
someone learning Sudoku rather than by someone reading this code.

Build and test:

```bash
./gradlew build
./gradlew :lib:test --tests 'net.niebes.sudoku.SudokuSolverTest.round15'
```

Read results from `lib/build/test-results/test/TEST-*.xml` (JUnit XML: `<system-out>`, `<failure>`),
not the HTML report Gradle links to.

### The API you will call

```kotlin
val puzzle: Field = CompactFieldParser().parse("53..7....6..195....98....6.8...")   // 81 chars, any non-digit = blank
                    // also CsvFieldParser, PipeFieldParser

val trace = RecordingDeductionListener()
when (val result = SudokuSolver(trace).solve(puzzle)) {
    is Solved        -> result.field          // every cell has a value
    is Stalled       -> result.field          // partially solved; no technique can go further
    is Contradiction -> result.at             // the position that proves it cannot be completed
}

trace.deductions      // List<Deduction>, in the order they were made
trace.placements      // Placement(technique, at, value)
trace.eliminations    // Elimination(technique, at, values: Candidates)
trace.guesses         // how many assumptions search had to make
trace.techniquesUsed()
```

`Technique` is an enum of 20 constants, ordered cheapest first — `FULL_HOUSE`, `NAKED_SINGLE`,
`HIDDEN_SINGLE`, `PEER_ELIMINATION`, `POINTING`, `CLAIMING`, `NAKED_SUBSET`, `HIDDEN_SUBSET`,
`BASIC_FISH`, `TURBOT_FISH`, `XY_WING`, `XYZ_WING`, `W_WING`, `EMPTY_RECTANGLE`, `UNIQUE_RECTANGLE`,
`BUG_PLUS_ONE`, `AIC`, plus `GUESS` and a few implemented but not in the default chain.

`SudokuSolver` also exposes `propagate(field)`, which stops rather than guessing, and `processors`,
the ordered chain. `SolutionWriter().render(field)` gives a text grid. `Field.cells` is 81 cells in
row-major order; each is `SolvedCell(position, value)` or `UnsolvedCell(position, candidates)`, and
`Candidates.values` gives the remaining digits as a `Set<Int>`.

---

## Three problems you must solve

These are the real work. Everything else is plumbing.

### 1. The trace has no grid snapshots

`RecordingDeductionListener` records *what was deduced*, never *what the grid looked like*. A
step-through UI needs the state at each step. Two options:

- **Replay** — start from the givens and apply deductions in order, rebuilding each state. Cheap and
  needs no library change, but see problem 2.
- **Snapshot** — extend the listener to capture the field with each deduction. Simple, and honest
  about what it costs: a few hundred grids per puzzle.

Traces run 186–375 deductions for a typical puzzle, roughly a third placements and two thirds
eliminations. Sending 300 full grids is a few hundred KB; sending the initial grid plus per-step
deltas is a few KB. Either is fine — decide deliberately.

### 2. Abandoned search branches are in the trace

When a puzzle needs search, the listener records deductions from branches that were later thrown
away. It says so, and it is not a bug — but for teaching it is poison. On one corpus puzzle, **39 of
the recorded placements contradict the real solution**. Replay them and you will confidently teach a
student a wrong move.

You must handle this. Options, roughly in order of honesty:

- Show only puzzles the solver finishes without guessing — `propagate` returns `Solved` — and tell
  the user plainly when a puzzle is beyond the technique chain. About 50 of the 66 corpus puzzles
  qualify (see `Puzzles`, `GeneratedPuzzles`, `HardPuzzles` in the test sources).
- Filter the trace to the successful line. The library does not currently mark which deductions
  survived, so this needs a library change.
- Present guesses honestly as guesses: "no technique applies here, so the solver assumed a value" is
  itself an educational moment, and the doc has a section on exactly that boundary.

### 3. Deductions record the conclusion, not the reason

`Placement(HIDDEN_SINGLE, r0c2, 4)` says *what*. Teaching needs *why*: "in box 1, 4 can only go in
r0c2, because r0c0 and r1c1 already see every other cell that could take it."

The library does not record supporting cells. This is the most important design decision in the
project, and it is a **library change** — weigh it with the user before starting:

- **Extend `Deduction` with the cells that justify it** (`because: List<CellPosition>`, or something
  richer). The honest place for it, and it makes explanations exact. It touches
  `EliminationTechnique.process` and all 20 techniques, and every technique has tests that will need
  updating.
- **Re-derive in the `api` module** — given a grid and a deduction, work out which house and cells
  produced it. No library change, but it duplicates each technique's logic and will drift.
- **Generic explanations per technique**, from `docs/solving-techniques.md`. Cheapest, and much
  weaker: the student is told what the technique *is*, not what it *did here*.

---

## Suggested shape

Take these as defaults, not requirements; propose something better if you see it.

**`api`** — Ktor with `kotlinx.serialization`, depending on `lib`. One endpoint is enough:

```
POST /solve   { "puzzle": "53..7....", "allowGuessing": false }
->  { "outcome": "solved" | "stalled" | "invalid",
      "givens": "...", "solution": "...",
      "steps": [ { "technique": "HIDDEN_SINGLE", "kind": "placement",
                   "at": {"row":0,"column":2}, "value": 4,
                   "because": [...], "explanation": "...", "grid": ... } ] }
```

Reject bad input at the edge — the parsers already throw `IllegalArgumentException` with a usable
message for the wrong number of rows, short rows and givens that already conflict. Do not let that
become a 500.

**`web`** — the input grid, the step-through player (back/forward/play, jump to a technique), and a
panel explaining the current technique with a worked example drawn from
`docs/solving-techniques.md`. Highlight the cells involved: the ones that justify the deduction and
the ones it changes, distinctly. Static assets served by `api` are fine; Kotlin/JS is fine if you
want shared types. Do not reach for a framework the project does not need.

Wire both into `settings.gradle.kts` and put dependencies in `gradle/libs.versions.toml` — the
version catalog is the convention here and currently holds only AssertJ and the Kotlin plugin.

## How to work

The repository has strong conventions. Follow them.

- **One type per file.** Packages mirror purpose; tests mirror the package they cover.
- **Test first.** Every technique in `lib` was built by writing a failing test, watching it fail,
  then implementing. Do the same.
- **One commit per coherent unit**, with a message explaining *why*, not what changed.
- **Measure before believing.** This project has three techniques that are implemented, sound and
  tested, and earn no place in the solver because measurement said so — and two detectors that were
  confidently wrong until checked against a case where they had to fire. If you claim the UI is
  fast, or that a step count is small, measure it.
- Do not weaken a test to make it pass. If a test breaks because reality moved, say so and change
  what it asserts deliberately.

## Done looks like

- `./gradlew build` green across all three modules.
- A puzzle can be entered, solved, and stepped through deduction by deduction, with the grid
  updating and the reasoning shown.
- A puzzle the technique chain cannot finish is handled honestly rather than silently mis-taught.
- Someone who has never heard of a *pointing pair* can watch one and understand it.
