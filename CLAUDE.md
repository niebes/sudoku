# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & test

Gradle wrapper, single subproject `lib` (Kotlin/JVM 2.1.20, Java toolchain 21, JUnit 5 + AssertJ).

```bash
./gradlew build                 # compile + test
./gradlew :lib:test             # tests only
./gradlew :lib:test --tests 'net.niebes.sudoku.technique.XyWingEliminatorTest'      # one class
./gradlew :lib:test --tests 'net.niebes.sudoku.SudokuSolverTest.round15'            # one test
```

**Read results from `lib/build/test-results/test/TEST-*.xml`, not the HTML report.** It is JUnit
XML: `<system-out>` holds the test's stdout and `<failure>` its message. Gradle's failure line links
to the HTML, which is far more painful to parse.

There is no lint/format task and no CI configuration. Configuration cache, parallel builds and the
build cache are all enabled in `gradle.properties`, so a stale `.gradle/configuration-cache` is a
likely suspect for odd build behaviour.

## Layout

One type per file. Packages, and the direction dependencies run:

```
net.niebes.sudoku            SudokuSolver, SolveResult and its three cases
net.niebes.sudoku.model      Field, Cell, Candidates, CellPosition, House, Intersection, ConjugatePair
net.niebes.sudoku.deduction  Technique, Deduction (Placement / Elimination), the listeners
net.niebes.sudoku.technique  FieldProcessor, EliminationTechnique and fourteen techniques
net.niebes.sudoku.io         parsers and writers
```

`Technique` lives with the deductions, not with the implementations, so techniques depend on
deductions and not the reverse. Tests mirror the package they cover.

## Architecture

A Sudoku solver: constraint propagation with backtracking search underneath it. There is no `main` —
the library is exercised through its tests.

**Immutable model.** `Cell` is a sealed interface of `SolvedCell` (a `value`) and `UnsolvedCell` (its
remaining `Candidates`); both are data classes, and that generated equality is load-bearing — it
terminates the propagation loop and backs the test assertions. `Candidates` is an inline value class
over a nine-bit mask, so elimination is one AND and set algebra between cells is one instruction.

`Field` stores 81 cells row-major, indexed by `CellPosition.index` (`row * 9 + column`), and
normalises whatever order its constructor is handed so equality stays canonical. Houses, the 20 peers
of each position and the 54 segment-line overlaps are identical for every field, so they are computed
once in the companion.

**A house is the unit of reasoning**, and `House` carries its `HouseKind` because techniques above
singles need to tell them apart — locked candidates relates a segment to a line, fish relate rows to
columns. The queries techniques are built from, all on `Field`:

| | |
|---|---|
| `houses()` / `housesOf(position)` | the 27 groups; `House.holds(v)`, `candidatesFor(v)` |
| `intersections()` | the 54 segment-line overlaps, split into shared / segment-only / line-only |
| `conjugatePairs(v)` | houses where `v` has exactly two homes — a strong link |
| `sees(a, b)` / `seenByBoth(a, b)` | shared-house test, and where a "one of these two" proof eliminates |

**Solver** (`SudokuSolver`), two layers:

- `propagate` runs the chain until the field is solved, contradicts itself, or stops changing. Each
  step applies the **first** technique that changes anything, then restarts from the top, so an
  expensive technique is reached only once every cheaper one is stuck. That is both why solving is
  fast and why the deduction trace is a difficulty rating — a technique appears in it only when
  nothing cheaper was available. The chain runs levels 1–8 of `docs/solving-techniques.md`, cheapest
  first: full house, peer elimination, naked then hidden singles, pointing, claiming, naked and
  hidden subsets, basic fish, turbot fish, XY/XYZ/W-wings, empty rectangle, unique rectangle,
  BUG+1, AIC. Only the singles and full house place values; everything below exists
  to create work for them.

  Two of those reason from the **puzzle** rather than the grid: unique rectangle and BUG+1 assume
  the input has exactly one solution. That survives search — assigning a value can only reduce how
  many solutions remain — but not an improper puzzle, and nothing checks properness. Drop them from
  the chain if the solver must tolerate one.
