# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & test

Gradle wrapper, single subproject `lib` (Kotlin/JVM 2.1.20, Java toolchain 21, JUnit 5 + AssertJ).

```bash
./gradlew build                 # compile + test
./gradlew :lib:test             # tests only
./gradlew :lib:test --tests 'net.niebes.sudoku.FieldWriterTest.getCluster'   # single test
./gradlew :lib:test --info      # see the solver's stdout trace (see below)
```

There is no lint/format task and no CI configuration. Configuration cache, parallel builds and the build cache are all enabled in `gradle.properties`, so a stale `.gradle/configuration-cache` is a likely suspect for odd build behaviour.

## Architecture

A constraint-propagation Sudoku solver. There is no `main` — the library is exercised entirely through `lib/src/test/.../FieldWriterTest.kt`.

**Immutable model** (`net.niebes.sudoku.model`): a `Field` is a `Set<Cell>`; `Cell` is a sealed class of `SolvedCell` (has a `value`) and `UnsolvedCell` (has `Candidates`, defaulting to 1..9). Nothing mutates — every transformation rebuilds the whole `Field`. Both cell subclasses hand-roll `equals`/`hashCode` over position + value/candidates; that equality is load-bearing in two places: it terminates the solver loop and it backs the `assertThat(output).isEqualTo(expectedSolution)` test assertions. Changing it changes both.

`Field.init` requires exactly 9 distinct rows and 9 distinct columns spanning 0..8, so malformed puzzle input fails at parse time with `IllegalArgumentException`, not later in the solver.

`CellPosition.segment` derives the 3x3 box from `row/3, column/3`; `Field` exposes `getRow` / `getColumn` / `getSegment` as the three constraint groups every eliminator works over.

**Solver pipeline** (`SudokuSolver`): holds an ordered `List<FieldProcessor>` and folds the field through all of them repeatedly until an iteration produces an equal `Field` (fixpoint), then returns. The default chain is:

1. `RowCandidateEliminator`, `ColumnCandidateEliminator`, `SegmentCandidateEliminator` — strip solved values from the candidates of unsolved cells in the same group. All three implement `UnsolvedCellFieldProcessor`, which supplies the "map over cells, pass `SolvedCell` through untouched" boilerplate.
2. `SingleCandidateMarker` — hidden singles: within a row/column/segment, a candidate appearing in exactly one cell narrows that cell to it.
3. `SolveSingleCandidateTransformer` — naked singles: a one-candidate cell becomes a `SolvedCell`; a zero-candidate cell throws `IllegalStateException("sudoku unsolvable")`.

Adding a solving technique means implementing `FieldProcessor` (or `UnsolvedCellFieldProcessor`) and inserting it into the default list in `SudokuSolver`'s secondary constructor. Order matters: eliminators must run before the two that commit to values.

**Known limitation**: the solver only does constraint propagation — no backtracking or guessing. Puzzles needing it stall at the fixpoint and return a partially-solved `Field`. Three tests (`unsolved`, `unsolved3`, `round15`) are `@Disabled` for exactly this reason; treat them as the target for any new technique rather than as broken tests.

**I/O**: `CsvFieldParser` (comma-separated, blank cell = unknown) and `PipeFieldParser` (pipe-separated; a cell may carry a comma-separated candidate list, and a single candidate collapses straight to a `SolvedCell`). `SolutionWriter` prints solved cells as digits and unsolved ones as `{1,2,3}`. The solver and `SingleCandidateMarker` also `println` their deductions as they go — that trace is the primary debugging tool here.
