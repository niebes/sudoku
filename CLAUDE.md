# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & test

Gradle wrapper, single subproject `lib` (Kotlin/JVM 2.1.20, Java toolchain 21, JUnit 5 + AssertJ).

```bash
./gradlew build                 # compile + test
./gradlew :lib:test             # tests only
./gradlew :lib:test --tests 'net.niebes.sudoku.SudokuSolverTest.round15'   # single test
```

There is no lint/format task and no CI configuration. Configuration cache, parallel builds and the build cache are all enabled in `gradle.properties`, so a stale `.gradle/configuration-cache` is a likely suspect for odd build behaviour.

## Layout

One type per file. Packages, and the direction dependencies run:

```
net.niebes.sudoku            SudokuSolver, SolveResult and its three cases
net.niebes.sudoku.model      Field, Cell, Candidates, CellPosition, House, Intersection
net.niebes.sudoku.deduction  Technique, Deduction (Placement / Elimination), the listeners
net.niebes.sudoku.technique  FieldProcessor, EliminationTechnique and the eight techniques
net.niebes.sudoku.io         parsers and writers
```

`Technique` lives with the deductions, not with the implementations, so techniques depend on
deductions and not the reverse. Tests mirror the package they cover.

## Architecture

A Sudoku solver: constraint propagation with backtracking search on top. There is no `main` — the library is exercised through its tests.

**Immutable model** (`net.niebes.sudoku.model`). `Cell` is a sealed interface of `SolvedCell` (a `value`) and `UnsolvedCell` (its remaining `Candidates`); both are data classes, and that generated equality is load-bearing — it terminates the propagation loop and backs the test assertions. `Candidates` is an inline value class over a nine-bit mask, so elimination is one AND and set algebra between cells is one instruction; `values` is a derived view for printing.

`Field` stores 81 cells row-major, indexed by `CellPosition.index` (`row * 9 + column`), and normalises whatever order its constructor is handed so equality stays canonical. The 27 houses and the 20 peers of each position are identical for every field, so they are computed once in the companion — that is why `solvedPeers` is a 20-element walk rather than a scan.

**A house is the unit of reasoning.** Row, column and segment are one constraint, so techniques are written against `Field.houses()` / `housesOf(position)` rather than against three near-identical accessors. Any technique added next (naked pairs, pointing pairs, box-line reduction, X-wing) should be phrased the same way.

**Solver** (`SudokuSolver`). Two layers:

- `propagate` folds the field through an ordered `List<FieldProcessor>` until it solves the field, contradicts itself, or stops changing. Each step applies the **first** technique that changes
anything, not all of them - `propagate` then restarts from the top, so an expensive technique is
reached only once every cheaper one is stuck. That is what makes the deduction trace a difficulty
rating: a technique appears in it only when nothing cheaper was available. The default chain runs levels 1-5 of `docs/solving-techniques.md`, cheapest first: full house, peer elimination, hidden and naked singles, pointing, claiming, naked and hidden subsets, basic fish, turbot fish, XY/XYZ/W-wings, empty rectangle. Only the singles and full house place values; everything else exists to create work for them.
- `search` propagates, then branches on the unsolved cell with the fewest candidates (MRV), recursing. Because the model is immutable an assumption is just another field, so a wrong branch needs no rollback.

Adding a technique means implementing `EliminationTechnique` and inserting it into the default chain. `eliminations(field)` must read only the field it is handed - the batch is applied afterwards, so a technique cannot observe its own partial results. Implement `FieldProcessor` directly only for a technique that *places* values, as `FullHouseSolver` does.

**Two rules every technique above singles follows**, both learned the hard way here, each with a test named after the grid that catches it:

1. **Ask `House.holds(value)` whether a value is already placed**, rather than inferring it from candidates. A cell can still carry a candidate peer elimination has not caught up with, and reading it makes a value look confined to a region where it is in fact settled elsewhere - locked candidates then concludes the exact opposite of the truth.
2. **Never derive "is this cell decided?" from candidate-set size while eliminating.** That is what the batch application in `EliminationTechnique` exists to make impossible.

**Contradiction is a value, not an exception** — `SolveResult` is `Solved` / `Stalled` / `Contradiction`, because search hits contradictions constantly on its hot path. Two traps behind this:

- `Field.contradictionAt()` reports **both** a cell with no candidates and two solved cells colliding in a house. The second is not redundant: propagation is incomplete, so `SingleCandidateMarker` can narrow two cells to the same value through different house types, and without that check search accepts complete-but-invalid grids.
- `SingleCandidateMarker` re-reads cell state on every lookup rather than working from the grouped snapshot. Narrowing one cell changes which candidates are still hidden singles; deciding against a stale snapshot lets two candidates claim the same cell.

**No I/O in the domain.** Techniques report a typed `Deduction` to a `DeductionListener` the caller supplies (`IGNORE`, `Printing…`, `Recording…`). Guesses are recorded as a technique, so `RecordingDeductionListener.guesses` is a difficulty signal: how far a puzzle outruns the chain.

**Which techniques to add next** — `docs/solving-techniques.md` is a standalone,
levelled reference for every non-brute-force Sudoku technique, with worked examples.
It is written independently of this codebase; read it before adding a processor.

**Parsing** (`FieldParser`). `DelimitedFieldParser` holds the row/column bookkeeping; `CsvFieldParser` and `PipeFieldParser` differ only in delimiter and cell tokenizer (pipe cells may carry a candidate list), and `CompactFieldParser` reads the 81-character format published puzzle sets use. Parsers reject the wrong number of rows, short rows, and givens that already conflict — that validation lives here rather than in `Field`'s constructor, because propagation legitimately passes through inconsistent intermediate states that search handles as ordinary values.