- `search` propagates, then branches on the unsolved cell with the fewest candidates (MRV). Because
  the model is immutable an assumption is just another field, so a wrong branch needs no rollback.

**Contradiction is a value, not an exception** — `SolveResult` is `Solved` / `Stalled` /
`Contradiction`, because search hits contradictions constantly on its hot path.

**No I/O in the domain.** Techniques report a typed `Deduction` to a `DeductionListener` the caller
supplies (`IGNORE` — the default — plus `Printing…` and `Recording…`).

**Parsing.** `DelimitedFieldParser` holds the row/column bookkeeping; `CsvFieldParser` and
`PipeFieldParser` differ only in delimiter and cell tokenizer, and `CompactFieldParser` reads the
81-character format published puzzle sets use. Parsers reject the wrong number of rows, short rows,
and givens that already conflict. That validation lives here rather than in `Field`'s constructor,
because propagation legitimately passes through inconsistent intermediate states that search handles
as ordinary values.

## Adding a technique

`docs/solving-techniques.md` is a standalone levelled reference for every non-brute-force technique,
with worked examples, written independently of this codebase. Read it first. Levels 1–8 are implemented apart
from ALS, Sue de Coq and the exotic fish; level 9 is what `search` already does. Simple colouring
and finned fish are implemented and tested but deliberately **not** in the
chain. Turbot fish covers short strong-link chains two levels before colouring; AIC at nine links
covers everything finned fish finds. Both were measured in and out — neither changes how many
puzzles need search.

Implement `EliminationTechnique` and insert it into the default chain in cost order. `eliminations(field)`
must read only the field it is handed; the batch is applied afterwards, so a technique cannot observe
its own partial results. Implement `FieldProcessor` directly only for a technique that *places*
values, as `FullHouseSolver` does.

**Three rules, each learned here the hard way and each with a test named after the grid that catches it:**

1. **Ask `House.holds(value)` whether a value is already placed** rather than inferring it from
   candidates. A cell can still carry a candidate peer elimination has not caught up with, and
   reading it makes a value look confined to a region where it is in fact settled elsewhere — locked
   candidates then concludes the exact opposite of the truth.
2. **Never derive "is this cell decided?" from candidate-set size while eliminating.** That is what
   the batch application in `EliminationTechnique` exists to make impossible.
3. **Take pairs of anything unordered.** Turbot fish reported every elimination twice because it
   iterated ordered pairs of strong links; trying all end-combinations of one ordering already covers
   the reverse.

Two traps in existing code worth knowing before you change them:

- `Field.contradictionAt()` reports **both** a cell with no candidates and two solved cells colliding
  in a house. The second is not redundant: propagation is incomplete, so `SingleCandidateMarker` can
  narrow two cells to the same value through different house types, and without that check search
  accepts complete-but-invalid grids.
- `SingleCandidateMarker` re-reads cell state on every lookup rather than working from its grouped
  snapshot, for the same reason rule 2 exists.

## Tests

`FieldBuilder` (`field { candidates(row, column, …); solved(row, column, v) }`) builds a grid with a
specific candidate layout rather than a puzzle — zero-based, every cell starting with all nine
candidates. Use it for the rule, and the puzzle corpus for everything else.

Three corpora, all with verified unique solutions: `Puzzles` (six named, hand-picked), `GeneratedPuzzles`
(26 random, 24–32 clues), `HardPuzzles` (34 dug to their minimum clue count). Every technique gets:

- a **soundness** test — run it alone over all three corpora and assert it never removes a value the
  solution needs. This is the important one. These techniques do not crash when they are wrong; they
  make a plausible elimination that is false, and it surfaces much later as a wrong answer.
- an **earns-its-place** test — it is the cheapest available technique on at least one corpus puzzle.
  Do not pin this to a named puzzle: whether a technique is reached depends on what the cheaper ones
  leave behind, so it breaks whenever something above it improves.

If a new technique fires nowhere, that is usually the corpus being too easy rather than the technique
being useless — `HardPuzzles` exists because XYZ-wing and empty rectangle fired on nothing until it did.
