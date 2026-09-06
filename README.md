# Sudoku

A Sudoku solver that shows its work: constraint propagation over fourteen human solving
techniques, with backtracking search underneath. The website lets you enter a puzzle and
watch it solved step by step, each step named after the technique that found it.

Three Gradle subprojects:

- `lib` — the solver itself, a plain Kotlin/JVM library.
- `api` — a Spring Boot app with one endpoint, `POST /solve`.
- `web` — the site: static assets only, packaged onto the classpath so `api` serves them.

## Running the API and the website

One process serves both. Start it with:

```bash
./gradlew :api:bootRun
```

then open <http://localhost:8080/>. The port is Spring's default 8080; there is no
custom port configuration. The site is served from the classpath, so after editing files
under `web/src/main/resources/static/` restart `bootRun` to pick the changes up.

To run from a jar instead:

```bash
./gradlew :api:bootJar
java -jar api/build/libs/api.jar
```

## The API

`POST /solve` takes the 81-character compact puzzle format (digits for givens, `.` for
empty cells):

```bash
curl -s http://localhost:8080/solve \
  -H 'Content-Type: application/json' \
  -d '{"puzzle": "52...6.........7.13...........4..8..6......5...........418.........3..2...87.....", "allowGuessing": true}'
```

The response carries the solution and the step-by-step reasoning that reaches it. With
`allowGuessing: false` the solver only reports what the techniques can honestly deduce
and stalls rather than guess.

## Build and test

```bash
./gradlew build        # compile + test everything
./gradlew :lib:test    # solver tests only
```

Requires nothing installed beyond a JDK the toolchain resolver can find — the build
targets Java 21 and downloads it if needed.

`docs/solving-techniques.md` is a standalone reference for every technique the solver
implements, with worked examples.
