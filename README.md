# Sudoku

A Sudoku solver that shows its work: constraint propagation over fourteen human solving
techniques, with backtracking search underneath. The website lets you enter a puzzle and
watch it solved step by step, each step named after the technique that found it.

The solver runs entirely in the browser — the Kotlin library is compiled to JavaScript,
so the site is static files with no backend. Two Gradle subprojects:

- `lib` — the solver, a Kotlin Multiplatform module. The JVM target carries the test
  suite; the JS target compiles the same code into the script the site ships.
- `web` — the site: HTML, CSS and the player, joined at build time by the compiled solver.

## Running the website

```bash
./gradlew :web:site
```

lays the finished site out in `web/build/site/`. Open its `index.html` straight from the
file system — the site is plain scripts, so `file://` works — or serve the directory with
any static file server:

```bash
python3 -m http.server --directory web/build/site
```

The site deploys itself: every push to `main` builds and publishes it to GitHub Pages at
<https://niebes.github.io/sudoku/>. For any other host, copy `web/build/site/` there.

## Using the solver from the page

`solver.js` registers a `sudokuSolver` global. Puzzles use the 81-character compact
format (digits for givens, `.` for empty cells):

```js
const result = sudokuSolver.solve(
  '52...6.........7.13...........4..8..6......5...........418.........3..2...87.....',
  true // allowGuessing: false stops at what the techniques can honestly deduce
);
result.outcome;      // 'solved' | 'stalled' | 'invalid'
result.solution;     // the completed grid, compact format
result.steps;        // the reasoning, one technique-named step at a time
```

## Build and test

```bash
./gradlew build             # compile all targets, run the tests, assemble the site
./gradlew :lib:jvmTest      # solver tests only
```

Requires nothing installed beyond a JDK the toolchain resolver can find — the build
targets Java 21 and downloads it (and the Node.js the JS target needs) if necessary.

`docs/solving-techniques.md` is a standalone reference for every technique the solver
implements, with worked examples.
